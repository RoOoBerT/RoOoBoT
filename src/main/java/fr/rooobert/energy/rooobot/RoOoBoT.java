package fr.rooobert.energy.rooobot;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import fr.rooobert.energy.rooobot.db.Database;

/** RoOoBoT est un robot IRC effroyablement efficient, hautement paramétrable et dynamique à la volée. Avec des cookies. */
public class RoOoBoT {
	// --- Constantes
	private static final Logger logger = LogManager.getLogger(RoOoBoT.class);
	
	// --- Attributs
	
	// --- Methodes
	public static void main(String args[]) {
		logger.info("Starting " + RoOoBoT.class.getCanonicalName());
		
		// Check parameters
		if (args.length == 1) {
			// Try to load configuration
			File configFile = new File(args[0]);
			
			logger.info("Loading configuration file : " + configFile.getPath() + "...");
			Properties props = new Properties();
			try {
				props.load(new FileReader(configFile));
			} catch (Exception e) {
				logger.error("Error loading configuration file !", e);
				props = null;
			}
			
			// Run program if config OK
			if (props != null) {
				logger.info("Initializing database...");
				try {
					Database.initialize(props);
				} catch (Exception e) {
					logger.error("Error initializing database" + e.getMessage());
				}
				
				// Plugin manager initialization
				Database database = Database.getInstance();
				if (database != null) {
					logger.info("Initializing plugin manager...");
					try {
						PluginManager.instance = new PluginManager(props);
					} catch (Exception e) {
						logger.error("Error loading plugin manager : " + e.getMessage(), e);
					}
					
					// Plugin manager
					PluginManager pluginManager = PluginManager.getInstance();
					if (pluginManager != null) {
						// Initialize all plugins
						int plugins = pluginManager.loadAllPlugins();
						logger.info(plugins + " plugins loaded !");
						Bot bot = null;
						try {
							bot = new Bot(props, pluginManager);
						} catch (Exception e) {
							logger.error("Error initializing bot", e);
						}
						
						// Run the bot
						if (bot != null) {
							logger.info("Running the bot...");
							bot.run();
							logger.info("Finished running bot");
						}
						
						// Cleanup
						bot = null;
						try {
							database.close();
							logger.info("Database closed correctly");
						} catch (Exception e) {
							logger.error("Error closing database : " + e.getMessage());
						}
					}
					database = null;
				}
			}
		} else {
			logger.error("Usage : " + RoOoBoT.class.getName() + " <config.properties>" );
		}
		
		logger.info("Quitting " + RoOoBoT.class.getCanonicalName());
	}
}
