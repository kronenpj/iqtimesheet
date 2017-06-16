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
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.database.SQLException
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.Toast

import com.github.kronenpj.iqtimesheet.TimeHelpers

/**
 * Activity to provide an interface to change an entry.

 * @author Paul Kronenwetter <kronenpj></kronenpj>@gmail.com>
 */
class ChangeEntryHandler : AppCompatActivity() {
    private var entryCursor: Cursor? = null
    private var child: Array<Button>? = null
    private var db: TimeSheetDbAdapter? = null
    private var entryID: Long = -1
    private var newTask: String? = null
    private var newTimeIn: Long = -1
    private var newTimeOut: Long = -1
    private var newDate: Long = -1
    private var alignMinutes = 0
    private var alignMinutesAuto = false

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            showChangeLayout()
        } catch (e: RuntimeException) {
            Log.e(TAG, e.toString() + " calling showChangeLayout")
        }

        alignMinutes = TimeSheetActivity.prefs.alignMinutes
        alignMinutesAuto = TimeSheetActivity.prefs.alignMinutesAuto

        val changeButton = findViewById(R.id.changealign) as Button
        changeButton.text = "Align ($alignMinutes min)"
        if (alignMinutesAuto)
            changeButton.visibility = View.INVISIBLE

        db = TimeSheetDbAdapter(this)
        setupDB()

        retrieveData()
        fillData()
    }

    /**
     * Called when the activity destroyed.
     */
    public override fun onDestroy() {
        entryCursor!!.close()
        db!!.close()
        super.onDestroy()
    }

    /**
     * Called when the activity is first created to create a dialog.
     */
    override fun onCreateDialog(dialogId: Int): Dialog {
        val dialog: Dialog
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure you want to delete this entry?")
                .setCancelable(true)
                .setPositiveButton("Yes"
                ) { dialog, id ->
                    val intent = Intent()
                    intent.putExtra(EditDayEntriesHandler.ENTRY_ID,
                            entryID)
                    intent.action = "delete"
                    setResult(Activity.RESULT_OK, intent)
                    this@ChangeEntryHandler.finish()
                }
                .setNegativeButton("No") { dialog, id -> dialog.cancel() }
        dialog = builder.create()
        return dialog
    }

    internal fun showChangeLayout() {
        setContentView(R.layout.changeentry)

        // NOTE: The order of these is important, the array is referenced
        // by index a few times below.
        child = arrayOf(findViewById(R.id.defaulttask) as Button, findViewById(R.id.date) as Button, findViewById(R.id.starttime) as Button, findViewById(R.id.endtime) as Button, findViewById(R.id.changeok) as Button, findViewById(R.id.changecancel) as Button, findViewById(R.id.changedelete) as Button, findViewById(R.id.changealign) as Button, findViewById(R.id.changeadjacent) as Button)

        for (aChild in child!!) {
            try {
                aChild.setOnClickListener(mButtonListener)
            } catch (e: NullPointerException) {
                Toast.makeText(this@ChangeEntryHandler, "NullPointerException",
                        Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun retrieveData() {
        val extras = intent.extras
        if (extras == null) {
            Log.d(TAG, "Extras bundle is empty.")
            return
        }
        entryID = extras.getLong(EditDayEntriesHandler.ENTRY_ID)
        entryCursor = db!!.fetchEntry(entryID)

        val chargeNo = entryCursor!!.getLong(entryCursor!!
                .getColumnIndex(TimeSheetDbAdapter.KEY_CHARGENO))
        newTask = db!!.getTaskNameByID(chargeNo)

        newTimeIn = entryCursor!!.getLong(entryCursor!!
                .getColumnIndex(TimeSheetDbAdapter.KEY_TIMEIN))

        newTimeOut = entryCursor!!.getLong(entryCursor!!
                .getColumnIndex(TimeSheetDbAdapter.KEY_TIMEOUT))

        newDate = TimeHelpers.millisToStartOfDay(newTimeIn)
    }

    private fun fillData() {
        var hour: Int
        var minute: Int

        child!![0].text = newTask
        child!![1].text = TimeHelpers.millisToDate(newDate)

        if (alignMinutesAuto) {
            newTimeIn = TimeHelpers.millisToAlignMinutes(newTimeIn, alignMinutes)
        }
        hour = TimeHelpers.millisToHour(newTimeIn)
        minute = TimeHelpers.millisToMinute(newTimeIn)
        child!![2].text = "${TimeHelpers.formatHours(hour)}:${TimeHelpers.formatMinutes(minute)}"

        if (newTimeOut == 0L) {
            child!![3].text = "Now"
        } else {
            if (alignMinutesAuto) {
                newTimeOut = TimeHelpers.millisToAlignMinutes(newTimeOut, alignMinutes)
            }
            hour = TimeHelpers.millisToHour(newTimeOut)
            minute = TimeHelpers.millisToMinute(newTimeOut)
            child!![3].text = "${TimeHelpers.formatHours(hour)}:${TimeHelpers.formatMinutes(minute)}"
        }
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
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
            finish()
        }

    }

    /**
     * This method is what is registered with the button to cause an action to
     * occur when it is pressed.
     */
    private val mButtonListener = OnClickListener { v ->
        val intent: Intent
        // Perform action on selected list item.

        Log.d(TAG, "onClickListener view id: " + v.id)
        Log.d(TAG, "onClickListener defaulttask id: " + R.id.defaulttask)

        when (v.id) {
            R.id.changecancel -> {
                setResult(Activity.RESULT_CANCELED, Intent().setAction("cancel"))
                finish()
            }
            R.id.defaulttask -> {
                intent = Intent(this@ChangeEntryHandler,
                        ChangeTaskList::class.java)
                try {
                    startActivityForResult(intent, TASKCHOOSE_CODE)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "startActivity ChangeTaskList: " + e.toString())
                }

            }
            R.id.date -> {
                intent = Intent(this@ChangeEntryHandler, ChangeDate::class.java)
                intent.putExtra("time", newDate)
                try {
                    startActivityForResult(intent, CHANGEDATE_CODE)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "startActivity ChangeDate: " + e.toString())
                }

            }
            R.id.starttime -> {
                intent = Intent(this@ChangeEntryHandler, ChangeTime::class.java)
                intent.putExtra("time", newTimeIn)
                try {
                    startActivityForResult(intent, CHANGETIMEIN_CODE)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "startActivity ChangeTime: " + e.toString())
                }

            }
            R.id.endtime -> {
                intent = Intent(this@ChangeEntryHandler, ChangeTime::class.java)
                intent.putExtra("time", newTimeOut)
                try {
                    startActivityForResult(intent, CHANGETIMEOUT_CODE)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "startActivity ChangeTime: " + e.toString())
                }

            }
            R.id.changealign -> {
                newTimeIn = TimeHelpers.millisToAlignMinutes(newTimeIn,
                        alignMinutes)
                if (newTimeOut != 0L) {
                    newTimeOut = TimeHelpers.millisToAlignMinutes(newTimeOut,
                            alignMinutes)
                }
                fillData()
            }
            R.id.changeok -> {
                intent = Intent()
                intent.putExtra(EditDayEntriesHandler.ENTRY_ID, entryID)
                // Push task title into response.
                intent.putExtra(TimeSheetDbAdapter.KEY_TASK, newTask)
                // Push start and end time milliseconds into response
                // bundle.
                intent.putExtra(TimeSheetDbAdapter.KEY_TIMEIN, newTimeIn)
                intent.putExtra(TimeSheetDbAdapter.KEY_TIMEOUT, newTimeOut)
                intent.action = "accept"
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            R.id.changeadjacent -> {
                intent = Intent()
                intent.putExtra(EditDayEntriesHandler.ENTRY_ID, entryID)
                // Push task title into response.
                intent.putExtra(TimeSheetDbAdapter.KEY_TASK, newTask)
                // Push start and end time milliseconds into response
                // bundle.
                intent.putExtra(TimeSheetDbAdapter.KEY_TIMEIN, newTimeIn)
                intent.putExtra(TimeSheetDbAdapter.KEY_TIMEOUT, newTimeOut)
                intent.action = "acceptadjacent"
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            R.id.changedelete -> showDialog(CONFIRM_DELETE_DIALOG)
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
            TASKCHOOSE_CODE -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Log.d(TAG, "onActivityResult action: " + data.action)
                    // db.updateEntry(entryID, Long.parseLong(data.getAction()),
                    // null, -1, -1);
                    newTask = db!!
                            .getTaskNameByID(java.lang.Long.valueOf(data.action)!!)
                    fillData()
                }
            }
            CHANGETIMEIN_CODE -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    newTimeIn = java.lang.Long.valueOf(data.action)!!
                    Log.d(TAG, "onActivityResult action: " + newTimeIn)
                    fillData()
                }
            }
            CHANGETIMEOUT_CODE -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    newTimeOut = java.lang.Long.valueOf(data.action)!!
                    Log.d(TAG, "onActivityResult action: " + newTimeOut)
                    fillData()
                }
            }
            CHANGEDATE_CODE -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Log.d(TAG, "onActivityResult action: " + data.action)
                    newDate = java.lang.Long.valueOf(data.action)!!
                    newTimeIn = TimeHelpers.millisSetTime(newDate,
                            TimeHelpers.millisToHour(newTimeIn),
                            TimeHelpers.millisToMinute(newTimeIn))
                    newTimeOut = TimeHelpers.millisSetTime(newDate,
                            TimeHelpers.millisToHour(newTimeOut),
                            TimeHelpers.millisToMinute(newTimeOut))
                    fillData()
                }
            }
        }
    }

    companion object {
        private val TAG = "ChangeEntryHandler"
        private val TASKCHOOSE_CODE = 0x01
        private val CHANGETIMEIN_CODE = 0x02
        private val CHANGETIMEOUT_CODE = 0x03
        private val CHANGEDATE_CODE = 0x04
        private val CONFIRM_DELETE_DIALOG = 0x10
    }
}
