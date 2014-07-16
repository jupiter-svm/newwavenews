package ru.chikov.andrey.newwavenews;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class NewWaveNewsActivity extends Activity {

	private static final String TYPE = "value";
	private static final String DEL_VALUE = "deleted";
	private static final int CLEAR_DIALOG = 0;
	private static final int FILTER_DIALOG = 1;

	int[] colors = { 0x559966CC, 0x55336699 };
	ArrayList<String> links = new ArrayList<String>();
	UpdateTask updateTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminateVisibility(false);

		setContentView(R.layout.main);

		// Отрисовка новостей идёт отдельной функцией
		DrawNews(new StringBuilder());

		//Удаляю уведомления (одно должно быть)
		NotificationManager mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifyMgr.cancelAll();

	}

	// При сокрытии главного экрана прекращаю поток обновления БД
	protected void onPause() {
		if (updateTask != null) {
			updateTask.cancel(true);
		}

		super.onPause();
	}

	// Создаю меню из файла ресурса
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		return true;
	}

	// Обрабатываю выбор пунктов меню
	public boolean onOptionsItemSelected(MenuItem item) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		switch (item.getItemId()) {
		case R.id.refresh:
			// Сначала проверим, нужно ли Wi-Fi подключение для обновления
			if (prefs.getBoolean("pref_wifi-3g", false))
				if (!checkWiFiStatus()) {
					Toast.makeText(
							getApplicationContext(),
							"Настройки требуют включенного Wi-Fi для синхронизации новостей",
							Toast.LENGTH_LONG).show();

					return true;
				}

			// Новый экземпляр не запускаю, потому что в этом мало смысла
			if (updateTask == null) {
				updateTask = new UpdateTask();
				updateTask.execute();
			} else {
				Toast.makeText(getApplicationContext(),
						"Обновление уже запущенно", Toast.LENGTH_SHORT).show();
			}

			return true;
		case R.id.clear:
			commitDelDialog();

			return true;
		case R.id.about:
			Intent i = new Intent(this, AboutActivity.class);
			startActivity(i);
			return true;
		case R.id.pref:
			Intent intent = new Intent(this, Prefs.class);
			startActivity(intent);
			return true;
		case R.id.filter:
			filterDialog();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// Вызываем диалог фильтрации выводимых данных
	private void filterDialog() {
		showDialog(FILTER_DIALOG);
	}

	// Вызываем диалог подтверждения очистки базы
	private void commitDelDialog() {
		showDialog(CLEAR_DIALOG);
	}

	// Сам диалог подтверждения удаления
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case (CLEAR_DIALOG):
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Действительно удалить все записи?")
					.setCancelable(false)
					.setPositiveButton("Да",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									DBHelper dbh = new DBHelper(
											getApplicationContext());
									SQLiteDatabase db = dbh
											.getWritableDatabase();
									int delCount = 0;

									try {
										delCount = db.delete("POSTS", null,
												null);
									} catch (Exception ex) {
										Toast.makeText(
												getApplicationContext(),
												"Возникла ошибка при удалении записей",
												Toast.LENGTH_SHORT).show();
									}

									// TODO Перенести в настройки
									Toast.makeText(
											getApplicationContext(),
											"Удалено записей: "
													+ String.valueOf(delCount),
											Toast.LENGTH_SHORT).show();

									dbh.close();

									// Обятельно перерисуем экран
									LinearLayout linLayout = (LinearLayout) findViewById(R.id.linLayout);
									linLayout.removeAllViews();

									// Обновим настройки, чтобы не возникало
									// конфликтов с виджетом
									SharedPreferences prefs = PreferenceManager
											.getDefaultSharedPreferences(getApplicationContext());
									Editor editor = prefs.edit();
									editor.putInt("numnews", 0);
									editor.commit();

									// Обновляю виджет, чтобы не ждать его
									// планового обновления
									WidgetUpdate();
								}
							})
					.setNegativeButton("Нет",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							});
			return builder.create();
			// TODO Доделать фильтрацию
		case (FILTER_DIALOG):
			final boolean[] mCheckedItems = { true, true, true };

			builder = new AlertDialog.Builder(this);
			builder.setTitle("Выбрать группы");
			builder.setMultiChoiceItems(R.array.filter, mCheckedItems,
					new DialogInterface.OnMultiChoiceClickListener() {
						public void onClick(DialogInterface dialog, int which,
								boolean isChecked) {
							mCheckedItems[which] = isChecked;
						}
					})
					.setPositiveButton("Фильтровать",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									StringBuilder params = new StringBuilder();

									if (mCheckedItems[0])
										params.append(" WHERE page='Основная группа'");

									if (mCheckedItems[1])
										if (params.length() == 0)
											params.append(" WHERE page='KickAssCrew'");
										else
											params.append(" OR page='KickAssCrew'");

									if (mCheckedItems[2])
										if (params.length() == 0)
											params.append(" WHERE page='KnockOutCrew'");
										else
											params.append(" OR page='KnockOutCrew'");

									DrawNews(params);
								}
							})
					.setNegativeButton("Отмена",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							});
			return builder.create();

		default:
			return null;
		}
	}

	// Отрисовка новостей
	public void DrawNews(StringBuilder queryParams) {
		int linesCounter = 0;
		String queryString = "SELECT * FROM POSTS " + queryParams.toString()
				+ " ORDER BY postdate DESC";

		LinearLayout linLayout = (LinearLayout) findViewById(R.id.linLayout);
		LayoutInflater ltInflater = getLayoutInflater();

		// Ссылки на видео из новостей обнуляются
		links.clear();
		linLayout.removeAllViews();

		// Вывожу список из базы
		DBHelper dbh = new DBHelper(this);
		SQLiteDatabase db = dbh.getWritableDatabase();

		Cursor c = db.rawQuery(queryString, null);

		if (c.moveToFirst()) {
			int idDateIndex = c.getColumnIndex("postdate");
			int idAuthorIndex = c.getColumnIndex("author");
			int idPostIndex = c.getColumnIndex("post");
			int idLinkIndex = c.getColumnIndex("videotitle");
			int idPageIndex = c.getColumnIndex("page");
			int idNewTagIndex = c.getColumnIndex("newtag");

			do {
				View item = ltInflater.inflate(R.layout.news_layout, linLayout,
						false);

				TextView tvName = (TextView) item.findViewById(R.id.tvAuthor);
				tvName.setText(c.getString(idAuthorIndex));

				TextView tvDate = (TextView) item.findViewById(R.id.tvDate);
				tvDate.setText(DateParser.dateToPost(c.getString(idDateIndex)));

				TextView tvPost = (TextView) item.findViewById(R.id.tvPost);

				// Если видео существует, то смотрим, если ли комментарий к нему
				// Плюс различаем, превью к фото или это всё же видео
				if (!c.getString(idLinkIndex).equalsIgnoreCase(""))
					if (!c.getString(idPostIndex).equalsIgnoreCase(""))
						if (c.getString(idLinkIndex).indexOf("jpg") == -1)
							tvPost.setText("Видео: " + c.getString(idPostIndex));
						else
							tvPost.setText("Фото: " + c.getString(idPostIndex));
					else if (c.getString(idLinkIndex).indexOf("jpg") == -1) {
						tvPost.setText("Видео");
					} else
						tvPost.setText("Фото");
				else
					tvPost.setText(c.getString(idPostIndex));

				TextView tvGroup = (TextView) item.findViewById(R.id.tvGroup);
				tvGroup.setText(c.getString(idPageIndex));

				TextView tvNewTag = (TextView) item.findViewById(R.id.tvNewTag);

				tvNewTag.setText(c.getString(idNewTagIndex));

				// Заполняю массив ссылок для просмотра видео
				links.add(c.getString(idLinkIndex));

				item.getLayoutParams().width = LayoutParams.MATCH_PARENT;
				item.setBackgroundColor(colors[linesCounter % 2]);
				item.setId(linesCounter);

				if (!c.getString(idLinkIndex).equalsIgnoreCase(""))
					item.setOnClickListener(itemClick);

				linLayout.addView(item);

				linesCounter++;
			} while (c.moveToNext());
		}

		// После отображения информации на экране снимаю с новостей метки
		// "Новая"
		ContentValues cv = new ContentValues();
		cv.put("newtag", "");

		try {
			db.update("POSTS", cv, null, null); // Включить условия в запрос
		} catch (Exception ex) {
		} finally {

			dbh.close();
		}

		// Обновляю виждет, чтобы не ждать его планового обновления
		WidgetUpdate();
	}

	// Открытие ссылок из постов в браузере
	private View.OnClickListener itemClick = new View.OnClickListener() {
		Intent intent;

		public void onClick(View v) {
			intent = new Intent(Intent.ACTION_VIEW, Uri.parse(links.get(v
					.getId())));
			startActivity(intent);
		}
	};

	// Проверяем, включен Wi-Fi или нет
	private boolean checkWiFiStatus() {
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

		// Если Wi-Fi нет, то приложение тут может вылететь, если не обработать
		// исключение
		try {
			return wifiManager.isWifiEnabled();
		} catch (Exception ex) {
			return false;
		}
	}

	// Обновление виджета после очистки БД или просмотра новостей
	private void WidgetUpdate() {
		Intent wIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
				.setClass(getApplicationContext(), WidgetUpdater.class)
				.putExtra(TYPE, DEL_VALUE);
		sendBroadcast(wIntent);
	}

	private class UpdateTask extends AsyncTask<Object, String, Boolean> {
		int syncResult = 0;
		boolean errFlag = false;

		@Override
		protected void onPostExecute(Boolean result) {
			NewWaveNewsActivity.this
					.setProgressBarIndeterminateVisibility(false);

			if (errFlag) {
				Toast.makeText(getApplicationContext(),
						"Не удалось получить новости", Toast.LENGTH_SHORT)
						.show();
			}

			// Вывожу сообщения об ошибках и обновляю экран после этого
			switch (syncResult) {
			case 1:
				Toast.makeText(getApplicationContext(),
						"Неверный логин или пароль", Toast.LENGTH_SHORT).show();
				break;
			case 2:
				Toast.makeText(getApplicationContext(),
						"Не удалось получить информацию с сайта",
						Toast.LENGTH_SHORT).show();
				break;
			case 3:
				Toast.makeText(getApplicationContext(),
						"Не найдено записей на стене", Toast.LENGTH_SHORT)
						.show();
				break;
			}

			// Очищаю главный экран и перерисовываю его заново
			LinearLayout linLayout = (LinearLayout) findViewById(R.id.linLayout);
			linLayout.removeAllViews();

			try {
				DrawNews(new StringBuilder());
			} catch (Exception ex) {
				Toast.makeText(getApplicationContext(),
						"Не удалось отобразить новости", Toast.LENGTH_SHORT)
						.show();
			}
		}

		@Override
		protected void onPreExecute() {
			NewWaveNewsActivity.this
					.setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected Boolean doInBackground(Object... params) {

			DBUpdater dbUpdater = new DBUpdater(getApplicationContext());

			try {
				syncResult = dbUpdater.Sync();
			} catch (Exception ex) {
				// Здесь сразу не получится вывести Toast. Скорее всего проблема
				// с синхронизацией потоков
				errFlag = true;
			}

			return true;
		}
	}
}