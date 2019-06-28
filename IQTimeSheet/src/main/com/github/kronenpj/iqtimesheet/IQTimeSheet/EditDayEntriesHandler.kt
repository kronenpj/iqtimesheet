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
import android.database.Cursor
import android.database.SQLException
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View.OnClickListener
import android.widget.AdapterView.OnItemClickListener
import android.widget.SimpleCursorAdapter
import android.widget.Toast
import com.github.kronenpj.iqtimesheet.TimeHelpers
import kotlinx.android.synthetic.main.daybuttons.*
import kotlinx.android.synthetic.main.main.*

/**
 * Activity to provide an interface to edit entries for a selected day.

 * @author Paul Kronenwetter <kronenpj></kronenpj>@gmail.com>
 */
class EditDayEntriesHandler : ActionBarListActivity() {
    private var db: TimeSheetDbAdapter? = null
    private var timeEntryCursor: Cursor? = null
    private var day = TimeHelpers.millisNow()
    private val ENTRY_CODE = 0x01

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "In onCreate.")
        showReport()
        title = "Entries for today"

        db = TimeSheetDbAdapter(this)

        try {
            fillData()
        } catch (e: Exception) {
            Log.e(TAG, "fillData: $e")
            finish()
        }

        Log.d(TAG, "Back from fillData.")

        try {
            // Register listeners for the list items.
            list.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                val itemID = parent.getItemIdAtPosition(position)
                Log.d(TAG, "itemID: $itemID")
                processChange(itemID)
            }
        } catch (e: Exception) {
            Log.e(TAG, "setOnItemClickLister setup")
            Log.e(TAG, e.toString())
        }

        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } catch (e: NullPointerException) {
            Log.d(TAG, "setDisplayHomeAsUpEnabled returned null.")
        }
    }

    /**
     * Called when the activity destroyed.
     */
    public override fun onDestroy() {
        try {
            timeEntryCursor!!.close()
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy: $e")
        }

        super.onDestroy()
    }

    /*
     * Populate the view elements.
     */
    private fun fillData() {
        Log.d(TAG, "In fillData.")

        // Cheat a little on the date. This was originally referencing timeIn
        // from the cursor below.
        val date = TimeHelpers.millisToDate(day)
        title = "Entries for $date"

        try {
            timeEntryCursor!!.close()
        } catch (e: NullPointerException) {
            // Do nothing, this is expected sometimes.
        } catch (e: Exception) {
            Log.e(TAG, "timeEntryCursor.close: $e")
        }

        timeEntryCursor = db!!.dayEntryReport(day)

        try {
            timeEntryCursor!!.moveToFirst()
        } catch (e: NullPointerException) {
            Log.d(TAG, "timeEntryCursor.moveToFirst: $e")
            return
        }

        Log.d(TAG, "timeEntryCursor has ${timeEntryCursor!!.count} entries.")

        try {
            list.adapter = SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_2, timeEntryCursor,
                    arrayOf("task", "range"),
                    intArrayOf(android.R.id.text1, android.R.id.text2))
        } catch (e: Exception) {
            Log.d(TAG, "list.setAdapter: $e")
        }
    }

    /*
     * Change the view to the report.
     */
    internal fun showReport() {
        Log.d(TAG, "Changing to report layout.")

        try {
            setContentView(R.layout.report)
        } catch (e: Exception) {
            Log.e(TAG, "Caught $e while calling setContentView(R.layout.report)")
        }

        val child = arrayOf(previous, today, next)

        for (aChild in child) {
            try {
                aChild.setOnClickListener(mButtonListener)
            } catch (e: NullPointerException) {
                Toast.makeText(this@EditDayEntriesHandler,
                        "NullPointerException", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /*
     * Process the changes
     *
     * @param itemID The entry ID being changed.
     */
    private fun processChange(itemID: Long) {
        Log.d(TAG, "processChange: $itemID")
        val intent = Intent(this@EditDayEntriesHandler, ChangeEntryHandler::class.java)

        val entryCursor = db!!.fetchEntry(itemID)
        entryCursor!!.moveToFirst()
        if (entryCursor.isAfterLast) {
            Log.d(TAG, "processChange cursor had no entries for itemID $itemID")
            return
        }

        try {
            intent.putExtra(ENTRY_ID, itemID)
            intent.putExtra("chargeno", entryCursor
                    .getString(entryCursor.getColumnIndex("chargeno")))
            intent.putExtra("timein", entryCursor
                    .getLong(entryCursor.getColumnIndex("timein")))
            intent.putExtra("timeout", entryCursor
                    .getLong(entryCursor.getColumnIndex("timeout")))
        } catch (e: Exception) {
            Log.d(TAG, "$e populating intent.")
        }

        try {
            startActivityForResult(intent, ENTRY_CODE)
        } catch (e: RuntimeException) {
            Log.e(TAG, "RuntimeException caught in processChange")
            Log.e(TAG, e.toString())
        }
    }

    /**
     * This method is called when the sending activity has finished, with the
     * result it supplied.

     * @param requestCode The original request code as given to startActivity().
     * *
     * @param resultCode  From sending activity as per setResult().
     * *
     * @param data        From sending activity as per setResult().
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check to see that what we received is what we wanted to see.
        when (requestCode) {
            ENTRY_CODE -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val result = data.action
                    Log.d(TAG, "Got result from activity: $result")
                    if (result.equals("accept", ignoreCase = true)) {
                        val extras = data.extras
                        if (extras != null) {
                            Log.d(TAG, "Processing returned data.")
                            val entryID = extras.getLong(ENTRY_ID)
                            val newTask = extras.getString("task")
                            val newTimeIn = extras.getLong("timein")
                            val newTimeOut = extras.getLong("timeout")
                            val chargeNo = db!!.getTaskIDByName(newTask)
                            db!!.updateEntry(entryID, chargeNo, null, newTimeIn, newTimeOut)
                        }
                    } else if (result.equals("acceptadjacent", ignoreCase = true)) {
                        // Pass something back in the extra package to specify adjust adjacent.
                        val extras = data.extras
                        if (extras != null) {
                            Log.d(TAG, "Processing returned data.")
                            val entryID = extras.getLong(ENTRY_ID)
                            val newTask = extras.getString("task")
                            val newTimeIn = extras.getLong("timein")
                            val newTimeOut = extras.getLong("timeout")
                            val chargeNo = db!!.getTaskIDByName(newTask)
                            try {
                                val prev = db!!.getPreviousClocking(entryID)
                                if (prev > 0)
                                    db!!.updateEntry(prev, -1, null, -1, newTimeIn)
                            } catch (e: SQLException) {
                                // Don't do anything.
                            }

                            try {
                                val next = db!!.getNextClocking(entryID)
                                if (next > 0)
                                    db!!.updateEntry(next, -1, null, newTimeOut, -1)
                            } catch (e: SQLException) {
                                // Don't do anything.
                            }

                            // Change this last because the getNext/Previous
                            // depend on the DB data.
                            db!!.updateEntry(entryID, chargeNo, null, newTimeIn, newTimeOut)
                        }
                    } else if (result.equals("delete", ignoreCase = true)) {
                        val extras = data.extras
                        if (extras != null) {
                            Log.d(TAG, "Processing returned data.")
                            val entryID = extras.getLong(ENTRY_ID)
                            db!!.deleteEntry(entryID)
                        }
                    }
                }
                fillData()
            }
        }
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

    /**
     * This method is what is registered with the button to cause an action to
     * occur when it is pressed.
     */
    private val mButtonListener = OnClickListener { v ->
        Log.d(TAG, "onClickListener view id: " + v.id)

        when (v.id) {
            R.id.previous -> day = TimeHelpers.millisToStartOfDay(day) - 1000
            R.id.today -> day = TimeHelpers.millisNow()
            R.id.next -> day = TimeHelpers.millisToEndOfDay(day) + 1000
        }
        fillData()
    }

    companion object {
        private const val TAG = "EditDayEntriesHandler"
        const val ENTRY_ID = "entryID"
    }
}
