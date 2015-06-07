/*
 * Copyright 2010 TimeSheet authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author (classes and interfaces only, required)
 * @version (classes and interfaces only, required. See footnote 1)
 * @param       (methods and constructors only)
 * @return (methods only)
 * @exception (@throws is a synonym added in Javadoc 1.2)
 * @see
 * @ deprecated  (see How and When To Deprecate APIs)
 */
package com.googlecode.iqapps.IQTimeSheet;

import java.util.Calendar;
import java.util.TimeZone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * @author kronenpj Borrowed heavily from ConnectBot's SettingsActivity.
 */
public class MyPreferenceActivity extends SherlockPreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String TAG = "MyPreferenceActivity";

	@Override
	protected void onResume() {
		super.onResume();

		/*
		 * try { addPreferencesFromResource(R.xml.preferences); } catch
		 * (Exception e) { // Something bad happened when reading the
		 * preferences. Try to // recover. SharedPreferences prefs =
		 * PreferenceManager .getDefaultSharedPreferences(this);
		 * 
		 * Editor prefEditor = prefs.edit(); // Make sure we're starting from
		 * scratch prefEditor.clear(); prefEditor.commit();
		 * 
		 * // This apparently needs a new editor created...
		 * PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
		 * prefEditor = prefs.edit(); prefEditor.commit();
		 * 
		 * // Try loading the preferences again.
		 * addPreferencesFromResource(R.xml.preferences); }
		 */

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		addPreferencesFromResource(R.xml.preferences);

		// SharedPreferences sp = getPreferenceScreen().getSharedPreferences();

		ListPreference tzAnchor = (ListPreference) findPreference("tz.anchor");
		tzAnchor.setOrder(70);
		String[] timeZones = TimeZone.getAvailableIDs();
		tzAnchor.setEntries(timeZones);
		tzAnchor.setEntryValues(timeZones);
    }

	@Override
	protected void onPause() {
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		finish();
		super.onPause();
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		/*
		 * Preference pref = findPreference(key); if (pref instanceof
		 * EditTextPreference) { EditTextPreference etp = (EditTextPreference)
		 * pref; if (pref.getKey().equals("password")) {
		 * pref.setSummary(etp.getText().replaceAll(".", "*")); } else {
		 * pref.setSummary(etp.getText()); } } else if (pref instanceof
		 * CheckBoxPreference) { if (((CheckBoxPreference) pref).isChecked())
		 * pref.setSummary("On"); else pref.setSummary("Off"); }
		 */
	}

	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case android.R.id.home: {
			finish();
			return true;
		}
		default:
			return super
					.onOptionsItemSelected((android.view.MenuItem) menuItem);
		}
	}
}
