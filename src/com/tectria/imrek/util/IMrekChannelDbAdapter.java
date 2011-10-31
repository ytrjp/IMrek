package com.tectria.imrek.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class IMrekChannelDbAdapter {

	public static final String KEY_ID = "_id";
	public static final String KEY_CHANNELNAME = "channel_name";
	
	private static final String DATABASE_TABLE = "channels";
	private Context context;
	private SQLiteDatabase database;
	private IMrekDatabaseHelper dbhelper;
	
	
	public IMrekChannelDbAdapter(Context context) {
		this.context = context;
	}
	
	public synchronized IMrekChannelDbAdapter open() {
		dbhelper = new IMrekDatabaseHelper(context);
		database = dbhelper.getWritableDatabase();
		return this;
	}
	
	public synchronized void close() {
		database.close();
		dbhelper.close();
	}
	
	public synchronized long addChannel(String channel_name) {
//		if (!database.isOpen()) {
//			this.open();
//		}
		ContentValues cv = new ContentValues();
		cv.put(KEY_CHANNELNAME, channel_name);
		long id = database.insert(DATABASE_TABLE, null, cv);
//		this.close();
		return id;
	}
	
	public synchronized boolean removeChannel(String channel_name) {
		IMrekMessageDbAdapter messageAdapter = new IMrekMessageDbAdapter(this.context);
		messageAdapter.open();
		long id = getChannelId(channel_name);
		boolean ret;
		if (!messageAdapter.clearChannel(id)) {
			// return false if we can't clear the messages for the channel
			// this likely means that the channel doesn't actually exist and something
			// is messed up.
			ret = false;
		} else {
			ret = database.delete(DATABASE_TABLE, KEY_ID + " = ?", new String[]{((Long)id).toString()}) > 0;
		}
		return ret;
	}
	
	public synchronized Cursor getChannels() {
		Cursor c = database.query(DATABASE_TABLE, null, null, null, null, null, null);
		return c;
	}
	
	public synchronized long getChannelId(String name) {
		Cursor c = database.query(DATABASE_TABLE, new String[]{KEY_ID}, KEY_CHANNELNAME + " = ? ", new String[]{name}, null, null, null);
		long ret;
		if (!c.moveToFirst()) {
			// if there's no results or an error, return -1
			ret = -1L;
		}
		ret = c.getLong(c.getColumnIndex(KEY_ID));
		c.close();
		return ret;
	}
	
	public synchronized String getChannelName(long id) {
		Cursor c = database.query(DATABASE_TABLE, new String[]{KEY_CHANNELNAME}, KEY_ID + " = ? ", new String[]{((Long)id).toString()}, null, null, null);
		String s;
		if (!c.moveToFirst()) {
			// If it doesn't come up with anything, return null	
			s = null;
		}
		s = c.getString(0);
		c.close();
		return s;
	}
}
