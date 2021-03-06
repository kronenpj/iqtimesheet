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
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import kotlinx.android.synthetic.main.fragment_revivelist.*
import java.util.*

/**
 * Activity to allow the user to select a task to revive after being "deleted."
 * Tasks are "never" removed from the database so that entries always reference
 * a valid task.

 * @author Paul Kronenwetter <kronenpj@gmail.com>
 */
class ReviveTaskFragment : ActionBarListActivity() {
    private val taskCursor = ArrayList<String>(0)

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "In onCreate.")
        title = "Select a task to reactivate"
        setContentView(R.layout.fragment_revivelist)

        try {
            revivetasklist!!.choiceMode = ListView.CHOICE_MODE_SINGLE
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } catch (e: NullPointerException) {
            Log.d(TAG, "setDisplayHomeAsUpEnabled returned null.")
        }

        try {
            fillData()
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

        try {
            // Register listeners for the list items.
            revivetasklist!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                val taskName = parent.getItemAtPosition(position) as String
                reactivateTask(taskName)
                setResult(Activity.RESULT_OK, Intent())
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
        super.onDestroy()
    }

    private fun reloadTaskCursor(db: TimeSheetDbAdapter) {
        val temp: Array<ITimeSheetDbAdapter.tasksTuple>? = db.fetchAllDisabledTasks()
        taskCursor.clear()
        if (temp != null) {
            for ((_, task) in temp) {
                // component2 is the tasks element of the tuple
                taskCursor.add(task)
            }
        }
    }

    private fun reactivateTask(taskName: String) {
        Log.d(TAG, "Reactivating task $taskName")
        val db = TimeSheetDbAdapter(applicationContext)

        db.activateTask(taskName)
        val parentTaskID = db.getTaskIDByName(taskName)
        val children = db.fetchChildTasks(parentTaskID)
        for (childID in children) {
            try {
                db.activateTask(db.getTaskNameByID(childID)!!)
                Log.v(TAG, "Reactivated Child item: " + childID + " (" + db.getTaskNameByID(childID) + ")")
            } catch (e: NullPointerException) {
                Log.d(TAG, "getTaskNameById($childID) returned null.")
            }
        }
    }

    private fun fillData() {
        // Get all of the entries from the database and create the list
        val db = TimeSheetDbAdapter(applicationContext)
        //db.open();

        reloadTaskCursor(db)

        val items = taskCursor.toTypedArray()

        revivetasklist!!.adapter = MyArrayAdapter(applicationContext,
                android.R.layout.simple_list_item_single_choice, items)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    companion object {
        private const val TAG = "ReviveTaskHandler"
    }
}