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
	
	public IMrekMessageDbAdapter open() {
		dbhelper = new IMrekDatabaseHelper(context);
		database = dbhelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		database.close();
		dbhelper.close();
	}
	
	public long addMessage(long channel_id, String username, String message) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_CHANNELID, channel_id);
		cv.put(KEY_USERNAME, username);
		cv.put(KEY_MESSAGE, message);
		cv.put(KEY_TIMESTAMP, "NOW()");
		return database.insert(DATABASE_TABLE, null, cv);
	}
	
	public boolean removeMessage(long messageId) {
		// TODO: cascade delete to any messages stored for this channel
		return database.delete(DATABASE_TABLE, KEY_ID + " = ?", new String[]{((Long)messageId).toString()}) > 0;
	}
	
	public Cursor getMessagesForChannel(long channel_id) {
		return database.query(DATABASE_TABLE, null, KEY_CHANNELID + " = ?", new String[]{((Long)channel_id).toString()}, null, null, null);
	}
	
	public Cursor getMessagesSince(long channel_id, Long messageId) {
		return database.query(DATABASE_TABLE, null, KEY_CHANNELID + " = ? AND " + KEY_ID + " > ? ", new String[]{((Long)channel_id).toString(), messageId.toString()}, null, null, null);
	}
	
	public boolean clearChannel(long channel_id) {
		return database.delete(DATABASE_TABLE, KEY_CHANNELID + " = ? ", new String[]{((Long)channel_id).toString()}) > 0;
	}
	
	public Cursor openChannelMessages(long channel_id) {
		return database.query(DATABASE_TABLE, null, KEY_CHANNELID + " = ? ", new String[]{((Long)channel_id).toString()}, null,null, KEY_CHANNELID + " DESC");
	}
}
