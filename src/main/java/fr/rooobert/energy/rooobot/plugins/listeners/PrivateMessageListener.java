package fr.rooobert.energy.rooobot.plugins.listeners;

import fr.rooobert.energy.rooobot.Plugin;
import fr.rooobert.energy.rooobot.event.IrcPrivateMessageEvent;
import fr.rooobert.energy.rooobot.listeners.IrcPrivateMessageListener;

public class PrivateMessageListener extends EventListener {
	// --- Constants
	
	// --- Attributes
	private final String nick;
	private final IrcPrivateMessageListener listener;
	
	// --- Methods
	public PrivateMessageListener(Plugin plugin, String nick, IrcPrivateMessageListener listener) {
		super(plugin);
		this.nick = nick;
		this.listener = listener;
	}
	
	public String getNick() {
		return nick;
	}
	
	public IrcPrivateMessageListener getListener() {
		return listener;
	}
	
	public boolean match(IrcPrivateMessageEvent event) {
		boolean result = false;
		if (this.nick == null || event.getUser().equals(this.nick)) {
			result = true;
		}
		return result;
	}
}
