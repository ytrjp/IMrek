package com.tectria.imrek.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class ChannelDbAdapter {

	public static final String KEY_ID = "_id";
	public static final String KEY_CHANNELNAME = "channel_name";
	
	private static final String DATABASE_TABLE = "channels";
	private Context context;
	private SQLiteDatabase database;
	
}
