package ru.chikov.andrey.newwavenews;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {	
	
	public DBHelper(Context context) {
		super(context, "NWNews", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE POSTS ("
				+"id integer primary key autoincrement,"
				+"postdate int,"
				+"author text,"
				+"post text,"
				+"videotitle text,"
				+"page text,"
				+"newtag text);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {				
	}

}
