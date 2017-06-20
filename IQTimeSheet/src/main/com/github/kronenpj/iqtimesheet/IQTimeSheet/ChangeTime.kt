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
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.Toast
import com.github.kronenpj.iqtimesheet.TimeHelpers
import com.github.kronenpj.iqtimesheet.TimePicker

/**
 * Activity that provides an interface to change the time of an entry.

 * @author Paul Kronenwetter <kronenpj></kronenpj>@gmail.com>
 */
class ChangeTime : Activity() {
    private var timeChange: TimePicker? = null
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

        timeChange = findViewById(R.id.TimePicker01) as TimePicker

        if (timeChange != null) {
            timeChange!!.setIs24HourView(true)
            timeChange!!.currentHour = TimeHelpers.millisToHour(timeMillis)
            timeChange!!.currentMinute = TimeHelpers.millisToMinute(timeMillis)

            if (TimeSheetActivity.prefs!!.alignTimePicker)
                timeChange!!.setInterval(TimeSheetActivity.prefs!!.alignMinutes)
        }

        val child = arrayOf(findViewById(R.id.changeok) as Button, findViewById(R.id.changecancel) as Button)

        for (aChild in child) {
            try {
                aChild.setOnClickListener(mButtonListener)
            } catch (e: NullPointerException) {
                Toast.makeText(this@ChangeTime, "NullPointerException",
                        Toast.LENGTH_SHORT).show()
            }

        }
    }

    /**
     * This method is what is registered with the button to cause an action to
     * occur when it is pressed.
     */
    private val mButtonListener = OnClickListener { v ->
        val newTime = TimeHelpers.millisSetTime(timeMillis,
                timeChange!!.currentHour!!, timeChange!!.currentMinute!!)

        Log.d(TAG, "onClickListener view id: " + v.id)
        Log.d(TAG, "onClickListener defaulttask id: " + R.id.defaulttask)

        when (v.id) {
            R.id.changecancel -> {
                setResult(Activity.RESULT_CANCELED, Intent().setAction("cancel"))
                finish()
            }
            R.id.changeok -> {
                setResult(Activity.RESULT_OK,
                        Intent().setAction(java.lang.Long.toString(newTime)))
                finish()
            }
        }
    }

    companion object {
        private val TAG = "ChangeTime"
    }
}