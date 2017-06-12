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

package com.github.kronenpj.iqtimesheet.IQTimeSheet;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Activity to allow the user to select a task to revive after being "deleted."
 * Tasks are "never" removed from the database so that entries always reference
 * a valid task.
 * 
 * @author Paul Kronenwetter <kronenpj@gmail.com>
 */
public class ReviveTaskFragment extends ActionBarListActivity {
	private static final String TAG = "ReviveTaskHandler";
	private ListView tasksList;

	private Cursor taskCursor;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "In onCreate.");
		setTitle("Select a task to reactivate");
		setContentView(R.layout.fragment_revivelist);

		try {
			tasksList = (ListView) findViewById(android.R.id.list);
			tasksList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		try {
			fillData();
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}

		try {
			// Register listeners for the list items.
			tasksList.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					String taskName = (String) parent
							.getItemAtPosition(position);
					reactivateTask(taskName);
					setResult(RESULT_OK, (new Intent()));
					finish();
				}
			});
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

	/**
	 * Called when the activity destroyed.
	 */
	@Override
	public void onDestroy() {
		try {
			taskCursor.close();
		} catch (Exception e) {
			Log.e(TAG, "onDestroy: " + e.toString());
		}

		super.onDestroy();
	}

	private void reloadTaskCursor(TimeSheetDbAdapter db) {
		taskCursor = db.fetchAllDisabledTasks();
	}

	private void reactivateTask(String taskName) {
		Log.d(TAG, "Reactivating task " + taskName);
		TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());
		db.open();
		db.activateTask(taskName);
		db.close();
	}

	private void fillData() {
		// Get all of the entries from the database and create the list
		TimeSheetDbAdapter db = new TimeSheetDbAdapter(getApplicationContext());
		db.open();

		reloadTaskCursor(db);

		String[] items = new String[taskCursor.getCount()];
		taskCursor.moveToFirst();
		int i = 0;
		while (!taskCursor.isAfterLast()) {
			items[i] = taskCursor.getString(1);
			taskCursor.moveToNext();
			i++;
		}

		tasksList.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
				android.R.layout.simple_list_item_single_choice, items));
	}

	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case android.R.id.home: {
			finish();
			return true;
		}
		default:
			return super
					.onOptionsItemSelected((android.view.MenuItem) menuItem);
		}
	}
}