package fr.rooobert.energy.rooobot;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import fr.rooobert.JarClassLoader;

/** Class to manage plugins */
public class PluginManager implements Iterable<Plugin> {
	// --- Constants
	private static final Logger logger = LogManager.getLogger(PluginManager.class);

	// --- Attributes
	protected static PluginManager instance = null;

	private final File directory;
	private final ArrayList<Plugin> plugins = new ArrayList<>();

	// --- Methods
	protected PluginManager(Properties props) throws Exception {
		this.directory = new File(props.getProperty("plugins.directory", "plugins"));
		if (!this.directory.isDirectory() || !this.directory.canRead()) {
			throw new Exception("Plugins path must be a readable directory : " + directory.getPath());
		}
	}

	/** @return PluginManager */
	public static PluginManager getInstance() {
		return instance;
	}

	/** Loads all plugins
	 * @return Number of plugins loaded */
	public synchronized int loadAllPlugins() {
		int plugins = 0;

		//
		logger.debug("Loading all plugins...");
		final String extension = ".properties";
		File[] files = this.directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(extension);
			}
		});
		for (File file : files) {
			logger.debug("Found " + file.getName() + "...");

			String pluginName = file.getName().substring(0, file.getName().length() - extension.length());
			Plugin plugin = this.getOrLoadPlugin(pluginName);
			if (plugin != null) {
				this.registerPlugin(plugin);
				plugins++;
			} else {
				logger.warn("Error loading plugin : " + pluginName);
			}
		}

		return plugins;
	}

	/** Adds a plugin to the list of installed plugins
	 * @param plugin */
	public synchronized void registerPlugin(Plugin plugin) {
		String name = plugin.getName();

		Plugin p = this.getPlugin(name);
		if (p == null) {
			logger.info("Registering plugin " + name);
			this.plugins.add(plugin);
		} else {
			logger.warn("Plugin already registered : " + name);
		}
	}

	/** Returns a plugin with the provided name, and tries to load
	 * it if it is not already done. 
	 * @param name Name of the plugin
	 * @return The plugin with the provided <code>name</code> or <code>null</code>
	 * if no plugin with that name is available */
	public synchronized Plugin getOrLoadPlugin(String name) {
		Plugin plugin = this.getPlugin(name);
		if (plugin == null) {
			plugin = this.loadPlugin(name);
		}
		return plugin;
	}

	/** @param name Name of the plugin
	 * @return a plugin already loaded */
	public synchronized Plugin getPlugin(String name) {
		for (Plugin p : this.plugins) {
			if (p.getName().equalsIgnoreCase(name)) {
				return p;
			}
		}
		return null;
	}

	private synchronized Plugin loadPlugin(String name) {
		Plugin plugin = null;
		
		// Attention a ne pas charger/executer n'importe quel JAR !
		
		// Try to load the configuration file
		File propsFile = new File(this.directory, new File(name + ".properties").getName());
		Properties props = new Properties();
		try (FileReader reader = new FileReader(propsFile)) {
			props.load(reader);
		} catch (IOException e) {
			logger.warn("Failure loading plugin configuration file : " + propsFile.getPath(), e);
			props = null;
		}

		if (props != null) {
			// Construct plugin info
			String className = props.getProperty("plugin.class", null);
			boolean nativePlugin = Boolean.parseBoolean(props.getProperty("plugin.native", "true"));

			// Load plugin
			if (className != null) {
				// Path to the jar file
				Class<?> clazz = null;
				
				// TODO Lire le JAR a charger depuis le fichier de config !
				File jarFile = new File(this.directory, new File(name + ".jar").getName());
				if (!nativePlugin) {
					logger.info("Loading JAR file : " + jarFile.getName());
					if (jarFile.isFile() && jarFile.canRead()) {
						try (JarClassLoader jarClassLoader = new JarClassLoader(jarFile.toURI().toURL())) {
							logger.info("Loading class " + className + " from JAR file...");
							clazz = jarClassLoader.loadClass(className);
						} catch (IOException e) {
							logger.error("Error loading jar file " + jarFile.getPath() + " : ", e);
						} catch (ClassNotFoundException e) {
							logger.error("Class not found : " + e.getMessage(), e);
						}
					} else {
						logger.error("Expected JAR not found : " + jarFile.getPath());
					}
				} else {
					// Development mode
					logger.info("Native plugin (or development mode). JAR NOT loaded : " + jarFile.getPath());
					try {
						clazz = Class.forName(className);
					} catch (ClassNotFoundException e) {
						logger.error("Class not found : " + e.getMessage(), e);
					}
				}

				if (clazz != null) {
					try {
						// Check this is the right subclass
						if (Plugin.class.isAssignableFrom(clazz)) {
							@SuppressWarnings("unchecked")
							Class<? extends Plugin> clazzPlugin = (Class<? extends Plugin>) clazz;
							Constructor<? extends Plugin> constructor = clazzPlugin.getConstructor(String.class, Properties.class);
							
							logger.debug("Creating a new " + clazzPlugin.getCanonicalName() + "...");
							plugin = constructor.newInstance(name, props);
						} else {
							logger.error("Class must be a subclass of Thread : " + clazz.getCanonicalName());
						}
					} catch (NoSuchMethodException e) {
						logger.error("Class must have a public constructor with one parameter : " + className, e);
					} catch (InstantiationException | InvocationTargetException e) {
						logger.error("Error instanciating plugin class", e.getCause());
					} catch (Exception e) {
						logger.error("Error instanciating plugin class", e);
					}
				}
			} else {
				logger.error("Plugin class not defined in configuration file " + propsFile.getPath());
			}
		}
		
		return plugin;
	}

	/** Disables and unload a plugin from memory
	 * @param plugin */
	public synchronized void unloadPlugin(Plugin plugin) {
		// Disable plugin
		if (plugin.isEnabled()) {
			try {
				plugin.disable();
			} catch (Exception e) {
				logger.error("Error disabling plugin " + plugin.getName() + " : " + e.getMessage(), e);
			}
		}

		// Deregister plugin
		Iterator<Plugin> it = this.plugins.iterator();
		while (it.hasNext()) {
			if (it.next() == plugin) {
				it.remove();
				logger.debug("Plugin unregistered : " + plugin.getName());
			}
		}

		// Collect garbage
		System.gc();
	}

	@Override
	public Iterator<Plugin> iterator() {
		return this.plugins.iterator();
	}
}
