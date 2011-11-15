package com.tectria.imrek.util;

import java.util.ArrayList;
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
		c.close();
		waitingMessages = new HashMap<String, Vector<String>>();
		
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
		String[] message = payload.split(":", 2);
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
	}
	
	// this should be called when a channel comes back into focus. Grabs messages
	// from the database that have been added since the channel was last in focus. 
	public synchronized ArrayList<HashMap<String, String>> getChannelUpdate(String channel) {
		if (waitingMessages.containsKey(channel)){
			Vector<String> v = waitingMessages.remove(channel);
			ArrayList<HashMap<String, String>> msgs = new ArrayList<HashMap<String, String>>();
			for (String s : v) {
				HashMap<String, String> m = new HashMap<String, String>();
				String[] str = s.split(":", 2);
				m.put("name", str[0]);
				m.put("message", str[1]);
				msgs.add(m);
			}
			return msgs;
		}
		return new ArrayList<HashMap<String, String>>();
		
	}
	
	public synchronized void clearChat(String channel) {
		// Remove messages from database
		messageAdapter.clearChannel(channelAdapter.getChannelId(channel));
		// TODO: clear messages on screen.
	}
	
	public synchronized ArrayList<HashMap<String, String>> openChannelMessages(String channel) {
		Cursor c = messageAdapter.openChannelMessages(channelAdapter.getChannelId(channel));
		ArrayList<HashMap<String, String>> msgs = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < 25; i++) {
			if (!c.moveToNext()) {
				c.close();
				return msgs;
			}
			HashMap<String, String> m = new HashMap<String, String>();
			//.add(0, c.getString(c.getColumnIndex("username")).toString() + ": " + c.getString(c.getColumnIndex("message")));
			m.put("name", c.getString(c.getColumnIndex("username")).toString());
			m.put("message", c.getString(c.getColumnIndex("message")).toString());
			msgs.add(m);
		}
		c.close();
		return msgs;
	}
	
	public synchronized Vector<String> getChannelList() {
		return channelList;
	}
	
	public synchronized ArrayList<HashMap<String, String>> getChannelsLastMessages() {
		ArrayList<HashMap<String, String>> msgs = new ArrayList<HashMap<String, String>>();
		
		for (String channel : channelList) {
			Cursor c = messageAdapter.getMessagesForChannel(channelAdapter.getChannelId(channel));
			HashMap<String, String> m = new HashMap<String, String>();
			m.put("channel", channel);
			if (c.moveToFirst()) {
				m.put("message", c.getString(c.getColumnIndex("username")).toString() + ": "+c.getString(c.getColumnIndex("message")).toString());
			} else {
				m.put("message", "");
			}
			c.close();
			msgs.add(m);
		}
		return msgs;
	}
	
	public synchronized void updateChannelList() {
		channelList = null;
		channelList = new Vector<String>();
		Cursor c = channelAdapter.getChannels();
		while (c.moveToNext()) {
			channelList.add(c.getString(c.getColumnIndex("channel_name")));
		}
		c.close();
	} 
	
	public synchronized void addChannel(String channel_name) {
		if (!channelList.contains(channel_name)) {
			channelAdapter.addChannel(channel_name);
			channelList.add(channel_name);
		}
	}
	
	public synchronized boolean removeChannel(String channel_name) {
		boolean ret = false;
		if (channelList.contains(channel_name)) {
			channelList.remove(channelList.indexOf(channel_name));
			ret = channelAdapter.removeChannel(channel_name);
		}
		return ret;
	}
}
