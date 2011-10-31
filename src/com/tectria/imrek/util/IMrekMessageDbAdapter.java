package com.tectria.imrek.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


public class IMrekMessageDbAdapter {


	public static final String KEY_ID = "_id";
	public static final String KEY_CHANNELID = "channel_id";
	public static final String KEY_USERNAME = "username";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_TIMESTAMP = "timestamp";
	
	private static final String DATABASE_TABLE = "messages";
	private Context context;
	private SQLiteDatabase database;
	private IMrekDatabaseHelper dbhelper;
	
	
	public IMrekMessageDbAdapter(Context context) {
		this.context = context;
	}
	
	public synchronized IMrekMessageDbAdapter open() {
		dbhelper = new IMrekDatabaseHelper(context);
		database = dbhelper.getWritableDatabase();
		return this;
	}
	
	public synchronized void close() {
		database.close();
		dbhelper.close();
	}
	
	public synchronized long addMessage(long channel_id, String username, String message) {
//		if (!database.isOpen()) {
//			this.open();
//		}
		ContentValues cv = new ContentValues();
		cv.put(KEY_CHANNELID, channel_id);
		cv.put(KEY_USERNAME, username);
		cv.put(KEY_MESSAGE, message);
		cv.put(KEY_TIMESTAMP, "NOW()");
		long id = database.insert(DATABASE_TABLE, null, cv);
//		this.close();
		return id;
	}
	
	public synchronized boolean removeMessage(long messageId) {
//		if (!database.isOpen()) {
//			this.open();
//		}
		boolean b = database.delete(DATABASE_TABLE, KEY_ID + " = ?", new String[]{((Long)messageId).toString()}) > 0;
//		this.close();
		return b;
	}
	
	public synchronized Cursor getMessagesForChannel(long channel_id) {
//		if (!database.isOpen()) {
//			this.open();
//		}
		Cursor c = database.query(DATABASE_TABLE, null, KEY_CHANNELID + " = ?", new String[]{((Long)channel_id).toString()}, null, null, null);
//		this.close();
		return c;
	}
	
	public synchronized Cursor getMessagesSince(long channel_id, Long messageId) {
//		if (!database.isOpen()) {
//			this.open();
//		}
		Cursor c = database.query(DATABASE_TABLE, null, KEY_CHANNELID + " = ? AND " + KEY_ID + " > ? ", new String[]{((Long)channel_id).toString(), messageId.toString()}, null, null, null);
//		this.close();
		return c;
	}
	
	public synchronized boolean clearChannel(long channel_id) {
//		if (!database.isOpen()) {
//			this.open();
//		}
		boolean b = database.delete(DATABASE_TABLE, KEY_CHANNELID + " = ? ", new String[]{((Long)channel_id).toString()}) > 0;
//		this.close();
		return b;
	}
	
	public synchronized Cursor openChannelMessages(long channel_id) {
//		if (!database.isOpen()) {
//			this.open();
//		}
		Cursor c = database.query(DATABASE_TABLE, null, KEY_CHANNELID + " = ? ", new String[]{((Long)channel_id).toString()}, null,null, KEY_CHANNELID + " DESC");
//		this.close();
		return c;
	}
}
