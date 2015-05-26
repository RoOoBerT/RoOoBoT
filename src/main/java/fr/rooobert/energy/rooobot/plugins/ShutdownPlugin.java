package fr.rooobert.energy.rooobot.plugins;

import java.util.Properties;
import java.util.regex.Matcher;

import fr.rooobert.energy.rooobot.Plugin;

/** Simple native plugin that enables to shutdown the bot */
public class ShutdownPlugin extends Plugin {
	// --- Constants
	
	// --- Attributes
	
	// --- Methods
	public ShutdownPlugin(String name, Properties props) {
		super(name, props);
	}
	
	@Override
	public void onCommand(String channel, String sender, String login, String hostname, String command) {
		String cmd = "";
		
		Matcher matcher = COMMAND_ARGUMENT.matcher(command);
		if (matcher.find()) {
			cmd = matcher.group();
		}
		
		switch (cmd) {
		case "":
			this.doShutdown(sender);
			break;
		case "help":
		default: // Print help
			this.doPrintHelp(channel);
			break;
		}
	}
	
	/** Shutdown the bot */
	private void doShutdown(String sender) {
		super.shutdown("C'est " + sender + " qui l'a demandé !");
	}
	
	/** Displays help for this plugin */
	private void doPrintHelp(String target) {
		super.ircSendMessage(target, "Syntax : !" + this.getName() + " : Arret du bot");
	}
}
