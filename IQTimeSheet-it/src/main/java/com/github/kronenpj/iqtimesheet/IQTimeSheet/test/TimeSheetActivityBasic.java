package com.github.kronenpj.iqtimesheet.IQTimeSheet.test;

import android.app.Instrumentation;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Suppress;
import android.view.View;
import android.widget.ListView;

import com.github.kronenpj.iqtimesheet.TimeHelpers;
import com.github.kronenpj.iqtimesheet.IQTimeSheet.MenuItems;
import com.github.kronenpj.iqtimesheet.IQTimeSheet.TimeSheetActivity;
import com.github.kronenpj.iqtimesheet.testtools.Helpers;
import com.jayway.android.robotium.solo.Solo;

// @Suppress //#$##
public class TimeSheetActivityBasic extends
		ActivityInstrumentationTestCase2<TimeSheetActivity> {
	private static final int SLEEPTIME = 50;

	final String renamedTaskText = "Renamed Task";

	private Solo solo;
	private TimeSheetActivity mActivity;
	private ListView mView;
	private Context mCtx;
	private Instrumentation mInstr;

	public TimeSheetActivityBasic() {
		super(TimeSheetActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mInstr = getInstrumentation();
		mCtx = mInstr.getTargetContext();
		solo = new Solo(mInstr, getActivity());
	}

	/**
	 * Make sure the application is ready for us to test it.
	 */
	public void test00Preconditions() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		setAlignTimePreferenceViaMenu(0); // 1-minute

		mView = (ListView) mActivity
				.findViewById(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.id.tasklistfragment);
		assertNotNull(mView);
	}

	/**
	 * Make sure the application is ready for us to test it.
	 */
	public void test01BackupDB() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		Helpers.backup(solo, mInstr, mActivity);
	}

	/**
	 * Make sure the application is ready for us to test it.
	 */
	public void test10EraseDB() {
		// TODO: This should be more kind to existing data. Perhaps backing it
		// up and replacing it once we're done testing...

		// Delete the databases associated with the project.
		String[] databases = mCtx.databaseList();
		for (String database : databases) {
			// assertTrue("dbList: " + databases[db], false);
			mCtx.deleteDatabase(database);
		}
	}

	public void test11ForEmptyDatabase() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		assertTrue(solo.searchText("Example task entry"));
	}

	public void test20RenameTask() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		solo.clickLongInList(0, 0);
		solo.clickOnText(Helpers.EDITTASKNAME);

		// Enter renamedTaskText in first editfield
		solo.clearEditText(0);
		solo.enterText(0, renamedTaskText);
		solo.sleep(SLEEPTIME);

		// Click on Accept button
		final String acceptButton = solo
				.getString(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.accept);
		solo.clickOnButton(acceptButton);

		// Verify that renamedTaskText is correctly displayed
		assertTrue(solo.searchText(renamedTaskText));
	}

	public void test30StartStopTask() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		// Press the task to start recording time.
		solo.clickOnText(renamedTaskText);
		solo.sleep(SLEEPTIME);
		// Press the task to stop recording time.
		solo.clickOnText(renamedTaskText);
	}

	public void test40EditTaskStart() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		int whatHour = TimeHelpers.millisToHour(TimeHelpers.millisNow());

		// Bring up the edit day activity.
		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		// Select the first item in the list, which was just created.
		solo.clickInList(1);
		solo.sleep(SLEEPTIME);

		// Find the start time button and select it.
		// If it's not midnight or the preceding hour, change end time
		if (whatHour != 23) {
			View btn = solo.getView(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.id.starttime);
			solo.clickOnView(btn);
		} else {
			View btn = solo.getView(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.id.endtime);
			solo.clickOnView(btn);
		}
		solo.sleep(SLEEPTIME);

		// Change the time to one hour prior.
		// If it's midnight or the following hour, increase instead
		if (whatHour == 23) {
			View btn = solo.getView(
					com.github.kronenpj.iqtimesheet.IQTimeSheet.R.id.increment, 0);
			solo.clickOnView(btn);
		} else {
			View btn = solo.getView(
					com.github.kronenpj.iqtimesheet.IQTimeSheet.R.id.decrement, 0);
			solo.clickOnView(btn);
		}
		solo.sleep(SLEEPTIME);

		// Accept this change.
		solo.clickOnButton(mActivity
				.getString(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.accept));
		solo.sleep(SLEEPTIME);

		// Accept the edit
		solo.clickOnButton(mActivity
				.getString(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.accept));
		solo.sleep(SLEEPTIME);
	}

	public void test50EditTaskEnd() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		int whatHour = TimeHelpers.millisToHour(TimeHelpers.millisNow());

		// Bring up the edit day activity.
		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		// Select the first item in the list, which was just created.
		solo.clickInList(1);
		solo.sleep(SLEEPTIME);

		// Find the end time button and select it.
		// If it's not midnight or the following hour, change start time
		if (whatHour != 0) {
			View btn = solo.getView(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.id.endtime);
			solo.clickOnView(btn);
		} else {
			View btn = solo.getView(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.id.starttime);
			solo.clickOnView(btn);
		}
		solo.sleep(SLEEPTIME);

		// Change the time to one hour prior.
		// If it's midnight or the following hour, decrease instead
		if (whatHour == 0) {
			View btn = solo.getView(
					com.github.kronenpj.iqtimesheet.IQTimeSheet.R.id.decrement, 0);
			solo.clickOnView(btn);
		} else {
			View btn = solo.getView(
					com.github.kronenpj.iqtimesheet.IQTimeSheet.R.id.increment, 0);
			solo.clickOnView(btn);
		}

		// Accept this change.
		solo.clickOnButton(mActivity
				.getString(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.accept));
		solo.sleep(SLEEPTIME);

		// Accept the edit
		solo.clickOnButton(mActivity
				.getString(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.accept));
		solo.sleep(SLEEPTIME);
	}

	public void test60Report() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		solo.scrollToSide(Solo.LEFT);
		solo.scrollToSide(Solo.LEFT);
		solo.scrollToSide(Solo.RIGHT);
		// assertTrue(solo.waitForFragmentByTag("DayReport", 1500));

		// Select the footer
		// The percentage of time accumulated can vary a little due to rounding
		// when the snap time is one minute. Account for that variance here.
		assertTrue(solo.searchText("2.00") || solo.searchText("2.01")
				|| solo.searchText("2.02"));
		solo.sleep(SLEEPTIME);
	}

	/**
	 * Restore the database after we're done messing with it.
	 */
	public void testzzzRestoreDB() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		Helpers.restore(solo, mInstr, mActivity);
	}

	@Override
	protected void tearDown() throws Exception {
		solo.finishOpenedActivities();
	}

	private void setAlignTimePreferenceViaMenu(int downCount) {
		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.SETTINGS.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("MyPreferenceActivity", 1500));

		solo.clickOnText(solo
				.getString(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.align_minutes_title));
		solo.sleep(SLEEPTIME);
		while (solo.scrollUpList(0))
			;

		String[] durations = getActivity().getResources().getStringArray(
				com.github.kronenpj.iqtimesheet.IQTimeSheet.R.array.alignminutetext);
		solo.clickOnText(durations[downCount]);
		solo.sleep(SLEEPTIME);

		solo.goBack();
	}
}
