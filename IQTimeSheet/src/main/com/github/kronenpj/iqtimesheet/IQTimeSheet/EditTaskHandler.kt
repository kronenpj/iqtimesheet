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
import android.database.CursorIndexOutOfBoundsException
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnFocusChangeListener
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView.OnEditorActionListener
import kotlinx.android.synthetic.main.addtask.*
import java.util.*

/**
 * Activity to provide an interface to change a task name and, potentially,
 * other attributes of a task.

 * @author Paul Kronenwetter <kronenpj></kronenpj>@gmail.com>
 */
class EditTaskHandler : AppCompatActivity() {
    private var oldData: String? = null
    private var db: TimeSheetDbAdapter? = null
    internal var oldSplitState = 0
    internal var oldParent: Long = 0
    internal var oldPercentage = 100
    internal var items: Array<String>? = null

    /**
     * Called when the activity is resumed or created.
     */
    public override fun onResume() {
        super.onResume()
        Log.d(TAG, "In onResume.")

        db = TimeSheetDbAdapter(this)

        showTaskEdit()

        items = db!!.fetchParentTasks()

        TaskSpinner!!.adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_item, items)

        PercentSlider!!.max = 100
        PercentSlider!!.progress = 100
        PercentLabel!!.setText(R.string.DefaultPercentage)

        try {
            oldParent = db!!.getSplitTaskParent(oldData!!)
            oldPercentage = db!!.getSplitTaskPercentage(oldData!!)
            oldSplitState = db!!.getSplitTaskFlag(oldData!!)

            SplitTask!!.isChecked = oldSplitState == 1
            // TODO: There must be a better way to find a string in the spinner.
            val parentName = db!!.getTaskNameByID(oldParent)
            Log.d(TAG, "showTaskEdit: trying to find: " + parentName!!)
            var i = 0
            for (j in items!!.indices) {
                Log.d(TAG, "showTaskEdit: $j / ${items!![j]}")
                if (items!![j].equals(parentName, ignoreCase = true)) {
                    i = j
                    break
                }
            }
            Log.d(TAG, "showTaskEdit: trying to select: $i / ${items!![i]}")
            TaskSpinner!!.setSelection(i)
            PercentSlider!!.progress = oldPercentage
            PercentLabel!!.setText(oldPercentage.toString())

            if (oldSplitState == 1) {
                ParentLabel!!.visibility = View.VISIBLE
                TaskSpinner!!.visibility = View.VISIBLE
                PercentLabel!!.visibility = View.VISIBLE
                PercentSymbol!!.visibility = View.VISIBLE
                PercentSlider!!.visibility = View.VISIBLE
            }
        } catch (e: CursorIndexOutOfBoundsException) {
            Log.i(TAG, "showTaskEdit: $e")
            if (oldData == null) Log.d(TAG, "showTaskEdit: oldData is null")
            if (db == null) Log.d(TAG, "showTaskEdit: db is null")
        }
    }

    internal fun showTaskEdit() {
        Log.d(TAG, "Changing to addtask layout.")
        setContentView(R.layout.addtask)
        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } catch (e: NullPointerException) {
            Log.d(TAG, "setDisplayHomeAsUpEnabled returned NullPointerException.")
        }

        val extras = intent.extras
        if (extras != null) {
            oldData = extras.getString("taskName")
            EditTask!!.setText(oldData)
        }

        SplitTask!!.setOnClickListener(mCheckBoxListener)
        PercentSlider!!.setOnSeekBarChangeListener(mSeekBarListener)
        PercentLabel!!.onFocusChangeListener = mTextListener
        PercentLabel!!.setOnEditorActionListener(mEditorListener)
        LastUsedLabel!!.text = db!!.getTaskUsageTuple(db!!.getTaskIDByName(oldData.toString()))?.usage.toString()

        val child = arrayOf(ChangeTask, CancelEdit)

        for (aChild in child) {
            try {
                aChild.setOnClickListener(mButtonListener)
            } catch (e: NullPointerException) {
                Log.e(TAG, "NullPointerException adding listener to button.")
            }

        }

        // Look for tasks that are split children of this task and list them.
        val parentTaskID = db!!.getTaskIDByName(oldData!!)
        // Log.v(TAG, "Parent item: " + parentTaskID + " (" + db.getTaskNameByID(parentTaskID) + ")");
        val children = db!!.fetchChildTasks(parentTaskID)
        if (children.isNotEmpty()) {
            SplitTask!!.visibility = View.GONE
            val myChildList = childlist
            val childNames = ArrayList<String>(children.size)
            for (childID in children) {
                childNames.add(db!!.getTaskNameByID(childID)!!)
                // Log.v(TAG, "  Child item: " + childID + " (" + db.getTaskNameByID(childID) + ")");
            }
            myChildList.adapter = MyArrayAdapter(this,
                    android.R.layout.simple_list_item_1, childNames)
            // TODO: Find some way to be able to edit these items and still be able to return here.
            // myChildList.setOnItemClickListener(mChildListener);
            myChildList.isEnabled = true
            myChildList.visibility = View.VISIBLE
        }
    }

    /**
     * This method is what is registered with the button to cause an action to
     * occur when it is pressed.
     */
    private val mButtonListener = OnClickListener { v ->
        // Perform action on selected list item.

        val item = (v as Button).text.toString()
        if (item.equals("cancel", ignoreCase = true)) {
            setResult(Activity.RESULT_CANCELED, Intent().setAction(item))
        } else {
            val result = EditTask!!.text.toString()
            val intent = Intent()
            intent.action = result
            intent.putExtra("oldTaskName", oldData)
            // TODO: Test case to make sure this doesn't clobber split in
            // parent tasks, where it == 2.
            if (oldSplitState != 2) {
                intent.putExtra("split", if (SplitTask!!.isChecked) 1 else 0)
            } else {
                intent.putExtra("split", oldSplitState)
            }
            intent.putExtra("oldSplit", oldSplitState)
            if (SplitTask!!.isChecked) {
                intent.putExtra("parent", TaskSpinner!!.selectedItem as String)
                intent.putExtra("oldParent", oldParent)
                intent.putExtra("percentage", PercentSlider!!.progress)
                intent.putExtra("oldPercentage", oldPercentage)
            }
            setResult(Activity.RESULT_OK, intent)
        }
        finish()
    }

    /**
     * This method is what is registered with the button to cause an action to
     * occur when it is pressed.
     */
    private val mChildListener = AdapterView.OnItemClickListener { parent, v, position, id ->
        // Perform action on selected list item.

        //String item = ((TextView) v).getText().toString();
        Log.d(TAG, "Edit (child) task")
        val intent = Intent(applicationContext, EditTaskHandler::class.java)
        intent.putExtra("taskName", (v as TextView).text.toString())
        try {
            startActivityForResult(intent, ActivityCodes.TASKEDIT.ordinal)
        } catch (e: RuntimeException) {
            Toast.makeText(applicationContext, "RuntimeException",
                    Toast.LENGTH_SHORT).show()
            Log.d(TAG, e.localizedMessage)
            Log.e(TAG, "RuntimeException caught.")
        }

        // TODO: This finishes the entire activity, need to find a better way
        // to display/edit this. Maybe another (duplicate) activity?`1
        finish()
    }

    /**
     * This method is what is registered with the checkbox to cause an action to
     * occur when it is pressed.
     */
    private val mCheckBoxListener = OnClickListener { v ->
        // Perform action on selected list item.

        if ((v as CheckBox).isChecked) {
            ParentLabel!!.visibility = View.VISIBLE
            TaskSpinner!!.visibility = View.VISIBLE
            PercentLabel!!.visibility = View.VISIBLE
            PercentSymbol!!.visibility = View.VISIBLE
            PercentSlider!!.visibility = View.VISIBLE
        } else {
            ParentLabel!!.visibility = View.GONE
            TaskSpinner!!.visibility = View.GONE
            PercentLabel!!.visibility = View.GONE
            PercentSymbol!!.visibility = View.GONE
            PercentSlider!!.visibility = View.GONE
        }
    }

    /**
     * This method is registered with the percent slider to cause an action to
     * occur when it is changed.
     */
    private val mSeekBarListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                       fromUser: Boolean) {
            // percentLabel.setText(String.valueOf(seekBar.getProgress()));
            PercentLabel!!.setText(progress.toString())
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            seekBar.requestFocus()
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }

    /**
     * This method is registered with the percent slider to cause an action to
     * occur when it is changed.
     */
    private val mTextListener = OnFocusChangeListener { v, hasFocus ->
        if (!hasFocus) {
            try {
                var temp = Integer.valueOf((v as TextView).text.toString())!!
                if (temp > 100) temp = 100
                if (temp < 0) temp = 0
                PercentSlider!!.progress = temp
            } catch (e: NumberFormatException) {
                PercentLabel!!.setText(PercentSlider!!.progress.toString())
            }
        }
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

    /**
     * This method is registered with the percent label to cause an action to
     * occur when it is changed.
     */
    private val mEditorListener = OnEditorActionListener { v, actionId, event ->
        try {
            var temp = Integer.valueOf(v.text.toString())!!
            if (temp > 100) temp = 100
            if (temp < 0) temp = 0
            PercentSlider!!.progress = temp
        } catch (e: NumberFormatException) {
            PercentLabel!!.setText(PercentSlider!!.progress.toString())
        }

        v.clearFocus()
        true
    }

    companion object {
        private val TAG = "EditTaskHandler"
    }
}