package ru.chikov.andrey.newwavenews;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

public class Prefs extends PreferenceActivity implements
		OnPreferenceChangeListener {
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		//Вывожу в значения summary для полей значения из настроек
		ListPreference connTimeout = (ListPreference) this
				.findPreference("pref_conn_timeout");
		ListPreference widgetUpdInerval = (ListPreference) this
				.findPreference("pref_widget_updates");

		connTimeout.setOnPreferenceChangeListener(this);
		widgetUpdInerval.setOnPreferenceChangeListener(this);

		connTimeout.setSummary(connTimeout.getEntry()+" сек.");
		widgetUpdInerval.setSummary("Раз в "+widgetUpdInerval.getEntry()+" мин.");

	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();
		
		CharSequence sec=String.valueOf(newValue)+" сек.";
		CharSequence min="Раз в "+String.valueOf(newValue)+" мин.";
		
		if (key.equals("pref_conn_timeout")) 
			preference.setSummary(sec);		

		if (key.equals("pref_widget_updates")) 
			preference.setSummary(min);
		

		return true;
	}
}
