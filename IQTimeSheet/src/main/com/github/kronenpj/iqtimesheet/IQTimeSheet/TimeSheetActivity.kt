package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.*
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.Checkable
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.github.kronenpj.iqtimesheet.TimeHelpers
import java.util.*

class TimeSheetActivity : AppCompatActivity() {

    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * [android.support.v4.app.FragmentPagerAdapter] derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    /**
     * The [ViewPager] that will host the section contents.
     */
    private var mViewPager: ViewPager? = null
    /**
     * Return the menu object for testing.

     * @return optionMenu
     */
    var optionsMenu: Menu? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_time_sheet)

        prefs = PreferenceHelper(applicationContext)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.pager) as ViewPager
        mViewPager!!.adapter = mSectionsPagerAdapter

        mViewPager!!.addOnPageChangeListener(object : OnPageChangeListener {

            override fun onPageScrolled(position: Int, positionOffset: Float,
                                        positionOffsetPixels: Int) {
                // Log.i(TAG, "Should I handle onPageScrolled??");
            }

            override fun onPageSelected(position: Int) {
                Log.v(TAG, "In onPageSelected")
                Log.d(TAG, "Today         : " + TimeHelpers.millisToDayOfWeek(TimeHelpers.millisNow()))
                Log.d(TAG, "DoW Preference: " + prefs!!.weekStartDay)
                Log.d(TAG, "HoD Preference: " + prefs!!.weekStartHour)
                checkCrossDayClock()
                if (TimeHelpers.millisToDayOfWeek(TimeHelpers.millisNow()) == TimeSheetActivity.prefs!!.weekStartDay && TimeSheetActivity.prefs!!.weekStartHour > 0)
                    checkCrossSplitClock(TimeSheetActivity.prefs!!.weekStartHour)
                when (position) {
                    0 -> {
                        run {
                            Log.d(TAG, "Selected task page")
                            refreshTaskListAdapter()
                        }
                        run {
                            Log.d(TAG, "Selected day report page")
                            refreshReportListAdapter()
                        }
                        run {
                            Log.d(TAG, "Selected week report page")
                            refreshWeekReportListAdapter()
                        }
                    }
                    1 -> {
                        run {
                            Log.d(TAG, "Selected day report page")
                            refreshReportListAdapter()
                        }
                        run {
                            Log.d(TAG, "Selected week report page")
                            refreshWeekReportListAdapter()
                        }
                    }
                    2 -> {
                        Log.d(TAG, "Selected week report page")
                        refreshWeekReportListAdapter()
                    }
                }
                updateTitleBar()
            }

            override fun onPageScrollStateChanged(state: Int) {
                // Log.i(TAG, "Should I handle onPageScrollStateChanged??");
            }
        })

        setSelected()
        updateTitleBar()
        processPermissions()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        try {
            refreshTaskListAdapter()
        } catch (e: NullPointerException) {
            Log.d(TAG, "onResume refreshTaskListAdapter: " + e.toString())
        }

        setSelected()
        try {
            refreshReportListAdapter()
        } catch (e: NullPointerException) {
            Log.d(TAG, "onResume refreshReportListAdapter: " + e.toString())
        }

        try {
            refreshWeekReportListAdapter()
        } catch (e: NullPointerException) {
            Log.d(TAG, "onResume refreshWeekReportListAdapter: " + e.toString())
        }

        try {
            updateTitleBar()
        } catch (e: NullPointerException) {
            Log.d(TAG, "onResume updateTitleBar: " + e.toString())
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.time_sheet, menu)
        // Hanging on to this so it can be used for testing.
        optionsMenu = menu
        return true
    }

    /**
     * This method is called when the sending activity has finished, with the
     * result it supplied.

     * @param requestCode The original request code as given to startActivity().
     * *
     * @param resultCode  From sending activity as per setResult().
     * *
     * @param data        From sending activity as per setResult().
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val db = TimeSheetDbAdapter(applicationContext)
        //try {
        //    db.open();
        //} catch (Exception e) {
        //    Log.i(TAG, "Database open threw exception" + e);
        //}

        // Check to see that what we received is what we wanted to see.
        if (requestCode == ActivityCodes.TASKADD.ordinal) {
            // This is a standard resultCode that is sent back if the
            // activity doesn't supply an explicit result. It will also
            // be returned if the activity failed to launch.
            if (resultCode == Activity.RESULT_OK) {
                // Our protocol with the sending activity is that it will send
                // text in 'data' as its result.
                if (data != null) {
                    try {
                        if (!data.hasExtra("parent"))
                            db.createTask(data.action)
                        else {
                            db.createTask(data.action,
                                    data.getStringExtra("parent"),
                                    data.getIntExtra("percentage", 100))
                        }
                    } catch (e: NullPointerException) {
                        Log.d(TAG, "TaskAdd Result: " + e.toString())
                    }

                }
                try {
                    refreshTaskListAdapter(findViewById(R.id.tasklistfragment) as ListView)
                } catch (e: NullPointerException) {
                    Log.d(TAG, "TaskAdd refreshTaskListAdapter: " + e.toString())
                }

            }
        } else if (requestCode == ActivityCodes.TASKREVIVE.ordinal) {
            // This one is a special case, since it has its own database
            // adapter, we let it change the state itself rather than passing
            // the result back to us.
            if (resultCode == Activity.RESULT_OK) {
                try {
                    refreshTaskListAdapter(findViewById(R.id.tasklistfragment) as ListView)
                } catch (e: NullPointerException) {
                    Log.d(TAG, "TaskRevive refreshTaskListAdapter: " + e.toString())
                }

            }
        } else if (requestCode == ActivityCodes.TASKEDIT.ordinal) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val result = data.action
                    var oldData: String? = null

                    val extras = data.extras
                    if (extras != null) {
                        oldData = extras.getString("oldTaskName")
                    }

                    // TODO: Determine what needs to be done to change these
                    // database fields.
                    if (data.hasExtra("parent")) {
                        val taskID = db.getTaskIDByName(oldData!!)
                        // int oldSplit = db.getSplitTaskFlag(oldData);
                        val parentID = db.getTaskIDByName(data
                                .getStringExtra("parent"))
                        db.alterSplitTask(taskID, parentID,
                                data.getIntExtra("percentage", 100),
                                data.getIntExtra("split", 0))
                    }

                    if (oldData != null && result != null) {
                        db.renameTask(oldData, result)
                    }
                }
                try {
                    refreshTaskListAdapter()
                    refreshReportListAdapter()
                    refreshWeekReportListAdapter()
                    updateTitleBar()
                } catch (e: NullPointerException) {
                    Log.d(TAG, "TaskEdit refreshTaskListAdapter: " + e.toString())
                }

            }
        }
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_settings -> {
                val intent = Intent(applicationContext, MyPreferenceActivity::class.java)
                try {
                    startActivityForResult(intent, ActivityCodes.PREFS.ordinal)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "RuntimeException caught in onOptionsItemSelected for MyPreferenceActivity")
                    Log.e(TAG, e.localizedMessage)
                }

                return true
            }
            R.id.menu_edit_day_entries -> {
                val intent = Intent(applicationContext, EditDayEntriesHandler::class.java)
                try {
                    startActivityForResult(intent, ActivityCodes.EDIT.ordinal)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "RuntimeException caught in onOptionsItemSelected for EditDayEntriesHandler")
                    Log.e(TAG, e.localizedMessage)
                }

                return true
            }
            R.id.menu_revive_task -> {
                val intent = Intent(applicationContext, ReviveTaskFragment::class.java)
                try {
                    startActivityForResult(intent, ActivityCodes.TASKREVIVE.ordinal)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "RuntimeException caught in onOptionsItemSelected for ReviveTaskHandler")
                    Log.e(TAG, e.localizedMessage)
                }

                return true
            }
            R.id.menu_new_task -> {
                val intent = Intent(applicationContext, AddTaskHandler::class.java)
                try {
                    startActivityForResult(intent, ActivityCodes.TASKADD.ordinal)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "RuntimeException caught in onOptionsItemSelected for AddTaskHandler")
                    Log.e(TAG, e.localizedMessage)
                }

                return true
            }
            R.id.menu_backup -> {
                if (!SDBackup.doSDBackup(MySqlHelper.DATABASE_NAME,
                        applicationContext.packageName)) {
                    Log.w(TAG, "doSDBackup failed.")
                    Toast.makeText(applicationContext,
                            "Database backup failed.", Toast.LENGTH_LONG).show()
                } else {
                    Log.i(TAG, "doSDBackup succeeded.")
                    Toast.makeText(applicationContext,
                            "Database backup succeeded.", Toast.LENGTH_SHORT)
                            .show()
                }

                return true
            }
            R.id.menu_restore -> {
                Log.d(TAG, "in onOptionsItemSelected (restore)")
                if (prefs != null && prefs!!.SDCardBackup) {
                    val newFragment = MyYesNoDialog
                            .newInstance(R.string.restore_title)
                    newFragment.show(supportFragmentManager, "restore_dialog")
                }
                refreshTaskListAdapter()
                updateTitleBar()
                setSelected()
                return true
            }
            R.id.menu_about -> {
                val intent = Intent(applicationContext, AboutDialog::class.java)
                try {
                    startActivity(intent)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "RuntimeException caught in onOptionsItemSelected for AboutDialog")
                    Log.e(TAG, e.localizedMessage)
                }

                return true
            }
            else -> return super.onOptionsItemSelected(menuItem)
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
     * android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    override fun onCreateContextMenu(menu: ContextMenu, v: View,
                                     menuInfo: ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.add(0, ActivityCodes.EDIT_ID.ordinal, 0, R.string.taskedit)
        menu.add(0, ActivityCodes.RETIRE_ID.ordinal, 0, R.string.taskretire)
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    override fun onContextItemSelected(item: android.view.MenuItem): Boolean {
        val info = item
                .menuInfo as AdapterContextMenuInfo
        if (item.itemId == ActivityCodes.EDIT_ID.ordinal) {
            Log.d(TAG, "Edit task: " + info.id)
            val intent = Intent(applicationContext,
                    EditTaskHandler::class.java)
            intent.putExtra("taskName", (info.targetView as TextView).text
                    .toString())
            try {
                startActivityForResult(intent, ActivityCodes.TASKEDIT.ordinal)
            } catch (e: RuntimeException) {
                Toast.makeText(applicationContext, "RuntimeException",
                        Toast.LENGTH_SHORT).show()
                Log.d(TAG, e.localizedMessage)
                Log.e(TAG, "RuntimeException caught.")
            }

            refreshTaskListAdapter(info.targetView.parent as ListView)
            return true
        }
        if (item.itemId == ActivityCodes.RETIRE_ID.ordinal) {
            val db = TimeSheetDbAdapter(
                    applicationContext)

            val parentTaskID = db.getTaskIDByName((info.targetView as TextView).text.toString())
            // Retire children.
            val children = db.fetchChildTasks(parentTaskID)
            for (childID in children) {
                Log.d(TAG, "Trying to retire item: " + childID + " (" + db.getTaskNameByID(childID) + ")")
                db.deactivateTask(db.getTaskNameByID(childID)!!)
                Log.v(TAG, "Retired Child item: " + childID + " (" + db.getTaskNameByID(childID) + ")")
            }

            // Retire original task
            db.deactivateTask(parentTaskID)

            refreshTaskListAdapter(info.targetView.parent as ListView)
            return true
        }
        return super.onContextItemSelected(item)
    }

    /**
     * Called when the activity is first created to create a dialog.
     */
    override fun onCreateDialog(dialogId: Int): Dialog? {
        Log.d(TAG, "In onCreateDialog")
        var dialog: Dialog? = null
        val builder: AlertDialog.Builder

        when (dialogId) {
            CROSS_DIALOG -> {
                builder = AlertDialog.Builder(applicationContext)
                builder.setMessage(
                        "The last entry is still open from yesterday." + "  What should I do?")
                        .setCancelable(false)
                        .setPositiveButton("Close"
                        ) { dialog, id ->
                            val db = TimeSheetDbAdapter(
                                    applicationContext)
                            db.closeEntry()
                            //db.close();
                            clearSelected()
                        }
                        .setNegativeButton("Close & Re-open"
                        ) { dialog, id ->
                            val db = TimeSheetDbAdapter(
                                    applicationContext)
                            val taskID = db.taskIDForLastClockEntry()
                            val now = TimeHelpers.millisNow()
                            val today = TimeHelpers
                                    .millisToStartOfDay(now)
                            db.createEntry(taskID, today)
                            //db.close();
                            setSelected()
                        }
                dialog = builder.create()
            }
        // This may be dead code now...
            CONFIRM_RESTORE_DIALOG -> {
                Log.d(TAG, "in onCreateDialog (restore)")
                builder = AlertDialog.Builder(applicationContext)
                builder.setMessage(
                        "This will overwrite the database." + "  Proceed?")
                        .setCancelable(true)
                        .setPositiveButton("Yes"
                        ) { dialog, id ->
                            Log.d(TAG, "in onClick (restore dialog)")
                            if (!SDBackup.doSDRestore(
                                    MySqlHelper.DATABASE_NAME,
                                    applicationContext
                                            .packageName)) {
                                Log.w(TAG, "doSDRestore failed.")
                                Toast.makeText(applicationContext,
                                        "Database restore failed.",
                                        Toast.LENGTH_LONG).show()
                            } else {
                                Log.i(TAG, "doSDRestore succeeded.")
                                Toast.makeText(applicationContext,
                                        "Database restore succeeded.",
                                        Toast.LENGTH_SHORT).show()
                            }
                            // fillData();
                            Log.d(TAG,
                                    "onCreateDialog restore dialog.  Calling refreshTaskListAdapter")
                            refreshTaskListAdapter()
                            updateTitleBar()
                            setSelected()
                        }
                        .setNegativeButton("No"
                        ) { dialog, id -> dialog.cancel() }
                dialog = builder.create()
            }
        }

        return dialog
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a DummySectionFragment (defined as a static inner class
            // below) with the page number as its lone argument.
            val fragment = SectionFragment()
            val args = Bundle()
            args.putInt(SectionFragment.ARG_SECTION_NUMBER, position + 1)
            fragment.arguments = args

            return fragment
        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            val l = Locale.getDefault()
            when (position) {
                0 -> return getString(R.string.title_section1).toUpperCase(l)
                1 -> return getString(R.string.title_section2).toUpperCase(l)
                2 -> return getString(R.string.title_section3).toUpperCase(l)
            }
            return null
        }
    }

    /**
     * Notify the task list that there are no items selected.
     */
    private fun clearSelected() {
        Log.d(TAG, "in clearSelected()")
        val myTaskList = findViewById(R.id.tasklistfragment) as ListView
        //if (myTaskList == null) {
        //    Log.i(TAG, "findViewByID(tasklistfragment) returned null.")
        //    return
        //}

        clearSelected(myTaskList)
    }

    /**
     * Notify the task list that there are no items selected.

     * @param myTaskList The view ID of the task list.
     */
    internal fun clearSelected(myTaskList: ListView) {
        Log.d(TAG, "clearSelected")
        myTaskList.clearChoices()
        for (i in 0..myTaskList.count - 1)
            try {
                (myTaskList.getChildAt(i) as Checkable).isChecked = false
            } catch (e: NullPointerException) {
                Log.d(TAG, "NullPointerException at item " + i)
            }

    }

    /**
     * Enable the currently selected item in the task list.
     */
    internal fun setSelected() {
        Log.d(TAG, "in setSelected()")
        if (findViewById(R.id.tasklistfragment) == null) {
            Log.i(TAG, "findViewByID(tasklistfragment) returned null.")
            return
        }

        val myTaskList = findViewById(R.id.tasklistfragment) as ListView
        setSelected(myTaskList)
    }

    /**
     * Enable the currently selected item in the task list.

     * @param myTaskList The view ID of the task list.
     */
    internal fun setSelected(myTaskList: ListView) {
        Log.d(TAG, "in setSelected")
        val db = TimeSheetDbAdapter(applicationContext)
        //try {
        //    db.open();
        //} catch (Exception e) {
        //    Log.i(TAG, "Database open threw exception" + e);
        //}
        val timeOut = db.timeOutForLastClockEntry()
        Log.d(TAG, "Last Time Out: " + timeOut + " / " + TimeHelpers.millisToTimeDate(timeOut))

        if (timeOut != 0L) {
            Log.d(TAG, "Returning.")
            //db.close();
            return
        }

        Log.e(TAG, "myTaskList child count is: " + myTaskList.count)

        myTaskList.clearChoices()

        val lastTaskID = db.taskIDForLastClockEntry()
        Log.d(TAG, "Last Task ID: " + lastTaskID)

        val taskName = db.getTaskNameByID(lastTaskID)
        // TODO: There should be a better way to do this.
        // Iterate over the entire ListView to find the name of the
        // entry that is to be selected.
        for (i in 0..myTaskList.count - 1) {
            if (taskName!!.equals(myTaskList
                    .getItemAtPosition(i) as String, ignoreCase = true)) {
                myTaskList.setItemChecked(i, true)
                myTaskList.setSelection(i)
            }
        }

        //db.close();
    }

    /**
     * Refresh the task list. Looking up the list view.
     */
    private fun refreshTaskListAdapter() {
        Log.d(TAG, "In refreshTaskListAdapter()")
        if (findViewById(R.id.tasklistfragment) == null) {
            Log.i(TAG, "findViewByID(tasklistfragment) returned null.")
            return
        }
        refreshTaskListAdapter(findViewById(R.id.tasklistfragment) as ListView)
    }

    /**
     * Refresh the task list.

     * @param myTaskList The view ID of the task list.
     */
    internal fun refreshTaskListAdapter(myTaskList: ListView) {
        Log.d(TAG, "In refreshTaskListAdapter")
        val db = TimeSheetDbAdapter(applicationContext)

        // (Re-)Populate the ListView with an array adapter with the task items.
        myTaskList.adapter = MyArrayAdapter<String>(applicationContext,
                android.R.layout.simple_list_item_single_choice, db.getTasksList())

        setSelected(myTaskList)
    }

    /**
     * Refresh the day report list. Looking up the list view.
     */
    private fun refreshReportListAdapter() {
        Log.d(TAG, "In refreshReportListAdapter()")
        if (findViewById(R.id.reportList) == null) {
            Log.i(TAG, "findViewByID(reportList) returned null.")
            return
        }
        refreshReportListAdapter(findViewById(R.id.reportList) as ListView)
    }

    /**
     * Refresh the day report list.

     * @param myReportList The view ID of the list to refresh.
     */
    internal fun refreshReportListAdapter(myReportList: ListView) {
        Log.d(TAG, "In refreshReportListAdapter")

        val db = TimeSheetDbAdapter(applicationContext)
        val dayHours = TimeSheetActivity.prefs!!.hoursPerDay
        val date = TimeHelpers.millisToDate(day)
        Log.d(TAG, "refreshReportListAdapter: Updating to " + TimeHelpers.millisToTimeDate(day))

        val headerView = myReportList.rootView
                .findViewById(R.id.reportheader) as TextView
        headerView.text = "Day Report - $date"

        val footerView = myReportList.rootView.findViewById(R.id.reportfooter) as TextView
        footerView.text = String.format(Locale.US, "Hours worked this day: 0\n" +
                "Hours remaining this day: %.2f", dayHours)

        val timeEntryCursor: Cursor

        // If the day being reported is the current week, most probably
        // where the current open task exists, then include it, otherwise
        // omit.
        try {
            if (day >= TimeHelpers.millisToStartOfDay(TimeHelpers.millisNow()) &&
                    day <= TimeHelpers.millisToEndOfDay(TimeHelpers.millisNow())) {
                timeEntryCursor = db.daySummary(day, false)!!
            } else {
                timeEntryCursor = db.daySummary(day, true)!!
            }

            timeEntryCursor.moveToFirst()
        } catch (e: NullPointerException) {
            Log.e(TAG, "timeEntryCursor.moveToFirst: " + e.toString())
            myReportList.adapter = null
            return
        } catch (e: Exception) {
            Log.e(TAG, "timeEntryCursor.moveToFirst: " + e.toString())
            return
        }

        var accum = 0f
        while (!timeEntryCursor.isAfterLast) {
            accum += timeEntryCursor.getFloat(timeEntryCursor.getColumnIndex("total"))
            timeEntryCursor.moveToNext()
        }

        //footerView.text = "Hours worked this day: "
        //+String.format(Locale.US, "%.2f", accum) + "\nHours remaining this day: "
        //+String.format(Locale.US, "%.2f", dayHours - accum)
        footerView.text = String.format(Locale.US, "Hours worked this day: %.2f\n" +
                "Hours remaining this day: %.2f", accum, dayHours - accum)

        try {
            myReportList.adapter = ReportCursorAdapter(
                    applicationContext, R.layout.mysimple_list_item_2,
                    timeEntryCursor, arrayOf("task", "total"),
                    intArrayOf(android.R.id.text1, android.R.id.text2))
            Log.i(TAG, "reportList.setAdapter: updated")
        } catch (e: Exception) {
            Log.e(TAG, "reportList.setAdapter: " + e.toString())
        }

    }

    /**
     * Refresh the week report list. Looking up the list view.
     */
    private fun refreshWeekReportListAdapter() {
        Log.d(TAG, "In refreshWeekReportListAdapter()")
        if (findViewById(R.id.weekList) == null) {
            Log.i(TAG, "findViewByID(weekList) returned null.")
            return
        }
        refreshWeekReportListAdapter(findViewById(R.id.weekList) as ListView)
    }

    /**
     * Refresh the week report list.

     * @param myReportList The view ID of the list to refresh.
     */
    internal fun refreshWeekReportListAdapter(myReportList: ListView) {
        Log.d(TAG, "In refreshWeekReportListAdapter")

        val db = TimeSheetDbAdapter(applicationContext)
        val weekHours = TimeSheetActivity.prefs!!.hoursPerWeek
        val date = TimeHelpers.millisToDate(TimeHelpers.millisToEndOfWeek(day,
                prefs!!.weekStartDay, prefs!!.weekStartHour))
        Log.d(TAG, "refreshWeekReportListAdapter: Updating to " + date)

        try {
            val headerView = myReportList.rootView
                    .findViewById(R.id.weekheader) as TextView
            headerView.text = "Week Report - W/E: $date"
        } catch (e: NullPointerException) {
            Log.d(TAG, "Caught NullPointerException in refreshWeekReportListAdapter.")
            return
        }

        val footerView = myReportList.rootView
                .findViewById(R.id.weekfooter) as TextView
        footerView.text = String.format(Locale.US, "Hours worked this week: 0\n" +
                "Hours remaining this week: %.2f", weekHours)

        val timeEntryCursor: Cursor

        // If the day being reported is the current week, most probably
        // where the current open task exists, then include it, otherwise
        // omit.
        if (day >= TimeHelpers.millisToStartOfWeek(TimeHelpers.millisNow()) && day <= TimeHelpers.millisToEndOfWeek(TimeHelpers.millisNow(),
                prefs!!.weekStartDay, prefs!!.weekStartHour)) {
            timeEntryCursor = db.weekSummary(day, false)!!
        } else {
            timeEntryCursor = db.weekSummary(day, true)!!
        }

        try {
            timeEntryCursor.moveToFirst()
        } catch (e: NullPointerException) {
            Log.e(TAG, "timeEntryCursor.moveToFirst: " + e.toString())
            myReportList.adapter = null
            return
        } catch (e: Exception) {
            Log.e(TAG, "timeEntryCursor.moveToFirst: " + e.toString())
            return
        }

        var accum = 0f
        while (!timeEntryCursor.isAfterLast) {
            accum += timeEntryCursor.getFloat(timeEntryCursor.getColumnIndex("total"))
            timeEntryCursor.moveToNext()
        }

        footerView.text = String.format(Locale.US, "Hours worked this week: %.2f\n" +
                "Hours remaining this week: %.2f", accum, weekHours - accum)

        try {
            myReportList.adapter = ReportCursorAdapter(
                    applicationContext, R.layout.mysimple_list_item_2,
                    timeEntryCursor, arrayOf("task", "total"), intArrayOf(android.R.id.text1, android.R.id.text2))
        } catch (e: Exception) {
            Log.e(TAG, "reportList.setAdapter: " + e.toString())
        }

    }

    /**
     * Instantiate the persistent notification.

     * @param taskName Name of the task currently active (clocked into)
     * *
     * @param timeIn   The time (in nanoseconds) the task started
     */
    internal fun startNotification(taskName: String, timeIn: Long) {
        if (!prefs!!.persistentNotification) {
            return
        }
        val myNotification = NotificationCompat.Builder(
                applicationContext)
                .setContentTitle(resources.getString(R.string.notification_title))
                .setContentText(taskName).setWhen(timeIn)
                .setContentIntent(contentIntent).setAutoCancel(false).setOngoing(true)
                .setSmallIcon(R.drawable.icon_small)

        // From: guide/topics/ui/notifiers/notifications.html
        // Creates an explicit intent for an Activity in your app
        val resultIntent = Intent(this, TimeSheetActivity::class.java)

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        val stackBuilder = TaskStackBuilder.create(this)
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(TimeSheetActivity::class.java)
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        myNotification.setContentIntent(resultPendingIntent)

        // mId allows you to update the notification later on.
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(MY_NOTIFICATION_ID, myNotification.build())
    }

    /**
     * Stop / turn off the persistent notification.
     */
    internal fun stopNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(MY_NOTIFICATION_ID)
        } catch (e: NullPointerException) {
            // Do nothing. The preference was probably set to false, so this was
            // never created.
        }

    }

    /**
     * Handle the button event from the restore dialog.
     */
    fun doRestoreClick() {
        Log.d(TAG, "in doRestoreClick")
        if (!SDBackup.doSDRestore(MySqlHelper.DATABASE_NAME,
                applicationContext.packageName)) {
            Log.w(TAG, "doSDRestore failed.")
            Toast.makeText(applicationContext, "Database restore failed.",
                    Toast.LENGTH_LONG).show()
        } else {
            Log.i(TAG, "doSDRestore succeeded.")
            Toast.makeText(applicationContext,
                    "Database restore succeeded.", Toast.LENGTH_SHORT).show()
            refreshTaskListAdapter()
            refreshReportListAdapter()
            updateTitleBar()
            setSelected()
        }
        Log.d(TAG, "leaving doRestoreClick")
    }

    /**
     * Update the subtitle in the Action Bar.
     */
    private fun updateTitleBar() {
        Log.d(TAG, "updateTitleBar")
        val format = "(%.2fh / %.2fh) / (%.2fh / %.2fh)"
        val locale = Locale.getDefault()
        val hoursPerDay = prefs!!.hoursPerDay
        val hoursPerWeek = prefs!!.hoursPerWeek
        var dayAdder = 0f
        var weekAdder = 0f
        val db = TimeSheetDbAdapter(applicationContext)

        // Display the time accumulated for today with time remaining.
        var reportCursor = db.daySummary(false)
        if (reportCursor == null) {
            supportActionBar!!.subtitle = String.format(locale, format, dayAdder, hoursPerDay, weekAdder,
                    hoursPerWeek)
            return
        }
        reportCursor.moveToFirst()
        if (!reportCursor.isAfterLast) {
            val column = reportCursor.getColumnIndex("total")
            while (!reportCursor.isAfterLast) {
                dayAdder += reportCursor.getFloat(column)
                reportCursor.moveToNext()
            }
        }
        try {
            reportCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "updateTitleBar $e")
        }

        reportCursor = db.weekSummary(day, false)
        if (reportCursor == null) {
            supportActionBar!!.subtitle = String.format(locale, format, dayAdder,
                    hoursPerDay - dayAdder, weekAdder, hoursPerWeek)
            return
        }
        reportCursor.moveToFirst()
        if (!reportCursor.isAfterLast) {
            val column = reportCursor.getColumnIndex("total")
            while (!reportCursor.isAfterLast) {
                weekAdder += reportCursor.getFloat(column)
                reportCursor.moveToNext()
            }
        }
        try {
            reportCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "updateTitleBar $e")
        }

        supportActionBar!!.subtitle = String.format(locale, format, dayAdder,
                hoursPerDay - dayAdder, weekAdder, hoursPerWeek - weekAdder)
    }

    private fun checkCrossSplitClock(hour: Int) {
        Log.d(TAG, "In checkCrossSplitClock")
        val db = TimeSheetDbAdapter(applicationContext)
        //try {
        //    db.open();
        //} catch (Exception e) {
        //    Log.i(TAG, "Database open threw exception" + e);
        //}
        val lastRowID = db.lastClockEntry()
        val lastTaskID = db.taskIDForLastClockEntry()
        val tempClockCursor = db.fetchEntry(lastRowID)

        val timeOut = tempClockCursor!!.getLong(tempClockCursor
                .getColumnIndex("timeout"))

        // If the last task is not "open" then skip.
        if (timeOut != 0L) {
            try {
                tempClockCursor.close()
            } catch (e: IllegalStateException) {
                Log.d(TAG, "checkCrossDayClock " + e.toString())
            }

            return
        }

        // Handle cross-boundary clockings.
        val lastClockIn = tempClockCursor.getLong(tempClockCursor
                .getColumnIndex("timein"))
        val boundary = TimeHelpers.millisToStartOfDay(lastClockIn) + prefs!!.weekStartHour * 3600 * 1000

        try {
            tempClockCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "checkCrossDayClock " + e.toString())
        }

        Log.d(TAG, "boundary time   : " + boundary)
        Log.d(TAG, "lastClockIn time: " + lastClockIn)
        Log.d(TAG, "timeOut         : " + timeOut)
        if (lastClockIn != boundary && boundary != timeOut) {
            db.closeEntry(lastTaskID, boundary - 1000)
            db.createEntry(lastTaskID, boundary)
        }
    }

    private fun checkCrossDayClock() {
        Log.d(TAG, "In checkCrossDayClock")
        val db = TimeSheetDbAdapter(applicationContext)
        //try {
        //    db.open();
        //} catch (Exception e) {
        //    Log.i(TAG, "Database open threw exception" + e);
        //}
        val lastRowID = db.lastClockEntry()
        val lastTaskID = db.taskIDForLastClockEntry()
        val tempClockCursor = db.fetchEntry(lastRowID)

        val timeOut = tempClockCursor!!.getLong(tempClockCursor
                .getColumnIndex("timeout"))

        // If the last task is not "open" then skip.
        if (timeOut != 0L) {
            try {
                tempClockCursor.close()
            } catch (e: IllegalStateException) {
                Log.d(TAG, "checkCrossDayClock " + e.toString())
            }

            return
        }

        // Handle cross-day clockings.
        val now = TimeHelpers.millisNow()
        val lastClockIn = tempClockCursor.getLong(tempClockCursor
                .getColumnIndex("timein"))
        val boundary = TimeHelpers.millisToEoDBoundary(lastClockIn, prefs!!.timeZone)

        // Calculate where we are in relation to the boundary time.
        val delta = now - boundary

        // If the difference in days is 1, ask. If it's greater than 1, just
        // close it.
        Log.d(TAG, "checkCrossDayClock: now=" + now + " / " + TimeHelpers.millisToTimeDate(now))
        Log.d(TAG, "checkCrossDayClock: lastClockIn=" + lastClockIn + " / "
                + TimeHelpers.millisToTimeDate(lastClockIn))
        Log.d(TAG, "checkCrossDayClock: delta=" + delta)

        // TODO: This should be handled better.
        // Less than one day
        if (delta < 1) {
            Log.d(TAG, "Ignoring.  delta = " + delta)
        } else if (now - TimeHelpers.millisToStartOfDay(lastClockIn) > 86400000) { // More
            // than one day.
            Log.d(TAG, "Closing entry.  delta = " + delta / 86400000.0
                    + " days.")
            Log.d(TAG, "With timeOut = " + boundary)
            db.closeEntry(lastTaskID, boundary)
            refreshTaskListAdapter()
        } else if (delta > 0) { // Now is beyond the boundary.
            Log.d(TAG, "Opening dialog.  delta = " + delta)
            db.closeEntry(lastTaskID, boundary)
            showDialog(CROSS_DIALOG)
        }

        try {
            tempClockCursor.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "taskIDForLastClockEntry " + e.toString())
        }

    }

    private fun processPermissions() {
        val thisActivity = this

        if (ContextCompat.checkSelfPermission(thisActivity.applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(thisActivity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)

                // MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                prefs!!.SDCardBackup = grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    companion object {
        private val TAG = "TimeSheetActivity"
        private val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0xdead
        var day = TimeHelpers.millisNow()

        private val CROSS_DIALOG = 0x40
        private val CONFIRM_RESTORE_DIALOG = 0x41
        private val MY_NOTIFICATION_ID = 0x73
        internal var prefs: PreferenceHelper? = null

        private val contentIntent: PendingIntent? = null
    }
}
