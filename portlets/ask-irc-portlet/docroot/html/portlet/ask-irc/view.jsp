<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="com.bvakili.portlet.askirc.util.IRCBot"%>
<%@ include file="init.jspf"%>

<liferay-portlet:resourceURL var="update" />

<span class="hide-before-show">
<%
boolean isControlPanel = layout.isTypeControlPanel();
if (isControlPanel) {

}
String input_title = "Ask questions regarding " + topic + " below.";
IRCBot ircBot = (IRCBot)request.getAttribute("ircBot");
if (!ircBot.isDisabled()) {
	if (showTopic) {
%>
<h6><%= input_title %> (<%= ircBot.getServerName() %> <%= ircBot.getChannelName() %> channel.)</h6>
<%	
	}
%>
<div class='ask-irc-wrapper'>
<div id="<portlet:namespace />irc-errors" class="irc-errors">
</div>
<div title="IRC channel chat window display." id="<portlet:namespace />irc-results" class="irc-results">
</div>
<aui:input title="<%= input_title %>" placeholder="Type chat message here and press Enter" wrapperCssClass="aip-input-text-height" name="message" type="text" cssClass="irc-input" label="" onKeyPress="if (event.keyCode == 13) { askIRCRef.sendIRCMsg(this.value); this.value=''; }"/>
<div id="<portlet:namespace />irc-users-count" class="irc-users-count">
</div>
</div>
<div class='blackhole' style="display: none">

</div>
<%
}
%>
<script type="text/javascript">
var askIRC = new Object();

</script>
<aui:script position="inline" use="aui-io-request,aui-modal">
window.askIRCRef = askIRC;
askIRC.connLost = false;
askIRC.disabled = <%= ircBot.isDisabled() %>;
askIRC.connLostCount = 0;
askIRC.portletNamespace = '<portlet:namespace />';
askIRC.timeout;
askIRC.userFullName = '<%= user.getFullName() %>';
askIRC.userId = '<%= user.getUserId() %>';
askIRC.debug = false;
askIRC.isIntegratedWithLiferayChat = false, askIRC.groupChat = null, askIRC.groupChatMsg = '';
askIRC.isIntegrateLiferayChat = <%= ircBot.isIntegrateLiferayChat() %>;
askIRC.showUsersList = <%= showUsersList %>;
askIRC.showInitMessage = <%= showInitMessage %>;
askIRC.showTopic = <%= showTopic %>;
askIRC.usersListDialog;
askIRC.usersList;
askIRC.initMessage = !askIRC.showInitMessage;
askIRC.setupComplete = false, askIRC.setupCounter = 0;
askIRC.INIT_MESSAGE_CONSTANT = 3;
askIRC.refreshCounter = 0;
askIRC.pollingInterval= <%= (ircBot != null? String.valueOf(ircBot.getPollingInterval()) : "2000") %>;
askIRC.appendToResults = function(x) {
	if (askIRC.debug) {
		console.log('appendToResults ' + x);
	}
	if (askIRC.isIntegrateLiferayChat) {
		var now = Liferay.Chat.Util.getCurrentTimestamp();
		var entry = {
				content : x,
				incoming: 'incoming',
				createDate: now
		};
		askIRC.groupChat.update(entry);
	} else {
		z1 = document.getElementById("<portlet:namespace />irc-results");
		z = AUI().one("#<portlet:namespace />irc-results");
		z.append('<div class="irc-chat-row">' + x + '</div>');
		if (askIRC.debug) {
			console.log("Appended " + x);
		}
		z1.scrollTop = z1.scrollHeight;
	}

}
askIRC.showDialog = function() {
	askIRC.usersListDialog.show();
}
askIRC.setUsers = function(x, y) {
	var usersMessageEl = AUI().one("#<portlet:namespace />users-message");
	var umel = (usersMessageEl ? usersMessageEl.html().length : 0);
	if (umel <= 0) {
		askIRC.usersListDialog = new A.Modal({
			bodyContent: x,
			centered: true,
			headerContent: '<h3>Users List</h3>',
			modal: false,
	        resizable: {
	            handles: 'b, r'
	          },
			/**draggable: true, **/
			render: '#<portlet:namespace />irc-modal',
			width: 450
		}).render();
		askIRC.usersListDialog.hide();
		z = AUI().one("#<portlet:namespace />irc-users-count");
		z.html("<div id='<portlet:namespace />users-message'><a href='javascript:askIRCRef.showDialog()'>There are " + y.length + " users in the channel.</a></div>");
	} else {
		var usersListDialogEl = AUI().one("#<portlet:namespace />users-list-dialog");
		usersMessageEl.html("<a href='javascript:askIRCRef.showDialog()'>There are " + y.length + " users in the channel.</a>");
		usersListDialogEl.html(x);
	}
	askIRC.usersList = y;
}
askIRC.checkUndeploy = function() {
	errorsEl = AUI().one("#<portlet:namespace />irc-errors");
	if (errorsEl == null) {
		return true;
	}
	return false;

}
askIRC.undeployed = function() {
	clearTimeout(askIRC.timeout);
	askIRC.connLost = true;
	if (askIRC.connLostCount == 0) {
		askIRC.connLostCount = 60;
	} else {
		askIRC.connLostCount = askIRC.connLostCount * 3;
	}
}
askIRC.lostConn = function() {
	askIRC.connLost = true;
	if (askIRC.connLostCount == 0) {
		askIRC.connLostCount = 1;
	} else {
		askIRC.connLostCount = askIRC.connLostCount * 2;
	}
}
askIRC.request = function(x) {

	if (askIRC.checkUndeploy()) {
		clearTimeout(askIRC.timeout);
		return;
	}
	if (askIRC.debug) {
		console.log('test');
	}
	A.io.request(
		'${update}',
		{
			data: {
				<portlet:namespace/>MSG: x
			}, 
			dataType: 'json',
			after: {
				failure: function(event, id, obj) {
					askIRC.undeployed();
					z = AUI().one("#<portlet:namespace />irc-errors");
					z.html("This plugin's no longer available. An admin may have just undeployed it.");
					return;
				},
				success: function(event, id, obj) {
					var response;
					try {
						response = A.JSON.parse(obj.responseText);
						if (askIRC.connLost) {
							askIRC.connLost = false;
							z = AUI().one("#<portlet:namespace />irc-errors");
							z.html("");
						}
					} catch(e) {
						z = AUI().one("#<portlet:namespace />irc-errors");
						if (obj.status == 302) {
							askIRC.lostConn();
							z.html("Connection has been lost; will try again. Please note you must be logged in to use this.");
						} else if (obj.status == 200) {
							askIRC.undeployed();
							z.html("This plugin's no longer available. It has been undeployed by an admin.");
							return;
						}
					}
					if (response) {
							var x = response.data;
							var y = response.users;
							if (askIRC.debug) {
								console.log("data: " + x);
							}
							if (askIRC.showUsersList) {
								
								if (askIRC.refreshCounter == 0) {
									users = '<div id="<portlet:namespace />users-list-dialog" class="users-list-dialog">';								
								} else {
									users = '';
								}

								for (i = 0; i < y.length; i++) {
									users += '<div class="users-list-row">' + y[i] + '</div>';
								}	
								if (askIRC.refreshCounter == 0) {
									users += '</div>';
								} else {
								}
								askIRC.setUsers(users, y);	
								
							}

							askIRC.refreshCounter++;
							
							if (!askIRC.initMessage && askIRC.refreshCounter > askIRC.INIT_MESSAGE_CONSTANT) {
								askIRC.appendToResults(y.length + " users in the <%= ircBot.getChannelName() %> channel:");
								var usersList = "";
								for (i = 0; i < y.length; i++) {
									if (usersList.length > 0 && i < y.length) {
										usersList += ", ";
									}
									usersList += y[i];
								}
								askIRC.appendToResults(usersList);
								askIRC.initMessage = true;
							}
							
							for (i = 0; i < x.length; i++) {
								if (askIRC.debug) {
									console.log('isIntegrateLiferayChat ' + askIRC.isIntegrateLiferayChat + " xyz: " + (x[i].indexOf("<span class='chat-user-id'>" + askIRC.userId + "</span>") != 0));
								}
								if (askIRC.isIntegrateLiferayChat && x[i].indexOf("<span class='chat-user-id'>" + askIRC.userId + "</span>") != 0) {
									
								} else {
									askIRC.appendToResults(x[i]);
								}
							}
					}
				}
			}
		}
	);
};
askIRC.fetchIRCUpdate = function() {
	if (askIRC.disabled) {
		return;
	}
	askIRC.request(null);
	clearTimeout(askIRC.timeout);
	if (!askIRC.connLost) {
		clearTimeout(askIRC.timeout);
		askIRC.timeout = setTimeout(askIRC.fetchIRCUpdate, askIRC.pollingInterval);
	} else { 
		askIRC.timeout = setTimeout(askIRC.fetchIRCUpdate, 13200 * askIRC.connLostCount);
	}
}
askIRC.sendIRCMsg = function(x) {
	askIRC.request(x);
}

if (!askIRC.disabled) {
	askIRC.timeout = setTimeout(askIRC.fetchIRCUpdate, askIRC.pollingInterval);
	
	AUI().one("body").append("<div id='<portlet:namespace />irc-users' class='irc-users'><div id='<portlet:namespace />irc-modal' class='irc-modal'></div></div>");
	
	if (typeof(Storage)!="undefined") {
		
		var listener = function() {
			var content = "";
			if (askIRC.isIntegrateLiferayChat) {
				content = AUI().one('.group-chat .panel-output');
			} else {
				content = AUI().one('.irc-results');
			}
			if (content) {
				content = content.getHTML();
				sessionStorage.setItem('group-chat-panel-output-html',content);
			}
				
		};
		var loadListener = function() {
			var content = sessionStorage.getItem('group-chat-panel-output-html');
			var c2;
			if (askIRC.isIntegrateLiferayChat) {
				c2 = AUI().one('.group-chat .panel-output');
				
			} else {
				c2 = AUI().one('.irc-results');
			}
			if (c2) {
				c2.setHTML(content);
			}
			if (askIRC.debug) {
				console.log("found content and loaded it "+ content);
			}
		};
		AUI().one('window').on('beforeunload', listener);
		AUI().one('window').on('askircready',loadListener);
		if (askIRC.debug) {
			console.log('setup all listeners');
		}

		if (askIRC.debug) {
			console.log('called fire');
		}
	}
}

</aui:script>

<%
if (isAdmin && !ircBot.isDisabled()) {

%>
<div style="display:none" class="group-chat-settings-chat-container">
<li class="group-chat-settings-chat">
	<div class="panel-trigger" panelId="group-chat-settings">
		<span class="trigger-name"><liferay-ui:message key="group-chat-settings" /></span>
	</div>
	<div class="chat-panel">
		<div class="panel-window">
			<div class="minimize panel-button"></div>
	
			<div class="panel-title"><liferay-ui:message key="configuration" /></div>
	
			<ul class="lfr-component group-chat-settings">
				<iframe class="edit-settings" src="<liferay-portlet:actionURL windowState="pop_up" portletMode="EDIT"/>" ></iframe>
				<a href="#" onclick="Liferay.Portlet.openWindow('#p_p_id_askirc_WAR_askircportlet_','askirc_WAR_askircportlet','<liferay-portlet:actionURL windowState="pop_up" portletMode="EDIT"/>','_askirc_WAR_askircportlet_','<liferay-ui:message key="group-chat-settings" />'); return false;">Pop-out</a>				
								
			</ul>
	
			<div class="ctrl-holder">
				
			</div>
		</div>
	</div>

	
</li>
</div>
<%
}
%>

</span>