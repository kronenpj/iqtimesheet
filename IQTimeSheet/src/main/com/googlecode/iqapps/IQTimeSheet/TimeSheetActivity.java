package com.googlecode.iqapps.IQTimeSheet;

import java.sql.Time;
import java.util.Calendar;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Checkable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockDialogFragment;
import com.googlecode.iqapps.TimeHelpers;

public class TimeSheetActivity extends RoboSherlockFragmentActivity {
    // TabSwipeActivity
    private static final String TAG = "TimeSheetActivity";
    protected static long day = TimeHelpers.millisNow();

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private Menu optionsMenu;

    private static final int CROSS_DIALOG = 0x40;
    private static final int CONFIRM_RESTORE_DIALOG = 0x41;
    private static final int MY_NOTIFICATION_ID = 0x73;
    static PreferenceHelper prefs;

    private static PendingIntent contentIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_time_sheet);

        prefs = new PreferenceHelper(getApplicationContext());

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(
                getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
                // Log.i(TAG, "Should I handle onPageScrolled??");
            }

            @Override
            public void onPageSelected(int position) {
                Log.v(TAG, "In onPageSelected");
                Log.d(TAG, "Today         : " + TimeHelpers.millisToDayOfWeek(TimeHelpers.millisNow()));
                Log.d(TAG, "DoW Preference: " + prefs.getWeekStartDay());
                Log.d(TAG, "HoD Preference: " + prefs.getWeekStartHour());
                checkCrossDayClock();
                if (TimeHelpers.millisToDayOfWeek(TimeHelpers.millisNow()) ==
                        TimeSheetActivity.prefs.getWeekStartDay()
                        && TimeSheetActivity.prefs.getWeekStartHour() > 0)
                    checkCrossSplitClock(TimeSheetActivity.prefs.getWeekStartHour());
                switch (position) {
                    case 0: {
                        Log.d(TAG, "Selected task page");
                        refreshTaskListAdapter();
                    }
                    case 1: {
                        Log.d(TAG, "Selected day report page");
                        refreshReportListAdapter();
                    }
                    case 2: {
                        Log.d(TAG, "Selected week report page");
                        refreshWeekReportListAdapter();
                    }
                }
                updateTitleBar();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // Log.i(TAG, "Should I handle onPageScrollStateChanged??");
            }
        });

        setSelected();
        updateTitleBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        try {
            refreshTaskListAdapter();
        } catch (NullPointerException e) {
            Log.d(TAG, "onResume refreshTaskListAdapter: " + e.toString());
        }
        setSelected();
        try {
            refreshReportListAdapter();
        } catch (NullPointerException e) {
            Log.d(TAG, "onResume refreshReportListAdapter: " + e.toString());
        }
        try {
            refreshWeekReportListAdapter();
        } catch (NullPointerException e) {
            Log.d(TAG, "onResume refreshWeekReportListAdapter: " + e.toString());
        }
        try {
            updateTitleBar();
        } catch (NullPointerException e) {
            Log.d(TAG, "onResume updateTitleBar: " + e.toString());
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.time_sheet, menu);
        // Hanging on to this so it can be used for testing.
        optionsMenu = menu;
        return true;
    }

    /**
     * This method is called when the sending activity has finished, with the
     * result it supplied.
     *
     * @param requestCode The original request code as given to startActivity().
     * @param resultCode  From sending activity as per setResult().
     * @param data        From sending activity as per setResult().
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());
        try {
            db.open();
        } catch (Exception e) {
            Log.i(TAG, "Database open threw exception" + e);
        }

        // Check to see that what we received is what we wanted to see.
        if (requestCode == ActivityCodes.TASKADD.ordinal()) {
            // This is a standard resultCode that is sent back if the
            // activity doesn't supply an explicit result. It will also
            // be returned if the activity failed to launch.
            if (resultCode == RESULT_OK) {
                // Our protocol with the sending activity is that it will send
                // text in 'data' as its result.
                if (data != null) {
                    try {
                        if (!data.hasExtra("parent"))
                            db.createTask(data.getAction());
                        else {
                            db.createTask(data.getAction(),
                                    data.getStringExtra("parent"),
                                    data.getIntExtra("percentage", 100));
                        }
                    } catch (NullPointerException e) {
                        Log.d(TAG, "TaskAdd Result: " + e.toString());
                    }
                }
                try {
                    refreshTaskListAdapter((ListView) findViewById(R.id.tasklistfragment));
                } catch (NullPointerException e) {
                    Log.d(TAG,
                            "TaskAdd refreshTaskListAdapter: " + e.toString());
                }
            }
        } else if (requestCode == ActivityCodes.TASKREVIVE.ordinal()) {
            // This one is a special case, since it has its own database
            // adapter, we let it change the state itself rather than passing
            // the result back to us.
            if (resultCode == RESULT_OK) {
                try {
                    refreshTaskListAdapter((ListView) findViewById(R.id.tasklistfragment));
                } catch (NullPointerException e) {
                    Log.d(TAG,
                            "TaskRevive refreshTaskListAdapter: "
                                    + e.toString());
                }
            }
        } else if (requestCode == ActivityCodes.TASKEDIT.ordinal()) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    String result = data.getAction();
                    String oldData = null;

                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        oldData = extras.getString("oldTaskName");
                    }

                    // TODO: Determine what needs to be done to change these
                    // database fields.
                    if (data.hasExtra("parent")) {
                        long taskID = db.getTaskIDByName(oldData);
                        // int oldSplit = db.getSplitTaskFlag(oldData);
                        long parentID = db.getTaskIDByName(data
                                .getStringExtra("parent"));
                        db.alterSplitTask(taskID, parentID,
                                data.getIntExtra("percentage", 100),
                                data.getIntExtra("split", 0));
                    }

                    if (oldData != null && result != null) {
                        db.renameTask(oldData, result);
                    }
                }
                try {
                    refreshTaskListAdapter();
                    refreshReportListAdapter();
                    refreshWeekReportListAdapter();
                    updateTitleBar();
                } catch (NullPointerException e) {
                    Log.d(TAG, "TaskEdit refreshTaskListAdapter: " + e.toString());
                }
            }
        }
        db.close();
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_settings: {
                Intent intent = new Intent(getApplicationContext(),
                        MyPreferenceActivity.class);
                try {
                    startActivityForResult(intent, ActivityCodes.PREFS.ordinal());
                } catch (RuntimeException e) {
                    Log.e(TAG, "RuntimeException caught in "
                            + "onOptionsItemSelected for MyPreferenceActivity");
                    Log.e(TAG, e.getLocalizedMessage());
                }
                return true;
            }
            case R.id.menu_edit_day_entries: {
                Intent intent = new Intent(getApplicationContext(),
                        EditDayEntriesHandler.class);
                try {
                    startActivityForResult(intent, ActivityCodes.EDIT.ordinal());
                } catch (RuntimeException e) {
                    Log.e(TAG, "RuntimeException caught in "
                            + "onOptionsItemSelected for EditDayEntriesHandler");
                    Log.e(TAG, e.getLocalizedMessage());
                }
                return true;
            }
            case R.id.menu_revive_task: {
                Intent intent = new Intent(getApplicationContext(),
                        ReviveTaskFragment.class);
                try {
                    startActivityForResult(intent,
                            ActivityCodes.TASKREVIVE.ordinal());
                } catch (RuntimeException e) {
                    Log.e(TAG, "RuntimeException caught in "
                            + "onOptionsItemSelected for ReviveTaskHandler");
                    Log.e(TAG, e.getLocalizedMessage());
                }
                return true;
            }
            case R.id.menu_new_task: {
                Intent intent = new Intent(getApplicationContext(),
                        AddTaskHandler.class);
                try {
                    startActivityForResult(intent, ActivityCodes.TASKADD.ordinal());
                } catch (RuntimeException e) {
                    Log.e(TAG, "RuntimeException caught in "
                            + "onOptionsItemSelected for AddTaskHandler");
                    Log.e(TAG, e.getLocalizedMessage());
                }
                return true;
            }
            case R.id.menu_backup: {
                if (!SDBackup.doSDBackup(TimeSheetDbAdapter.DATABASE_NAME,
                        getApplicationContext().getPackageName())) {
                    Log.w(TAG, "doSDBackup failed.");
                    Toast.makeText(getApplicationContext(),
                            "Database backup failed.", Toast.LENGTH_LONG).show();
                } else {
                    Log.i(TAG, "doSDBackup succeeded.");
                    Toast.makeText(getApplicationContext(),
                            "Database backup succeeded.", Toast.LENGTH_SHORT)
                            .show();
                }

                return true;
            }
            case R.id.menu_restore: {
                Log.d(TAG, "in onOptionsItemSelected (restore)");
                if (prefs.getSDCardBackup()) {
                    RoboSherlockDialogFragment newFragment = MyYesNoDialog
                            .newInstance(R.string.restore_title);
                    newFragment.show(getSupportFragmentManager(), "restore_dialog");
                }
                refreshTaskListAdapter();
                updateTitleBar();
                setSelected();
                return true;
            }
            case R.id.menu_about: {
                Intent intent = new Intent(getApplicationContext(),
                        AboutDialog.class);
                try {
                    startActivity(intent);
                } catch (RuntimeException e) {
                    Log.e(TAG, "RuntimeException caught in "
                            + "onOptionsItemSelected for AboutDialog");
                    Log.e(TAG, e.getLocalizedMessage());
                }
                return true;
            }
            default:
                return super
                        .onOptionsItemSelected((android.view.MenuItem) menuItem);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
     * android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, ActivityCodes.EDIT_ID.ordinal(), 0, R.string.taskedit);
        menu.add(0, ActivityCodes.RETIRE_ID.ordinal(), 0, R.string.taskretire);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        if (item.getItemId() == ActivityCodes.EDIT_ID.ordinal()) {
            Log.d(TAG, "Edit task: " + info.id);
            Intent intent = new Intent(getApplicationContext(),
                    EditTaskHandler.class);
            intent.putExtra("taskName", ((TextView) info.targetView).getText()
                    .toString());
            try {
                startActivityForResult(intent, ActivityCodes.TASKEDIT.ordinal());
            } catch (RuntimeException e) {
                Toast.makeText(getApplicationContext(), "RuntimeException",
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, e.getLocalizedMessage());
                Log.e(TAG, "RuntimeException caught.");
            }
            refreshTaskListAdapter((ListView) info.targetView.getParent());
            return true;
        }
        if (item.getItemId() == ActivityCodes.RETIRE_ID.ordinal()) {
            TimeSheetDbAdapter db = new TimeSheetDbAdapter(
                    getApplicationContext());
            try {
                db.open();
            } catch (Exception e) {
                Log.i(TAG, "Database open threw exception" + e);
            }
            db.deactivateTask(((TextView) info.targetView).getText().toString());
            try {
                db.close();
            } catch (Exception e) {
                Log.i(TAG, "Database close threw exception" + e);
            }
            refreshTaskListAdapter((ListView) info.targetView.getParent());
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Called when the activity is first created to create a dialog.
     */
    @Override
    protected Dialog onCreateDialog(int dialogId) {
        Log.d(TAG, "In onCreateDialog");
        Dialog dialog = null;
        AlertDialog.Builder builder;

        switch (dialogId) {
            case CROSS_DIALOG:
                builder = new AlertDialog.Builder(getApplicationContext());
                builder.setMessage(
                        "The last entry is still open from yesterday."
                                + "  What should I do?")
                        .setCancelable(false)
                        .setPositiveButton("Close",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        TimeSheetDbAdapter db = new TimeSheetDbAdapter(
                                                getApplicationContext());
                                        db.closeEntry();
                                        db.close();
                                        clearSelected();
                                    }
                                })
                        .setNegativeButton("Close & Re-open",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        TimeSheetDbAdapter db = new TimeSheetDbAdapter(
                                                getApplicationContext());
                                        long taskID = db.taskIDForLastClockEntry();
                                        long now = TimeHelpers.millisNow();
                                        long today = TimeHelpers
                                                .millisToStartOfDay(now);
                                        db.createEntry(taskID, today);
                                        db.close();
                                        setSelected();
                                    }
                                });
                dialog = builder.create();
                break;
            // This may be dead code now...
            case CONFIRM_RESTORE_DIALOG:
                Log.d(TAG, "in onCreateDialog (restore)");
                builder = new AlertDialog.Builder(getApplicationContext());
                builder.setMessage(
                        "This will overwrite the database." + "  Proceed?")
                        .setCancelable(true)
                        .setPositiveButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        Log.d(TAG, "in onClick (restore dialog)");
                                        if (!SDBackup.doSDRestore(
                                                TimeSheetDbAdapter.DATABASE_NAME,
                                                getApplicationContext()
                                                        .getPackageName())) {
                                            Log.w(TAG, "doSDRestore failed.");
                                            Toast.makeText(getApplicationContext(),
                                                    "Database restore failed.",
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            Log.i(TAG, "doSDRestore succeeded.");
                                            Toast.makeText(getApplicationContext(),
                                                    "Database restore succeeded.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        // fillData();
                                        Log.d(TAG,
                                                "onCreateDialog restore dialog.  Calling refreshTaskListAdapter");
                                        refreshTaskListAdapter();
                                        updateTitleBar();
                                        setSelected();
                                    }
                                })
                        .setNegativeButton("No",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        dialog.cancel();
                                    }
                                });
                dialog = builder.create();
                break;
        }

        return dialog;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a DummySectionFragment (defined as a static inner class
            // below) with the page number as its lone argument.
            Fragment fragment = new SectionFragment();
            Bundle args = new Bundle();
            args.putInt(SectionFragment.ARG_SECTION_NUMBER, position + 1);
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * Notify the task list that there are no items selected.
     */
    private void clearSelected() {
        Log.d(TAG, "in clearSelected()");
        ListView myTaskList = (ListView) findViewById(R.id.tasklistfragment);
        if (myTaskList == null) {
            Log.i(TAG, "findViewByID(tasklistfragment) returned null.");
            return;
        }

        clearSelected(myTaskList);
    }

    /**
     * Notify the task list that there are no items selected.
     *
     * @param myTaskList The view ID of the task list.
     */
    void clearSelected(ListView myTaskList) {
        Log.d(TAG, "clearSelected");
        myTaskList.clearChoices();
        for (int i = 0; i < myTaskList.getCount(); i++)
            try {
                ((Checkable) myTaskList.getChildAt(i)).setChecked(false);
            } catch (NullPointerException e) {
                Log.d(TAG, "NullPointerException at item " + i);
            }
    }

    /**
     * Enable the currently selected item in the task list.
     */
    void setSelected() {
        Log.d(TAG, "in setSelected()");
        ListView myTaskList = (ListView) findViewById(R.id.tasklistfragment);
        if (myTaskList == null) {
            Log.i(TAG, "findViewByID(tasklistfragment) returned null.");
            return;
        }

        setSelected(myTaskList);
    }

    /**
     * Enable the currently selected item in the task list.
     *
     * @param myTaskList The view ID of the task list.
     */
    void setSelected(ListView myTaskList) {
        Log.d(TAG, "in setSelected");
        TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());
        try {
            db.open();
        } catch (Exception e) {
            Log.i(TAG, "Database open threw exception" + e);
        }
        long timeOut = db.timeOutForLastClockEntry();
        Log.d(TAG, "Last Time Out: " + timeOut + " / " + TimeHelpers.millisToTimeDate(timeOut));

        if (timeOut != 0) {
            Log.d(TAG, "Returning.");
            db.close();
            return;
        }

        Log.e(TAG, "myTaskList child count is: " + myTaskList.getCount());

        myTaskList.clearChoices();

        long lastTaskID = db.taskIDForLastClockEntry();
        Log.d(TAG, "Last Task ID: " + lastTaskID);

        String taskName = db.getTaskNameByID(lastTaskID);
        // TODO: There should be a better way to do this.
        // Iterate over the entire ListView to find the name of the
        // entry that is to be selected.
        for (int i = 0; i < myTaskList.getCount(); i++) {
            if (taskName.equalsIgnoreCase((String) myTaskList
                    .getItemAtPosition(i))) {
                myTaskList.setItemChecked(i, true);
                myTaskList.setSelection(i);
            }
        }

        db.close();
    }

    /**
     * Refresh the task list. Looking up the list view.
     */
    private void refreshTaskListAdapter() {
        Log.d(TAG, "In refreshTaskListAdapter()");
        refreshTaskListAdapter((ListView) findViewById(R.id.tasklistfragment));
    }

    /**
     * Refresh the task list.
     *
     * @param myTaskList The view ID of the task list.
     */
    void refreshTaskListAdapter(ListView myTaskList) {
        Log.d(TAG, "In refreshTaskListAdapter");
        TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());
        try {
            db.open();
        } catch (Exception e) {
            Log.i(TAG, "Database open threw exception" + e);
        }
        // (Re-)Populate the ListView with an array adapter with the task items.
        myTaskList.setAdapter(new MyArrayAdapter<String>(
                getApplicationContext(),
                android.R.layout.simple_list_item_single_choice, db.getTasksList()));
        setSelected(myTaskList);
    }

    /**
     * Refresh the day report list. Looking up the list view.
     */
    private void refreshReportListAdapter() {
        Log.d(TAG, "In refreshReportListAdapter()");
        refreshReportListAdapter((ListView) findViewById(R.id.reportList));
    }

    /**
     * Refresh the day report list.
     *
     * @param myReportList The view ID of the list to refresh.
     */
    void refreshReportListAdapter(ListView myReportList) {
        Log.d(TAG, "In refreshReportListAdapter");

        TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());
        float dayHours = TimeSheetActivity.prefs.getHoursPerDay();
        String date = TimeHelpers.millisToDate(day);
        Log.d(TAG, "refreshReportListAdapter: Updating to " + TimeHelpers.millisToTimeDate(day));

        TextView headerView = (TextView) myReportList.getRootView()
                .findViewById(R.id.reportheader);
        headerView.setText("Day Report - " + date);

        TextView footerView = (TextView) myReportList.getRootView().findViewById(R.id.reportfooter);
        footerView.setText("Hours worked this day: 0\nHours remaining this day: "
                + String.format("%.2f", dayHours));

        Cursor timeEntryCursor;

        // If the day being reported is the current week, most probably
        // where the current open task exists, then include it, otherwise
        // omit.
        if (day >= TimeHelpers.millisToStartOfDay(TimeHelpers.millisNow())
                && day <= TimeHelpers.millisToEndOfDay(TimeHelpers.millisNow())) {
            timeEntryCursor = db.daySummary(day, false);
        } else {
            timeEntryCursor = db.daySummary(day, true);
        }

        try {
            timeEntryCursor.moveToFirst();
        } catch (NullPointerException e) {
            Log.e(TAG, "timeEntryCursor.moveToFirst: " + e.toString());
            myReportList.setAdapter(null);
            return;
        } catch (Exception e) {
            Log.e(TAG, "timeEntryCursor.moveToFirst: " + e.toString());
            return;
        }

        float accum = 0;
        while (!timeEntryCursor.isAfterLast()) {
            accum = accum
                    + timeEntryCursor.getFloat(timeEntryCursor
                    .getColumnIndex(TimeSheetDbAdapter.KEY_TOTAL));
            timeEntryCursor.moveToNext();
        }

        footerView.setText("Hours worked this day: "
                + String.format("%.2f", accum) + "\nHours remaining this day: "
                + String.format("%.2f", dayHours - accum));

        try {
            myReportList.setAdapter(new ReportCursorAdapter(
                    getApplicationContext(), R.layout.mysimple_list_item_2,
                    timeEntryCursor, new String[]{
                    TimeSheetDbAdapter.KEY_TASK,
                    TimeSheetDbAdapter.KEY_TOTAL}, new int[]{
                    android.R.id.text1, android.R.id.text2}));
            Log.i(TAG, "reportList.setAdapter: updated");
        } catch (Exception e) {
            Log.e(TAG, "reportList.setAdapter: " + e.toString());
        }
    }

    /**
     * Refresh the week report list. Looking up the list view.
     */
    private void refreshWeekReportListAdapter() {
        Log.d(TAG, "In refreshWeekReportListAdapter()");
        refreshWeekReportListAdapter((ListView) findViewById(R.id.weekList));
    }

    /**
     * Refresh the week report list.
     *
     * @param myReportList The view ID of the list to refresh.
     */
    void refreshWeekReportListAdapter(ListView myReportList) {
        Log.d(TAG, "In refreshWeekReportListAdapter");

        TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());
        float weekHours = TimeSheetActivity.prefs.getHoursPerWeek();
        String date = TimeHelpers.millisToDate(TimeHelpers.millisToEndOfWeek(day,
                prefs.getWeekStartDay(), prefs.getWeekStartHour()));
        Log.d(TAG, "refreshWeekReportListAdapter: Updating to " + date);

        try {
            TextView headerView = (TextView) myReportList.getRootView()
                    .findViewById(R.id.weekheader);
            headerView.setText("Week Report - W/E: " + date);
        } catch (NullPointerException e) {
            Log.d(TAG, "Caught NullPointerException in refreshWeekReportListAdapter.");
            return;
        }

        TextView footerView = (TextView) myReportList.getRootView()
                .findViewById(R.id.weekfooter);
        footerView.setText("Hours worked this week: 0\nHours remaining this week: "
                + String.format("%.2f", weekHours));

        Cursor timeEntryCursor;

        // If the day being reported is the current week, most probably
        // where the current open task exists, then include it, otherwise
        // omit.
        if (day >= TimeHelpers.millisToStartOfWeek(TimeHelpers.millisNow())
                && day <= TimeHelpers.millisToEndOfWeek(TimeHelpers.millisNow(),
                prefs.getWeekStartDay(), prefs.getWeekStartHour())) {
            timeEntryCursor = db.weekSummary(day, false);
        } else {
            timeEntryCursor = db.weekSummary(day, true);
        }

        try {
            timeEntryCursor.moveToFirst();
        } catch (NullPointerException e) {
            Log.e(TAG, "timeEntryCursor.moveToFirst: " + e.toString());
            myReportList.setAdapter(null);
            return;
        } catch (Exception e) {
            Log.e(TAG, "timeEntryCursor.moveToFirst: " + e.toString());
            return;
        }

        float accum = 0;
        while (!timeEntryCursor.isAfterLast()) {
            accum = accum
                    + timeEntryCursor.getFloat(timeEntryCursor
                    .getColumnIndex(TimeSheetDbAdapter.KEY_TOTAL));
            timeEntryCursor.moveToNext();
        }

        footerView.setText("Hours worked this week: "
                + String.format("%.2f", accum)
                + "\nHours remaining this week: "
                + String.format("%.2f", weekHours - accum));

        try {
            myReportList.setAdapter(new ReportCursorAdapter(
                    getApplicationContext(), R.layout.mysimple_list_item_2,
                    timeEntryCursor, new String[]{
                    TimeSheetDbAdapter.KEY_TASK,
                    TimeSheetDbAdapter.KEY_TOTAL}, new int[]{
                    android.R.id.text1, android.R.id.text2}));
        } catch (Exception e) {
            Log.e(TAG, "reportList.setAdapter: " + e.toString());
        }
    }

    /**
     * Instantiate the persistent notification.
     *
     * @param taskName Name of the task currently active (clocked into)
     * @param timeIn   The time (in nanoseconds) the task started
     */
    void startNotification(String taskName, long timeIn) {
        if (!prefs.getPersistentNotification()) {
            return;
        }
        NotificationCompat.Builder myNotification = new NotificationCompat.Builder(
                getApplicationContext())
                .setContentTitle(getResources().getString(R.string.notification_title))
                .setContentText(taskName).setWhen(timeIn)
                .setContentIntent(contentIntent).setAutoCancel(false).setOngoing(true)
                .setSmallIcon(R.drawable.icon_small);

        // From: guide/topics/ui/notifiers/notifications.html
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, TimeSheetActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(TimeSheetActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        myNotification.setContentIntent(resultPendingIntent);

        // mId allows you to update the notification later on.
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(MY_NOTIFICATION_ID, myNotification.build());
    }

    /**
     * Stop / turn off the persistent notification.
     */
    void stopNotification() {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(MY_NOTIFICATION_ID);
        } catch (NullPointerException e) {
            // Do nothing. The preference was probably set to false, so this was
            // never created.
        }
    }

    /**
     * Handle the button event from the restore dialog.
     */
    public void doRestoreClick() {
        Log.d(TAG, "in doRestoreClick");
        if (!SDBackup.doSDRestore(TimeSheetDbAdapter.DATABASE_NAME,
                getApplicationContext().getPackageName())) {
            Log.w(TAG, "doSDRestore failed.");
            Toast.makeText(getApplicationContext(), "Database restore failed.",
                    Toast.LENGTH_LONG).show();
        } else {
            Log.i(TAG, "doSDRestore succeeded.");
            Toast.makeText(getApplicationContext(),
                    "Database restore succeeded.", Toast.LENGTH_SHORT).show();
            refreshTaskListAdapter();
            refreshReportListAdapter();
            updateTitleBar();
            setSelected();
        }
        Log.d(TAG, "leaving doRestoreClick");
    }

    /**
     * Update the subtitle in the Action Bar.
     */
    private void updateTitleBar() {
        Log.d(TAG, "updateTitleBar");
        final String format = "(%.2fh / %.2fh) / (%.2fh / %.2fh)";
        float hoursPerDay = prefs.getHoursPerDay();
        float hoursPerWeek = prefs.getHoursPerWeek();
        float dayAdder = 0;
        float weekAdder = 0;
        TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());

        // Display the time accumulated for today with time remaining.
        Cursor reportCursor = db.daySummary(false);
        if (reportCursor == null) {
            getSupportActionBar().setSubtitle(
                    String.format(format, dayAdder, hoursPerDay, weekAdder,
                            hoursPerWeek));
            return;
        }
        reportCursor.moveToFirst();
        if (!reportCursor.isAfterLast()) {
            int column = reportCursor.getColumnIndex(TimeSheetDbAdapter.KEY_TOTAL);
            while (!reportCursor.isAfterLast()) {
                dayAdder = dayAdder + reportCursor.getFloat(column);
                reportCursor.moveToNext();
            }
        }
        try {
            reportCursor.close();
        } catch (IllegalStateException e) {
            Log.d(TAG, "updateTitleBar " + e.toString());
        }

        reportCursor = db.weekSummary(day, false);
        if (reportCursor == null) {
            getSupportActionBar().setSubtitle(
                    String.format(format, dayAdder, hoursPerDay - dayAdder,
                            weekAdder, hoursPerWeek));
            return;
        }
        reportCursor.moveToFirst();
        if (!reportCursor.isAfterLast()) {
            int column = reportCursor.getColumnIndex(TimeSheetDbAdapter.KEY_TOTAL);
            while (!reportCursor.isAfterLast()) {
                weekAdder = weekAdder + reportCursor.getFloat(column);
                reportCursor.moveToNext();
            }
        }
        try {
            reportCursor.close();
        } catch (IllegalStateException e) {
            Log.d(TAG, "updateTitleBar " + e.toString());
        }

        getSupportActionBar().setSubtitle(
                String.format(format, dayAdder, hoursPerDay - dayAdder,
                        weekAdder, hoursPerWeek - weekAdder));
    }

    private void checkCrossSplitClock(int hour) {
        Log.d(TAG, "In checkCrossSplitClock");
        TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());
        try {
            db.open();
        } catch (Exception e) {
            Log.i(TAG, "Database open threw exception" + e);
        }
        long lastRowID = db.lastClockEntry();
        long lastTaskID = db.taskIDForLastClockEntry();
        Cursor tempClockCursor = db.fetchEntry(lastRowID);

        long timeOut = tempClockCursor.getLong(tempClockCursor
                .getColumnIndex(TimeSheetDbAdapter.KEY_TIMEOUT));

        // If the last task is not "open" then skip.
        if (timeOut != 0) {
            try {
                tempClockCursor.close();
            } catch (IllegalStateException e) {
                Log.d(TAG, "checkCrossDayClock " + e.toString());
            }
            return;
        }

        // Handle cross-boundary clockings.
        long lastClockIn = tempClockCursor.getLong(tempClockCursor
                .getColumnIndex(TimeSheetDbAdapter.KEY_TIMEIN));
        long boundary = TimeHelpers.millisToStartOfDay(lastClockIn) +
                prefs.getWeekStartHour() * 3600 * 1000;

        try {
            tempClockCursor.close();
        } catch (IllegalStateException e) {
            Log.d(TAG, "checkCrossDayClock " + e.toString());
        }

        Log.d(TAG, "boundary time   : " + boundary);
        Log.d(TAG, "lastClockIn time: " + lastClockIn);
        Log.d(TAG, "timeOut         : " + timeOut);
        if (lastClockIn != boundary && boundary != timeOut) {
            db.closeEntry(lastTaskID, boundary - 1000);
            db.createEntry(lastTaskID, boundary);
        }
    }

    private void checkCrossDayClock() {
        Log.d(TAG, "In checkCrossDayClock");
        TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());
        try {
            db.open();
        } catch (Exception e) {
            Log.i(TAG, "Database open threw exception" + e);
        }
        long lastRowID = db.lastClockEntry();
        long lastTaskID = db.taskIDForLastClockEntry();
        Cursor tempClockCursor = db.fetchEntry(lastRowID);

        long timeOut = tempClockCursor.getLong(tempClockCursor
                .getColumnIndex(TimeSheetDbAdapter.KEY_TIMEOUT));

        // If the last task is not "open" then skip.
        if (timeOut != 0) {
            try {
                tempClockCursor.close();
            } catch (IllegalStateException e) {
                Log.d(TAG, "checkCrossDayClock " + e.toString());
            }
            return;
        }

        // Handle cross-day clockings.
        long now = TimeHelpers.millisNow();
        long lastClockIn = tempClockCursor.getLong(tempClockCursor
                .getColumnIndex(TimeSheetDbAdapter.KEY_TIMEIN));
        long boundary = TimeHelpers.millisToEoDBoundary(lastClockIn, prefs.getTimeZone());

        // Calculate where we are in relation to the boundary time.
        long delta = now - boundary;

        // If the difference in days is 1, ask. If it's greater than 1, just
        // close it.
        Log.d(TAG, "checkCrossDayClock: now=" + now + " / " + TimeHelpers.millisToTimeDate(now));
        Log.d(TAG, "checkCrossDayClock: lastClockIn=" + lastClockIn + " / "
                + TimeHelpers.millisToTimeDate(lastClockIn));
        Log.d(TAG, "checkCrossDayClock: delta=" + delta);

        // TODO: This should be handled better.
        // Less than one day
        if (delta < 1) {
            Log.d(TAG, "Ignoring.  delta = " + delta);
        } else if ((now - TimeHelpers.millisToStartOfDay(lastClockIn)) > 86400000) { // More
            // than one day.
            Log.d(TAG, "Closing entry.  delta = " + delta / 86400000.0
                    + " days.");
            Log.d(TAG, "With timeOut = " + boundary);
            db.closeEntry(lastTaskID, boundary);
            refreshTaskListAdapter();
        } else if (delta > 0) { // Now is beyond the boundary.
            Log.d(TAG, "Opening dialog.  delta = " + delta);
            db.closeEntry(lastTaskID, boundary);
            showDialog(CROSS_DIALOG);
        }

        try {
            tempClockCursor.close();
        } catch (IllegalStateException e) {
            Log.d(TAG, "taskIDForLastClockEntry " + e.toString());
        }
    }


    /**
     * Return the menu object for testing.
     *
     * @return optionMenu
     */
    @SuppressWarnings("unused")
    public Menu getOptionsMenu() {
        return optionsMenu;
    }
}
