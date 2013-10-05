package com.googlecode.iqapps.IQTimeSheet.test;

import android.app.Instrumentation;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.FlakyTest;
import android.test.suitebuilder.annotation.Suppress;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;

import com.googlecode.iqapps.TimeHelpers;
import com.googlecode.iqapps.IQTimeSheet.AddTaskHandler;
import com.googlecode.iqapps.IQTimeSheet.MenuItems;
import com.googlecode.iqapps.IQTimeSheet.TimeSheetActivity;
import com.googlecode.iqapps.testtools.Helpers;
import com.jayway.android.robotium.solo.Solo;

// @Suppress //#$##
public class EditTaskHandlerTest extends
		ActivityInstrumentationTestCase2<TimeSheetActivity> {
	private static final String EXAMPLE_TASK_ENTRY = "Example task entry";
	private static final String CHILD_TASK_1_65 = "Child Task 1 - 65%";
	private static final String CHILD_TASK_2_35 = "Child Task 2 - 35%";

	private static final int SLEEPTIME = 99;

	final String renamedTaskText = "Renamed Task";

	private Solo solo;
	private TimeSheetActivity mActivity;
	private Context mCtx;
	private Instrumentation mInstr;

	public EditTaskHandlerTest() {
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

		ListView mView = (ListView) solo
				.getView(com.googlecode.iqapps.IQTimeSheet.R.id.tasklistfragment);
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
	public void test02EraseDB() {
		// TODO: This should be more kind to existing data. Perhaps backing it
		// up and replacing it once we're done testing...

		// Delete the databases associated with the project.
		String[] databases = mCtx.databaseList();
		for (String database : databases) {
			// assertTrue("dbList: " + databases[db], false);
			mCtx.deleteDatabase(database);
		}
	}

	public void test03ForEmptyDatabase() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		assertTrue(solo.searchText(EXAMPLE_TASK_ENTRY));
	}

	@FlakyTest(tolerance = 3)
	public void test10CreateSplitTasks() {
		createSplitTask(CHILD_TASK_1_65, EXAMPLE_TASK_ENTRY, 65);
		createSplitTask(CHILD_TASK_2_35, EXAMPLE_TASK_ENTRY, 35);
	}

	public void test20StartStopTask() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		// Press the task to start recording time.
		solo.clickOnText(EXAMPLE_TASK_ENTRY);
		solo.sleep(SLEEPTIME);
		// Press the task to stop recording time.
		solo.clickOnText(EXAMPLE_TASK_ENTRY);
	}

	public void test30EditTask() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		int whatHour = TimeHelpers.millisToHour(TimeHelpers.millisNow());

		// Bring up the edit day activity.
		mInstr.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
		solo.sleep(SLEEPTIME);
		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		// Select the first item in the list, which was just created.
		assertTrue(solo.waitForText(EXAMPLE_TASK_ENTRY, 1, SLEEPTIME * 5));
		solo.clickOnText(EXAMPLE_TASK_ENTRY);

		// Find the end time button and select it.
		// If it's not midnight or the following hour, change start time
		if (whatHour != 0) {
			solo.clickOnButton(2);
		} else {
			solo.clickOnButton(3);
		}
		solo.sleep(SLEEPTIME);

		// Change the time to one hour prior.
		// If it's midnight or the following hour, increase instead
		if (whatHour == 0) {
			View increment = solo.getView(
					com.googlecode.iqapps.IQTimeSheet.R.id.increment, 0);
			solo.clickOnView(increment);
		} else {
			View decrement = solo.getView(
					com.googlecode.iqapps.IQTimeSheet.R.id.decrement, 0);
			solo.clickOnView(decrement);
		}

		// Accept this change.
		solo.clickOnButton(mActivity
				.getString(com.googlecode.iqapps.IQTimeSheet.R.string.accept));
		solo.sleep(SLEEPTIME);

		// Accept the edit
		solo.clickOnButton(mActivity
				.getString(com.googlecode.iqapps.IQTimeSheet.R.string.accept));
		solo.sleep(SLEEPTIME);
	}

	public void test40EditTaskCancel() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		// Bring up the edit day activity.
		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		// Select the first item in the list, which was just created.
		solo.clickOnText(EXAMPLE_TASK_ENTRY);

		// Select the cancel button.
		solo.clickOnButton(solo
				.getString(com.googlecode.iqapps.IQTimeSheet.R.string.cancel));
		solo.sleep(SLEEPTIME);
	}

	public void test50Report() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		// Bring up the report activity.
		solo.scrollToSide(Solo.LEFT);
		solo.scrollToSide(Solo.LEFT);
		solo.scrollToSide(Solo.RIGHT);
		// assertTrue(solo.waitForFragmentByTag("DayReport", 1500));

		// Locate the larger percentage task
		// The percentage of time accumulated can vary a little due to rounding
		// when the snap time is one minute. Account for that variance here.
		assertTrue(solo.searchText("0.65") || solo.searchText("0.66"));
		solo.sleep(SLEEPTIME);

		// Locate the smaller percentage task
		// The percentage of time accumulated can vary a little due to rounding
		// when the snap time is one minute. Account for that variance here.
		assertTrue(solo.searchText("0.35") || solo.searchText("0.36"));
		solo.sleep(SLEEPTIME);

		// Locate the footer
		// The total time accumulated can vary a little due to rounding when the
		// snap time is one minute. Account for that variance here.
		assertTrue(solo.searchText("1.00") || solo.searchText("1.01")
				|| solo.searchText("1.02"));
		solo.sleep(SLEEPTIME);
	}

	public void test60EditTaskDelete() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		// Bring up the edit day activity.
		mInstr.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
		solo.sleep(SLEEPTIME);
		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		// Select the first item in the list, which was just created.
		solo.clickOnText(EXAMPLE_TASK_ENTRY);

		// Select the delete button.
		solo.clickOnButton(mActivity
				.getString(com.googlecode.iqapps.IQTimeSheet.R.string.delete));
		solo.sleep(SLEEPTIME);

		// Answer the dialog.
		solo.clickOnText("Yes");
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

	private void createSplitTask(String name, String parent, int percentage) {
		mActivity = getActivity();
		assertNotNull(mActivity);

		// Bring up the new task activity.
		mInstr.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
		solo.sleep(SLEEPTIME);
		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.NEW_TASK.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity(AddTaskHandler.class, 1500));
		// solo.sleep(SLEEPTIME);

		// Enter the name of the new task
		solo.clearEditText(0);
		solo.enterText(0, name);
		solo.sleep(SLEEPTIME);

		// Check the split task check box.
		solo.clickOnCheckBox(0);
		solo.sleep(SLEEPTIME * 2);

		// Choose the first child task from the list.
		solo.pressSpinnerItem(0, 0);
		solo.sleep(SLEEPTIME);

		// Set the percentage desired.
		solo.setProgressBar(0, percentage);
		solo.sleep(SLEEPTIME);

		// Verify the percentage is reflected in the text field.
		assertTrue(solo.searchEditText(String.valueOf(percentage)));

		// Click on Accept button
		final String acceptButton = mActivity
				.getString(com.googlecode.iqapps.IQTimeSheet.R.string.accept);
		solo.clickOnButton(acceptButton);
		solo.sleep(SLEEPTIME);
	}
}
