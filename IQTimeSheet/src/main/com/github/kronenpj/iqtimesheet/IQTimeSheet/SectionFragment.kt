package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.TextView

import com.github.kronenpj.iqtimesheet.TimeHelpers

/**
 * A fragment representing a section of the application.
 */
class SectionFragment : Fragment() {

    /*
     * (non-Javadoc)
     *
     * @see com.github.rtyley.android.sherlock.roboguice.activity.
     * RoboFragmentActivity#onCreateView(LayoutInflater, ViewGroup,
     * Bundle)
     */
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(TAG, "in onCreateView (SectionFragment)")

        when (arguments.getInt(ARG_SECTION_NUMBER)) {
            1 -> return setupTaskListFragment(inflater!!, container!!)
            2 -> return setupDayReportFragment(inflater!!, container!!)
            3 -> return setupWeekReportFragment(inflater!!, container!!)
        }
        return null
    }

    /**
     * Set up the task list fragment.

     * @param inflater  The inflater given the task of instantiating the view.
     * *
     * @param container The view group into which the view will be inflated.
     * *
     * @return The inflated view.
     */
    private fun setupTaskListFragment(inflater: LayoutInflater,
                                      container: ViewGroup): View {
        Log.d(TAG, "in setupTaskListFragment")
        val db = TimeSheetDbAdapter(
                activity.applicationContext)
        //db.open();

        val rootView = inflater.inflate(R.layout.fragment_tasklist,
                container, false)
        val myTaskList = rootView.findViewById(R.id.tasklistfragment) as ListView

        // Populate the ListView with an array adapter with the task items.
        (activity as TimeSheetActivity).refreshTaskListAdapter(myTaskList)

        // Make list items selectable.
        myTaskList.choiceMode = ListView.CHOICE_MODE_SINGLE
        (activity as TimeSheetActivity).setSelected(myTaskList)

        registerForContextMenu(myTaskList)

        myTaskList.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val taskName = parent.getItemAtPosition(position) as String
            val taskID = db.getTaskIDByName(taskName)
            Log.i(TAG, "Processing change for task: $taskName / $taskID")
            if (db.processChange(taskID)) {
                val timeIn = db.timeInForLastClockEntry()

                (activity as TimeSheetActivity).startNotification(db.getTaskNameByID(taskID)!!, timeIn)
                (activity as TimeSheetActivity).setSelected()
            } else {
                (activity as TimeSheetActivity).clearSelected(myTaskList)
                Log.d(TAG, "Closed task ID: $taskID")
                (activity as TimeSheetActivity).stopNotification()
            }
        }

        // This can't be closed because of the onItemClickListener routine
        // above.
        // This probably leaks something, but I'll deal with that later.
        // TODO: See if not closing the DB causes problems..
        // try {
        // db.close();
        // } catch (Exception e) {
        // Log.i(TAG, "setupTaskListFragment db.close: " + e.toString());
        // }

        return rootView
    }

    /**
     * Set up the day report fragment.

     * @param inflater  The inflater given the task of instantiating the view.
     * *
     * @param container The view group into which the view will be inflated.
     * *
     * @return The inflated view.
     */
    private fun setupDayReportFragment(inflater: LayoutInflater,
                                       container: ViewGroup): View {
        Log.d(TAG, "in setupDayReportFragment")
        //val db = TimeSheetDbAdapter(activity.applicationContext)
        //try {
        //    db.open();
        //} catch (Exception e) {
        //    Log.i(TAG, "Database open threw exception" + e);
        //}

        val rootView = inflater.inflate(R.layout.fragment_reportlist,
                container, false)
        val reportList = rootView.findViewById(R.id.reportList) as ListView

        (activity as TimeSheetActivity).refreshReportListAdapter(reportList)

        val footerView = rootView.findViewById(R.id.reportfooter) as TextView

        try {
            footerView.textSize = TimeSheetActivity.prefs!!.totalsFontSize
        } catch (e: NullPointerException) {
            Log.d(TAG, "setupDayReportFragment: NullPointerException prefs: $e")
        }

        val child = arrayOf(rootView.findViewById(R.id.previous) as Button, rootView.findViewById(R.id.today) as Button, rootView.findViewById(R.id.next) as Button)

        /**
         * This method is what is registered with the button to cause an
         * action to occur when it is pressed.
         */
        val mButtonListener = View.OnClickListener { v ->
            Log.d(TAG, "onClickListener view id: " + v.id)

            when (v.id) {
                R.id.previous -> {
                    Log.d(TAG, "onClickListener button: previous")
                    Log.d(TAG, "onClickListener Day of week #: ${TimeHelpers.millisToDayOfWeek(TimeSheetActivity.day)}")
                    Log.d(TAG, "onClickListener Preference  #: ${TimeSheetActivity.prefs!!.weekStartDay}")
                    Log.d(TAG, "onClickListener Previous date: ${TimeHelpers.millisToTimeDate(TimeSheetActivity.day)}")
                    if (TimeHelpers.millisToDayOfWeek(TimeSheetActivity.day) != TimeSheetActivity.prefs!!.weekStartDay) {
                        TimeSheetActivity.day = TimeHelpers.millisToStartOfDay(TimeSheetActivity.day) - 1000
                        Log.d(TAG, "onClickListener New date: ${TimeHelpers.millisToTimeDate(TimeSheetActivity.day)}")
                    } else {
                        if (TimeHelpers.millisToHour(TimeSheetActivity.day) < TimeSheetActivity.prefs!!.weekStartHour) {
                            TimeSheetActivity.day = TimeHelpers.millisToStartOfDay(TimeSheetActivity.day) - 1000
                            Log.d(TAG, "onClickListener New date: ${TimeHelpers.millisToTimeDate(TimeSheetActivity.day)}")
                        } else {
                            TimeSheetActivity.day = TimeHelpers.millisToStartOfDay(TimeSheetActivity.day) + TimeSheetActivity.prefs!!.weekStartHour * 3600 * 1000 - 1000
                            Log.d(TAG, "onClickListener New date: ${TimeHelpers.millisToTimeDate(TimeSheetActivity.day)}")
                        }
                    }
                }
                R.id.today -> {
                    Log.d(TAG, "onClickListener button: today")
                    TimeSheetActivity.day = TimeHelpers.millisNow()
                }
                R.id.next -> {
                    Log.d(TAG, "onClickListener button: next")
                    Log.d(TAG, "onClickListener Day of week #: ${TimeHelpers.millisToDayOfWeek(TimeSheetActivity.day)}")
                    Log.d(TAG, "onClickListener Preference  #: ${TimeSheetActivity.prefs!!.weekStartDay}")
                    Log.d(TAG, "onClickListener Previous date: ${TimeHelpers.millisToTimeDate(TimeSheetActivity.day)}")
                    if (TimeHelpers.millisToDayOfWeek(TimeSheetActivity.day) != TimeSheetActivity.prefs!!.weekStartDay) {
                        TimeSheetActivity.day = TimeHelpers.millisToEndOfDay(TimeSheetActivity.day) + 1000
                        Log.d(TAG, "onClickListener New date: ${TimeHelpers.millisToTimeDate(TimeSheetActivity.day)}")
                    } else {
                        if (TimeHelpers.millisToHour(TimeSheetActivity.day) > TimeSheetActivity.prefs!!.weekStartHour) {
                            TimeSheetActivity.day = TimeHelpers.millisToEndOfDay(TimeSheetActivity.day) + 1000
                            Log.d(TAG, "onClickListener New date: ${TimeHelpers.millisToTimeDate(TimeSheetActivity.day)}")
                        } else {
                            TimeSheetActivity.day = TimeHelpers.millisToEndOfDay(TimeSheetActivity.day) +
                                    (TimeSheetActivity.prefs!!.weekStartHour * 3600 * 1000).toLong() + 1000
                            Log.d(TAG, "onClickListener New date: ${TimeHelpers.millisToTimeDate(TimeSheetActivity.day)}")
                        }
                    }
                }
            }

            val headerView = v.rootView
                    .findViewById(R.id.reportheader) as TextView
            val date = TimeHelpers.millisToDate(TimeSheetActivity.day)
            headerView.text = "Day Report - $date"
            Log.d(TAG, "New day is: " + date)

            (activity as TimeSheetActivity).refreshReportListAdapter(v.rootView
                    .findViewById(R.id.reportList) as ListView)
        }

        for (aChild in child) {
            try {
                aChild.setOnClickListener(mButtonListener)
            } catch (e: NullPointerException) {
                Log.e(TAG, "setOnClickListener: " + e)
            }

        }

        return rootView
    }

    /**
     * Set up the week report fragment.

     * @param inflater  The inflater given the task of instantiating the view.
     * *
     * @param container The view group into which the view will be inflated.
     * *
     * @return The inflated view.
     */
    private fun setupWeekReportFragment(inflater: LayoutInflater,
                                        container: ViewGroup): View {
        Log.d(TAG, "in setupWeekReportFragment")
        //val db = TimeSheetDbAdapter(activity.applicationContext)
        //try {
        //    db.open();
        //} catch (Exception e) {
        //    Log.i(TAG, "Database open threw exception" + e);
        //}

        val rootView = inflater.inflate(R.layout.fragment_weekreportlist,
                container, false)
        val reportList = rootView.findViewById(R.id.weekList) as ListView

        (activity as TimeSheetActivity).refreshWeekReportListAdapter(reportList)

        val footerView = rootView.findViewById(R.id.weekfooter) as TextView
        try {
            footerView.textSize = TimeSheetActivity.prefs!!.totalsFontSize
        } catch (e: NullPointerException) {
            Log.d(TAG, "setupWeekeportFragment: NullPointerException prefs: $e")
        }

        val child = arrayOf(rootView.findViewById(R.id.wprevious) as Button,
                rootView.findViewById(R.id.wtoday) as Button,
                rootView.findViewById(R.id.wnext) as Button)

        /**
         * This method is what is registered with the button to cause an
         * action to occur when it is pressed.
         */
        val mButtonListener = View.OnClickListener { v ->
            Log.d(TAG, "onClickListener view id: " + v.id)

            when (v.id) {
                R.id.wprevious -> {
                    Log.d(TAG, "onClickListener button: wprevious")
                    Log.d(TAG, "onClickListener Day of week #: ${TimeHelpers.millisToDayOfWeek(TimeSheetActivity.day)}")
                    Log.d(TAG, "onClickListener Preference  #: ${TimeSheetActivity.prefs!!.weekStartDay}")
                    Log.d(TAG, "onClickListener Previous date: ${TimeHelpers.millisToTimeDate(TimeSheetActivity.day)}")

                    // TimeSheetActivity.day = TimeHelpers.millisToStartOfWeek(TimeSheetActivity.day) - 1000;
                    TimeSheetActivity.day = TimeHelpers.millisToStartOfWeek(TimeSheetActivity.day,
                            TimeSheetActivity.prefs!!.weekStartDay,
                            TimeSheetActivity.prefs!!.weekStartHour) - 1000
                    Log.d(TAG, "onClickListener New date: ${TimeHelpers.millisToTimeDate(TimeSheetActivity.day)}")
                }
                R.id.wtoday -> {
                    TimeSheetActivity.day = TimeHelpers.millisNow()
                    Log.d(TAG, "onClickListener button: wtoday")
                }
                R.id.wnext -> {
                    Log.d(TAG, "onClickListener button: wnext")
                    Log.d(TAG, "onClickListener Day of week #: ${TimeHelpers.millisToDayOfWeek(TimeSheetActivity.day)}")
                    Log.d(TAG, "onClickListener Preference  #: ${TimeSheetActivity.prefs!!.weekStartDay}")
                    Log.d(TAG, "onClickListener Previous date: ${TimeHelpers.millisToTimeDate(TimeSheetActivity.day)}")

                    // TimeSheetActivity.day = TimeHelpers.millisToEndOfWeek(TimeSheetActivity.day) + 1000;
                    TimeSheetActivity.day = TimeHelpers.millisToEndOfWeek(TimeSheetActivity.day,
                            TimeSheetActivity.prefs!!.weekStartDay,
                            TimeSheetActivity.prefs!!.weekStartHour) + 1000
                    Log.d(TAG, "onClickListener New date: ${TimeHelpers.millisToTimeDate(TimeSheetActivity.day)}")
                }
            }

            val headerView = v.rootView
                    .findViewById(R.id.weekheader) as TextView
            val date = TimeHelpers.millisToDate(TimeSheetActivity.day)
            headerView.text = "Week Report - $date"
            Log.d(TAG, "New day is: " + date)

            (activity as TimeSheetActivity).refreshWeekReportListAdapter(v.rootView
                    .findViewById(R.id.weekList) as ListView)
        }

        for (aChild in child) {
            try {
                aChild.setOnClickListener(mButtonListener)
            } catch (e: NullPointerException) {
                Log.e(TAG, "setOnClickListener: " + e)
            }

        }

        return rootView
    }

    companion object {
        private val TAG = "SectionFragment"

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        val ARG_SECTION_NUMBER = "section_number"
    }
}
