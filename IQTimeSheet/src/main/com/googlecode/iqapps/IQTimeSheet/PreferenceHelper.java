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
 * Remove the # following @...  Was causing Eclipse to think this class was deprecated.
 * @#author      (classes and interfaces only, required)
 * @#version     (classes and interfaces only, required. See footnote 1)
 * @#param       (methods and constructors only)
 * @#return      (methods only)
 * @#exception   (@throws is a synonym added in Javadoc 1.2)
 * @#see
 * @#deprecated  (see How and When To Deprecate APIs)
 */
package com.googlecode.iqapps.IQTimeSheet;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Class to encapsulate preference handling for the application.
 * 
 * @author Paul Kronenwetter <kronenpj@gmail.com>
 */
public class PreferenceHelper {
	private static final float FONTSIZE_TOTAL_DEFAULT = 14.0f;
	private static final float FONTSIZE_TASKLIST_DEFAULT = 12.0f;
	private static final String TAG = "PreferenceHelper";
	private SharedPreferences prefs;

	public static final String KEY_ALIGN_MINUTES = "align.minutes";
	public static final String KEY_ALIGN_MINUTES_AUTO = "align.minutes.auto";
	public static final String KEY_ALIGN_TIME_PICKER = "align.time.picker";
	public static final String KEY_HOURS_DAY = "hours.day";
	public static final String KEY_HOURS_WEEK = "hours.week";
	public static final String KEY_FONTSIZE_TASKLIST = "fontSize.tasklist";
	public static final String KEY_TOTAL_FONTSIZE = "total.fontSize";
	public static final String KEY_PERSISTENT_NOTIFICATION = "persistent.notification";
	public static final String KEY_SDCARD_BACKUP = "db.on.sdcard";
	public static final String KEY_TIMEZONE_ANCHOR = "tz.anchor";
	public static final String KEY_WEEK_START_DAY = "week.startday";
	public static final String KEY_WEEK_START_HOUR = "week.starthour";

	public PreferenceHelper(Context mCtx) {
        prefs = PreferenceManager.getDefaultSharedPreferences(mCtx);
    }

	public int getAlignMinutes() {
		// Be careful here - The list used by the preferences activity is based
		// on String, not any other primitive or class... This threw cast
		// exceptions early on in development.
		int alignMinutes = 1;
		try {
			// Throws ClassCastException
			// alignMinutes = prefs.getInt(KEY_ALIGN_MINUTES, 1);
			alignMinutes = Integer.valueOf(prefs.getString(KEY_ALIGN_MINUTES,
					"1"));
		} catch (Exception e) {
			Log.e(TAG, KEY_ALIGN_MINUTES + " threw exception: " + e.toString());
		}
		Log.d(TAG, "Preference " + KEY_ALIGN_MINUTES + ": " + alignMinutes);
		return alignMinutes;
	}

	public boolean getAlignMinutesAuto() {
		boolean alignMinutesAuto = false;
		try {
			alignMinutesAuto = prefs.getBoolean(KEY_ALIGN_MINUTES_AUTO, false);
		} catch (Exception e) {
			Log.e(TAG,
					KEY_ALIGN_MINUTES_AUTO + " threw exception: "
							+ e.toString());
		}
		Log.d(TAG, "Preference " + KEY_ALIGN_MINUTES_AUTO + ": "
				+ alignMinutesAuto);
		return alignMinutesAuto;
	}

	public boolean getAlignTimePicker() {
		boolean alignTimePicker = true;
		try {
			alignTimePicker = prefs.getBoolean(KEY_ALIGN_TIME_PICKER, true);
		} catch (Exception e) {
			Log.e(TAG,
					KEY_ALIGN_TIME_PICKER + " threw exception: " + e.toString());
		}
		Log.d(TAG, "Preference " + KEY_ALIGN_TIME_PICKER + ": "
				+ alignTimePicker);
		return alignTimePicker;
	}

	public float getHoursPerDay() {
		// Be careful here - The list used by the preferences activity is based
		// on String, not any other primitive or class... This threw cast
		// exceptions early on in development.
		float hoursDay = (float) 8.0;
		try {
			// Throws ClassCastException
			// hoursDay = prefs.getFloat(KEY_HOURS_DAY, (float) 8.0);
			hoursDay = Float.valueOf(prefs.getString(KEY_HOURS_DAY, "8"));
		} catch (Exception e) {
			Log.e(TAG, KEY_HOURS_DAY + " threw exception: " + e.toString());
		}
		Log.d(TAG, "Preference " + KEY_HOURS_DAY + ": " + hoursDay);
		return hoursDay;
	}

	public float getHoursPerWeek() {
		// Be careful here - The list used by the preferences activity is based
		// on String, not any other primitive or class... This threw cast
		// exceptions early on in development.
		float hoursWeek = (float) 40.0;
		try {
			// Throws ClassCastException
			// hoursWeek = prefs.getFloat(KEY_HOURS_WEEK, (float) 40.0);
			hoursWeek = Float.valueOf(prefs.getString(KEY_HOURS_WEEK, "40"));
		} catch (Exception e) {
			Log.e(TAG, KEY_HOURS_WEEK + " threw exception: " + e.toString());
		}
		Log.d(TAG, "Preference " + KEY_HOURS_WEEK + ": " + hoursWeek);
		return hoursWeek;
	}

	public float getTotalsFontSize() {
		// Be careful here - The list used by the preferences activity is based
		// on String, not any other primitive or class... This threw cast
		// exceptions early on in development.
		float totalsFontSize = FONTSIZE_TOTAL_DEFAULT;
		try {
			// Throws ClassCastException
			// hoursWeek = prefs.getFloat(KEY_HOURS_WEEK, (float) 40.0);
			totalsFontSize = Float.valueOf(prefs.getString(KEY_TOTAL_FONTSIZE,
					String.valueOf(FONTSIZE_TOTAL_DEFAULT)));
		} catch (Exception e) {
			Log.e(TAG, KEY_TOTAL_FONTSIZE + " threw exception: " + e.toString());
		}
		Log.d(TAG, "Preference " + KEY_TOTAL_FONTSIZE + ": " + totalsFontSize);
		return totalsFontSize;
	}

	public float getFontSizeTaskList() {
		// Be careful here - The list used by the preferences activity is based
		// on String, not any other primitive or class... This threw cast
		// exceptions early on in development.
		float fontSizeTaskList = FONTSIZE_TASKLIST_DEFAULT;
		try {
			// Throws ClassCastException
			// hoursWeek = prefs.getFloat(KEY_HOURS_WEEK, (float) 40.0);
			fontSizeTaskList = Float.valueOf(prefs.getString(
					KEY_FONTSIZE_TASKLIST,
					String.valueOf(FONTSIZE_TASKLIST_DEFAULT)));
		} catch (Exception e) {
			Log.e(TAG,
					KEY_FONTSIZE_TASKLIST + " threw exception: " + e.toString());
		}
		// Log.d(TAG, "Preference " + KEY_FONTSIZE_TASKLIST + ": "
		// + fontSizeTaskList);
		return fontSizeTaskList;
	}

	public TimeZone getTimeZone() {
		// Make the default the current, where ever it might be.
		TimeZone timeZoneAnchor = TimeZone.getDefault();
		Log.d(TAG, "TZAnchor: " + timeZoneAnchor.getID());
		Log.d(TAG, "TZAnchor long: " + TimeZone.getTimeZone(timeZoneAnchor.getID()).getID());
		try {
			// Throws ClassCastException
			String tzpref = prefs.getString(KEY_TIMEZONE_ANCHOR,
					timeZoneAnchor.getDisplayName());
			timeZoneAnchor = TimeZone.getTimeZone(tzpref);
		} catch (Exception e) {
			Log.e(TAG,
					KEY_TIMEZONE_ANCHOR + " threw exception: " + e.toString());
		}
		// Log.d(TAG, "Preference " + KEY_TIMEZONE_ANCHOR + ": "
		// + timeZoneAnchor.getDisplayName());
		return timeZoneAnchor;
	}

	public boolean getPersistentNotification() {
		boolean persistentNotification = false;
		try {
			persistentNotification = prefs.getBoolean(
					KEY_PERSISTENT_NOTIFICATION, false);
		} catch (Exception e) {
			Log.e(TAG,
					KEY_PERSISTENT_NOTIFICATION + " threw exception: "
							+ e.toString());
		}
		// Log.d(TAG, "Preference " + KEY_PERSISTENT_NOTIFICATION + ": "
		// + persistentNotification);
		return persistentNotification;
	}

	public boolean getSDCardBackup() {
		boolean backup = false;
		try {
			backup = prefs.getBoolean(KEY_SDCARD_BACKUP, true);
		} catch (Exception e) {
			Log.e(TAG, KEY_SDCARD_BACKUP + " threw exception: " + e.toString());
		}
		Log.d(TAG, "Preference " + KEY_SDCARD_BACKUP + ": " + backup);
		return backup;
	}

	public int getWeekStartDay() {
		int startDay = Calendar.MONDAY;
		try {
			startDay = Integer.valueOf(prefs.getString(KEY_WEEK_START_DAY,
					Integer.toString(Calendar.MONDAY)));
		} catch (Exception e) {
			Log.e(TAG, KEY_WEEK_START_DAY + " threw exception: " + e.toString());
		}
		Log.d(TAG, "Preference " + KEY_WEEK_START_DAY + ": " + startDay);
		return startDay;
	}

	public int getWeekStartHour() {
		int startHour = 0;
		try {
			startHour = Integer.valueOf(prefs.getString(KEY_WEEK_START_HOUR, "0"));
		} catch (Exception e) {
			Log.e(TAG, KEY_WEEK_START_HOUR + " threw exception: " + e.toString());
		}
		Log.d(TAG, "Preference " + KEY_WEEK_START_HOUR + ": " + startHour);
		return startHour;
	}
}
