package org.pircbotx;

import java.util.HashMap;
import java.util.Map;

import org.pircbotx.hooks.events.DisconnectEvent;

public class PircBotXPatched extends PircBotX {

	@Override
	public void shutdown(boolean noReconnect) {
		try {
			outputThread.interrupt();
			inputThread.interrupt();
		} catch (Exception e) {
			logException(e);
		}

		// Close the socket from here and let the threads die
		if (!socket.isClosed())
			try {
				socket.shutdownInput();
				socket.close();
			} catch (Exception e) {
				logException(e);
			}

		// Close the DCC Manager
		try {
			dccManager.close();
		} catch (Exception ex) {
			// Not much we can do with it here. And throwing it would not let
			// other things shutdown
			logException(ex);
		}

		// Cache channels for possible next reconnect
		Map<String, String> previousChannels = new HashMap();
//		for (Channel curChannel : getChannels()) {
//			String key = (curChannel.getChannelKey() == null) ? "" : curChannel
//					.getChannelKey();
//			previousChannels.put(curChannel.getName(), key);
//		}

		// Clear relevant variables of information
		userChanInfo.clear();
		userNickMap.clear();
		channelListBuilder.finish();

		// Dispatch event
		if (autoReconnect && !noReconnect)
			try {
				reconnect();
				if (autoReconnectChannels)
					for (Map.Entry<String, String> curEntry : previousChannels
							.entrySet())
						joinChannel(curEntry.getKey(), curEntry.getValue());
			} catch (Exception e) {
				// Not much we can do with it
				throw new RuntimeException("Can't reconnect to server", e);
			}
		else {
			getListenerManager().dispatchEvent(new DisconnectEvent(this));
			log("*** Disconnected.");
		}
	}
}
