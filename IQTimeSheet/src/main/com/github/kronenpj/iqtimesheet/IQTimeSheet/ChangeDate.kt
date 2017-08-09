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
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.DatePicker
import android.widget.DatePicker.OnDateChangedListener
import android.widget.TextView
import android.widget.Toast
import com.github.kronenpj.iqtimesheet.TimeHelpers
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.android.synthetic.main.changedate.*

/**
 * Activity that provides an interface to change the date of an entry.

 * @author Paul Kronenwetter <kronenpj@gmail.com>
 */
class ChangeDate : Activity() {
    private var dateChange: DatePicker? = null
    private var dateText: TextView? = null

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Choose a date"
        setContentView(R.layout.changedate)
        dateText = findViewById(R.id.DateText) as TextView

        val extras = intent.extras
        val timeMillis = extras.getLong("time")

        dateChange = findViewById(R.id.DatePicker01) as DatePicker

        val mDateChangedListener = OnDateChangedListener { view, year, monthOfYear, dayOfMonth -> updateDateText(year, monthOfYear, dayOfMonth) }

        dateChange!!.init(TimeHelpers.millisToYear(timeMillis),
                TimeHelpers.millisToMonthOfYear(timeMillis),
                TimeHelpers.millisToDayOfMonth(timeMillis),
                mDateChangedListener)
        updateDateText(TimeHelpers.millisToYear(timeMillis),
                TimeHelpers.millisToMonthOfYear(timeMillis),
                TimeHelpers.millisToDayOfMonth(timeMillis))

        val child = arrayOf(findViewById(R.id.changeok) as Button,
                findViewById(R.id.changecancel) as Button)

        for (aChild in child) {
            try {
                aChild.setOnClickListener(mButtonListener)
            } catch (e: NullPointerException) {
                Toast.makeText(this@ChangeDate, "NullPointerException",
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * This method is what is registered with the button to cause an action to
     * occur when it is pressed.
     */
    private val mButtonListener = OnClickListener { v ->
        val newDate = TimeHelpers.millisSetDate(dateChange!!.year,
                dateChange!!.month + 1, dateChange!!.dayOfMonth)

        Log.d(TAG, "onClickListener view id: " + v.id)
        Log.d(TAG, "onClickListener defaulttask id: " + R.id.defaulttask)

        when (v.id) {
            R.id.changecancel -> {
                setResult(Activity.RESULT_CANCELED, Intent().setAction("cancel"))
                finish()
            }
            R.id.changeok -> {
                setResult(Activity.RESULT_OK, Intent().setAction(java.lang.Long.toString(newDate)))
                finish()
            }
        }
    }

    private fun updateDateText(year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val date = GregorianCalendar(year, monthOfYear, dayOfMonth)
        val simpleDate = SimpleDateFormat("E, MMM d, yyyy", Locale.US)
        dateText!!.text = simpleDate.format(date.time)
    }

    companion object {
        private val TAG = "ChangeDate"
    }
}
