package com.tectria.imrek.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class ChannelDbAdapter {

	public static final String KEY_ID = "_id";
	public static final String KEY_CHANNELNAME = "channel_name";
	
	private static final String DATABASE_TABLE = "channels";
	private Context context;
	private SQLiteDatabase database;
	
}
