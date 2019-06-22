package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.support.v7.app.AppCompatActivity
import android.widget.HeaderViewListAdapter
import android.widget.ListAdapter
import android.widget.ListView

/**
 * Created by kronenpj on 7/9/16.
 */
abstract class ActionBarListActivity : AppCompatActivity() {

    private var mListView: ListView? = null

    protected val listView: ListView
        get() {
            if (mListView == null) {
                mListView = findViewById(android.R.id.list)
            }
            return mListView as ListView
        }

    protected var listAdapter: ListAdapter
        get() {
            val adapter = listView.adapter
            return if (adapter is HeaderViewListAdapter) {
                adapter.wrappedAdapter
            } else {
                adapter
            }
        }
        set(adapter) {
            listView.adapter = adapter
        }
}