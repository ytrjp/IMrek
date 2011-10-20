package com.tectria.imrek.util;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;

public class IMrekConversationManager {

	private static IMrekConversationManager instance;
	private Context context;
	private IMrekChannelDbAdapter channelAdapter;
	private IMrekMessageDbAdapter messageAdapter;
	private HashMap<String, Long> lastMessageMap;		// Keep a map of the last message ID for each channel
	
	protected IMrekConversationManager(Context ctx) {
		this.context = ctx;
		channelAdapter = new IMrekChannelDbAdapter(this.context);
		channelAdapter.open();
		messageAdapter = new IMrekMessageDbAdapter(this.context);
		messageAdapter.open();
		lastMessageMap = new HashMap<String, Long>();
	}
	
	// Get singleton instance
	public static IMrekConversationManager getInstance(Context ctx) {
		if (instance == null) {
			instance = new IMrekConversationManager(ctx);
		}
		return instance;
	}
	
	// Call when a new message comes in
	// Pass channel/topic, message payload, and whether the channel is currently in focus
	public void newMessageReceived(String channel, String payload, boolean inFocus) {
		String[] message = payload.split(":", 1);
		long msgId = messageAdapter.addMessage(channelAdapter.getChannelId(channel), message[0], message[1]);
		// inFocus marks whether or not the channel the messages is being added to is currently in focus
		if (inFocus) {
			lastMessageMap.put(channel, msgId);
		}
		// TODO: add to conversation window
	}
	
	// this should be called when a channel comes back into focus. Grabs messages
	// from the database that have been added since the channel was last in focus. 
	public ArrayList<String> getChannelUpdate(String channel) {
		Cursor c = messageAdapter.getMessagesSince(channelAdapter.getChannelId(channel), lastMessageMap.get(channel));
		ArrayList<String> msgs = new ArrayList<String>();
		while (c.moveToNext()) {
			msgs.add(c.getString(c.getColumnIndex("username")).toString().concat(c.getString(c.getColumnIndex("message"))));
		}
		if (msgs.size() == 0) {
			return null;
		} else {
			return msgs;
		}
	}
	
	public void clearChat(String channel) {
		// Remove messages from database
		messageAdapter.clearChannel(channelAdapter.getChannelId(channel));
		// TODO: clear messages on screen.
	}
}
