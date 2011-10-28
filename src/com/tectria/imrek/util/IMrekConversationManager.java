package com.tectria.imrek.util;

import java.util.HashMap;
import java.util.Vector;

import android.content.Context;
import android.database.Cursor;

public class IMrekConversationManager {

	private static IMrekConversationManager instance;
	private Context context;
	private IMrekChannelDbAdapter channelAdapter;
	private IMrekMessageDbAdapter messageAdapter;
	private IMrekNotificationManager notificationManager;
	private Vector<String> channelList;
	private HashMap<String, Vector<String>> waitingMessages;
	
	protected IMrekConversationManager(Context ctx) {
		this.context = ctx;
		channelAdapter = new IMrekChannelDbAdapter(this.context);
		channelAdapter.open();
		messageAdapter = new IMrekMessageDbAdapter(this.context);
		messageAdapter.open();
		notificationManager = IMrekNotificationManager.getInstance(this.context);
		channelList = new Vector<String>();
		Cursor c = channelAdapter.getChannels();
		while (c.moveToNext()) {
			channelList.add(c.getString(c.getColumnIndex("channel_name")));
		}
		
		waitingMessages = new HashMap<String, Vector<String>>();
		
		// Just for testing data
		channelList.add("channel 1");
		channelList.add("channel 2");
		channelList.add("channel 3");
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
	public synchronized void newMessageReceived(String channel, String payload) {
		String[] message = payload.split(":", 1);
		long msgId = messageAdapter.addMessage(channelAdapter.getChannelId(channel), message[0], message[1]);
		if (waitingMessages.containsKey(channel)) {
			Vector<String> v = waitingMessages.get(channel);
			v.add(payload);
			waitingMessages.put(channel, v);
		} else {
			Vector<String> v = new Vector<String>();
			v.add(payload);
			waitingMessages.put(channel, v);
		}
		// TODO: add to conversation window
	}
	
	// this should be called when a channel comes back into focus. Grabs messages
	// from the database that have been added since the channel was last in focus. 
	public synchronized Vector<String> getChannelUpdate(String channel) {
		if (waitingMessages.containsKey(channel)){
			Vector<String> v = waitingMessages.get(channel);
			waitingMessages.remove(channel);
			return v;
		}
		return new Vector<String>();
		
	}
	
	public synchronized void clearChat(String channel) {
		// Remove messages from database
		messageAdapter.clearChannel(channelAdapter.getChannelId(channel));
		// TODO: clear messages on screen.
	}
	
	public Vector<String> openChannelMessages(String channel) {
		Cursor c = messageAdapter.openChannelMessages(channelAdapter.getChannelId(channel));
		Vector<String> msgs = new Vector<String>();
		for (int i = 0; i < 25; i++) {
			if (!c.moveToNext()) {
				c.close();
				return msgs;
			}
			msgs.add(0, c.getString(c.getColumnIndex("username")).toString() + ": " + c.getString(c.getColumnIndex("message")));
		}
		c.close();
		return msgs;
	}
	
	public Vector<String> getChannelList() {
		return channelList;
	}
	
	// TODO: get actual last messages
	public Vector<String> getChannelsLastMessages() {
		Vector<String> v = new Vector<String>();
		
		for (String channel : channelList) {
			Cursor c = messageAdapter.getMessagesForChannel(channelAdapter.getChannelId(channel));
			if (c.moveToFirst()) {
				v.add(c.getString(c.getColumnIndex("username")).toString() + ": " + c.getString(c.getColumnIndex("message")));
			} else {
				v.add("");
			}
			c.close();
		}
		// Just for testing
		v.add("message 1");
		v.add("message 2");
		v.add("message 3");
		return v;
	}
	
	public void updateChannelList() {
		channelList = null;
		channelList = new Vector<String>();
		Cursor c = channelAdapter.getChannels();
		while (c.moveToNext()) {
			channelList.add(c.getString(c.getColumnIndex("channel_name")));
		}
	}
}
