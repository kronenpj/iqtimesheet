package com.googlecode.iqapps.IQTimeSheet;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Checkable;
import android.widget.ListView;

import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockFragment;

public class TaskSelectionFragment extends RoboSherlockFragment {

	public static final String EXTRA_TITLE = "title";
	protected static final String TAG = "TaskSelectionFragment";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
		final TimeSheetDbAdapter db = new TimeSheetDbAdapter(
				inflater.getContext());
		db.open();

		View rootView = inflater.inflate(R.layout.fragment_tasklist, container,
				false);
		final ListView myTaskList = (ListView) rootView
				.findViewById(R.id.tasklistfragment);

		// Populate the ListView with an array adapter with the task items.
		refreshTaskListAdapter(myTaskList);
		Log.d(TAG, "myTaskList getCount: " + myTaskList.getCount());

		// Make list items selectable.
		myTaskList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		setSelected(myTaskList);

		registerForContextMenu(myTaskList);

		myTaskList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				String taskName = (String) parent.getItemAtPosition(position);
				long taskID = db.getTaskIDByName(taskName);
				Log.i(TAG, "Processing change for task: " + taskName + " / "
						+ taskID);
				if (db.processChange(taskID)) {
					long timeIn = db.timeInForLastClockEntry();

					((TimeSheetActivity) getActivity()).startNotification(
							db.getTaskNameByID(taskID), timeIn);
					setSelected();
				} else {
					((TimeSheetActivity) getActivity()).stopNotification();
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

	public static Bundle createBundle(String title) {
		Bundle bundle = new Bundle();
		bundle.putString(EXTRA_TITLE, title);
		return bundle;
	}

	void refreshTaskListAdapter(ListView myTaskList) {
		Log.d(TAG, "refreshTaskListAdapter");
		TimeSheetDbAdapter db = new TimeSheetDbAdapter(getSherlockActivity());
		db.open();
		// (Re-)Populate the ListView with an array adapter with the task items.
		myTaskList.setAdapter(new MyArrayAdapter<String>(getSherlockActivity(),
				android.R.layout.simple_list_item_single_choice, db
						.getTasksList()));
	}

	void clearSelected() {
		Log.d(TAG, "clearSelected");
		ListView myTaskList = (ListView) getSherlockActivity().findViewById(
				R.id.tasklistfragment);
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
		ListView myTaskList = (ListView) getSherlockActivity().findViewById(
				R.id.tasklistfragment);
		if (myTaskList == null) {
			Log.i(TAG, "findViewByID(tasklistfragment) returned null.");
			return;
		}

		setSelected(myTaskList);
	}

	void setSelected(ListView myTaskList) {
		Log.d(TAG, "setSelected");
		TimeSheetDbAdapter db = new TimeSheetDbAdapter(getSherlockActivity());
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

}
