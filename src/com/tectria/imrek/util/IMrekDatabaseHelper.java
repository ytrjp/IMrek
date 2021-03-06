package com.tectria.imrek.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class IMrekDatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "IMrekdb";
	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_SQL1 = "CREATE TABLE IF NOT EXISTS channels (_id integer primary key autoincrement, "
						+ "channel_name text not null);";
	private static final String DATABASE_SQL2 = "CREATE TABLE IF NOT EXISTS messages(_id integer primary key autoincrement, channel_id integer not null, "
						+ "username text not null, message text not null, timestamp text not null);";
	
	
	public IMrekDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);	
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_SQL1);
		db.execSQL(DATABASE_SQL2);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS messages");
		onCreate(db);
	}

}
