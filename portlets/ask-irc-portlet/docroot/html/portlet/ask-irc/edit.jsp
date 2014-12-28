
<%@page import="com.liferay.portal.kernel.util.Constants"%>
<%@page import="com.liferay.portal.service.UserGroupRoleLocalServiceUtil"%>
<%@ include file="init.jspf"%>

<%
String redirect = ParamUtil.getString(request, "redirect");
IRCBot ircBot = (IRCBot)request.getAttribute("ircBot");
String serverName = (ircBot != null? ircBot.getServerName() : com.bvakili.portlet.askirc.util.Constants.DEFAULT_SERVER);
String disabled = (ircBot != null? String.valueOf(ircBot.isDisabled()) : "true");
String portNumber = (ircBot != null? String.valueOf(ircBot.getPortNumber()) : String.valueOf(com.bvakili.portlet.askirc.util.Constants.DEFAULT_PORT_NUMBER));
String pollingInterval = (ircBot != null? String.valueOf(ircBot.getPollingInterval()) : String.valueOf(com.bvakili.portlet.askirc.util.Constants.DEFAULT_POLLING_INTERVAL));
String botName = (ircBot != null? ircBot.getBotName() : com.bvakili.portlet.askirc.util.Constants.DEFAULT_BOT_NAME);
String channelName = (ircBot != null? ircBot.getChannelName() : com.bvakili.portlet.askirc.util.Constants.DEFAULT_CHANNEL_NAME);
String isIntegrateLiferayChat = (ircBot != null? String.valueOf(ircBot.isIntegrateLiferayChat()) : String.valueOf(com.bvakili.portlet.askirc.util.Constants.DEFAULT_INTEGRATE_LIFERAY_CHAT));

if (isAdmin) {
%>
<script type="text/javascript">
var askIRC = new Object();

</script>

<liferay-portlet:actionURL portletMode="edit" var="configurationURL" />
<aui:form action="<%= configurationURL %>" method="post" name="fm">
	<aui:input name="<%= Constants.CMD %>" type="hidden" value="<%= Constants.UPDATE %>" />
        <aui:input name="redirect" type="hidden" value="<%= redirect %>" />

	<aui:fieldset>
		<aui:input name="preferences--server--" label="server-label" type="text" value="<%= serverName %>" >
			<aui:validator name="required"></aui:validator>
		</aui:input>
		<aui:input name="preferences--portNumber--" label="port-number-label" helpMessage="port-number-help" type="text" value="<%= portNumber %>" >
			<aui:validator name="required"></aui:validator>
			<aui:validator name="number"></aui:validator>
			<aui:validator errorMessage="please-enter-a-valid-port-number" name="range">[1,65535]</aui:validator>
		</aui:input>

		<aui:input name="preferences--botName--" label="bot-name-label" helpMessage="bot-name-help" type="text" value="<%= botName %>" >
			<aui:validator name="required"></aui:validator>
		</aui:input>
		<aui:input name="preferences--channelName--" label="channel-name-label" helpMessage="channel-name-help" type="text" value="<%= channelName %>" >
			<aui:validator name="required"></aui:validator>
		</aui:input>
		<aui:input name="preferences--topic--" label="topic-name-label" helpMessage="topic-name-help" type="text" value="<%= topic %>" ></aui:input>
		<aui:input name="preferences--showTopic--" label="show-topic-label" helpMessage="show-topic-help" type="checkbox" value="<%= showTopic %>" ></aui:input>
		<aui:input name="preferences--integratelr--" label="integrate-liferay-chat-label" helpMessage="integrate-liferay-chat-help" type="checkbox" value="<%= isIntegrateLiferayChat %>" ></aui:input>
		<aui:input name="preferences--ircchatdisabled--" label="irc-chat-disabled-label" helpMessage="irc-chat-disabled-help" type="checkbox" value="<%= disabled %>" ></aui:input>
		<aui:input name="preferences--showInitMessage--" label="show-init-message-label" helpMessage="show-init-message-help" type="checkbox" value="<%= showInitMessage %>" ></aui:input>
		<aui:input name="preferences--showUsersList--" label="show-users-list-label" helpMessage="show-users-list-help" type="checkbox" value="<%= showUsersList %>" ></aui:input>
		<aui:input name="preferences--pollingInterval--" label="polling-interval-label" helpMessage="polling-interval-help" type="text" value="<%= pollingInterval %>" >
			<aui:validator name="required"></aui:validator>
			<aui:validator name="number"></aui:validator>
			<aui:validator errorMessage="please-enter-a-valid-polling-interval" name="range">[200,1000000]</aui:validator>
		</aui:input>

	</aui:fieldset>
        <aui:button-row>
                <aui:button type="submit" />
        </aui:button-row>
</aui:form>
<%
}
%>