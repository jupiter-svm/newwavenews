package ru.chikov.andrey.newwavenews;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class WidgetUpdater extends AppWidgetProvider {
	Timer timer;
	private static final int NOTIFY_ID = 101;
	private NotificationManager mNotifyMgr;

	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		// Вывожу количество новых постов и текущую дату при создании виджета
		startWidget(context, appWidgetManager);

		// Узнаю из настроек интервал обновления
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		// Время в минутах из настроек перевожу во время в миллисекундах для
		// программы
		long updTimeout = Long.parseLong(pref.getString("pref_widget_updates",
				"10"));
		updTimeout *= 60000;
		//updTimeout = 15000;

		// Запускаю только один поток с таймером и не пересоздаю его при каждом
		// обновлении виджета
		if (timer == null) {
			timer = new Timer();
			timer.scheduleAtFixedRate(
					new UpdateProc(context, appWidgetManager), 1, updTimeout);
		}

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
					R.layout.widget);
			ComponentName thisWidget = new ComponentName(context,
					WidgetUpdater.class);

			Bundle extras = intent.getExtras();
			String value = extras.getString("value");

			if (value != null) {
				if (value.indexOf("deleted") != -1) {
					remoteViews.setTextViewText(R.id.text, "0");

					AppWidgetManager appWidgetManager = AppWidgetManager
							.getInstance(context);
					appWidgetManager.updateAppWidget(thisWidget, remoteViews);

					return;
				}
			}
		}

		super.onReceive(context, intent);
	}

	// Останавливаю планировщик обновления после удаления экземпляра виджета
	public void onDeleted(Context context, int[] appWidgetIds) {
		if (timer != null) {
			timer.cancel();
		}
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
	}

	// Поток планировщика обновления базы и виджета
	private class UpdateProc extends TimerTask {
		RemoteViews remoteViews;
		AppWidgetManager appWidgetManager;
		ComponentName thisWidget;
		Context context;

		public UpdateProc(Context context, AppWidgetManager appWidgetManager) {
			this.appWidgetManager = appWidgetManager;
			remoteViews = new RemoteViews(context.getPackageName(),
					R.layout.widget);
			thisWidget = new ComponentName(context, WidgetUpdater.class);
			this.context = context;

			Intent launchAppIntent = new Intent(context,
					NewWaveNewsActivity.class);
			PendingIntent launchAppPendingIntent = PendingIntent.getActivity(
					context, 0, launchAppIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.widget_layout,
					launchAppPendingIntent);
		}

		@Override
		public void run() {
			// Смотрим настройки для того, чтобы проветить, нужно ли уведомлять
			// о новых постах
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(context);

			// Проверка подключения только по Wi-Fi
			if (prefs.getBoolean("pref_wifi-3g", false))
				if (!checkWiFiStatus(context))
					return;

			boolean flag_notify = prefs.getBoolean("pref_notifications", false);

			DBUpdater dbUpdater = new DBUpdater(context);
			DBHelper dbh = null;

			// Сама процедура обновления виджета и базы
			try {
				dbUpdater.Sync();
			} catch (Exception ex) {
				// TODO здесь нужно логирование, сообщения из потока не
				// выводятся
			}

			dbh = new DBHelper(context);

			try {
				SQLiteDatabase db = dbh.getReadableDatabase();

				String selection = "SELECT * FROM POSTS WHERE newtag=?";

				Cursor c = db.rawQuery(selection, new String[] { "Новое" });

				// Непосредственная проверка на уведомления о новых сообщениях
				// Количество должно различаться, быть больше нуля и
				// пользователь должен указать в растройках, что ему нужны такие
				// уведомления
				if ((c.getCount() != prefs.getInt("numnews", 0))
						&& c.getCount() > 0 && flag_notify == true) {
					ShowNotification(context, c.getCount() + " новых постов");
				}

				// Обновляю настройку количества новостей
				Editor editor = prefs.edit();
				editor.putInt("numnews", c.getCount());
				editor.commit();

				remoteViews.setTextViewText(R.id.text,
						String.valueOf(c.getCount()));

				// Вывожу дату обновления
				remoteViews.setTextViewText(R.id.updDate,
						DateParser.getCurDate());

			} catch (Exception ex) {
			} finally {
				dbh.close();
			}

			appWidgetManager.updateAppWidget(thisWidget, remoteViews);
		}
	}

	// Процедура первоначальной настройки виджета: новые сообщения и текущее
	// время
	// Вообще, новых новостей при создании виджета быть не должно, можно
	// ограничиться выводом теущей даты
	public void startWidget(Context context, AppWidgetManager appWidgetManager) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
				R.layout.widget);
		ComponentName thisWidget = new ComponentName(context,
				WidgetUpdater.class);

		DBHelper dbh = null;

		try {
			dbh = new DBHelper(context);
			SQLiteDatabase db = dbh.getReadableDatabase();

			String selection = "SELECT * FROM POSTS WHERE newtag=?";

			Cursor c = db.rawQuery(selection, new String[] { "Новое" });

			remoteViews
					.setTextViewText(R.id.text, String.valueOf(c.getCount()));

			// Вывожу дату обновления
			remoteViews.setTextViewText(R.id.updDate, DateParser.getCurDate());

		} catch (Exception ex) {
		} finally {
			dbh.close();
		}

		appWidgetManager.updateAppWidget(thisWidget, remoteViews);
	}

	// Метод вывода уведомления
	public void ShowNotification(Context context, String text) {
		mNotifyMgr = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		CharSequence tickerText = "Новые сообщения на стене";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(R.drawable.ic_launcher,
				tickerText, when);

		Intent notificationIntent = new Intent(context,
				NewWaveNewsActivity.class);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, "Уведомление", text,
				contentIntent);

		mNotifyMgr.notify(NOTIFY_ID, notification);
	}

	// Проверяем, включен Wi-Fi или нет
	private boolean checkWiFiStatus(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		// Если Wi-Fi нет, то приложение тут может вылететь, если не обработать
		// исключение
		try {
			return wifiManager.isWifiEnabled();
		} catch (Exception ex) {
			return false;
		}
	}

}
