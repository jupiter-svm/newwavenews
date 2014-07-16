package ru.chikov.andrey.newwavenews;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

public class DBUpdater {
	// Получаю из настроек
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

		// Логин, пароль и таймаут на подключение передаю в класс получения
		// новостей
		prefs = PreferenceManager.getDefaultSharedPreferences(appContext);

		login = prefs.getString("pref_login", "");
		pass = prefs.getString("pref_pass", "");
		connTimeout = Integer.parseInt(prefs.getString("pref_conn_timeout",
				"10")) * 1000;

		totalgroup = prefs.getBoolean("pref_maingroup", false);
		kickassgroup = prefs.getBoolean("pref_kickass", false);
		knockoutgroup = prefs.getBoolean("pref_knockout", false);

		// Контекст придётся брать из главного приложения
		dbHelper = new DBHelper(appContext);

		NWNews nwnews = new NWNews(login, pass, connTimeout, totalgroup,
				kickassgroup, knockoutgroup);

		// Когда возвращается 0, то всё нормально, в остальных случаях вывожу
		// сообщения и прерываю работу
		switch (nwnews.Connect()) {
		case 1: // Неверный логин или пароль
			return 1;
		case 2: // Не удалось получить информацию с сайта
			return 2;
		}

		// Получаем массив записей стены
		theWall = nwnews.getNews();

		db = dbHelper.getWritableDatabase();
		ContentValues cv = new ContentValues();

		if (theWall.size() == 0) {
			dbHelper.close(); // Не забываем закрыть БД перед выходом из метода

			return 3;
		} else {
			// Делаем транзакцию, чтобы не возникало конфликтов между
			// экземплярами класса, запущенными из приложения и виджета
			db.beginTransaction();

			for (int i = 0; i < theWall.size(); i++)
				// Добавляю только новые записи
				if (!checkNewsExist(theWall.get(i).getDate(), theWall.get(i)
						.getAuthor(), theWall.get(i).getText(), theWall.get(i)
						.getVideoTitle(), theWall.get(i).getPage())) {

					cv.clear();

					cv.put("postdate", theWall.get(i).getDate());
					cv.put("author", theWall.get(i).getAuthor());
					cv.put("post", theWall.get(i).getText());
					cv.put("videotitle", theWall.get(i).getVideoTitle());
					cv.put("page", theWall.get(i).getPage());
					cv.put("newtag", "Новое");

					// Такое возомжно, если виджет будет в то же время обновлять
					// базу
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

	// Функция проверки существования новости
	private Boolean checkNewsExist(String postdate, String author, String post,
			String videotitle, String page) {
		Boolean existFlag = false;

		// TODO обязательно добавить проверку на дату поста! Почему-то c
		// postdate выборка не находит одинаковые посты
		String selection = "SELECT * FROM POSTS WHERE author=? AND post=? AND videotitle=? AND page=?";
		String[] selectionArgs = { author, post, videotitle, page };

		Cursor c = db.rawQuery(selection, selectionArgs);

		if (c.getCount() > 0)
			existFlag = true;

		return existFlag;
	}
}
