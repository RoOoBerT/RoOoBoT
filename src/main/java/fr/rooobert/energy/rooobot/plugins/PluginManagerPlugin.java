package fr.rooobert.energy.rooobot.plugins;

import java.util.Properties;
import java.util.regex.Matcher;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import fr.rooobert.energy.rooobot.Plugin;
import fr.rooobert.energy.rooobot.PluginManager;

// XXX Envisager une interface Web pour la gestion des plugins
/** Native plugin that enables dynamic management of plugins via IRC commands. */
public class PluginManagerPlugin extends Plugin {
	// --- Constants
	private static final Logger logger = LogManager.getLogger(PluginManagerPlugin.class);
	
	// --- Attributes
	
	// --- Methods
	public PluginManagerPlugin(String name, Properties props) {
		super(name, props);
	}
	
	@Override
	public void onCommand(String channel, String sender, String login, String hostname, String command) {
		String cmd = "";
		String pluginName = null;
		
		Matcher matcher = COMMAND_ARGUMENT.matcher(command);
		if (matcher.find()) {
			cmd = matcher.group();
			pluginName = (matcher.find() ? matcher.group() : null);
		}
		
		switch (cmd) {
		case "load": // Load a plugin
			this.doSetPluginLoaded(channel, pluginName, true);
			break;
		case "unload": // Unload a plugin
			this.doSetPluginLoaded(channel, pluginName, false);
			break;
		case "reload": // Reload a plugin
			this.doSetPluginLoaded(channel, pluginName, false);
			this.doSetPluginLoaded(channel, pluginName, true);
			break;
		case "enable": // Enable a plugin
			this.doSetPluginEnabled(channel, pluginName, true);
			break;
		case "disable": // Disable a plugin
			this.doSetPluginEnabled(channel, pluginName, false);
			break;
		case "list": // List all loaded plugins
			this.doListPlugins(channel);
			break;
		case "help":
		default: // Print help
			this.doPrintHelp(channel);
			break;
		}
	}
	
	/** Enable/disable a plugin
	 * @param target #Channel or username
	 * @param pluginName
	 * @param enable */
	private void doSetPluginEnabled(String target, String pluginName, boolean enable) {
		// This plugin cannot load/unload itself
		if (!pluginName.equalsIgnoreCase(this.getName())) {

			// Lookup plugin
			PluginManager pm = PluginManager.getInstance();
			synchronized (pm) {
				Plugin plugin = pm.getPlugin(pluginName);
				if (plugin != null) {
					// Check plugin status
					if (enable != plugin.isEnabled()) {
						// Enable/disable plugin
						super.ircSendMessage(target, (enable ? "Activation" : "Désactivation") + " du plugin " + plugin.getName());
						if (enable) {
							try {
								plugin.enable();
							} catch (Exception e) {
								logger.error("Error enabling plugin " + plugin.getName() + " : " + e.getMessage(), e);
								super.ircSendMessage(target, "Erreur activation du plugin " + plugin.getName() + " : " + e.getMessage());
							}
						} else {
							try {
								plugin.disable();
							} catch (Exception e) {
								logger.error("Error disabling plugin " + plugin.getName() + " : " + e.getMessage(), e);
								super.ircSendMessage(target, "Erreur désactivation du plugin " + plugin.getName() + " : " + e.getMessage());
							}
						}
					} else {
						super.ircSendMessage(target, "Le plugin " + plugin.getName() + " est déjà " + (enable ? "activé" : "désactivé"));
					}
				} else {
					super.ircSendMessage(target, "Je ne connais pas ce plugin : " + pluginName);
				}
			}
		} else {
			super.ircSendMessage(target, "Je ne veux pas désactiver le plugin " + pluginName);
		}
	}
	
	/** Load/unload a plugin from memory. This will not work on a native plugin.
	 * @param target #Channel or username
	 * @param pluginName
	 * @param enable */
	private void doSetPluginLoaded(String target, String pluginName, boolean load) {
		// This plugin cannot enable/disable itself
		if (!pluginName.equalsIgnoreCase(this.getName())) {
			
			// Lookup plugin
			PluginManager pm = PluginManager.getInstance();
			synchronized (pm) {
				
				// Load/unload plugin
				if (load) {
					// Load plugin
					Plugin plugin = pm.getPlugin(pluginName);
					if (plugin == null) {
						// Try to load plugin
						plugin = pm.getOrLoadPlugin(pluginName);
						if (plugin != null) {
							super.ircSendMessage(target, "Plugin chargé : " + plugin.getName());
						} else {
							super.ircSendMessage(target, "Echec " + (load ? "chargement" : "déchargement") + " du plugin : " + pluginName);
						}
					} else {
						super.ircSendMessage(target, "Ce plugin est déjà chargé : " + plugin.getName());
					}
				} else {
					// Check plugin status
					Plugin plugin = pm.getPlugin(pluginName);
					if (plugin != null) {
						// Try to unload plugin
						if (!plugin.isNativePlugin()) {
							super.ircSendMessage(target, "Arrêt et déchargement du plugin : " + plugin.getName());
							pm.unloadPlugin(plugin);
						} else {
							super.ircSendMessage(target, "Impossible de décharger un plugin natif : " + plugin.getName());
						}
					} else {
						super.ircSendMessage(target, "Ce plugin n'est pas chargé : " + pluginName);
					}
				}
			}
		} else {
			super.ircSendMessage(target, "Je ne veux pas désactiver le plugin " + pluginName);
		}
	}
	
	/** Displays help for this plugin */
	private void doPrintHelp(String target) {
		super.ircSendMessage(target, "Syntaxe : !" + this.getName() + " <load|unload|reload|enable|disable|list|help> [<plugin-name>]");
	}
	
	/** Output all available plugins */
	private void doListPlugins(String target) {
		// Construct message
		super.ircSendMessage(target, "=== Liste des plugins charges ===");
		
		// List all plugins
		StringBuffer sb = new StringBuffer();
		PluginManager pluginManager = PluginManager.getInstance();
		synchronized (pluginManager) {
			for (Plugin plugin : pluginManager) {
				sb.setLength(0);
				
				// Get plugin information
				sb.append(plugin.isEnabled() ? "[+]" : "[-]");
				sb.append(plugin.isNativePlugin() ? " (Native) " : " ");
				sb.append(plugin.getName());
				/*sb.append("@");
				sb.append(info.getClassName());*/
				sb.append(" : ");
				sb.append(plugin.getDescription());
				
				super.ircSendMessage(target, sb.toString());
			}
		}
		sb = null; 
	}
}
