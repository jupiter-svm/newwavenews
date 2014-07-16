package ru.chikov.andrey.newwavenews;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

public class DBUpdater {
	// ������� �� ��������
	private String login;
	private String pass;
	private int connTimeout;
	private boolean totalgroup;
	private boolean kickassgroup;
	private boolean knockoutgroup;

	// String LOG_TAG = "NWNews";

	DBHelper dbHelper;
	SQLiteDatabase db;
	Context appContext;

	SharedPreferences prefs;

	public DBUpdater(Context appContext) {
		this.appContext = appContext;
	}

	public int Sync() {
		ArrayList<PostData> theWall = new ArrayList<PostData>();

		// �����, ������ � ������� �� ����������� ������� � ����� ���������
		// ��������
		prefs = PreferenceManager.getDefaultSharedPreferences(appContext);

		login = prefs.getString("pref_login", "");
		pass = prefs.getString("pref_pass", "");
		connTimeout = Integer.parseInt(prefs.getString("pref_conn_timeout",
				"10")) * 1000;

		totalgroup = prefs.getBoolean("pref_maingroup", false);
		kickassgroup = prefs.getBoolean("pref_kickass", false);
		knockoutgroup = prefs.getBoolean("pref_knockout", false);

		// �������� ������� ����� �� �������� ����������
		dbHelper = new DBHelper(appContext);

		NWNews nwnews = new NWNews(login, pass, connTimeout, totalgroup,
				kickassgroup, knockoutgroup);

		// ����� ������������ 0, �� �� ���������, � ��������� ������� ������
		// ��������� � �������� ������
		switch (nwnews.Connect()) {
		case 1: // �������� ����� ��� ������
			return 1;
		case 2: // �� ������� �������� ���������� � �����
			return 2;
		}

		// �������� ������ ������� �����
		theWall = nwnews.getNews();

		db = dbHelper.getWritableDatabase();
		ContentValues cv = new ContentValues();

		if (theWall.size() == 0) {
			dbHelper.close(); // �� �������� ������� �� ����� ������� �� ������

			return 3;
		} else {
			// ������ ����������, ����� �� ��������� ���������� �����
			// ������������ ������, ����������� �� ���������� � �������
			db.beginTransaction();

			for (int i = 0; i < theWall.size(); i++)
				// �������� ������ ����� ������
				if (!checkNewsExist(theWall.get(i).getDate(), theWall.get(i)
						.getAuthor(), theWall.get(i).getText(), theWall.get(i)
						.getVideoTitle(), theWall.get(i).getPage())) {

					cv.clear();

					cv.put("postdate", theWall.get(i).getDate());
					cv.put("author", theWall.get(i).getAuthor());
					cv.put("post", theWall.get(i).getText());
					cv.put("videotitle", theWall.get(i).getVideoTitle());
					cv.put("page", theWall.get(i).getPage());
					cv.put("newtag", "�����");

					// ����� ��������, ���� ������ ����� � �� �� ����� ���������
					// ����
					try {
						db.insert("POSTS", null, cv);
					} catch (Exception ex) {
					}
				}
		}

		db.setTransactionSuccessful();
		db.endTransaction();

		dbHelper.close();

		return 0;
	}

	// ������� �������� ������������� �������
	private Boolean checkNewsExist(String postdate, String author, String post,
			String videotitle, String page) {
		Boolean existFlag = false;

		// TODO ����������� �������� �������� �� ���� �����! ������-�� c
		// postdate ������� �� ������� ���������� �����
		String selection = "SELECT * FROM POSTS WHERE author=? AND post=? AND videotitle=? AND page=?";
		String[] selectionArgs = { author, post, videotitle, page };

		Cursor c = db.rawQuery(selection, selectionArgs);

		if (c.getCount() > 0)
			existFlag = true;

		return existFlag;
	}
}
