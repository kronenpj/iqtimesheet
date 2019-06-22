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

package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View.OnClickListener
import android.widget.NumberPicker
import android.widget.TimePicker
import android.widget.Toast
import com.github.kronenpj.iqtimesheet.TimeHelpers
import kotlinx.android.synthetic.main.changehourmin.*

/**
 * Activity that provides an interface to change the time of an entry.

 * @author Paul Kronenwetter <kronenpj@gmail.com>
 */
class ChangeTime : Activity() {
    private var timeMillis: Long = -1

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Choose a time"
        setContentView(R.layout.changehourmin)

        val extras = intent.extras
        timeMillis = extras.getLong("time")

        if (TimePicker01 != null) {
            if (TimeSheetActivity.prefs!!.alignTimePicker)
                setTimePickerInterval(TimePicker01 as TimePicker)

            TimePicker01!!.setIs24HourView(true)
            TimePicker01!!.currentHour = TimeHelpers.millisToHour(timeMillis)
            TimePicker01!!.currentMinute = TimeHelpers.millisToMinute(timeMillis) /
                    TimeSheetActivity.prefs!!.alignMinutes
        }

        val child = arrayOf(changeok, changecancel)

        for (aChild in child) {
            try {
                aChild.setOnClickListener(mButtonListener)
            } catch (e: NullPointerException) {
                Toast.makeText(this@ChangeTime, "NullPointerException",
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setTimePickerInterval(timePicker: TimePicker) {
        Log.d(TAG, "In setTimePickerInterval")
        try {
            val fieldMin = Resources.getSystem().getIdentifier("minute",
                    "id", "android")
            val mMinutePicker = timePicker.findViewById(fieldMin) as NumberPicker

            // set number of minutes in desired interval
            mMinutePicker.minValue = 0
            mMinutePicker.maxValue = 59 / TimeSheetActivity.prefs!!.alignMinutes
            Log.d(TAG, "maxValue is: " + mMinutePicker.maxValue)
            val mDisplayedValuesMin = ArrayList<String>()

            // Populate array.
            for (i in 0..59 step TimeSheetActivity.prefs!!.alignMinutes) {
                mDisplayedValuesMin.add(String.format("%02d", i))
            }

            mMinutePicker.displayedValues = mDisplayedValuesMin.toArray(arrayOfNulls<String>(0))
            mMinutePicker.wrapSelectorWheel = true
        } catch (e: Exception) {
            Log.d(TAG, "setTimePickerInterval Exception:")
            Log.d(TAG, e.message)
        }
    }

    /**
     * This method is what is registered with the button to cause an action to
     * occur when it is pressed.
     */
    private val mButtonListener = OnClickListener { v ->
        val newTime = TimeHelpers.millisSetTime(timeMillis,
                TimePicker01!!.currentHour,
                TimePicker01!!.currentMinute * TimeSheetActivity.prefs!!.alignMinutes)

        Log.d(TAG, "onClickListener view id: " + v.id)
        Log.d(TAG, "onClickListener defaulttask id: " + R.id.defaulttask)

        when (v.id) {
            R.id.changecancel -> {
                setResult(RESULT_CANCELED, Intent().setAction("cancel"))
                finish()
            }
            R.id.changeok -> {
                setResult(RESULT_OK,
                        Intent().setAction(java.lang.Long.toString(newTime)))
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "ChangeTime"
    }
}