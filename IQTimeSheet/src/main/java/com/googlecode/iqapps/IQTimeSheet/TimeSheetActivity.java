package com.googlecode.iqapps.IQTimeSheet;

import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockDialogFragment;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockFragment;
import com.googlecode.iqapps.TimeHelpers;

public class TimeSheetActivity extends RoboSherlockFragmentActivity {
	// TabSwipeActivity
	private static final String TAG = "TimeSheetActivity";
	private static long day = TimeHelpers.millisNow();

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	private Menu optionsMenu;

	private static final int CROSS_DIALOG = 0x40;
	private static final int CONFIRM_RESTORE_DIALOG = 0x41;
	private static final int MY_NOTIFICATION_ID = 0x73;
	static PreferenceHelper prefs;

	static NotificationManager notificationManager;
	static Notification myNotification;
	static PendingIntent contentIntent;

	// @Override
	protected void onCreateOff(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setContentView(R.layout.activity_time_sheet);

		prefs = new PreferenceHelper(getApplicationContext());

		// addTab("Test Frag", TestFragment.class,
		// TestFragment.createBundle("Test Fragment"));
		// addTab("Select Task", TaskSelectionFragment.class,
		// TaskSelectionFragment.createBundle("Select Task"));
		// addTab("Day Report", DayReportFragment.class,
		// DayReportFragment.createBundle("Day Report"));
		// addTab("Week Report", WeekReportFragment.class,
		// WeekReportFragment.createBundle("Week Report"));

		setSelected();
	}

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

		setSelected();
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
	 * @param requestCode
	 *            The original request code as given to startActivity().
	 * @param resultCode
	 *            From sending activity as per setResult().
	 * @param data
	 *            From sending activity as per setResult().
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());
		db.open();

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
					Log.d(TAG, "TaskAdd fillData: " + e.toString());
				}
			}
		} else if (requestCode == ActivityCodes.TASKREVIVE.ordinal()) {
			// This one is a special case, since it has its own database
			// adapter, we let it change the state itself rather than passing
			// the result back to us.
			if (resultCode == RESULT_OK) {
				refreshTaskListAdapter((ListView) findViewById(R.id.tasklistfragment));
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
				refreshTaskListAdapter((ListView) findViewById(R.id.tasklistfragment));
			}
		}
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
					ReviveTaskHandler.class);
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
			if (prefs.getSDCardBackup()) {
				// showDialog(CONFIRM_RESTORE_DIALOG);
				RoboSherlockDialogFragment newFragment = MyYesNoDialog
						.newInstance(R.string.restore_title);
				newFragment.show(getSupportFragmentManager(), "restore_dialog");
			}
			// FIXME: Need to trigger task list view refresh here!!!
			refreshTaskListAdapter((ListView) findViewById(R.id.tasklistfragment));	
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
			db.open();
			db.deactivateTask(((TextView) info.targetView).getText().toString());
			db.close();
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
		case CONFIRM_RESTORE_DIALOG:
			builder = new AlertDialog.Builder(getApplicationContext());
			builder.setMessage(
					"This will overwrite the database." + "  Proceed?")
					.setCancelable(true)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
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
	 * A fragment representing a section of the application.
	 */
	public class SectionFragment extends RoboSherlockFragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		public SectionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
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
		 * @param inflater
		 * @param container
		 * @return The inflated view.
		 */
		private View setupTaskListFragment(LayoutInflater inflater,
				ViewGroup container) {
			Log.d(TAG, "setupTaskListFragment");
			final TimeSheetDbAdapter db = new TimeSheetDbAdapter(
					getSherlockActivity().getApplicationContext());
			db.open();

			View rootView = inflater.inflate(R.layout.fragment_tasklist,
					container, false);
			final ListView myTaskList = (ListView) rootView
					.findViewById(R.id.tasklistfragment);

			// Populate the ListView with an array adapter with the task items.
			refreshTaskListAdapter(myTaskList);

			// Make list items selectable.
			myTaskList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setSelected(myTaskList);

			registerForContextMenu(myTaskList);

			myTaskList.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {

					String taskName = (String) parent
							.getItemAtPosition(position);
					long taskID = db.getTaskIDByName(taskName);
					Log.i(TAG, "Processing change for task: " + taskName
							+ " / " + taskID);
					if (db.processChange(taskID)) {
						long timeIn = db.timeInForLastClockEntry();

						startNotification(db.getTaskNameByID(taskID), timeIn);
						setSelected();
					} else {
						stopNotification();
						clearSelected(myTaskList);
						Log.d(TAG, "Closed task ID: " + taskID);
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
		 * @param inflater
		 * @param container
		 * @return The inflated view.
		 */
		private View setupDayReportFragment(LayoutInflater inflater,
				ViewGroup container) {
			Log.d(TAG, "setupDayReportFragment");
			final TimeSheetDbAdapter db = new TimeSheetDbAdapter(
					getSherlockActivity().getApplicationContext());
			db.open();

			View rootView = inflater.inflate(R.layout.fragment_reportlist,
					container, false);
			ListView reportList = (ListView) rootView
					.findViewById(R.id.reportList);

			refreshReportListAdapter(reportList);

			TextView footerView = (TextView) rootView
					.findViewById(R.id.reportfooter);

			try {
				footerView.setTextSize(TimeSheetActivity.prefs
						.getTotalsFontSize());
			} catch (NullPointerException e) {
				Log.d(TAG, "setupDayReportFragment: NPE prefs: " + e);
			}

			Button[] child = new Button[] {
					(Button) rootView.findViewById(R.id.previous),
					(Button) rootView.findViewById(R.id.today),
					(Button) rootView.findViewById(R.id.next) };

			/**
			 * This method is what is registered with the button to cause an
			 * action to occur when it is pressed.
			 */
			OnClickListener mButtonListener = new OnClickListener() {
				public void onClick(View v) {
					Log.d(TAG, "onClickListener view id: " + v.getId());

					switch (v.getId()) {
					case R.id.previous:
						day = TimeHelpers.millisToStartOfDay(day) - 1000;
						Log.d(TAG, "onClickListener button: previous");
						break;
					case R.id.today:
						day = TimeHelpers.millisNow();
						Log.d(TAG, "onClickListener button: today");
						break;
					case R.id.next:
						day = TimeHelpers.millisToEndOfDay(day) + 1000;
						Log.d(TAG, "onClickListener button: next");
						break;
					}

					TextView headerView = (TextView) v.getRootView()
							.findViewById(R.id.reportheader);
					String date = TimeHelpers.millisToDate(day);
					headerView.setText("Week Report - " + date);
					Log.d(TAG, "New day is: " + date);

					refreshReportListAdapter((ListView) v.getRootView()
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
		 * @param inflater
		 * @param container
		 * @return The inflated view.
		 */
		private View setupWeekReportFragment(LayoutInflater inflater,
				ViewGroup container) {
			Log.d(TAG, "setupWeekReportFragment");
			final TimeSheetDbAdapter db = new TimeSheetDbAdapter(
					getSherlockActivity().getApplicationContext());
			db.open();

			View rootView = inflater.inflate(R.layout.fragment_weekreportlist,
					container, false);
			ListView reportList = (ListView) rootView
					.findViewById(R.id.weekList);

			refreshWeekReportListAdapter(reportList);

			TextView footerView = (TextView) rootView
					.findViewById(R.id.weekfooter);
			try {
				footerView.setTextSize(TimeSheetActivity.prefs
						.getTotalsFontSize());
			} catch (NullPointerException e) {
				Log.d(TAG, "setupWeekeportFragment: NPE prefs: " + e);
			}

			Button[] child = new Button[] {
					(Button) rootView.findViewById(R.id.wprevious),
					(Button) rootView.findViewById(R.id.wtoday),
					(Button) rootView.findViewById(R.id.wnext) };

			/**
			 * This method is what is registered with the button to cause an
			 * action to occur when it is pressed.
			 */
			OnClickListener mButtonListener = new OnClickListener() {
				public void onClick(View v) {
					Log.d(TAG, "onClickListener view id: " + v.getId());

					switch (v.getId()) {
					case R.id.wprevious:
						day = TimeHelpers.millisToStartOfWeek(day) - 1000;
						Log.d(TAG, "onClickListener button: wprevious");
						break;
					case R.id.wtoday:
						day = TimeHelpers.millisNow();
						Log.d(TAG, "onClickListener button: wtoday");
						break;
					case R.id.wnext:
						day = TimeHelpers.millisToEndOfWeek(day) + 1000;
						Log.d(TAG, "onClickListener button: wnext");
						break;
					}

					TextView headerView = (TextView) v.getRootView()
							.findViewById(R.id.weekheader);
					String date = TimeHelpers.millisToDate(day);
					headerView.setText("Week Report - " + date);
					Log.d(TAG, "New day is: " + date);

					refreshWeekReportListAdapter((ListView) v.getRootView()
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

		private void refreshReportListAdapter(ListView myReportList) {
			Log.d(TAG, "In refreshReportListAdapter.");

			TimeSheetDbAdapter db = new TimeSheetDbAdapter(
					getApplicationContext());
			float dayHours = TimeSheetActivity.prefs.getHoursPerDay();
			String date = TimeHelpers.millisToDate(day);
			Log.d(TAG, "refreshDayReportListAdapter: Updating to " + date);

			TextView headerView = (TextView) myReportList.getRootView()
					.findViewById(R.id.reportheader);
			headerView.setText("Day Report - " + date);

			TextView footerView = (TextView) myReportList.getRootView()
					.findViewById(R.id.reportfooter);
			footerView
					.setText("Hours worked this day: 0\nHours remaining this day: "
							+ String.format("%.2f", dayHours));

			Cursor timeEntryCursor;// = db.dayEntryReport();

			// If the day being reported is the current week, most probably
			// where the current open task exists, then include it, otherwise
			// omit.
			if (day >= TimeHelpers.millisToStartOfDay(TimeHelpers.millisNow())
					&& day <= TimeHelpers.millisToEndOfDay(TimeHelpers
							.millisNow())) {
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
					+ String.format("%.2f", accum)
					+ "\nHours remaining this day: "
					+ String.format("%.2f", dayHours - accum));

			try {
				myReportList.setAdapter(new ReportCursorAdapter(getActivity(),
						R.layout.mysimple_list_item_2, timeEntryCursor,
						new String[] { TimeSheetDbAdapter.KEY_TASK,
								TimeSheetDbAdapter.KEY_TOTAL }, new int[] {
								android.R.id.text1, android.R.id.text2 }));
				Log.i(TAG, "reportList.setAdapter: updated");
			} catch (Exception e) {
				Log.e(TAG, "reportList.setAdapter: " + e.toString());
			}
		}

		private void refreshWeekReportListAdapter(ListView myReportList) {
			Log.d(TAG, "In refreshWeekReportListAdapter.");

			TimeSheetDbAdapter db = new TimeSheetDbAdapter(
					getApplicationContext());
			float weekHours = TimeSheetActivity.prefs.getHoursPerWeek();
			String date = TimeHelpers.millisToDate(TimeHelpers
					.millisToEndOfWeek(day));
			Log.d(TAG, "refreshWeekReportListAdapter: Updating to " + date);

			TextView headerView = (TextView) myReportList.getRootView()
					.findViewById(R.id.weekheader);
			headerView.setText("Week Report - W/E: " + date);

			TextView footerView = (TextView) myReportList.getRootView()
					.findViewById(R.id.weekfooter);
			footerView
					.setText("Hours worked this week: 0\nHours remaining this week: "
							+ String.format("%.2f", weekHours));

			Cursor timeEntryCursor;// = db.weekEntryReport();

			// If the day being reported is the current week, most probably
			// where the current open task exists, then include it, otherwise
			// omit.
			if (day >= TimeHelpers.millisToStartOfWeek(TimeHelpers.millisNow())
					&& day <= TimeHelpers.millisToEndOfWeek(TimeHelpers
							.millisNow())) {
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
					+ "\nHours remaining this day: "
					+ String.format("%.2f", weekHours - accum));

			try {
				myReportList.setAdapter(new ReportCursorAdapter(getActivity(),
						R.layout.mysimple_list_item_2, timeEntryCursor,
						new String[] { TimeSheetDbAdapter.KEY_TASK,
								TimeSheetDbAdapter.KEY_TOTAL }, new int[] {
								android.R.id.text1, android.R.id.text2 }));
			} catch (Exception e) {
				Log.e(TAG, "reportList.setAdapter: " + e.toString());
			}
		}
	}

	void clearSelected() {
		Log.d(TAG, "clearSelected");
		ListView myTaskList = (ListView) findViewById(R.id.tasklistfragment);
		if (myTaskList == null) {
			Log.i(TAG, "findViewByID(tasklistfragment) returned null.");
			return;
		}

		clearSelected(myTaskList);
	}

	void clearSelected(ListView myTaskList) {
		Log.d(TAG, "clearSelected");
		myTaskList.clearChoices();
		for (int i = 0; i < myTaskList.getCount(); i++)
			((Checkable) myTaskList.getChildAt(i)).setChecked(false);
	}

	void setSelected() {
		Log.d(TAG, "setSelected");
		ListView myTaskList = (ListView) findViewById(R.id.tasklistfragment);
		if (myTaskList == null) {
			Log.i(TAG, "findViewByID(tasklistfragment) returned null.");
			return;
		}

		setSelected(myTaskList);
	}

	void setSelected(ListView myTaskList) {
		Log.d(TAG, "setSelected");
		TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());
		db.open();
		long timeOut = db.timeOutForLastClockEntry();
		Log.d(TAG, "Last Time Out: " + timeOut);

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

	void refreshTaskListAdapter(ListView myTaskList) {
		Log.d(TAG, "refreshTaskListAdapter");
		TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());
		db.open();
		// (Re-)Populate the ListView with an array adapter with the task items.
		myTaskList.setAdapter(new MyArrayAdapter<String>(
				getApplicationContext(),
				android.R.layout.simple_list_item_single_choice, db
						.getTasksList()));
	}

	void startNotification(String taskName, long timeIn) {
		if (!prefs.getPersistentNotification()) {
			return;
		}
		Notification myNotification = new NotificationCompat.Builder(
				getApplicationContext())
				.setContentTitle(
						getResources().getString(R.string.notification_title))
				.setContentText(taskName).setWhen(timeIn)
				.setContentIntent(contentIntent).setAutoCancel(false)
				.setSmallIcon(R.drawable.icon_small).getNotification();

		notificationManager.notify(MY_NOTIFICATION_ID, myNotification);
	}

	void stopNotification() {
		try {
			notificationManager.cancel(MY_NOTIFICATION_ID);
		} catch (NullPointerException e) {
			// Do nothing. The preference was probably set to false, so this was
			// never created.
		}
	}

	/**
	 * Return the menu object for testing.
	 * 
	 * @return optionMenu
	 */
	public Menu getOptionsMenu() {
		return optionsMenu;
	}

	public void doRestoreClick() {
		if (!SDBackup.doSDRestore(TimeSheetDbAdapter.DATABASE_NAME,
				getApplicationContext().getPackageName())) {
			Log.w(TAG, "doSDRestore failed.");
			Toast.makeText(getApplicationContext(), "Database restore failed.",
					Toast.LENGTH_LONG).show();
		} else {
			Log.i(TAG, "doSDRestore succeeded.");
			Toast.makeText(getApplicationContext(),
					"Database restore succeeded.", Toast.LENGTH_SHORT).show();
		}
	}
}