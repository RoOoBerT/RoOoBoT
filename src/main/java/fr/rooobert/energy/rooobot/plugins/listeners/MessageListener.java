package fr.rooobert.energy.rooobot.plugins.listeners;

import fr.rooobert.energy.rooobot.Plugin;
import fr.rooobert.energy.rooobot.event.IrcMessageEvent;
import fr.rooobert.energy.rooobot.listeners.IrcMessageListener;

public class MessageListener extends EventListener {
	// --- Constants
	
	// --- Attributes
	private final String nick;
	private final String channel;
	private final IrcMessageListener listener;
	
	// --- Methods
	public MessageListener(Plugin plugin, String nick, String channel, IrcMessageListener listener) {
		super(plugin);
		this.nick = nick;
		this.channel = channel;
		this.listener = listener;
	}
	
	public String getNick() {
		return nick;
	}
	
	public String getChannel() {
		return channel;
	}
	
	public IrcMessageListener getListener() {
		return listener;
	}
	
	public boolean match(IrcMessageEvent event) {
		boolean result = false;
		if ((this.channel == null || event.getChannel().equalsIgnoreCase(this.channel))
				&& (this.nick == null || event.getUser().equalsIgnoreCase(this.nick))) {
			result = true;
		}
		return result;
	}
}
