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
import android.widget.DatePicker.OnDateChangedListener
import android.widget.Toast
import com.github.kronenpj.iqtimesheet.TimeHelpers
import kotlinx.android.synthetic.main.changedate.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity that provides an interface to change the date of an entry.

 * @author Paul Kronenwetter <kronenpj@gmail.com>
 */
class ChangeDate : Activity() {

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Choose a date"
        setContentView(R.layout.changedate)

        val extras = intent.extras
        val timeMillis = extras.getLong("time")

        val mDateChangedListener = OnDateChangedListener { view, year, monthOfYear, dayOfMonth -> updateDateText(year, monthOfYear, dayOfMonth) }

        DatePicker01.init(TimeHelpers.millisToYear(timeMillis),
                TimeHelpers.millisToMonthOfYear(timeMillis),
                TimeHelpers.millisToDayOfMonth(timeMillis),
                mDateChangedListener)
        updateDateText(TimeHelpers.millisToYear(timeMillis),
                TimeHelpers.millisToMonthOfYear(timeMillis),
                TimeHelpers.millisToDayOfMonth(timeMillis))

        val child = arrayOf(changeok, changecancel)

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
        val newDate = TimeHelpers.millisSetDate(DatePicker01.year,
                DatePicker01.month + 1, DatePicker01.dayOfMonth)

        Log.d(TAG, "onClickListener view id: " + v.id)
        Log.d(TAG, "onClickListener defaulttask id: " + R.id.defaulttask)

        when (v.id) {
            R.id.changecancel -> {
                setResult(RESULT_CANCELED, Intent().setAction("cancel"))
                finish()
            }
            R.id.changeok -> {
                setResult(RESULT_OK, Intent().setAction(newDate.toString()))
                finish()
            }
        }
    }

    private fun updateDateText(year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val date = GregorianCalendar(year, monthOfYear, dayOfMonth)
        val simpleDate = SimpleDateFormat("E, MMM d, yyyy", Locale.US)
        DateText.text = simpleDate.format(date.time)
    }

    companion object {
        private const val TAG = "ChangeDate"
    }
}
