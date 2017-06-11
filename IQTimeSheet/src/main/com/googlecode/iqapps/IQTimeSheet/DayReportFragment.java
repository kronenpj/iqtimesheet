package com.googlecode.iqapps.IQTimeSheet;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.googlecode.iqapps.TimeHelpers;

public class DayReportFragment extends Fragment {
	private static final String TAG = "DayReportFragment";
	private TimeSheetDbAdapter db;
	private Cursor timeEntryCursor;
	private float dayHours;
	private TextView footerView;
	private long day = TimeHelpers.millisNow();
	private ListView reportList;
	public static final String EXTRA_TITLE = "title";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(TAG, "setupDayReportFragment");
		db = new TimeSheetDbAdapter(getActivity().getApplicationContext());
		db.open();

		dayHours = TimeSheetActivity.prefs.getHoursPerDay();

		View rootView = inflater.inflate(R.layout.fragment_reportlist,
				container, false);

		reportList = (ListView) getActivity().findViewById(R.id.reportList);
		reportList.setAdapter(new MyArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_2, db.getDayReportList()));

		footerView = (TextView) getActivity().findViewById(R.id.reportfooter);
		footerView.setTextSize(TimeSheetActivity.prefs.getTotalsFontSize());

		Button[] child = new Button[] {
				(Button) getActivity().findViewById(R.id.previous),
				(Button) getActivity().findViewById(R.id.today),
				(Button) getActivity().findViewById(R.id.next) };

		for (Button aChild : child) {
			try {
				aChild.setOnClickListener(mButtonListener);
			} catch (NullPointerException e) {
				Log.e(TAG, "setOnClickListener: " + e.toString());
			}
		}

		return rootView;
	}

	private void fillData() {
		Log.d(TAG, "In fillData.");

		// Cheat a little on the date. This was originally referencing timeIn
		// from the cursor below.
		String date = TimeHelpers.millisToDate(day);
		getActivity().setTitle("Day Report - " + date);

		footerView
				.setText("Hours worked this day: 0\nHours remaining this day: "
						+ String.format("%.2f", dayHours));

		try {
			timeEntryCursor.close();
		} catch (NullPointerException e) {
			// Do nothing, this is expected sometimes.
		} catch (Exception e) {
			Log.e(TAG, "timeEntryCursor.close: " + e.toString());
			return;
		}

		// If the day being reported is the current week, most probably where
		// the current open task exists, then include it, otherwise omit.
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
			return;
		} catch (Exception e) {
			Log.e(TAG, "timeEntryCursor.moveToFirst: " + e.toString());
			return;
		}

		float accum = 0;
		while (!timeEntryCursor.isAfterLast()) {
			accum = accum
					+ timeEntryCursor.getFloat(timeEntryCursor
							.getColumnIndex(TimeSheetDbAdapter.Companion.getKEY_TOTAL()));
			timeEntryCursor.moveToNext();
		}

		footerView.setText("Hours worked this day: "
				+ String.format("%.2f", accum) + "\nHours remaining this day: "
				+ String.format("%.2f", dayHours - accum));

		try {
			reportList.setAdapter(new ReportCursorAdapter(getActivity(),
					R.layout.mysimple_list_item_2, timeEntryCursor,
					new String[] {TimeSheetDbAdapter.Companion.getKEY_TASK(),
                            TimeSheetDbAdapter.Companion.getKEY_TOTAL()}, new int[] {
							android.R.id.text1, android.R.id.text2 }));
		} catch (Exception e) {
			Log.e(TAG, "reportList.setAdapter: " + e.toString());
		}
	}

	/**
	 * This method is what is registered with the button to cause an action to
	 * occur when it is pressed.
	 */
	private OnClickListener mButtonListener = new OnClickListener() {
		public void onClick(View v) {
			Log.d(TAG, "onClickListener view id: " + v.getId());

			switch (v.getId()) {
			case R.id.previous:
				day = TimeHelpers.millisToStartOfDay(day) - 1000;
				break;
			case R.id.today:
				day = TimeHelpers.millisNow();
				break;
			case R.id.next:
				day = TimeHelpers.millisToEndOfDay(day) + 1000;
				break;
			}
			fillData();
		}
	};

	public static Bundle createBundle(String title) {
		Bundle bundle = new Bundle();
		bundle.putString(EXTRA_TITLE, title);
		return bundle;
	}

}
