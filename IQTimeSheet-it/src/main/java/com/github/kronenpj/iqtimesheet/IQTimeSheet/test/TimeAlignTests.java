/*
 * Copyright 2010 TimeSheet authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either exmPositron.press or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.kronenpj.iqtimesheet.IQTimeSheet.test;

import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;
import android.view.KeyEvent;

import com.github.kronenpj.iqtimesheet.TimeHelpers;
import com.github.kronenpj.iqtimesheet.IQTimeSheet.MenuItems;
import com.github.kronenpj.iqtimesheet.IQTimeSheet.TimeSheetActivity;
import com.github.kronenpj.iqtimesheet.IQTimeSheet.TimeSheetDbAdapter;
import com.github.kronenpj.iqtimesheet.testtools.Helpers;
import com.jayway.android.robotium.solo.Solo;

// @Suppress //#$##
public class TimeAlignTests extends
		ActivityInstrumentationTestCase2<TimeSheetActivity> {
	// private Log log = LogFactory.getLog(TimeAlignTests.class);
	private static final String TAG = "TimeAlignTests";
	private static int SLEEPTIME = 99; // Milliseconds

	private Solo solo;
	private TimeSheetActivity mActivity;
	// private ListView mView;
	private Context mCtx;
	private Instrumentation mInstr;
	private TimeSheetDbAdapter db;
	private static final String insertIntoTasks = "INSERT INTO tasks (task, active, usage) "
			+ "VALUES ('";
	private boolean oldAlignAuto;

	public TimeAlignTests() {
		super(TimeSheetActivity.class);
	}

	public void setUp() throws Exception {
		super.setUp();
		Log.i(TAG, "Entering setup.");
		mActivity = getActivity();
		mInstr = getInstrumentation();
		mCtx = mInstr.getTargetContext();
		solo = new Solo(mInstr, getActivity());

		Helpers.backup(solo, mInstr, mActivity);

		db = new TimeSheetDbAdapter(mActivity);
		try {
			db.open();
		} catch (SQLException e) {
			assertFalse(e.toString(), true);
		}
		db.runSQL("DELETE FROM tasks;");
		db.runSQL("DELETE FROM timesheet;");
		db.runSQL(insertIntoTasks + Helpers.text1 + "', 1, 40);");
		db.runSQL(insertIntoTasks + Helpers.text2 + "', 0, 30);");
		db.runSQL(insertIntoTasks + Helpers.text3 + "', 1, 50);");
		db.runSQL(insertIntoTasks + Helpers.text4 + "', 1, 20);");
		db.runSQL(insertIntoTasks + Helpers.text5 + "', 1, 10);");

		Log.i(TAG, "Leaving setup.");
	}

	public void tearDown() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		Helpers.restore(solo, mInstr, mActivity);

		solo.finishOpenedActivities();
	}

	public void test000SetupPreferences() {
		Log.i(TAG, "Entering test000SetupPreferences.");
		mActivity = getActivity();
		assertNotNull(mActivity);

		mInstr.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.SETTINGS.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("MyPreferenceActivity", 1500));

		// The align button disappears if this option is set.
		SharedPreferences shPrefs = mActivity
				.getPreferences(Context.MODE_PRIVATE);
		oldAlignAuto = shPrefs.getBoolean("align.minutes.auto", false);

		if (oldAlignAuto) {
			String prefText = mActivity
					.getText(
							com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.align_minutes_auto)
					.toString();
			assertTrue(solo.searchText(prefText));
			solo.clickOnText(prefText);
		}
		Log.i(TAG, "Leaving test000SetupPreferences.");
	}

	public void testzzzRestorePreferences() {
		if (oldAlignAuto) {
			mActivity = getActivity();
			assertNotNull(mActivity);

			mInstr.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
			int menuItemID = mActivity.getOptionsMenu()
					.getItem(MenuItems.SETTINGS.ordinal()).getItemId();
			assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
			assertTrue(solo.waitForActivity("MyPreferenceActivity", 1500));
			solo.sleep(SLEEPTIME);

			String prefText = mActivity
					.getText(
							com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.align_minutes_auto)
					.toString();
			assertTrue(solo.searchText(prefText));
			solo.clickOnText(prefText);
		}
	}

	@Suppress
	public void testverifyTime2Align() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 0);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 59);
		setAlignTimePreferenceViaMenu(1); // 2-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:00 to 10:59"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:00 to 11:00");
		checkDayReport("3.00");
	}

	@Suppress
	public void testverifyTime3Align() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 1);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 59);
		setAlignTimePreferenceViaMenu(2); // 3-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:01 to 10:59"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:00 to 11:00");
		checkDayReport("3.00");
	}

	@Suppress
	public void testverifyTime4Align() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 1);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 58);
		setAlignTimePreferenceViaMenu(3); // 4-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:01 to 10:58"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:00 to 11:00");
		checkDayReport("3.00");
	}

	@Suppress
	public void testverifyTime5Align() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 2);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 58);
		setAlignTimePreferenceViaMenu(4); // 5-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:02 to 10:58"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:00 to 11:00");
		checkDayReport("3.00");
	}

	// @Suppress
	public void testverifyTime6Align() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 2);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 58);
		setAlignTimePreferenceViaMenu(5); // 6-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:02 to 10:58"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:00 to 11:00");
		checkDayReport("3.00");
	}

	@Suppress
	public void testverifyTime10Align() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 4);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 56);
		setAlignTimePreferenceViaMenu(6); // 10-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:04 to 10:56"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:00 to 11:00");
		checkDayReport("3.00");
	}

	@Suppress
	public void testverifyTime12Align() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 5);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 55);
		setAlignTimePreferenceViaMenu(7); // 12-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:05 to 10:55"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:00 to 11:00");
		checkDayReport("3.00");
	}

	// @Suppress
	public void testverifyTime15Align() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 7);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 53);
		setAlignTimePreferenceViaMenu(8); // 15-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:07 to 10:53"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:00 to 11:00");
		checkDayReport("3.00");
	}

	@Suppress
	public void testverifyTime20Align() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 9);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 51);
		setAlignTimePreferenceViaMenu(9); // 20-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:09 to 10:51"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:00 to 11:00");
		checkDayReport("3.00");
	}

	@Suppress
	public void testverifyTime30Align() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 14);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 46);
		setAlignTimePreferenceViaMenu(10); // 30-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:14 to 10:46"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:00 to 11:00");
		checkDayReport("3.00");
	}

	@Suppress
	public void testverifyTime60Align() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 29);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 31);
		setAlignTimePreferenceViaMenu(11); // 60-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:29 to 10:31"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:00 to 11:00");
		checkDayReport("3.00");
	}

	// @Suppress
	public void testverifyAlignEdge1() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 3);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 56);
		setAlignTimePreferenceViaMenu(5); // 6-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:03 to 10:56"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:06 to 10:54");
		checkDayReport("2.80");
	}

	// @Suppress
	public void testverifyAlignEdge2() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 8);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 51);
		setAlignTimePreferenceViaMenu(5); // 6-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:08 to 10:51"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:06 to 10:54");
		checkDayReport("2.80");
	}

	// @Suppress
	public void testverifyAlignEdge3() {
		long now = TimeHelpers.millisNow();
		long eightAM = TimeHelpers.millisSetTime(now, 8, 9);
		long elevenAM = TimeHelpers.millisSetTime(now, 10, 50);
		setAlignTimePreferenceViaMenu(5); // 6-minute
		pressTaskTwice(2);
		db.runSQL("UPDATE timesheet SET timein=" + eightAM + ", timeout="
				+ elevenAM + ";");
		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:09 to 10:50"));
		solo.clickOnText(Helpers.text1);
		solo.sleep(SLEEPTIME);
		alignWithButton("08:12 to 10:48");
		checkDayReport("2.60");
	}

	@Suppress
	public void testDailyReport() {
		long now = TimeHelpers.millisNow();

		setAlignTimePreferenceViaMenu(5); // 6-minute

		pressTaskTwice(2);
		pressTaskTwice(1);
		pressTaskTwice(4);

		long startTime = TimeHelpers.millisSetTime(now, 8, 2);
		long stopTime = TimeHelpers.millisSetTime(now, 9, 57);
		db.runSQL("UPDATE timesheet SET timein=" + startTime + ", timeout="
				+ stopTime + " where chargeno=2;");

		startTime = TimeHelpers.millisSetTime(now, 9, 58);
		stopTime = TimeHelpers.millisSetTime(now, 10, 32);
		db.runSQL("UPDATE timesheet SET timein=" + startTime + ", timeout="
				+ stopTime + " where chargeno=4;");

		startTime = TimeHelpers.millisSetTime(now, 10, 32);
		stopTime = TimeHelpers.millisSetTime(now, 11, 58);
		db.runSQL("UPDATE timesheet SET timein=" + startTime + ", timeout="
				+ stopTime + " where chargeno=5;");

		solo.sleep(SLEEPTIME);

		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("08:02 to 09:57"));

		assertTrue(solo.searchText(Helpers.text3));
		assertTrue(solo.searchText("09:58 to 10:32"));

		assertTrue(solo.searchText(Helpers.text4));
		assertTrue(solo.searchText("09:58 to 10:32"));
		// alignWithButton("08:00 to 10:00");

		menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		solo.clickInList(0);
		solo.clickOnText("Align");
		solo.clickOnText(solo
				.getString(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.accept));

		assertTrue(solo.searchText("10:00 to 10:30"));
		solo.goBack();

		menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.EDITDAY_ENTRIES.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));
		assertTrue(solo.waitForActivity("EditDayEntriesHandler", 1500));

		solo.clickInList(1);
		solo.clickOnText("Align");
		solo.clickOnText(solo
				.getString(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.accept));

		assertTrue(solo.searchText("10:30 to 12:00"));
		solo.goBack();

		solo.sleep(SLEEPTIME);

		solo.scrollToSide(Solo.LEFT);
		solo.scrollToSide(Solo.LEFT);
		solo.scrollToSide(Solo.RIGHT);
		// assertTrue(solo.waitForFragmentByTag("DayReport", 1500));
		solo.sleep(SLEEPTIME);

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText("2.00 hours"));

		assertTrue(solo.searchText(Helpers.text4));
		assertTrue(solo.searchText("1.50 hours"));

		assertTrue(solo.searchText(Helpers.text3));
		assertTrue(solo.searchText("0.50 hours"));

		assertTrue(solo.searchText("Hours worked this day: 4.00"));
		solo.goBack();
	}

	private void pressTaskTwice(int taskNo) {
		solo.sleep(SLEEPTIME);
		solo.clickInList(taskNo);
		solo.sleep(SLEEPTIME * 2); // Needed to make the test consistent.
		solo.clickInList(taskNo);
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

	private void alignWithButton(String expected) {
		solo.clickOnText("Align");
		solo.clickOnText(solo
				.getString(com.github.kronenpj.iqtimesheet.IQTimeSheet.R.string.accept));
		assertTrue(solo.searchText(expected));
		solo.goBack();
	}

	private void checkDayReport(String expected) {
		solo.scrollToSide(Solo.LEFT);
		solo.scrollToSide(Solo.LEFT);
		solo.scrollToSide(Solo.RIGHT);
		// assertTrue(solo.waitForFragmentByTag("DayReport", 1500));
		solo.sleep(SLEEPTIME);

		assertTrue(solo.searchText(Helpers.text1));
		assertTrue(solo.searchText(expected + " hours"));

		assertTrue(solo.searchText("Hours worked this day: " + expected));
		solo.goBack();
	}
}
