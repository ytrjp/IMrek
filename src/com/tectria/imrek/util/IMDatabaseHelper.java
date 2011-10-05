package com.tectria.imrek.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class IMDatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "IMrekdb";
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_SQL = "CREATE TABLE channels (_id integer primary key autoincrement, "
						+ "channel_name text not null);";
	
	
	public IMDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		
		
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_SQL);
		

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS messages");
		onCreate(db);
	}

}
