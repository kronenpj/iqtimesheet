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

package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.app.Activity
import android.app.ListActivity
import android.content.Intent
import android.database.Cursor
import android.database.SQLException
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.SimpleCursorAdapter

/**
 * Activity to provide an interface to choose a new task for an entry to be
 * changed to.

 * @author Paul Kronenwetter <kronenpj></kronenpj>@gmail.com>
 */
class ChangeTaskList : ListActivity() {
    private var db: TimeSheetDbAdapter? = null
    private var taskList: ListView? = null
    private var taskCursor: Cursor? = null

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Choose a new task"
        setContentView(R.layout.revivetask)

        taskList = findViewById(android.R.id.list) as ListView
        taskList!!.choiceMode = ListView.CHOICE_MODE_SINGLE

        db = TimeSheetDbAdapter(this)
        try {
            setupDB()
        } catch (e: Exception) {
            Log.d(TAG, "setupDB: " + e.toString())
        }

        try {
            fillData()
        } catch (e: Exception) {
            Log.d(TAG, "fillData: " + e.toString())
        }

        try {
            // Register listeners for the list items.
            taskList!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                val listCursor = parent
                        .getItemAtPosition(position) as Cursor
                val taskName = listCursor.getString(listCursor
                        .getColumnIndex(TimeSheetDbAdapter.KEY_TASK))
                val taskID = db!!.getTaskIDByName(taskName)
                setResult(Activity.RESULT_OK, Intent().setAction(java.lang.Long.valueOf(
                        taskID)!!.toString()))
                try {
                    listCursor.close()
                } catch (e: Exception) {
                    Log.e(TAG, "onItemClick: " + e.toString())
                }

                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

    }

    /**
     * Called when the activity destroyed.
     */
    public override fun onDestroy() {
        try {
            taskCursor!!.close()
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy: " + e.toString())
        }

        db!!.close()
        super.onDestroy()
    }

    /**
     * Encapsulate what's needed to open the database and make sure something is
     * in it.
     */
    private fun setupDB() {
        try {
            db!!.open()
        } catch (e: SQLException) {
            Log.e(TAG, e.toString())
            finish()
        }

    }

    private fun reloadTaskCursor() {
        taskCursor = db!!.fetchAllTaskEntries()
    }

    private fun fillData() {
        // Get all of the entries from the database and create the list
        reloadTaskCursor()

        // Populate list
        val adapter = SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, taskCursor,
                arrayOf(TimeSheetDbAdapter.KEY_TASK),
                intArrayOf(android.R.id.text1))

        taskList!!.adapter = adapter
    }

    companion object {
        private val TAG = "ChangeTaskList"
    }
}
