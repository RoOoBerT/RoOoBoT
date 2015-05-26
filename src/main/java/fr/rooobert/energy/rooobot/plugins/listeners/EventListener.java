package fr.rooobert.energy.rooobot.plugins.listeners;

import fr.rooobert.energy.rooobot.Plugin;

public abstract class EventListener {
	// --- Constants
	
	// --- Attributes
	private final Plugin plugin;
	
	// --- Methods
	protected EventListener(Plugin plugin) {
		this.plugin = plugin;
	}
	
	public Plugin getPlugin() {
		return this.plugin;
	}
}
