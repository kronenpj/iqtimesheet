package com.googlecode.iqapps.IQTimeSheet;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TaskListFragment extends ListFragment {
	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	public static final String ARG_SECTION_NUMBER = "section_number";
	private static final String TAG = "TaskListFragment";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "in onCreate");
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// super.onCreateView(savedInstanceState);
		Log.d(TAG, "in onCreateView");

		// val rootView = inflater.inflate(R.layout.fragment_tasklist,
		// container, false);
		return inflater.inflate(R.layout.fragment_tasklist, container,
				false);
	}
}
