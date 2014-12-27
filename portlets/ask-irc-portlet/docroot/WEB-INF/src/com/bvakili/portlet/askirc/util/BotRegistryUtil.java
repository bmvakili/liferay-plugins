package com.bvakili.portlet.askirc.util;

import static com.bvakili.portlet.askirc.util.Constants.BOT_NAME_PREF;
import static com.bvakili.portlet.askirc.util.Constants.CHANNEL_NAME_PREF;
import static com.bvakili.portlet.askirc.util.Constants.SERVER_PREF;
import static com.bvakili.portlet.askirc.util.Constants.PORT_NUMBER_PREF;
import static com.bvakili.portlet.askirc.util.Constants.POLLING_INTERVAL_PREF;
import static com.bvakili.portlet.askirc.util.Constants.DEFAULT_BOT_NAME;
import static com.bvakili.portlet.askirc.util.Constants.DEFAULT_CHANNEL_NAME;
import static com.bvakili.portlet.askirc.util.Constants.DEFAULT_PORT_NUMBER;
import static com.bvakili.portlet.askirc.util.Constants.DEFAULT_POLLING_INTERVAL;
import static com.bvakili.portlet.askirc.util.Constants.DEFAULT_SERVER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.portlet.PortletPreferences;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.model.LayoutSet;
import com.liferay.portal.model.LayoutSetPrototype;
import com.liferay.portal.model.LayoutTemplate;
import com.liferay.portal.model.LayoutType;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.LayoutSetPrototypeLocalServiceUtil;
import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;

public class BotRegistryUtil {

	private static Log _log;
	// one group can have at most one bot in it
	// this list will hold group-bot references
	private static Map<Long, IRCBot> groupsBots;
	private static boolean inUpdate = false;
	public static void init() {
		inUpdate = true;
		
		try {
			if (_log == null) {
				_log = LogFactoryUtil.getLog(BotRegistryUtil.class);
			}
			if (groupsBots != null) {
				groupsBots.clear();
			} else {
				groupsBots = new HashMap<Long, IRCBot>();
			}
			_log.debug("Init Started");
			List<Group> groups = GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
			long companyId = PortalUtil.getDefaultCompanyId();
			
			for (Group group : groups) {
				boolean hasAskIRCPortlet = false;
				long groupId = group.getGroupId();
				PortletPreferences portletPreferences = PortletPreferencesLocalServiceUtil.fetchPreferences(companyId, groupId, ResourceConstants.SCOPE_GROUP, LayoutConstants.DEFAULT_PLID, Constants.ASK_IRC_PORTLET_ID);
				
				_log.debug("(" + companyId + ", "  +groupId + ", " + ResourceConstants.SCOPE_GROUP + ", " + LayoutConstants.DEFAULT_PLID +" ) = " + portletPreferences);
				if (portletPreferences == null) {
					continue;
				}
				String botName = portletPreferences.getValue(BOT_NAME_PREF, DEFAULT_BOT_NAME);
				String channelName = portletPreferences.getValue(CHANNEL_NAME_PREF, DEFAULT_CHANNEL_NAME);
				String server = portletPreferences.getValue(SERVER_PREF, DEFAULT_SERVER);
				int portNumber = GetterUtil.getInteger(portletPreferences.getValue(PORT_NUMBER_PREF, String.valueOf(DEFAULT_PORT_NUMBER)), DEFAULT_PORT_NUMBER);
				int pollingInterval = GetterUtil.getInteger(portletPreferences.getValue(POLLING_INTERVAL_PREF, String.valueOf(DEFAULT_POLLING_INTERVAL)), DEFAULT_POLLING_INTERVAL);
				LayoutSet publicLayoutSet = group.getPublicLayoutSet();
				LayoutSet privateLayoutSet = group.getPrivateLayoutSet();
				
				boolean layoutSetPrototypeLinkEnabled = publicLayoutSet.getLayoutSetPrototypeLinkEnabled();
				List<Layout> groupLayouts = new ArrayList<Layout>(LayoutLocalServiceUtil.getLayouts(groupId, true));
				groupLayouts.addAll(LayoutLocalServiceUtil.getLayouts(groupId, false));
				
				if (layoutSetPrototypeLinkEnabled) {
					long layoutSetPrototypeId;
					try {
						layoutSetPrototypeId = publicLayoutSet.getLayoutSetPrototypeId();
						LayoutSetPrototype prototype = LayoutSetPrototypeLocalServiceUtil.getLayoutSetPrototype(layoutSetPrototypeId);
						long protoypeGroupId = prototype.getGroupId();

						if (prototype.getActive()) {
							try {
								PortletPreferences prototypePortletPreferences = PortletPreferencesLocalServiceUtil.fetchPreferences(companyId, protoypeGroupId, ResourceConstants.SCOPE_GROUP, LayoutConstants.DEFAULT_PLID, Constants.ASK_IRC_PORTLET_ID);				
							
								String botName2 = prototypePortletPreferences.getValue(BOT_NAME_PREF, DEFAULT_BOT_NAME);
								String channelName2 = prototypePortletPreferences.getValue(CHANNEL_NAME_PREF, DEFAULT_CHANNEL_NAME);
								String server2 = prototypePortletPreferences.getValue(SERVER_PREF, DEFAULT_SERVER);
								int portNumber2 = GetterUtil.getInteger(prototypePortletPreferences.getValue(PORT_NUMBER_PREF, String.valueOf(DEFAULT_PORT_NUMBER)), DEFAULT_PORT_NUMBER);
								int pollingInterval2 = GetterUtil.getInteger(prototypePortletPreferences.getValue(POLLING_INTERVAL_PREF, String.valueOf(DEFAULT_POLLING_INTERVAL)), DEFAULT_POLLING_INTERVAL);
								_log.debug("Found site template values : " + botName2 + " " + channelName2 + " " + server2 + " " + portNumber2);
								if (botName2 != null && channelName2 != null && server2 != null && portNumber2 >= 0 && portNumber2 <= 65535 && pollingInterval2 > 200 && pollingInterval2 < 1000000) {
									botName = botName2;
									channelName = channelName2;
									server = server2;
									portNumber = portNumber2;
									pollingInterval = pollingInterval2;
								}
							} catch (Exception e) {
								// it's fine if the prototype group doesn't
								// have preferences for this we'll just
								// use defaults0
							}
							
							groupLayouts.addAll(LayoutLocalServiceUtil.getLayouts(protoypeGroupId, false));
							groupLayouts.addAll(LayoutLocalServiceUtil.getLayouts(protoypeGroupId, true));
							
						}
						
					} catch (PortalException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}

				hasAskIRCPortlet = true;
				
				if (hasAskIRCPortlet) {
					_log.debug("Adding IRCBot (" + server + ", " + portNumber + ", " + botName + ", " + channelName + ") for group " + group.getName() + " (" + groupId + ") at polling interval " + pollingInterval + "ms");
					IRCBot ircBot = new IRCBot(server, portNumber, botName, channelName, groupId, pollingInterval);
					groupsBots.put(groupId, ircBot);
				}

			}
		} catch (SystemException e) {
			e.printStackTrace();
		}
		_log.debug("Init Ended");
		inUpdate = false;
	}
	public static IRCBot getBot(long groupId) {
		Set<Long> keys = groupsBots.keySet();
		//_log.debug("Key Set length " + keys.size());
		if (keys.size() == 0) {
			Collection<IRCBot> bots = BotRegistryUtil.getBots();
			for (IRCBot bot : bots) {
				try {
					bot.shutdown();
				} catch (NoClassDefFoundError e) {
					// This can happen if the portlet was never loaded on a page
					// Ignore since it is not a real issue;
					// The bot would only have to shutdown iff the portlet has been
					// loaded on a page.
				}
			}
			
			init();
			keys = groupsBots.keySet();
		}
		for (Long key : keys) {
			//_log.debug("is " + key.longValue() + " equal to " + groupId);
			if (key.longValue() == groupId) {
				return groupsBots.get(key);
			}
		}
		
		return null;
	}

	public static Collection<IRCBot> getBots() {
		return groupsBots.values();
	}
	public static boolean hasBot(long groupId) {
		return groupsBots.containsKey(groupId);
	}
	public static void addGroupBot(long groupId, IRCBot bot) {
		groupsBots.put(groupId, bot);
	}
	public static IRCBot removeGroupBot(long groupId) {
		IRCBot bot = groupsBots.remove(groupId);
		return bot;
	}
	
	public static boolean isInUpdate() {
		return inUpdate;
	}
	
	
}
