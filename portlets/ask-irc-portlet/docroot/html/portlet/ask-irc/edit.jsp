<%@page import="com.liferay.portal.service.UserGroupRoleLocalServiceUtil"%>
<%@ page import="com.liferay.portal.kernel.util.Constants" %> 
<%@ include file="init.jspf"%>

<%
String redirect = ParamUtil.getString(request, "redirect");
IRCBot ircBot = (IRCBot)request.getAttribute("ircBot");


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
		<aui:input name="preferences--server--" label="server-label" type="text" value="<%= ircBot.getServerName() %>" >
			<aui:validator name="required"></aui:validator>
		</aui:input>
		<aui:input name="preferences--portNumber--" label="port-number-label" helpMessage="port-number-help" type="text" value="<%= ircBot.getPortNumber() %>" >
			<aui:validator name="required"></aui:validator>
			<aui:validator name="number"></aui:validator>
			<aui:validator errorMessage="please-enter-a-valid-port-number" name="range">[1,65535]</aui:validator>
		</aui:input>

		<aui:input name="preferences--botName--" label="bot-name-label" helpMessage="bot-name-help" type="text" value="<%= ircBot.getBotName() %>" >
			<aui:validator name="required"></aui:validator>
		</aui:input>
		<aui:input name="preferences--channelName--" label="channel-name-label" helpMessage="channel-name-help" type="text" value="<%= ircBot.getChannelName() %>" >
			<aui:validator name="required"></aui:validator>
		</aui:input>
		<aui:input name="preferences--topic--" label="topic-name-label" helpMessage="topic-name-help" type="text" value="<%= topic %>" ></aui:input>
		<aui:input name="preferences--showTopic--" label="show-topic-label" helpMessage="show-topic-help" type="checkbox" value="<%= showTopic %>" ></aui:input>
		<aui:input name="preferences--integratelr--" label="integrate-liferay-chat-label" helpMessage="integrate-liferay-chat-help" type="checkbox" value="<%= ircBot.isIntegrateLiferayChat() %>" ></aui:input>
		<aui:input name="preferences--ircchatdisabled--" label="irc-chat-disabled-label" helpMessage="irc-chat-disabled-help" type="checkbox" value="<%= ircBot.isDisabled() %>" ></aui:input>
		<aui:input name="preferences--showInitMessage--" label="show-init-message-label" helpMessage="show-init-message-help" type="checkbox" value="<%= showInitMessage %>" ></aui:input>
		<aui:input name="preferences--showUsersList--" label="show-users-list-label" helpMessage="show-users-list-help" type="checkbox" value="<%= showUsersList %>" ></aui:input>
		<aui:input name="preferences--pollingInterval--" label="polling-interval-label" helpMessage="polling-interval-help" type="text" value="<%= ircBot.getPollingInterval() %>" >
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