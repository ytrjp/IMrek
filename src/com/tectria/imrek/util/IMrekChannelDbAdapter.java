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
	
	public IMrekChannelDbAdapter open() {
		dbhelper = new IMrekDatabaseHelper(context);
		database = dbhelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		database.close();
		dbhelper.close();
	}
	
	public long addChannel(String channel_name) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_CHANNELNAME, channel_name);
		return database.insert(DATABASE_TABLE, null, cv);
	}
	
	public boolean removeChannel(String channel_name) {
		IMrekMessageDbAdapter messageAdapter = new IMrekMessageDbAdapter(this.context);
		messageAdapter.open();
		if (!messageAdapter.clearChannel(getChannelId(channel_name))) {
			// return false if we can't clear the messages for the channel
			// this likely means that the channel doesn't actually exist and something
			// is messed up.
			return false;
		}
		return database.delete(DATABASE_TABLE, KEY_CHANNELNAME + " = ?", new String[]{channel_name}) > 0;
	}
	
	public Cursor getChannels() {
		return database.query(DATABASE_TABLE, null, null, null, null, null, null);
	}
	
	public long getChannelId(String name) {
		Cursor c = database.query(DATABASE_TABLE, new String[]{KEY_ID}, KEY_CHANNELNAME + " = ? ", new String[]{name}, null, null, null);
		if (!c.moveToFirst()) {
			// if there's no results or an error, return -1
			return -1;
		} 
		
		return c.getLong(0);
	}
	
	public String getChannelName(long id) {
		Cursor c = database.query(DATABASE_TABLE, new String[]{KEY_CHANNELNAME}, KEY_ID + " = ? ", new String[]{((Long)id).toString()}, null, null, null);
		if (!c.moveToFirst()) {
			// If it doesn't come up with anything, return null	
			return null;
		}
		return c.getString(0);
	}
}
