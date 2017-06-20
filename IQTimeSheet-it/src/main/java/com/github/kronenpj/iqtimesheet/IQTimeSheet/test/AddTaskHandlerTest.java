package com.github.kronenpj.iqtimesheet.IQTimeSheet.test;

import android.app.Instrumentation;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Suppress;
import android.view.KeyEvent;
import android.widget.ListView;

import com.github.kronenpj.iqtimesheet.IQTimeSheet.MenuItems;
import com.github.kronenpj.iqtimesheet.IQTimeSheet.TimeSheetActivity;
import com.github.kronenpj.iqtimesheet.testtools.Helpers;
import com.jayway.android.robotium.solo.Solo;

// @Suppress //#$##
public class AddTaskHandlerTest extends
		ActivityInstrumentationTestCase2<TimeSheetActivity> {
	private static final String EXAMPLE_TASK_ENTRY = "Example task entry";
	private static final String CHILD_TASK_1_65 = "Child Task 1 - 65%";
	private static final String CHILD_TASK_2_35 = "Child Task 2 - 35%";

	private static final int SLEEPTIME = 50;

	final String renamedTaskText = "Renamed Task";

	private Solo solo;
	private TimeSheetActivity mActivity;
	private Context mCtx;
	private Instrumentation mInstr;

	// private Positron mPositron;

	public AddTaskHandlerTest() {
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
				.getView(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.id.tasklistfragment);
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

	public void test10AddTaskExercise() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		// Bring up the add / new task activity.
		mInstr.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
		solo.sleep(SLEEPTIME);
		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.NEW_TASK.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		solo.sleep(SLEEPTIME);
		// assertTrue(solo.waitForActivity("DayReport", 1500));

		// Enter the name of the new task
		solo.clearEditText(0);
		solo.enterText(0, CHILD_TASK_1_65);
		solo.sleep(SLEEPTIME);

		// Verify the split task area is hidden.
		assertFalse(solo
				.searchText(
						solo.getString(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.select_parent_task),
						true));

		// Check the split task check box.
		solo.clickOnCheckBox(0);
		solo.sleep(SLEEPTIME);

		// Verify the percentage is reflected in the text field.
		assertTrue(solo.searchText(String.valueOf(100), true));

		// Check the split task check box.
		solo.clickOnCheckBox(0);
		solo.sleep(SLEEPTIME);

		// Verify the split task area is hidden again.
		assertFalse(solo
				.searchText(
						solo.getString(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.select_parent_task),
						true));

		// Cancel this change.
		solo.clickOnButton(mActivity
				.getString(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.cancel));
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
}
