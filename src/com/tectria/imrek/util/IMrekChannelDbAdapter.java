package com.tectria.imrek.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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
		if (database == null || dbhelper == null) {
			this.open();
		}
		ContentValues cv = new ContentValues();
		cv.put(KEY_CHANNELNAME, channel_name);
		long id = database.insert(DATABASE_TABLE, null, cv);
		this.close();
		return id;
	}
	
	public boolean removeChannel(String channel_name) {
		if (database == null || dbhelper == null) {
			this.open();
		}
		IMrekMessageDbAdapter messageAdapter = new IMrekMessageDbAdapter(this.context);
		messageAdapter.open();
		long id = getChannelId(channel_name);
		if (!messageAdapter.clearChannel(id)) {
			// return false if we can't clear the messages for the channel
			// this likely means that the channel doesn't actually exist and something
			// is messed up.
			return false;
		}
		boolean ret = database.delete(DATABASE_TABLE, KEY_ID + " = ?", new String[]{((Long)id).toString()}) > 0;
		this.close();
		return ret;
	}
	
	public Cursor getChannels() {
		if (database == null || dbhelper == null) {
			this.open();
		}
		Cursor c = database.query(DATABASE_TABLE, null, null, null, null, null, null);
		this.close();
		return c;
	}
	
	public long getChannelId(String name) {
		if (database == null || dbhelper == null) {
			this.open();
		}
		Cursor c = database.query(DATABASE_TABLE, new String[]{KEY_ID}, KEY_CHANNELNAME + " = ? ", new String[]{name}, null, null, null);
		if (!c.moveToFirst()) {
			// if there's no results or an error, return -1
			return -1;
		}
		long i = c.getLong(c.getColumnIndex(KEY_ID));
		c.close();
		this.close();
		return i;
	}
	
	public String getChannelName(long id) {
		if (database == null || dbhelper == null) {
			this.open();
		}
		Cursor c = database.query(DATABASE_TABLE, new String[]{KEY_CHANNELNAME}, KEY_ID + " = ? ", new String[]{((Long)id).toString()}, null, null, null);
		if (!c.moveToFirst()) {
			// If it doesn't come up with anything, return null	
			return null;
		}
		String s = c.getString(0);
		c.close();
		this.close();
		return s;
	}
}
