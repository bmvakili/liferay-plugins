package com.bvakili.portlet.askirc.action;

import static com.bvakili.portlet.askirc.util.Constants.BOT_NAME_PREF;
import static com.bvakili.portlet.askirc.util.Constants.CHANNEL_NAME_PREF;
import static com.bvakili.portlet.askirc.util.Constants.INTEGRATE_LIFERAY_CHAT;
import static com.bvakili.portlet.askirc.util.Constants.POLLING_INTERVAL_PREF;
import static com.bvakili.portlet.askirc.util.Constants.PORT_NUMBER_PREF;
import static com.bvakili.portlet.askirc.util.Constants.SERVER_PREF;
import static com.bvakili.portlet.askirc.util.Constants.DISABLED;

import com.bvakili.portlet.askirc.model.*;
import com.bvakili.portlet.askirc.util.*;

import javax.portlet.*;

import java.io.*;
import java.util.*;

import com.liferay.util.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PropertiesParamUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.UnmodifiableList;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.*;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.DefaultConfigurationAction;
import com.liferay.portal.kernel.portlet.PortletConfigurationLayoutUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.RoleServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextThreadLocal;
import com.liferay.portal.service.UserGroupRoleLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.service.permission.PortletPermissionUtil;
import com.liferay.portal.service.persistence.OrgGroupRoleUtil;
import com.liferay.portal.service.persistence.UserGroupRolePK;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroupRole;

public class Render extends MVCPortlet {

	private static final Log _log = LogFactoryUtil.getLog(Render.class);

	private static final String PREFERENCES_PREFIX = "preferences--";

	@Override
	public void destroy() {
		// IRCBot.disconnect();
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
	
	}

	@Override
	public void doEdit(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {
	
		try {
			setIRCBot(renderRequest);
	
			super.doEdit(renderRequest, renderResponse);
		} catch (Exception e) {
			include("/html/portlet/ask-irc/error.jsp", renderRequest,
					renderResponse);
		}
	
	}

	public void doView(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {
		// setRenderAttributes(renderRequest);
		ThemeDisplay themeDisplay = (ThemeDisplay) ((RenderRequest) renderRequest)
				.getAttribute(WebKeys.THEME_DISPLAY);
	
		boolean isTypeControlPanel = themeDisplay.getLayout()
				.isTypeControlPanel();
		if (isTypeControlPanel) {
			return;
		}
		try {
			setIRCBot(renderRequest);
	
			// super.doView(renderRequest, renderResponse);
			include("/html/portlet/ask-irc/view.jsp", renderRequest,
					renderResponse);
		} catch (Exception e) {
			include("/html/portlet/ask-irc/error.jsp", renderRequest,
					renderResponse);
		}
	
	}

	@Override
	public void init() throws PortletException {
	
		super.init();
		setupRoles();
		BotRegistryUtil.init();
	}

	@Override
	public void processAction(ActionRequest actionRequest,
			ActionResponse actionResponse) throws IOException, PortletException {
	
		try {
			ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest
					.getAttribute(WebKeys.THEME_DISPLAY);
	
			long userId = themeDisplay.getUserId();
			long scopeGroupId = themeDisplay.getScopeGroupId();
			boolean isAdmin = UserGroupRoleLocalServiceUtil.hasUserGroupRole(
					userId, scopeGroupId, "Chat Administrator") || PermissionThreadLocal.getPermissionChecker().isGroupAdmin(scopeGroupId);
			_log.debug("isAdmin : " + isAdmin);
	
			if (!isAdmin) {
				return;
			}
			String cmd = ParamUtil.getString(actionRequest, Constants.CMD);
	
			if (!cmd.equals(Constants.UPDATE)) {
				return;
			}
	
			IRCBot ircBot = BotRegistryUtil.getBot(scopeGroupId);
			String botName = ircBot.getBotName();
			String channelName = ircBot.getChannelName();
			String server = ircBot.getServerName();
			int portNumber = ircBot.getPortNumber();
			int pollingInterval = ircBot.getPollingInterval();
			boolean integrateLiferayChat = ircBot.isIntegrateLiferayChat();
			boolean disabled = ircBot.isDisabled();
			
			defaultProcessAction(actionRequest);
	
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
						// This can happen if the portlet was never loaded on a
						// page
						// Ignore since it is not a real issue;
						// The bot would only have to shutdown iff the portlet
						// has been
						// loaded on a page.
					}
				}
	
				BotRegistryUtil.init();
			} else {
				String botNamePref = properties.get(BOT_NAME_PREF);
				String channelNamePref = properties.get(CHANNEL_NAME_PREF);
				String serverPref = properties.get(SERVER_PREF);
				String portNumberPref = properties.get(PORT_NUMBER_PREF);
				String pollingIntervalPref = properties
						.get(POLLING_INTERVAL_PREF);
				String integrateLiferayChatPref = properties
						.get(INTEGRATE_LIFERAY_CHAT);
				String disabledPref = properties.get(DISABLED);
				
				int portNumberPrefNum = GetterUtil.getInteger(portNumberPref);
				boolean integrateLiferayChatPrefBoolean = GetterUtil
						.getBoolean(integrateLiferayChatPref);
				boolean disabledPrefBoolean = GetterUtil.getBoolean(disabledPref);
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
	
				if (server == null || !server.equalsIgnoreCase(serverPref)) {
					_log.debug("Server change request from " + server + " to "
							+ serverPref);
					ircBot.changeServer(serverPref);
				}
	
				if (!botName.equalsIgnoreCase(botNamePref)) {
					_log.debug("Bot name change request from " + botName
							+ " to " + botNamePref);
					ircBot.changeBotName(botNamePref);
				}
	
				if (!channelName.equalsIgnoreCase(channelNamePref)) {
					_log.debug("Channel name change request from "
							+ channelName + " to " + channelNamePref);
					ircBot.changeChannelName(channelNamePref);
				}
				_log.debug("integrateLiferayChat " + integrateLiferayChat + "\t"
						+ "integrateLiferayChatPrefBoolean "
						+ integrateLiferayChatPrefBoolean);
				if (integrateLiferayChat != integrateLiferayChatPrefBoolean) {
					_log.debug("Updated Liferay Chat Portlet Integration switch from "
							+ integrateLiferayChat
							+ " to "
							+ integrateLiferayChatPrefBoolean);
					ircBot.setIntegrateLiferayChat(integrateLiferayChatPrefBoolean);
				}
				if (disabled != disabledPrefBoolean) {
					_log.debug("Updated Disabled switch from "
							+ disabled
							+ " to "
							+ disabledPrefBoolean);
					ircBot.setDisabled(disabledPrefBoolean);
				}
				
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}

	@Override
	public void serveResource(ResourceRequest resourceRequest,
			ResourceResponse resourceResponse) throws IOException,
			PortletException {

		ThemeDisplay themeDisplay = (ThemeDisplay) resourceRequest
				.getAttribute(WebKeys.THEME_DISPLAY);
		if (!themeDisplay.isSignedIn()) {
			return;
		}
		
		long scopeGroupId = themeDisplay.getScopeGroupId();
		IRCBot ircBot = BotRegistryUtil.getBot(scopeGroupId);
		JSONObject json = JSONFactoryUtil.createJSONObject();
		JSONArray mssgs = JSONFactoryUtil.createJSONArray();
		JSONArray users = JSONFactoryUtil.createJSONArray();

		if (!BotRegistryUtil.isInUpdate() && !ircBot.isDisabled()) {
			String message = resourceRequest.getParameter("MSG");
			if (message != null && !message.trim().isEmpty()) {
				_log.debug(message);
			}
			
			if (message != null && message.length() > 0) {
				ircBot.sendMessage(resourceRequest.getPortletSession(),
						resourceRequest.getWindowID(), message);
			}
			List<String> lines = ircBot.fetch(
					resourceRequest.getPortletSession(),
					resourceRequest.getWindowID());

			for (String line : lines) {
				mssgs.put(line);
			}

			List<String> usersList = ircBot.getUsersList();

			for (String user : usersList) {
				users.put(user);
			}
		}

		json.put("data", mssgs);
		json.put("users", users);
		// System.out.println("users size : >>> " + usersList.size());

		resourceResponse.setContentType("application/json");
		resourceResponse.setCharacterEncoding("UTF-8");
		resourceResponse.getWriter().write(json.toString());
	}

	private void setIRCBot(Object request) {

		ThemeDisplay themeDisplay = null;

		if (request instanceof RenderRequest) {
			themeDisplay = (ThemeDisplay) ((RenderRequest) request)
					.getAttribute(WebKeys.THEME_DISPLAY);
		} else if (request instanceof ActionRequest) {

			themeDisplay = (ThemeDisplay) ((ActionRequest) request)
					.getAttribute(WebKeys.THEME_DISPLAY);
		}

		long scopeGroupId = themeDisplay.getScopeGroupId();

		IRCBot ircBot = BotRegistryUtil.getBot(scopeGroupId);
		if (ircBot == null) {
			_log.error("Should not be null - IRC Bot: " + ircBot);

		}

		_log.debug("In group : " + scopeGroupId + " and setting ircBot: "
				+ ircBot);

		if (request instanceof RenderRequest) {
			((RenderRequest) request).setAttribute("ircBot", ircBot);
		} else if (request instanceof ActionRequest) {
			((ActionRequest) request).setAttribute("ircBot", ircBot);
		}

	}

	private void setRenderAttribute(RenderRequest renderRequest) {
		PortletPreferences preferences = renderRequest.getPreferences();
		String portletResource = ParamUtil.getString(renderRequest,
				"portletResource");
		if (Validator.isNotNull(portletResource)) {
			try {
				preferences = PortletPreferencesFactoryUtil.getPortletSetup(
						renderRequest, portletResource);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// String ircLogin =
		// GetterUtil.getString(preferences.getValue("irclogin",
		// IRCBot.getBotName());

		// renderRequest.setAttribute("ircLogin", ircLogin);
	}

	private void setupRoles() {
		try {
			
			String chatAdministrator = "Chat Administrator";
			String chatModerator = "Chat Moderator";
			long defaultCompanyId = PortalUtil.getDefaultCompanyId();

			boolean chatAdministratorExists = false;
			boolean chatModeratorExists = false;
			List<Role> roles = RoleLocalServiceUtil.getRoles(defaultCompanyId);
			for (Role a : roles) {
				
				if (a.getType() != RoleConstants.TYPE_ORGANIZATION) {
					continue;
				}
				if (a.getName().equalsIgnoreCase(chatAdministrator)) {
					chatAdministratorExists = true;
				} else if (a.getName()
						.equalsIgnoreCase(chatModerator)) {
					chatModeratorExists = true;
				}
				if (chatAdministratorExists && chatModeratorExists) {
					break;
				}
			}
			if (!chatAdministratorExists || !chatModeratorExists) {
				Map<Locale, String> titleMap = null;
				Map<Locale, String> descriptionMap = null;
				int type = RoleConstants.TYPE_ORGANIZATION;
				long userid = UserLocalServiceUtil
						.getDefaultUserId(defaultCompanyId);
				ServiceContext serviceContext = new ServiceContext();
				serviceContext.setCompanyId(defaultCompanyId);
				
				if (!chatAdministratorExists) {
					RoleLocalServiceUtil.addRole(userid, null, 0,
							chatAdministrator, titleMap, descriptionMap, type,
							null, serviceContext);
				}
				if (!chatModeratorExists) {
					RoleLocalServiceUtil.addRole(userid, null, 0,
							chatModerator, titleMap, descriptionMap, type,
							null, serviceContext);
				}
			}
			

		} catch (SystemException e) {
			e.printStackTrace();
		} catch (PortalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void defaultProcessAction(ActionRequest actionRequest)
			throws ReadOnlyException, IOException {
		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		if (!cmd.equals(Constants.UPDATE)) {
			return;
		}

		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest
				.getAttribute(WebKeys.THEME_DISPLAY);

		UnicodeProperties properties = PropertiesParamUtil.getProperties(
				actionRequest, PREFERENCES_PREFIX);

		String portletResource = ParamUtil.getString(actionRequest,
				"portletResource");

		Layout layout = PortletConfigurationLayoutUtil.getLayout(themeDisplay);

		PortletPreferences portletPreferences = actionRequest.getPreferences();

		for (Map.Entry<String, String> entry : properties.entrySet()) {
			String name = entry.getKey();
			String value = entry.getValue();

			portletPreferences.setValue(name, value);
		}

		Map<String, String[]> portletPreferencesMap = (Map<String, String[]>) actionRequest
				.getAttribute(WebKeys.PORTLET_PREFERENCES_MAP);

		_log.debug(portletPreferences.getMap().size());
		if (portletPreferencesMap != null) {
			for (Map.Entry<String, String[]> entry : portletPreferencesMap
					.entrySet()) {

				String name = entry.getKey();
				String[] values = entry.getValue();
				_log.debug(name + "\t" + values);
				portletPreferences.setValues(name, values);
			}
		}

		if (SessionErrors.isEmpty(actionRequest)) {
			try {
				portletPreferences.store();
			} catch (ValidatorException ve) {
				SessionErrors.add(actionRequest,
						ValidatorException.class.getName(), ve);

				return;
			}

			SessionMessages.add(actionRequest,
					PortalUtil.getPortletId(actionRequest)
							+ SessionMessages.KEY_SUFFIX_REFRESH_PORTLET,
					portletResource);

			SessionMessages.add(actionRequest,
					PortalUtil.getPortletId(actionRequest)
							+ SessionMessages.KEY_SUFFIX_UPDATED_CONFIGURATION);
		}
	}
}
