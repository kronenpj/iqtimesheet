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

 * @#author (classes and interfaces only, required)
 * *
 * @#version (classes and interfaces only, required. See footnote 1)
 * *
 * @#param (methods and constructors only)
 * *
 * @#return (methods only)
 * *
 * @#exception (@throws is a synonym added in Javadoc 1.2)
 * *
 * @#see
 * *
 * @#deprecated (see How and When To Deprecate APIs)
 */
package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import java.util.*

/**
 * Class to encapsulate preference handling for the application.

 * @author Paul Kronenwetter <kronenpj></kronenpj>@gmail.com>
 */
class PreferenceHelper(mCtx: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCtx)

    // Be careful here - The list used by the preferences activity is based
    // on String, not any other primitive or class... This threw cast
    // exceptions early on in development.
    // Throws ClassCastException
    // alignMinutes = prefs.getInt(KEY_ALIGN_MINUTES, 1);
    val alignMinutes: Int
        get() {
            var alignMinutes = 1
            try {
                alignMinutes = Integer.valueOf(prefs.getString(KEY_ALIGN_MINUTES,
                        "1"))
            } catch (e: Exception) {
                Log.e(TAG, "$KEY_ALIGN_MINUTES threw exception: $e")
            }

            Log.d(TAG, "Preference $KEY_ALIGN_MINUTES: $alignMinutes")
            return alignMinutes
        }

    val alignMinutesAuto: Boolean
        get() {
            var alignMinutesAuto = false
            try {
                alignMinutesAuto = prefs.getBoolean(KEY_ALIGN_MINUTES_AUTO, false)
            } catch (e: Exception) {
                Log.e(TAG,
                        KEY_ALIGN_MINUTES_AUTO + " threw exception: "
                                + e.toString())
            }

            Log.d(TAG, "Preference " + KEY_ALIGN_MINUTES_AUTO + ": "
                    + alignMinutesAuto)
            return alignMinutesAuto
        }

    val alignTimePicker: Boolean
        get() {
            var alignTimePicker = true
            try {
                alignTimePicker = prefs.getBoolean(KEY_ALIGN_TIME_PICKER, true)
            } catch (e: Exception) {
                Log.e(TAG,
                        "$KEY_ALIGN_TIME_PICKER threw exception: $e")
            }

            Log.d(TAG, "Preference " + KEY_ALIGN_TIME_PICKER + ": "
                    + alignTimePicker)
            return alignTimePicker
        }

    // Be careful here - The list used by the preferences activity is based
    // on String, not any other primitive or class... This threw cast
    // exceptions early on in development.
    // Throws ClassCastException
    // hoursDay = prefs.getFloat(KEY_HOURS_DAY, (float) 8.0);
    val hoursPerDay: Float
        get() {
            var hoursDay = 8.0.toFloat()
            try {
                hoursDay = java.lang.Float.valueOf(prefs.getString(KEY_HOURS_DAY, "8"))
            } catch (e: Exception) {
                Log.e(TAG, "$KEY_HOURS_DAY threw exception: $e")
            }

            Log.d(TAG, "Preference $KEY_HOURS_DAY: $hoursDay")
            return hoursDay
        }

    // Be careful here - The list used by the preferences activity is based
    // on String, not any other primitive or class... This threw cast
    // exceptions early on in development.
    // Throws ClassCastException
    // hoursWeek = prefs.getFloat(KEY_HOURS_WEEK, (float) 40.0);
    val hoursPerWeek: Float
        get() {
            var hoursWeek = 40.0.toFloat()
            try {
                hoursWeek = java.lang.Float.valueOf(prefs.getString(KEY_HOURS_WEEK, "40"))
            } catch (e: Exception) {
                Log.e(TAG, "$KEY_HOURS_WEEK threw exception: $e")
            }

            Log.d(TAG, "Preference $KEY_HOURS_WEEK: $hoursWeek")
            return hoursWeek
        }

    // Be careful here - The list used by the preferences activity is based
    // on String, not any other primitive or class... This threw cast
    // exceptions early on in development.
    // Throws ClassCastException
    // hoursWeek = prefs.getFloat(KEY_HOURS_WEEK, (float) 40.0);
    val totalsFontSize: Float
        get() {
            var totalsFontSize = FONTSIZE_TOTAL_DEFAULT
            try {
                totalsFontSize = java.lang.Float.valueOf(prefs.getString(KEY_TOTAL_FONTSIZE,
                        FONTSIZE_TOTAL_DEFAULT.toString()))
            } catch (e: Exception) {
                Log.e(TAG, "$KEY_TOTAL_FONTSIZE threw exception: $e")
            }

            Log.d(TAG, "Preference $KEY_TOTAL_FONTSIZE: $totalsFontSize")
            return totalsFontSize
        }

    // Be careful here - The list used by the preferences activity is based
    // on String, not any other primitive or class... This threw cast
    // exceptions early on in development.
    // Throws ClassCastException
    // hoursWeek = prefs.getFloat(KEY_HOURS_WEEK, (float) 40.0);
    // Log.d(TAG, "Preference " + KEY_FONTSIZE_TASKLIST + ": "
    // + fontSizeTaskList);
    val fontSizeTaskList: Float
        get() {
            var fontSizeTaskList = FONTSIZE_TASKLIST_DEFAULT
            try {
                fontSizeTaskList = java.lang.Float.valueOf(prefs.getString(
                        KEY_FONTSIZE_TASKLIST,
                        FONTSIZE_TASKLIST_DEFAULT.toString()))
            } catch (e: Exception) {
                Log.e(TAG,
                        "$KEY_FONTSIZE_TASKLIST threw exception: $e")
            }

            return fontSizeTaskList
        }

    // Make the default the current, where ever it might be.
    // Throws ClassCastException
    // Log.d(TAG, "Preference " + KEY_TIMEZONE_ANCHOR + ": "
    // + timeZoneAnchor.getDisplayName());
    val timeZone: TimeZone
        get() {
            var timeZoneAnchor = TimeZone.getDefault()
            Log.d(TAG, "TZAnchor: " + timeZoneAnchor.id)
            Log.d(TAG, "TZAnchor long: " + TimeZone.getTimeZone(timeZoneAnchor.id).id)
            try {
                val tzpref = prefs.getString(KEY_TIMEZONE_ANCHOR,
                        timeZoneAnchor.displayName)
                timeZoneAnchor = TimeZone.getTimeZone(tzpref)
            } catch (e: Exception) {
                Log.e(TAG,
                        "$KEY_TIMEZONE_ANCHOR threw exception: $e")
            }

            return timeZoneAnchor
        }

    // Log.d(TAG, "Preference " + KEY_PERSISTENT_NOTIFICATION + ": "
    // + persistentNotification);
    val persistentNotification: Boolean
        get() {
            var persistentNotification = false
            try {
                persistentNotification = prefs.getBoolean(
                        KEY_PERSISTENT_NOTIFICATION, false)
            } catch (e: Exception) {
                Log.e(TAG,
                        KEY_PERSISTENT_NOTIFICATION + " threw exception: "
                                + e.toString())
            }

            return persistentNotification
        }

    // backup = prefs.setBoolean(KEY_SDCARD_BACKUP, backup);
    var SDCardBackup: Boolean
        get() {
            var backup = false
            try {
                backup = prefs.getBoolean(KEY_SDCARD_BACKUP, true)
            } catch (e: Exception) {
                Log.e(TAG, "$KEY_SDCARD_BACKUP threw exception: $e")
            }

            Log.d(TAG, "Preference $KEY_SDCARD_BACKUP: $backup")
            return backup
        }
        set(backup) {
            val editor = prefs.edit()
            try {
                editor.putBoolean(KEY_SDCARD_BACKUP, backup)
                editor.apply()
            } catch (e: Exception) {
                Log.e(TAG, "$KEY_SDCARD_BACKUP threw exception: $e")
            }

            Log.d(TAG, "Preference $KEY_SDCARD_BACKUP: $backup")
        }

    val weekStartDay: Int
        get() {
            var startDay = Calendar.MONDAY
            try {
                startDay = Integer.valueOf(prefs.getString(KEY_WEEK_START_DAY,
                        Calendar.MONDAY.toString()))
            } catch (e: Exception) {
                Log.e(TAG, "$KEY_WEEK_START_DAY threw exception: $e")
            }

            Log.d(TAG, "Preference $KEY_WEEK_START_DAY: $startDay")
            return startDay
        }

    val weekStartHour: Int
        get() {
            var startHour = 0
            try {
                startHour = Integer.valueOf(prefs.getString(KEY_WEEK_START_HOUR, "0"))
            } catch (e: Exception) {
                Log.e(TAG, "$KEY_WEEK_START_HOUR threw exception: $e")
            }

            Log.d(TAG, "Preference $KEY_WEEK_START_HOUR: $startHour")
            return startHour
        }

    companion object {
        private const val FONTSIZE_TOTAL_DEFAULT = 14.0f
        private const val FONTSIZE_TASKLIST_DEFAULT = 12.0f
        private const val TAG = "PreferenceHelper"

        const val KEY_ALIGN_MINUTES = "align.minutes"
        const val KEY_ALIGN_MINUTES_AUTO = "align.minutes.auto"
        const val KEY_ALIGN_TIME_PICKER = "align.time.picker"
        const val KEY_HOURS_DAY = "hours.day"
        const val KEY_HOURS_WEEK = "hours.week"
        const val KEY_FONTSIZE_TASKLIST = "fontSize.tasklist"
        const val KEY_TOTAL_FONTSIZE = "total.fontSize"
        const val KEY_PERSISTENT_NOTIFICATION = "persistent.notification"
        const val KEY_SDCARD_BACKUP = "db.on.sdcard"
        const val KEY_TIMEZONE_ANCHOR = "tz.anchor"
        const val KEY_WEEK_START_DAY = "week.startday"
        const val KEY_WEEK_START_HOUR = "week.starthour"
    }
}
