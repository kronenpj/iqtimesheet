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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.iqapps.IQTimeSheet.test;

import org.junit.Assert;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Suppress;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.Toast;

import com.googlecode.iqapps.IQTimeSheet.MenuItems;
import com.googlecode.iqapps.IQTimeSheet.R;
import com.googlecode.iqapps.IQTimeSheet.TimeSheetActivity;
import com.jayway.android.robotium.solo.Solo;

/**
 * Test to invoke the application once so that there is a database and
 * preferences file we can backup later.
 * 
 * @author kronenpj
 */
// @Suppress //#$##
public class AAA_CreateDBPrefs extends
		ActivityInstrumentationTestCase2<TimeSheetActivity> {
	// private static final String TAG = "AAA_CreateDBPrefs";
	private static final int SLEEPTIME = 50;

	private Solo solo;

	private TimeSheetActivity mActivity;
	private Instrumentation mInstr;

	public AAA_CreateDBPrefs() {
		super(TimeSheetActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mInstr = getInstrumentation();
		solo = new Solo(mInstr, getActivity());
	}

	/**
	 * Make sure the application is ready for us to test it.
	 */
	public void test00Preconditions() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		ListView mView = (ListView) solo.getView(R.id.tasklistfragment);
		assertNotNull(mView);
	}

	/**
	 * Start to restore the database but cancel before actually doing it.
	 */
	public void test10RestoreDBCancel() {
		mActivity = getActivity();
		assertNotNull(mActivity);

		while (!solo.getCurrentActivity().isTaskRoot()) {
			solo.goBack();
		}
		try {
			int menuItemID = mActivity.getOptionsMenu()
					.getItem(MenuItems.RESTORE.ordinal()).getItemId();
			Assert.assertTrue(mInstr.invokeMenuActionSync(mActivity,
					menuItemID, 0));
			solo.sleep(SLEEPTIME);

			solo.clickOnText(solo
					.getString(com.googlecode.iqapps.IQTimeSheet.R.string.cancel));
		} catch (IndexOutOfBoundsException e) {
			Toast.makeText(mActivity,
					"Cancellation of database restore failed.",
					Toast.LENGTH_LONG).show();
		}
	}

	public void tearDown() {
		solo.finishOpenedActivities();
	}

	public void empty() {
		assurePreferencesAreCreated();
	}

	private void assurePreferencesAreCreated() {
		int menuItemID = mActivity.getOptionsMenu()
				.getItem(MenuItems.SETTINGS.ordinal()).getItemId();
		assertTrue(mInstr.invokeMenuActionSync(mActivity, menuItemID, 0));

		assertTrue(solo.waitForActivity("MyPreferenceActivity", 1500));
		// solo.searchText("anchor time zone");
		solo.clickOnText("anchor time zone");
		// solo.searchText("America/New York");
		solo.clickOnText("America/New York");
		solo.goBack();
	}
}