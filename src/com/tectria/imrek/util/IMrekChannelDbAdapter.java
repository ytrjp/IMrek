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
		// TODO: cascade delete to any messages stored for this channel
		return database.delete(DATABASE_TABLE, "channel_name = ?", new String[]{channel_name}) > 0;
	}
	
	public Cursor getChannels() {
		return database.query(DATABASE_TABLE, null, null, null, null, null, null);
	}
	
}
