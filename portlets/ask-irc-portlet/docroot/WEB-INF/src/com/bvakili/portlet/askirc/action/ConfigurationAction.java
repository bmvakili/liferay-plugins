package com.bvakili.portlet.askirc.action;

import static com.bvakili.portlet.askirc.util.Constants.BOT_NAME_PREF;
import static com.bvakili.portlet.askirc.util.Constants.CHANNEL_NAME_PREF;
import static com.bvakili.portlet.askirc.util.Constants.SERVER_PREF;
import static com.bvakili.portlet.askirc.util.Constants.PORT_NUMBER_PREF;
import static com.bvakili.portlet.askirc.util.Constants.POLLING_INTERVAL_PREF;
import static com.bvakili.portlet.askirc.util.Constants.INTEGRATE_LIFERAY_CHAT;

import java.util.Collection;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.bvakili.portlet.askirc.util.BotRegistryUtil;
import com.bvakili.portlet.askirc.util.IRCBot;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.DefaultConfigurationAction;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropertiesParamUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;

public class ConfigurationAction extends DefaultConfigurationAction {
	private static final Log _log = LogFactoryUtil
			.getLog(ConfigurationAction.class);

	public void processAction(PortletConfig config,
			ActionRequest actionRequest, ActionResponse actionResponse)
			throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest
				.getAttribute(WebKeys.THEME_DISPLAY);
		long scopeGroupId = themeDisplay.getScopeGroupId();
		IRCBot ircBot = BotRegistryUtil.getBot(scopeGroupId);
		String botName = ircBot.getBotName();
		String channelName = ircBot.getChannelName();
		String server = ircBot.getServerName();
		int portNumber = ircBot.getPortNumber();
		int pollingInterval = ircBot.getPollingInterval();
		boolean integrateLiferayChat = ircBot.isIntegrateLiferayChat();
		
		super.processAction(config, actionRequest, actionResponse);

		UnicodeProperties properties = PropertiesParamUtil.getProperties(
				actionRequest, PREFERENCES_PREFIX);

		Group group = GroupLocalServiceUtil.getGroup(scopeGroupId);
		boolean isLayoutSetPrototype = group.isLayoutSetPrototype();
		if (isLayoutSetPrototype) {
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

			BotRegistryUtil.init();
		} else {
			String botNamePref = properties.get(BOT_NAME_PREF);
			String channelNamePref = properties.get(CHANNEL_NAME_PREF);
			String serverPref = properties.get(SERVER_PREF);
			String portNumberPref = properties.get(PORT_NUMBER_PREF);
			String pollingIntervalPref = properties.get(POLLING_INTERVAL_PREF);
			String integrateLiferayChatPref = properties.get(INTEGRATE_LIFERAY_CHAT);
			
			int portNumberPrefNum = GetterUtil.getInteger(portNumberPref);
			boolean integrateLiferayChatPrefBoolean = GetterUtil.getBoolean(integrateLiferayChatPref);
			int pollingIntervalPrefNum = GetterUtil
					.getInteger(pollingIntervalPref);

			if (portNumber != portNumberPrefNum) {
				_log.debug("Port number change request from " + portNumber
						+ " to " + portNumberPrefNum);
				ircBot.changePortNumber(portNumberPrefNum);
			}

			if (pollingInterval != pollingIntervalPrefNum) {
				_log.debug("Polling interval change request from "
						+ pollingInterval + " to " + pollingIntervalPrefNum);
				ircBot.setPollingInterval(pollingIntervalPrefNum);
			}

			if (!server.equalsIgnoreCase(serverPref)) {
				_log.debug("Server change request from " + server + " to "
						+ serverPref);
				ircBot.changeServer(serverPref);
			}

			if (!botName.equalsIgnoreCase(botNamePref)) {
				_log.debug("Bot name change request from " + botName + " to "
						+ botNamePref);
				ircBot.changeBotName(botNamePref);
			}

			if (!channelName.equalsIgnoreCase(channelNamePref)) {
				_log.debug("Channel name change request from " + channelName
						+ " to " + channelNamePref);
				ircBot.changeChannelName(channelNamePref);
			}
			
			if (integrateLiferayChat != integrateLiferayChatPrefBoolean) {
				_log.debug("Updated Liferay Chat Portlet Integration switch from " + integrateLiferayChat
						+ " to " + integrateLiferayChatPrefBoolean);
				ircBot.setIntegrateLiferayChat(integrateLiferayChatPrefBoolean);
			}

		}

	}

	@Override
	public String render(PortletConfig portletConfig,
			RenderRequest renderRequest, RenderResponse renderResponse)
			throws Exception {
		ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest
				.getAttribute(WebKeys.THEME_DISPLAY);
		long scopeGroupId = themeDisplay.getScopeGroupId();
		IRCBot ircBot = BotRegistryUtil.getBot(scopeGroupId);
		_log.debug("In group : " + scopeGroupId + " and adding ircBot: "
				+ ircBot);
		renderRequest.setAttribute("ircBot", ircBot);

		return super.render(portletConfig, renderRequest, renderResponse);
	}
}
