package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView

import com.github.kronenpj.iqtimesheet.TimeHelpers

class DayReportFragment : Fragment() {
    private var db: TimeSheetDbAdapter? = null
    private var timeEntryCursor: Cursor? = null
    private var dayHours: Float = 0.toFloat()
    private var footerView: TextView? = null
    private var day = TimeHelpers.millisNow()
    private var reportList: ListView? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(TAG, "setupDayReportFragment")
        db = TimeSheetDbAdapter(activity.applicationContext)
        db!!.open()

        dayHours = TimeSheetActivity.prefs.hoursPerDay

        val rootView = inflater!!.inflate(R.layout.fragment_reportlist,
                container, false)

        reportList = activity.findViewById(R.id.reportList) as ListView
        reportList!!.adapter = MyArrayAdapter<String>(activity,
                android.R.layout.simple_list_item_2, db!!.dayReportList)

        footerView = activity.findViewById(R.id.reportfooter) as TextView
        footerView!!.textSize = TimeSheetActivity.prefs.totalsFontSize

        val child = arrayOf(activity.findViewById(R.id.previous) as Button, activity.findViewById(R.id.today) as Button, activity.findViewById(R.id.next) as Button)

        for (aChild in child) {
            try {
                aChild.setOnClickListener(mButtonListener)
            } catch (e: NullPointerException) {
                Log.e(TAG, "setOnClickListener: " + e.toString())
            }

        }

        return rootView
    }

    private fun fillData() {
        Log.d(TAG, "In fillData.")

        // Cheat a little on the date. This was originally referencing timeIn
        // from the cursor below.
        val date = TimeHelpers.millisToDate(day)
        activity.title = "Day Report - " + date

        footerView!!.text = "Hours worked this day: 0\nHours remaining this day: " + String.format("%.2f", dayHours)

        try {
            timeEntryCursor!!.close()
        } catch (e: NullPointerException) {
            // Do nothing, this is expected sometimes.
        } catch (e: Exception) {
            Log.e(TAG, "timeEntryCursor.close: " + e.toString())
            return
        }

        // If the day being reported is the current week, most probably where
        // the current open task exists, then include it, otherwise omit.
        if (day >= TimeHelpers.millisToStartOfDay(TimeHelpers.millisNow()) && day <= TimeHelpers.millisToEndOfDay(TimeHelpers.millisNow())) {
            timeEntryCursor = db!!.daySummary(day, false)
        } else {
            timeEntryCursor = db!!.daySummary(day, true)
        }

        try {
            timeEntryCursor!!.moveToFirst()
        } catch (e: NullPointerException) {
            Log.e(TAG, "timeEntryCursor.moveToFirst: " + e.toString())
            return
        } catch (e: Exception) {
            Log.e(TAG, "timeEntryCursor.moveToFirst: " + e.toString())
            return
        }

        var accum = 0f
        while (!timeEntryCursor!!.isAfterLast) {
            accum = accum + timeEntryCursor!!.getFloat(timeEntryCursor!!
                    .getColumnIndex(TimeSheetDbAdapter.KEY_TOTAL))
            timeEntryCursor!!.moveToNext()
        }

        footerView!!.text = "Hours worked this day: ${String.format("%.2f", accum)}\nHours remaining this day: ${String.format("%.2f", dayHours - accum)}"

        try {
            reportList!!.adapter = ReportCursorAdapter(activity,
                    R.layout.mysimple_list_item_2, timeEntryCursor,
                    arrayOf(TimeSheetDbAdapter.KEY_TASK, TimeSheetDbAdapter.KEY_TOTAL), intArrayOf(android.R.id.text1, android.R.id.text2))
        } catch (e: Exception) {
            Log.e(TAG, "reportList.setAdapter: " + e.toString())
        }

    }

    /**
     * This method is what is registered with the button to cause an action to
     * occur when it is pressed.
     */
    private val mButtonListener = OnClickListener { v ->
        Log.d(TAG, "onClickListener view id: " + v.id)

        when (v.id) {
            R.id.previous -> day = TimeHelpers.millisToStartOfDay(day) - 1000
            R.id.today -> day = TimeHelpers.millisNow()
            R.id.next -> day = TimeHelpers.millisToEndOfDay(day) + 1000
        }
        fillData()
    }

    companion object {
        private val TAG = "DayReportFragment"
        val EXTRA_TITLE = "title"

        fun createBundle(title: String): Bundle {
            val bundle = Bundle()
            bundle.putString(EXTRA_TITLE, title)
            return bundle
        }
    }

}
