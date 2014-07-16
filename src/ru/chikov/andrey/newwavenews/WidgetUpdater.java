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
		// ������ ���������� ����� ������ � ������� ���� ��� �������� �������
		startWidget(context, appWidgetManager);

		// ����� �� �������� �������� ����������
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		// ����� � ������� �� �������� �������� �� ����� � ������������� ���
		// ���������
		long updTimeout = Long.parseLong(pref.getString("pref_widget_updates",
				"10"));
		updTimeout *= 60000;
		//updTimeout = 15000;

		// �������� ������ ���� ����� � �������� � �� ���������� ��� ��� ������
		// ���������� �������
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

	// ������������ ����������� ���������� ����� �������� ���������� �������
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

	// ����� ������������ ���������� ���� � �������
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
			// ������� ��������� ��� ����, ����� ���������, ����� �� ����������
			// � ����� ������
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(context);

			// �������� ����������� ������ �� Wi-Fi
			if (prefs.getBoolean("pref_wifi-3g", false))
				if (!checkWiFiStatus(context))
					return;

			boolean flag_notify = prefs.getBoolean("pref_notifications", false);

			DBUpdater dbUpdater = new DBUpdater(context);
			DBHelper dbh = null;

			// ���� ��������� ���������� ������� � ����
			try {
				dbUpdater.Sync();
			} catch (Exception ex) {
				// TODO ����� ����� �����������, ��������� �� ������ ��
				// ���������
			}

			dbh = new DBHelper(context);

			try {
				SQLiteDatabase db = dbh.getReadableDatabase();

				String selection = "SELECT * FROM POSTS WHERE newtag=?";

				Cursor c = db.rawQuery(selection, new String[] { "�����" });

				// ���������������� �������� �� ����������� � ����� ����������
				// ���������� ������ �����������, ���� ������ ���� �
				// ������������ ������ ������� � ����������, ��� ��� ����� �����
				// �����������
				if ((c.getCount() != prefs.getInt("numnews", 0))
						&& c.getCount() > 0 && flag_notify == true) {
					ShowNotification(context, c.getCount() + " ����� ������");
				}

				// �������� ��������� ���������� ��������
				Editor editor = prefs.edit();
				editor.putInt("numnews", c.getCount());
				editor.commit();

				remoteViews.setTextViewText(R.id.text,
						String.valueOf(c.getCount()));

				// ������ ���� ����������
				remoteViews.setTextViewText(R.id.updDate,
						DateParser.getCurDate());

			} catch (Exception ex) {
			} finally {
				dbh.close();
			}

			appWidgetManager.updateAppWidget(thisWidget, remoteViews);
		}
	}

	// ��������� �������������� ��������� �������: ����� ��������� � �������
	// �����
	// ������, ����� �������� ��� �������� ������� ���� �� ������, �����
	// ������������ ������� ������ ����
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

			Cursor c = db.rawQuery(selection, new String[] { "�����" });

			remoteViews
					.setTextViewText(R.id.text, String.valueOf(c.getCount()));

			// ������ ���� ����������
			remoteViews.setTextViewText(R.id.updDate, DateParser.getCurDate());

		} catch (Exception ex) {
		} finally {
			dbh.close();
		}

		appWidgetManager.updateAppWidget(thisWidget, remoteViews);
	}

	// ����� ������ �����������
	public void ShowNotification(Context context, String text) {
		mNotifyMgr = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		CharSequence tickerText = "����� ��������� �� �����";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(R.drawable.ic_launcher,
				tickerText, when);

		Intent notificationIntent = new Intent(context,
				NewWaveNewsActivity.class);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, "�����������", text,
				contentIntent);

		mNotifyMgr.notify(NOTIFY_ID, notification);
	}

	// ���������, ������� Wi-Fi ��� ���
	private boolean checkWiFiStatus(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		// ���� Wi-Fi ���, �� ���������� ��� ����� ��������, ���� �� ����������
		// ����������
		try {
			return wifiManager.isWifiEnabled();
		} catch (Exception ex) {
			return false;
		}
	}

}
