package com.github.kronenpj.iqtimesheet.IQTimeSheet;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.github.kronenpj.iqtimesheet.TimeHelpers;

/**
 * A fragment representing a section of the application.
 */
public class SectionFragment extends Fragment {
    private static final String TAG = "SectionFragment";

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    public static final String ARG_SECTION_NUMBER = "section_number";

    public SectionFragment() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.github.rtyley.android.sherlock.roboguice.activity.
     * RoboFragmentActivity#onCreateView(LayoutInflater, ViewGroup,
     * Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "in onCreateView (SectionFragment)");

        switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
            case 1:
                return setupTaskListFragment(inflater, container);
            case 2:
                return setupDayReportFragment(inflater, container);
            case 3:
                return setupWeekReportFragment(inflater, container);
        }
        return null;
    }

    /**
     * Set up the task list fragment.
     *
     * @param inflater  The inflater given the task of instantiating the view.
     * @param container The view group into which the view will be inflated.
     * @return The inflated view.
     */
    private View setupTaskListFragment(LayoutInflater inflater,
                                       ViewGroup container) {
        Log.d(TAG, "in setupTaskListFragment");
        final TimeSheetDbAdapter db = new TimeSheetDbAdapter(
                getActivity().getApplicationContext());
        //db.open();

        View rootView = inflater.inflate(R.layout.fragment_tasklist,
                container, false);
        final ListView myTaskList = (ListView) rootView.findViewById(R.id.tasklistfragment);

        // Populate the ListView with an array adapter with the task items.
        ((TimeSheetActivity) getActivity()).refreshTaskListAdapter(myTaskList);

        // Make list items selectable.
        myTaskList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        ((TimeSheetActivity) getActivity()).setSelected(myTaskList);

        registerForContextMenu(myTaskList);

        myTaskList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                String taskName = (String) parent.getItemAtPosition(position);
                long taskID = db.getTaskIDByName(taskName);
                Log.i(TAG, "Processing change for task: " + taskName + " / " + taskID);
                if (db.processChange(taskID)) {
                    long timeIn = db.timeInForLastClockEntry();

                    ((TimeSheetActivity) getActivity()).startNotification(db.getTaskNameByID(taskID), timeIn);
                    ((TimeSheetActivity) getActivity()).setSelected();
                } else {
                    ((TimeSheetActivity) getActivity()).clearSelected(myTaskList);
                    Log.d(TAG, "Closed task ID: " + taskID);
                    ((TimeSheetActivity) getActivity()).stopNotification();
                }
            }
        });

        // This can't be closed because of the onItemClickListener routine
        // above.
        // This probably leaks something, but I'll deal with that later.
        // TODO: See if not closing the DB causes problems..
        // try {
        // db.close();
        // } catch (Exception e) {
        // Log.i(TAG, "setupTaskListFragment db.close: " + e.toString());
        // }

        return rootView;
    }

    /**
     * Set up the day report fragment.
     *
     * @param inflater  The inflater given the task of instantiating the view.
     * @param container The view group into which the view will be inflated.
     * @return The inflated view.
     */
    private View setupDayReportFragment(LayoutInflater inflater,
                                        ViewGroup container) {
        Log.d(TAG, "in setupDayReportFragment");
        final TimeSheetDbAdapter db = new TimeSheetDbAdapter(
                getActivity().getApplicationContext());
        //try {
        //    db.open();
        //} catch (Exception e) {
        //    Log.i(TAG, "Database open threw exception" + e);
        //}

        View rootView = inflater.inflate(R.layout.fragment_reportlist,
                container, false);
        ListView reportList = (ListView) rootView.findViewById(R.id.reportList);

        ((TimeSheetActivity) getActivity()).refreshReportListAdapter(reportList);

        TextView footerView = (TextView) rootView.findViewById(R.id.reportfooter);

        try {
            footerView.setTextSize(TimeSheetActivity.prefs.getTotalsFontSize());
        } catch (NullPointerException e) {
            Log.d(TAG, "setupDayReportFragment: NullPointerException prefs: " + e);
        }

        Button[] child = new Button[]{
                (Button) rootView.findViewById(R.id.previous),
                (Button) rootView.findViewById(R.id.today),
                (Button) rootView.findViewById(R.id.next)};

        /**
         * This method is what is registered with the button to cause an
         * action to occur when it is pressed.
         */
        View.OnClickListener mButtonListener = new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "onClickListener view id: " + v.getId());

                switch (v.getId()) {
                    case R.id.previous:
                        Log.d(TAG, "onClickListener button: previous");
                        Log.d(TAG, "onClickListener Day of week #:" + TimeHelpers.millisToDayOfWeek(TimeSheetActivity.day));
                        Log.d(TAG, "onClickListener Preference  #:" + TimeSheetActivity.prefs.getWeekStartDay());
                        Log.d(TAG, "onClickListener Previous date:" + TimeHelpers.millisToTimeDate(TimeSheetActivity.day));
                        if (TimeHelpers.millisToDayOfWeek(TimeSheetActivity.day) !=
                                TimeSheetActivity.prefs.getWeekStartDay()) {
                            TimeSheetActivity.day = TimeHelpers.millisToStartOfDay(TimeSheetActivity.day) - 1000;
                            Log.d(TAG, "onClickListener New date:" + TimeHelpers.millisToTimeDate(TimeSheetActivity.day));
                        } else {
                            if (TimeHelpers.millisToHour(TimeSheetActivity.day) <
                                    TimeSheetActivity.prefs.getWeekStartHour()) {
                                TimeSheetActivity.day = TimeHelpers.millisToStartOfDay(TimeSheetActivity.day) - 1000;
                                Log.d(TAG, "onClickListener New date:" + TimeHelpers.millisToTimeDate(TimeSheetActivity.day));
                            } else {
                                TimeSheetActivity.day = TimeHelpers.millisToStartOfDay(TimeSheetActivity.day) +
                                        (TimeSheetActivity.prefs.getWeekStartHour() * 3600 * 1000) - 1000;
                                Log.d(TAG, "onClickListener New date:" + TimeHelpers.millisToTimeDate(TimeSheetActivity.day));
                            }
                        }
                        break;
                    case R.id.today:
                        Log.d(TAG, "onClickListener button: today");
                        TimeSheetActivity.day = TimeHelpers.millisNow();
                        break;
                    case R.id.next:
                        Log.d(TAG, "onClickListener button: next");
                        Log.d(TAG, "onClickListener Day of week #:" + TimeHelpers.millisToDayOfWeek(TimeSheetActivity.day));
                        Log.d(TAG, "onClickListener Preference  #:" + TimeSheetActivity.prefs.getWeekStartDay());
                        Log.d(TAG, "onClickListener Previous date:" + TimeHelpers.millisToTimeDate(TimeSheetActivity.day));
                        if (TimeHelpers.millisToDayOfWeek(TimeSheetActivity.day) !=
                                TimeSheetActivity.prefs.getWeekStartDay()) {
                            TimeSheetActivity.day = TimeHelpers.millisToEndOfDay(TimeSheetActivity.day) + 1000;
                            Log.d(TAG, "onClickListener New date:" + TimeHelpers.millisToTimeDate(TimeSheetActivity.day));
                        } else {
                            if (TimeHelpers.millisToHour(TimeSheetActivity.day) >
                                    TimeSheetActivity.prefs.getWeekStartHour()) {
                                TimeSheetActivity.day = TimeHelpers.millisToEndOfDay(TimeSheetActivity.day) + 1000;
                                Log.d(TAG, "onClickListener New date:" + TimeHelpers.millisToTimeDate(TimeSheetActivity.day));
                            } else {
                                TimeSheetActivity.day = TimeHelpers.millisToEndOfDay(TimeSheetActivity.day) +
                                        (TimeSheetActivity.prefs.getWeekStartHour() * 3600 * 1000) + 1000;
                                Log.d(TAG, "onClickListener New date:" + TimeHelpers.millisToTimeDate(TimeSheetActivity.day));
                            }
                        }
                        break;
                }

                TextView headerView = (TextView) v.getRootView()
                        .findViewById(R.id.reportheader);
                String date = TimeHelpers.millisToDate(TimeSheetActivity.day);
                headerView.setText("Day Report - " + date);
                Log.d(TAG, "New day is: " + date);

                ((TimeSheetActivity) getActivity()).refreshReportListAdapter((ListView) v.getRootView()
                        .findViewById(R.id.reportList));
            }
        };

        for (Button aChild : child) {
            try {
                aChild.setOnClickListener(mButtonListener);
            } catch (NullPointerException e) {
                Log.e(TAG, "setOnClickListener: " + e);
            }
        }

        return rootView;
    }

    /**
     * Set up the week report fragment.
     *
     * @param inflater  The inflater given the task of instantiating the view.
     * @param container The view group into which the view will be inflated.
     * @return The inflated view.
     */
    private View setupWeekReportFragment(LayoutInflater inflater,
                                         ViewGroup container) {
        Log.d(TAG, "in setupWeekReportFragment");
        final TimeSheetDbAdapter db = new TimeSheetDbAdapter(
                getActivity().getApplicationContext());
        //try {
        //    db.open();
        //} catch (Exception e) {
        //    Log.i(TAG, "Database open threw exception" + e);
        //}

        View rootView = inflater.inflate(R.layout.fragment_weekreportlist,
                container, false);
        ListView reportList = (ListView) rootView.findViewById(R.id.weekList);

        ((TimeSheetActivity) getActivity()).refreshWeekReportListAdapter(reportList);

        TextView footerView = (TextView) rootView.findViewById(R.id.weekfooter);
        try {
            footerView.setTextSize(TimeSheetActivity.prefs
                    .getTotalsFontSize());
        } catch (NullPointerException e) {
            Log.d(TAG, "setupWeekeportFragment: NullPointerException prefs: " + e);
        }

        Button[] child = new Button[]{
                (Button) rootView.findViewById(R.id.wprevious),
                (Button) rootView.findViewById(R.id.wtoday),
                (Button) rootView.findViewById(R.id.wnext)};

        /**
         * This method is what is registered with the button to cause an
         * action to occur when it is pressed.
         */
        View.OnClickListener mButtonListener = new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "onClickListener view id: " + v.getId());

                switch (v.getId()) {
                    case R.id.wprevious:
                        Log.d(TAG, "onClickListener button: wprevious");
                        Log.d(TAG, "onClickListener Day of week #:" + TimeHelpers.millisToDayOfWeek(TimeSheetActivity.day));
                        Log.d(TAG, "onClickListener Preference  #:" + TimeSheetActivity.prefs.getWeekStartDay());
                        Log.d(TAG, "onClickListener Previous date:" + TimeHelpers.millisToTimeDate(TimeSheetActivity.day));

                        // TimeSheetActivity.day = TimeHelpers.millisToStartOfWeek(TimeSheetActivity.day) - 1000;
                        TimeSheetActivity.day = TimeHelpers.millisToStartOfWeek(TimeSheetActivity.day,
                                TimeSheetActivity.prefs.getWeekStartDay(),
                                TimeSheetActivity.prefs.getWeekStartHour()) - 1000;
                        Log.d(TAG, "onClickListener New date:" + TimeHelpers.millisToTimeDate(TimeSheetActivity.day));
                        break;
                    case R.id.wtoday:
                        TimeSheetActivity.day = TimeHelpers.millisNow();
                        Log.d(TAG, "onClickListener button: wtoday");
                        break;
                    case R.id.wnext:
                        Log.d(TAG, "onClickListener button: wnext");
                        Log.d(TAG, "onClickListener Day of week #:" + TimeHelpers.millisToDayOfWeek(TimeSheetActivity.day));
                        Log.d(TAG, "onClickListener Preference  #:" + TimeSheetActivity.prefs.getWeekStartDay());
                        Log.d(TAG, "onClickListener Previous date:" + TimeHelpers.millisToTimeDate(TimeSheetActivity.day));

                        // TimeSheetActivity.day = TimeHelpers.millisToEndOfWeek(TimeSheetActivity.day) + 1000;
                        TimeSheetActivity.day = TimeHelpers.millisToEndOfWeek(TimeSheetActivity.day,
                                TimeSheetActivity.prefs.getWeekStartDay(),
                                TimeSheetActivity.prefs.getWeekStartHour()) + 1000;
                        Log.d(TAG, "onClickListener New date:" + TimeHelpers.millisToTimeDate(TimeSheetActivity.day));
                        break;
                }

                TextView headerView = (TextView) v.getRootView()
                        .findViewById(R.id.weekheader);
                String date = TimeHelpers.millisToDate(TimeSheetActivity.day);
                headerView.setText("Week Report - " + date);
                Log.d(TAG, "New day is: " + date);

                ((TimeSheetActivity) getActivity()).refreshWeekReportListAdapter((ListView) v.getRootView()
                        .findViewById(R.id.weekList));
            }
        };

        for (Button aChild : child) {
            try {
                aChild.setOnClickListener(mButtonListener);
            } catch (NullPointerException e) {
                Log.e(TAG, "setOnClickListener: " + e);
            }
        }

        return rootView;
    }
}
