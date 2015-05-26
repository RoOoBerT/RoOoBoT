package fr.rooobert.energy.rooobot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import com.google.common.collect.ImmutableSortedSet;

import fr.rooobert.energy.rooobot.comm.Messages;
import fr.rooobert.energy.rooobot.event.IrcMessageEvent;
import fr.rooobert.energy.rooobot.event.IrcPrivateMessageEvent;
import fr.rooobert.energy.rooobot.listeners.IrcMessageListener;
import fr.rooobert.energy.rooobot.listeners.IrcPrivateMessageListener;
import fr.rooobert.energy.rooobot.plugins.listeners.MessageListener;
import fr.rooobert.energy.rooobot.plugins.listeners.PrivateMessageListener;

/** The main Bot class */
public class Bot extends ListenerAdapter<PircBotX> implements Runnable, IrcBot {
	// --- Constants
	private static final Logger logger = LogManager.getLogger(IrcBot.class);
	
	/** Regular expression used to parse text command parameters */
	private static final Pattern COMMAND_REGEX = Pattern.compile("^!(\\w+)\\s*(.*)$");
	
	// --- Attributes
	private final Object objectShutdown = new Object();
	private final PluginManager pluginManager;
	private final PircBotX botX;
	
	// Configuration
	private final String host;
	private final String channel;
	
	// Event listeners
	private final List<MessageListener> messageListeners = new ArrayList<>();
	private final List<PrivateMessageListener> privateMessageListeners = new ArrayList<>();
	
	// --- Methods
	public Bot(Properties props, PluginManager pluginManager) throws Exception {
		super();
		this.pluginManager = pluginManager;
		this.host = props.getProperty("irc.server", "127.0.0.1");
		//super.setName(props.getProperty("irc.nickname", RoOoBoT.class.getName()));
		this.channel = props.getProperty("irc.channel", RoOoBoT.class.getName());
		//super.setVerbose(Boolean.parseBoolean(props.getProperty("irc.verbose", "false")));
		
		Configuration<?> configuration = new Configuration.Builder<>()
				.setName(props.getProperty("irc.nickname", RoOoBoT.class.getName())) // IRC nickname
				.setServerHostname(props.getProperty("irc.server", "127.0.0.1")) // IRC server
				.addAutoJoinChannel(props.getProperty("irc.channel", "#" + RoOoBoT.class.getName())) // IRC channel
				.addListener(this) // IRC event listener
				.buildConfiguration();
		
		this.botX = new PircBotX(configuration);
		// Change encoding
		/*try {
			super.setEncoding(props.getProperty("irc.encoding", "ISO_8859_1"));
		} catch (UnsupportedEncodingException e) {
			throw new Exception("Unsupported encoding : " + e.getMessage(), e);
		}*/
	}
	
	@Override
	public void run() {
		// Plugins initialization
		logger.info("Enabling plugins...");
		synchronized (this.pluginManager) {
			for (Plugin plugin : this.pluginManager) {
				plugin.setBot(this);
				if (plugin.isEnabled()) {
					logger.info("Enabling plugin " + plugin.getName());
					try {
						// Do not use enable()
						plugin.onEnable();
					} catch (Exception e) {
						logger.info("Exception enabling plugin " + plugin.getName() + " : " + e.getMessage());
					}
				} else {
					logger.debug("Found disabled plugin : " + plugin.getName());
				}
			}
		}
		
		logger.info("Connecting to " + this.host + "...");
		try {
			this.botX.startBot();
			//this.sendMessage(this.channel, Messages.GREETINGS.getText());
		} catch (IOException e) {
			logger.error("I/O exception running bot", e);
		} catch (IrcException e) {
			logger.error("IRC issue running bot ", e);
		}
		
		// Wait for shutdown
		if (this.botX.isConnected()) {
			synchronized (this.objectShutdown) {
				try {
					this.objectShutdown.wait();
				} catch (InterruptedException e) {
					logger.error("This cannot happen !", e);
				}
			}
			
			// Send goodbye message
//			this.sendMessage(this.channel, Messages.GOODBYE.getText());
		}
		
		// Disabling plugins
		logger.info("Disabling all plugins...");
		synchronized (this.pluginManager) {
			for (Plugin plugin : this.pluginManager) {
				logger.info("Disabling plugin " + plugin.getName());
				try {
					// FIXME disable() ou onDisable() ?
					plugin.disable();
				} catch (Exception e) {
					logger.error("Error while disabling plugin");
				}
			}
		}
		
		logger.info("Disconnecting from the server...");
		//this.disconnect();
	}
	
	/** Shuts down the bot */
	public void shutdown(String reason) {
		this.botX.stopBotReconnect();
		this.botX.sendIRC().quitServer(reason);
		synchronized (this.objectShutdown) {
			this.objectShutdown.notify();
		}
	}
	
	public static AccessLevel getUserAccess(Channel channel, User user) {
		AccessLevel al = AccessLevel.USER;
		if (channel.isOwner(user)) {
			al = AccessLevel.FOUNDER;
		} else if (channel.isSuperOp(user)) {
			al = AccessLevel.SOP;
		} else if (channel.isHalfOp(user)) {
			al = AccessLevel.HOP;
		} else if (channel.hasVoice(user)) {
			al = AccessLevel.VOP;
		}
		return al;
	}
	
	// - Getters
	public String getBotChannel() {
		return this.channel;
	}
	
	public User getUser(String channel, String nick) {
		for (User user : this.getUsers(channel)) {
			if (user.getNick().equals(nick)) {
				return user;
			}
		}
		return null;
	}
	
	// - Events
	@Override
	public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
		this.botX.getUserBot().send().mode("+B");
		this.botX.getUserChannelDao().getChannel(this.channel).send().message(Messages.GREETINGS.getText());
	}
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		final String channel = event.getChannel().getName();
		final String sender = event.getUser().getNick();
		final String login = event.getUser().getLogin();
		final String message = event.getMessage();

		// Filter messages from other channels or from self
		if (channel.equalsIgnoreCase(this.channel) && !sender.equals(this.getNick())) {
			
			// TODO Verifier qu'on ne traite pas les messages d'un autre bot !
			//User user = event.getUser().get;
			
			// Check if it is a command
			Matcher matcher = COMMAND_REGEX.matcher(message);
			if (matcher.find()) {
				// Remove command prefix
				String pluginName = matcher.group(1);
				String command = matcher.group(2);
				
				// Find the target plugin
				Plugin plugin = this.pluginManager.getPlugin(pluginName);
				if (plugin != null && plugin.isEnabled()) {
					
					// Ensure the user has the rights to use this plugin (perform access checks)
					AccessLevel requiredAccess = plugin.getAccess();
					
					// If the user is admin of this plugin, he is authorized
					boolean verified = event.getUser().isVerified();
					boolean authorized = verified && plugin.isAdmin(sender);
					if (!authorized) {
						// Otherwise, check if he is registered and has the right access
						boolean registration = !plugin.isRegistrationRequired() || verified;
						AccessLevel userAccess = getUserAccess(event.getChannel(), event.getUser());
						
						authorized = registration && !userAccess.lesserThan(requiredAccess);
					}
					if (authorized) {
						logger.info("Command on channel " + channel + " from " + sender + " to plugin " + pluginName + " : " + command);
						plugin.onCommand(channel, sender, login, event.getUser().getHostmask(), command);
					} else {
						logger.warn("Command denied for user " + sender + " : " + message);
					}
				}
			} else {
				// Standard user message
				IrcMessageEvent e = new IrcMessageEvent(new Date(event.getTimestamp()), channel, sender, message);
				
				// Dispatch the event to the right listener or until it is mark as consumed
				Iterator<MessageListener> it = this.messageListeners.iterator();
				while (it.hasNext() && !e.isConsumed()) {
					MessageListener ml = it.next();
					if (ml.match(e)) {
						ml.getListener().onMessage(e);
					}
				}
			}
		}
	}
	
	@Override
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) throws Exception {
		// Standard user message
		IrcPrivateMessageEvent e = new IrcPrivateMessageEvent(new Date(event.getTimestamp()), event.getUser().getNick(), event.getMessage());
		
		// Dispatch the event to the right listener or until it is mark as consumed
		Iterator<PrivateMessageListener> it = this.privateMessageListeners.iterator();
		while (it.hasNext() && !e.isConsumed()) {
			PrivateMessageListener ml = it.next();
			if (ml.match(e)) {
				ml.getListener().onPrivateMessage(e);
			}
		}
	}
	

	@Override
	public void sendMessage(String target, String message) {
		this.botX.getUserChannelDao().getChannel(target).send().message(message);
	}
	
	@Override
	public ImmutableSortedSet<User> getUsers(String channel) {
		Channel c = this.botX.getUserChannelDao().getChannel(channel);
		if (c != null) {
			return c.getUsers();
		}
		return null;
	}
	
	@Override
	public String getNick() {
		return this.botX.getNick();
	}
	
	@Override
	public void addPrivateMessageListener(Plugin plugin, String nick, IrcPrivateMessageListener listener) {
		this.privateMessageListeners.add(new PrivateMessageListener(plugin, nick, listener));
	}
	
	@Override
	public void removePrivateMessageListener(IrcPrivateMessageListener listener) {
		this.messageListeners.remove(listener);
	}
	
	@Override
	public int removePrivateMessageListener(Plugin plugin) {
		int count = 0;
		
		Iterator<PrivateMessageListener> it = this.privateMessageListeners.iterator();
		while (it.hasNext()) {
			PrivateMessageListener ml = it.next();
			if (ml.getPlugin() == plugin) {
				it.remove();
				count++;
			}
		}
		return count;
	}
	
	@Override
	public void addMessageListener(Plugin plugin, String nick, String channel, IrcMessageListener listener) {
		this.messageListeners.add(new MessageListener(plugin, nick, channel, listener));
	}
	
	@Override
	public void removeMessageListener(IrcMessageListener listener) {
		this.messageListeners.remove(listener);
	}
	
	@Override
	public int removeMessageListener(Plugin plugin) {
		int count = 0;
		
		Iterator<MessageListener> it = this.messageListeners.iterator();
		while (it.hasNext()) {
			MessageListener ml = it.next();
			if (ml.getPlugin() == plugin) {
				it.remove();
				count++;
			}
		}
		return count;
	}
}
