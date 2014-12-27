package com.bvakili.portlet.askirc.util;

import static com.liferay.portal.kernel.util.Time.MINUTE;

import com.bvakili.portlet.askirc.PollingIntervalException;
import com.bvakili.portlet.askirc.model.*;

import javax.portlet.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.liferay.util.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.UnmodifiableList;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Organization;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.OrganizationLocalServiceUtil;
import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.service.ServiceContextThreadLocal;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portlet.PortletPreferencesFactoryUtil;

import org.ietf.jgss.ChannelBinding;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.PircBotXPatched;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.Listener;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.WaitForQueue;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.managers.ThreadedListenerManager;

import java.util.*;

public class IRCBot extends ListenerAdapter implements Listener {

	private static final String RANDOM_TOKEN = "_RANDOM_";
	private static final String FULLNAME_TOKEN = "_FULLNAME_";
	private static final String SITENAME_TOKEN = "_SITENAME_";

	private static final Log _log = LogFactoryUtil.getLog(IRCBot.class);

	private int SESSION_TIMEOUT = -1;
	private String botName = Constants.DEFAULT_BOT_NAME;
	private String channelName = Constants.DEFAULT_CHANNEL_NAME;

	private String hostName = Constants.DEFAULT_SERVER;
	private int portNumber = Constants.DEFAULT_PORT_NUMBER;
	private PircBotX bot = new PircBotXPatched();
	private ListenerManager listenerManager = bot.getListenerManager();
	private boolean hasJoined = false;
	private boolean disabled = Constants.DEFAULT_DISABLED;
	private Map<String, List<String>> fetchBuffers = new HashMap<String, List<String>>();
	private Map<String, Long> accessTimeMap = new HashMap<String, Long>();
	private List<String> currentConnectedUsers = new ArrayList<String>();
	private List<String> nextConnectedUsers = new ArrayList<String>();
	private long groupId = 0;
	private int pollingInterval;
	private long lastUsersListRefresh;
	private boolean integrateLiferayChat = Constants.DEFAULT_INTEGRATE_LIFERAY_CHAT;
	
	public IRCBot(String server, int portNumber2, String botName2,
			String channelName2, long groupId2, int pollingInterval2) {

		pollingInterval = pollingInterval2;
		groupId = groupId2;
		hostName = server;
		portNumber = portNumber2;
		botName = processBotName(botName2);
		channelName = processChannelName(channelName2);

		bot.setName(botName);
		bot.setLogin(botName);
		// bot.setVerbose(true);
		bot.setAutoNickChange(true);
		bot.useShutdownHook(true);

		listenerManager.addListener(this);

		_log.debug("Connecting as " + botName + " to " + hostName + ":"
				+ portNumber + " " + channelName);
		try {
			bot.connect(hostName, portNumber);
			bot.joinChannel(channelName);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			SESSION_TIMEOUT = Integer.parseInt(PropsUtil
					.get(PropsKeys.SESSION_TIMEOUT));
			if (SESSION_TIMEOUT < 30) {
				SESSION_TIMEOUT = 30;
			}
		} catch (Exception e) {
		}

	}

	private String processChannelName(String channelName2) {

		if (channelName2.contains(SITENAME_TOKEN)) {
			Group group;
			try {
				group = GroupLocalServiceUtil.getGroup(groupId);
				String name = "";
				name = group.getFriendlyURL().substring(1);
				channelName2 = channelName2.replace(SITENAME_TOKEN, name);

			} catch (PortalException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SystemException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return channelName2;
	}

	private String processBotName(String botName2) {
		if (botName2 != null && !botName2.trim().isEmpty()) {
			botName2 = botName2.replace(RANDOM_TOKEN, getRandom());
		}
		return botName2;
	}

	@Override
	public void onJoin(JoinEvent event) throws Exception {
		hasJoined = true;
	}

	private static String getRandom() {

		Random rand = new Random();
		return String.valueOf(rand.nextInt(100))
				+ String.valueOf(rand.nextInt(100));

	}

	private void addLine(String line) {
		// System.out.pr\xxintln(">addLine");
		Set<String> keys = fetchBuffers.keySet();
		// System.out.println(">keys length = " + keys.size());
		List<String> removalList = new ArrayList<String>();
		for (String fetchBufferSessionId : keys) {
			// System.out.println(">last accessed time: " +
			// accessTimeMap.get(sessionId));
			// System.out.println(">Session Timeout : " + SESSION_TIMEOUT);
			// System.out.println(">Minute : " + MINUTE);
			// System.out.println(">Expire: " + (accessTimeMap.get(sessionId) +
			// SESSION_TIMEOUT * MINUTE));
			// System.out.println(">Now :  " + System.currentTimeMillis());
			// System.out.println(">" + (accessTimeMap.get(sessionId) +
			// SESSION_TIMEOUT * MINUTE < System.currentTimeMillis()));
			_log.debug("fetchBufferSessionId " + fetchBufferSessionId);
			if (accessTimeMap.get(fetchBufferSessionId) + SESSION_TIMEOUT * MINUTE < System
					.currentTimeMillis()) {
				removalList.add(fetchBufferSessionId);
			} else {
				List<String> value = fetchBuffers.get(fetchBufferSessionId);
				value.add(line);
			}
		}
		// System.out.println("removal list size : " + removalList.size());
		// System.out.println("fetch buffers size: " +
		// fetchBuffers.keySet().size());
		fetchBuffers.keySet().removeAll(removalList);
		accessTimeMap.keySet().removeAll(removalList);
	}

	@Override
	public void onMessage(MessageEvent event) throws Exception {
		if (!isDisabled()) {
			String message = event.getMessage();
			String user = event.getUser().getNick();
			String line = user + ": " + message;
			if (message.contains(botName)) {
			}
			_log.debug("Received message " + message);
			addLine(line);
		}
	}

	public void sendMessage(PortletSession session, String windowId,
			String message) {

		if (!isDisabled()) {
			bot.sendMessage(channelName, message);
			String line = "";
			if (botName.contains(FULLNAME_TOKEN)) {
				String userId = "<span class='chat-user-id'>" + PermissionThreadLocal.getPermissionChecker()
						.getUser().getUserId() + "</span>";
				String fullName = PermissionThreadLocal.getPermissionChecker()
						.getUser().getFullName();
				line += userId + fullName;
			} else {
				line += bot.getNick();
			}
			line += ": " + message;
			_log.debug("Channel: " + channelName + " Group: " + groupId +  " Message : " + line);
			accessTimeMap.put(session.getId() + windowId,
					session.getLastAccessedTime());
			addLine(line);
		}
	}

	public List<String> fetch(PortletSession session, String windowId) {
		// System.out.println(">fetch:");
		// System.out.println(">Session id : " + session.getId());
		// System.out.println(">Session : " + session);
		// System.out.println(">window Id : " + windowId);

		String sessionId = session.getId();

		long userId = PermissionThreadLocal.getPermissionChecker()
				.getUserId();
		String fetchBufferKey = userId + sessionId + windowId;
		

		if (botName.contains(FULLNAME_TOKEN)) {
			String fullname = PermissionThreadLocal.getPermissionChecker()
					.getUser().getFullName();
			boolean contains = false;
			for(String str: currentConnectedUsers) {
			    if(str != null && str.trim().contains(fullname.trim()))
			       contains = true;
			}
			if (!contains) {
				currentConnectedUsers.add(fullname);
			}
			contains = false;
			for(String str : nextConnectedUsers) {
			    if(str.trim().contains(fullname.trim()))
			       contains = true;
			}
			if (!contains) {
				nextConnectedUsers.add(fullname);
			}

		}		

		List<String> fetchBuffer = fetchBuffers.get(fetchBufferKey);
		List<String> old = fetchBuffer;
		fetchBuffer = new ArrayList<String>();
		fetchBuffers.put(fetchBufferKey, fetchBuffer);
		
		accessTimeMap.put(fetchBufferKey, session.getLastAccessedTime());
		if (old == null) {
			// System.out.println(">old session is null, assigning now oen");
			old = fetchBuffer;
		}
		return old;
	}

	private boolean getShouldRefreshUsersList() {
		boolean retVal = false;
		if (lastUsersListRefresh <= 0) {
			lastUsersListRefresh = System.currentTimeMillis();
			retVal = true;
		} else {
			long currentTime = System.currentTimeMillis();
			long requiredElapsedTime = getPollingInterval() * 11l;
			if (currentTime > lastUsersListRefresh + requiredElapsedTime) {
				retVal = true;
				lastUsersListRefresh = currentTime;
			}
		}

		return retVal;

	}

	public int getNumUsers() {
		return bot.getUsers().size();
	}

	public List<String> getUsersList() {
		List<String> retVal = new ArrayList<String>();
		String log = "";

		// Add the locally connected users
		// These users are connected through the bot
		if (botName.contains(FULLNAME_TOKEN)) {
			boolean isUpdateList = getShouldRefreshUsersList();
			//_log.debug("Is updated to users list needed? " + isUpdateList);
			//_log.debug(currentConnectedUsers.size());
			if (isUpdateList) {
				currentConnectedUsers.clear();
				currentConnectedUsers.addAll(nextConnectedUsers);
				nextConnectedUsers.clear();
			}
			retVal.addAll(currentConnectedUsers);
		}
		

		Channel channel = bot.getChannel(channelName);

		Set<User> users = channel.getUsers();

		List<String> userNicks = new ArrayList<String>();
		for (User user : users) {
			if (user.getNick().contains(FULLNAME_TOKEN)) {
				continue;
			}
			userNicks.add(user.getNick());
		}

		String nick = bot.getNick();
		String login = bot.getLogin();
		String name = bot.getName();
/**
		if (name != null) {
			log += (log.length() > 0 ? " " : "");
			log += "Name: " + name;
		}
		
		if (login != null) {
			log += (log.length() > 0 ? " " : "");
			log += "Login: " + login;
		}
**/

		String fullname = PermissionThreadLocal.getPermissionChecker().getUser().getFullName();
		log += (log.length() > 0 ? " " : "");
		log += "Fullname: " + fullname;
	
		
		if (nick != null) {
			log += (log.length() > 0 ? " " : "");
			log += "Nick: " + nick;
		}
		if (channel != null) {
			log += (log.length() > 0 ? " " : "");
			log += "Channel: " + channel.getName();
		}
		retVal.addAll(userNicks);
		
		
		log += (log.length() > 0 ? " " : "");
		log += "Num users: " + retVal.size();
		
		log += (log.length() > 0 ? " " : "");
		log += "Bot Name: " + botName;
		//_log.debug(log);
		
		Collections.sort(retVal);

		return retVal;
	}

	public String getChannelName() {
		return channelName;
	}

	public String getBotName() {
		return botName;
	}

	public void disconnect() {
		bot.disconnect();
	}

	public void shutdown() {
		Set<Listener> listenersSet = listenerManager.getListeners();
		ThreadedListenerManager tLM = (ThreadedListenerManager) listenerManager;
		try {
			ExecutorService executorService = tLM.shutdown();
			List<Runnable> terminatedTasks = executorService.shutdownNow();
		} catch (Exception e) {
			_log.error("Caught Exception " + e);
		}
		int numListeners = listenersSet.size();
		Listener[] listeners = new Listener[numListeners];
		listeners = listenersSet.toArray(listeners);

		for (int i = 0; i < numListeners; i++) {
			try {
				Listener aListener = listeners[i];
				listenerManager.removeListener(aListener);
			} catch (Exception e) {
				_log.error("Caught Exception " + e);
			}
		}

		try {
			
			if (bot.isConnected()) {
				bot.quitServer();
			}
		} catch (Exception e) {
			_log.error("Caught Exception " + e);
		} finally {
			try {
				bot.shutdown(true);
			} catch (Exception e) {
				_log.error("Caught Exception " + e);
			}
		}

	}

	public String getServerName() {
		return hostName;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public void changeBotName(String newBotName) {
		_log.debug("Changing bot name from " + botName + " to " + newBotName);
		botName = newBotName.replace(RANDOM_TOKEN, getRandom());
		bot.changeNick(botName);
		bot.setName(botName);
		bot.setLogin(botName);

	}

	public void changeChannelName(String newChannelName) {
		User userBot = bot.getUserBot();

		Set<Channel> channels = bot.getChannels(userBot);
		_log.debug("User: " + userBot);
		_log.debug("Nick: " + userBot.getNick());
		for (Channel channel : channels) {
			bot.partChannel(channel);
			_log.debug("Parting channel : " + channel.getName());

		}
		bot.joinChannel(newChannelName);
		_log.debug("Joining channel : " + newChannelName);
		channelName = newChannelName;

	}

	public void changePortNumber(int newPortNumber) {
		portNumber = newPortNumber;
	}

	public void changeServer(String newServer) {

		try {
			if (bot.isConnected()) {
				bot.disconnect();
			}
		} catch (Exception e) {
			_log.error("Caught Exception " + e);
		} finally {
			try {
				bot.shutdown(true);
			} catch (Exception e) {
				_log.error("Caught Exception " + e);
			}
		}

		hostName = newServer;
		try {
			_log.debug("Connecting to " + newServer + " " + portNumber);
			bot.connect(newServer, portNumber);
			bot.joinChannel(channelName);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void setPollingInterval(int pollingInterval2) {
		if (pollingInterval2 > 200 && pollingInterval2 < 1000000) {
			pollingInterval = pollingInterval2;
		} else {
			try {
				throw new PollingIntervalException(
						"Cannot poll less than 200ms or more than 1000000ms");
			} catch (PollingIntervalException e) {
				_log.error(e);
			}
		}
	}

	public int getPollingInterval() {
		return pollingInterval;
	}

	public boolean isIntegrateLiferayChat() {
		return integrateLiferayChat;
	}
	
	public void setIntegrateLiferayChat(boolean res) {
		this.integrateLiferayChat = res;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	
	public void setDisabled(boolean res) {
		this.disabled = res;
	}
}
