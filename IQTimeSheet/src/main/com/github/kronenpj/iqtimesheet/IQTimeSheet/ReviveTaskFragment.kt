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
import android.widget.ArrayAdapter
import android.widget.ListView
import java.util.*

/**
 * Activity to allow the user to select a task to revive after being "deleted."
 * Tasks are "never" removed from the database so that entries always reference
 * a valid task.

 * @author Paul Kronenwetter <kronenpj></kronenpj>@gmail.com>
 */
class ReviveTaskFragment : ActionBarListActivity() {
    private var tasksList: ListView? = null

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
            tasksList = findViewById(R.id.revivetasklist) as ListView
            tasksList!!.choiceMode = ListView.CHOICE_MODE_SINGLE
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
            tasksList!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                val taskName = parent
                        .getItemAtPosition(position) as String
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
        //try {
        //    taskCursor.close();
        //} catch (Exception e) {
        //    Log.e(TAG, "onDestroy: " + e.toString());
        //}

        super.onDestroy()
    }

    private fun reloadTaskCursor(db: TimeSheetDbAdapter) {
        val temp: Array<TimeSheetDbAdapter.tasksTuple>?
        temp = db.fetchAllDisabledTasks()
        taskCursor.clear()
        if (temp != null) {
            for ((_, task) in temp) {
                // component2 is the tasks element of the tuple
                taskCursor.add(task)
            }
        }
    }

    private fun reactivateTask(taskName: String) {
        Log.d(TAG, "Reactivating task " + taskName)
        val db = TimeSheetDbAdapter(applicationContext)
        //db.open();
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
        //db.close();
    }

    private fun fillData() {
        // Get all of the entries from the database and create the list
        val db = TimeSheetDbAdapter(applicationContext)
        //db.open();

        reloadTaskCursor(db)

        //String[] items = new String[taskCursor.getCount()];
        //taskCursor.moveToFirst();
        //int i = 0;
        //while (!taskCursor.isAfterLast()) {
        //    items[i] = taskCursor.getString(1);
        //    taskCursor.moveToNext();
        //    i++;
        //}
        val items = taskCursor.toTypedArray()

        tasksList!!.adapter = ArrayAdapter(applicationContext,
                android.R.layout.simple_list_item_single_choice, items)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(menuItem)
        }
    }

    companion object {
        private val TAG = "ReviveTaskHandler"
    }
}