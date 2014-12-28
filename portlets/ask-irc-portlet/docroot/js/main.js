

AUI().use(
	'anim-color',
	'anim-easing',
	'aui-base',
	'aui-datatype',
	'aui-live-search-deprecated',
	'liferay-poller',
	'stylesheet',
	'swfobject',
	function(A) {
		/**
		 * Liferay Chat Integration Code
		 */
		
		if (typeof(askIRC) == "undefined") {
			return;
		}
		
		if (askIRC.disabled) {
			return;
		}
		

		var lrct = AUI().one('.chat-tabs-container ul.chat-tabs');
		var aip = AUI().one('#_askirc_WAR_askircportlet_irc-results');

		// Return if Liferay Chat is not present
		// since integration option is turned on.
		if (askIRC.isIntegrateLiferayChat && (lrct == null || aip == null)) {
			askIRC.disabled = true;
			return;
		}
		// Return since Liferay Chat is not present
		// or integration option is turned off
		if (lrct === null || aip === null || !askIRC.isIntegrateLiferayChat) {
			var x= A.one('.hide-before-show');
			if (x) {
				x.toggleClass('hide-before-show');
			} else {
				
			}
			AUI().one('window').fire('askircready');
			return;
		}
		
		aip = aip.get('parentNode');

		askIRC.setupIntegration = function() {
			if (askIRC.debug) {
				console.log('Liferay.Chat' + Liferay.Chat);
			}
			
			if (Liferay.Chat === undefined) {
				setTimeout(askIRC.setupIntegration, 1500);
				return;
			}
			
			
			if (askIRC.debug) {
				console.log("liferay chat " + Liferay.Chat);
			}

			if (askIRC.setupCounter == 0) {

				askIRC.setupCounter++;
				var blackhole = $('.blackhole');
				lrct.prepend('<li class="group-chat">' +
						'<div class="panel-trigger" panelid="groupchat">' +
							'<span class="trigger-name">' +
								'Group Chat' +
							'</span>' +
						'</div>' +
						'<div class="chat-panel">' +
							'<div class="panel-window">' +
								'<div class="panel-button minimize">' +
								'</div>' +
								'<div class="panel-title">' +
									'Group Chat' +
								'</div>' +
								'<div class="panel-profile">' +
									'...' + 
								'</div>' +
								'<div class="panel-output">' +
								'</div>' +
								'<div class="panel-input">' +
									'<textarea class="message-input"></textarea>' +
								'</div>' +
								'<div class="panel-content">' +
									//aip.html() +
								'</div>' +
							'</div>' +
						'</div>' +
					'</li>');
				aip.hide();

				askIRC.configuration = $(".group-chat-settings-chat-container");
				if (askIRC.configuration) {
					//alert(askIRC.configuration.html());
					lrct.prepend(askIRC.configuration.html());
					askIRC.configuration.html('');
				}

				var _createGroupChatSession =  function(options) {
					var instance = Liferay.Chat.Manager;

					var userId = options.userId;

					var chat = new Liferay.Chat.Conversation(options);

					instance._addChat(userId, chat);
					instance._addPanel(userId, chat);

					if (instance._entryCache && instance._entryCache[userId]) {
						var entryCache = instance._entryCache[userId];

						var entries = entryCache.entries;

						for (var i in entries) {
							var entry = entries[i];

							var incomingEntry = (entry.fromUserId == userId);

							chat.update(
								{
									cache: entry.flag,
									content: entry.content,
									createDate: entry.createDate,
									incoming: incomingEntry
								}
							);

							entry.flag = 1;
						}
					}

					if (options.open) {
						chat.show();
					}

					return chat;
				};
				
				var _createConfigPanel = function() {
					var instance = Liferay.Chat.Manager;

					var configuration = new Liferay.Chat.Panel(
						{
							fromMarkup: '.group-chat-settings-chat',
							panelId: 'group-chat-settings'
						}
					);

					instance._addPanel('group-chat-settings', configuration);

				};

				askIRC.groupChat = _createGroupChatSession({
					fromMarkup : '.group-chat',
					panelId : 'groupchat',
					panelTitle : 'Group Chat',
					userId : -86,
					panelIcon: '/html/themes/_unstyled/images/common/guest_icon.png',
					statusMessage: ''
				});
				
				var configMarkup = A.one('.group-chat-settings-chat');
				if (configMarkup) {
					askIRC.configuration = _createConfigPanel();
				}
				
				$('.group-chat textarea').on('keyup',function(event) {
					if (event.keyCode == 13 && !event.shifKey) {
						var x = askIRC.groupChatMsg;
						askIRC.sendIRCMsg(x);
						this.value='';
					} else {
						askIRC.groupChatMsg = this.value;
					}
				});
				
				askIRC.isIntegratedWithLiferayChat = true;
				AUI().one('window').fire('askircready');
				
//				console.log("group chat panel: " + groupChatPanel);
//				console.log("setup complete " + askIRC.setupComplete);

				//Liferay.Chat.Manager._addPanel('groupchat', groupChatPanel);

			}

		}
		if (askIRC.debug) {
			console.log('askIRC.isIntegrateLiferayChat: ' + askIRC.isIntegrateLiferayChat);
		}
		if (lrct !== undefined && askIRC.isIntegrateLiferayChat) {

			if (askIRC.debug) {
				console.log('calling askIRC.setupIntegration');
			}
			Liferay.on('allPortletsReady', askIRC.setupIntegration);
			//Liferay.on('domready', askIRC.setupIntegration);

		} else {
			/**
			jQuery('body').append('<div id="ask-irc-chat-portlet-bottom"></div>');
			
			var aib = jQuery('#ask-irc-chat-portlet-bottom');
			aib.html('<div class="aib-chatbar">' +
						'<div class="aib-tabs">' +
							'<ul>' + 
								'<li>One</li>' + 
								'<li>Two</li>' +
							'</ul>' + 
						'</div>' +
					'</div>');
			aib.find('.aib-chatbar').css('position','fixed').css('bottom','0')
				.css('left','15px').css('right','15px').css('height','24px')
				.css('z-index','9999').css('border', '1px solid red');
			aib.find('.aib-chatbar ul').css('list-style-type','none').css('margin','0px')
				.css('padding','0px').css('position','relative');
			aib.find('.aib-chatbar ul li').css('float','left').css('margin','0px')
				.css('padding','0px').css('position','relative').css('line-height','20px');
			aib.find('.aib-chatbar ul li:nth-child(1)')
				.html('<div class="aib-trigger"><span>Group Chat</span></div>'+
					'<div class="aib-panel">' +
						'<div class="aib-window">' +
							'<div class="button">' +
								'<div class="title ask-irc-portlet">' +
									aip.html() +
								'</div>' +
							'</div>' +
						'</div>' +
					'</div>');
			aip.html('');
			//aip.css('display','none');
			aib.find('.aib-chatbar ul li:nth-child(2)')
				.html('<div class="aib-trigger"><span>Settings</span></div>'+
						'<div class="aib-panel">' +
							'<div class="aib-window">' +
								'<div class="button">' +
									'<div class="title">' +
										'Settings' +
									'</div>' +
								'</div>' +
							'</div>' +
						'</div>');
			aib.find('.aib-chatbar ul li .aib-trigger').css('font','bold 1.2em')
				.css('font-weight','700').css('padding','0px 6px').css('float','left')
				.css('border-left','1px solid rgba(255,0,0,0.3)')
				.css('border-right','1px solid rgba(255,0,0,0.3)').css('color','black')
				.css('cursor','pointer');
			aib.find('.aib-chatbar').css('position','fixed').css('bottom','0')
				.css('left','100px').css('right','100px');
			aib.find('.aib-chatbar').css('position','fixed').css('bottom','0')
				.css('left','200px').css('right','200px');
			aib.find('.aib-panel').css('bottom','24px').css('display','none')
				.css('position','absolute').css('left','0px')
				.css('z-index','50');
			aib.find('.aib-window').css('border','1px solid black')
				.css('position','relative').css('min-width','300px')
				.css('max-width','500px').css('background-color','white');
			aib.find('.aib-chatbar ul li:nth-child(1)').click(function() {
				aib.find('.aib-chatbar ul li:nth-child(1)').find('.aib-panel').css('display','block');
			});
			aib.find('.aib-chatbar ul li:nth-child(2)').click(function() {
				aib.find('.aib-chatbar ul li:nth-child(2)').find('.aib-panel').css('display','block');
			});
			

			 **/
		}
		

});
