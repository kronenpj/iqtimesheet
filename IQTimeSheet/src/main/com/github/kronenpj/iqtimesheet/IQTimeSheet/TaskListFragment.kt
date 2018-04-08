package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.os.Bundle
import android.support.v4.app.ListFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class TaskListFragment : ListFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "in onCreate")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        Log.d(TAG, "in onCreateView")

        return inflater.inflate(R.layout.fragment_tasklist, container, false)
    }

    companion object {
        /**
         * The fragment argument representing the section number for this fragment.
         */
        val ARG_SECTION_NUMBER = "section_number"
        private val TAG = "TaskListFragment"
    }
}
