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
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView

/**
 * Activity to provide an interface to choose a new task for an entry to be
 * changed to.

 * @author Paul Kronenwetter <kronenpj@gmail.com>
 */
class ChangeTaskList : ListActivity() {
    private var db: TimeSheetDbAdapter? = null
    private var taskList: ListView? = null
    private var taskCursor: Array<TimeSheetDbAdapter.tasksTuple>? = null

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
            fillData()
        } catch (e: Exception) {
            Log.d(TAG, "fillData: " + e.toString())
        }

        try {
            // Register listeners for the list items.
            taskList!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                val taskName: String = parent.getItemAtPosition(position) as String
                val taskID = db!!.getTaskIDByName(taskName)
                setResult(Activity.RESULT_OK, Intent().setAction(java.lang.Long.valueOf(
                        taskID).toString()))

                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

    }

    private fun fillData() {
        // Get all of the entries from the database and create the list
        taskCursor = db!!.fetchAllTaskEntries()

        // Need to extract task (component2()) from taskCursor data class.
        val textList: MutableList<String> = mutableListOf()
        // This is a replacement for a for-loop. Interesting.
        taskCursor!!.asList().mapTo(textList) { it.component2() }
        val adapter = ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                textList)

        taskList!!.adapter = adapter
    }

    companion object {
        private val TAG = "ChangeTaskList"
    }
}
