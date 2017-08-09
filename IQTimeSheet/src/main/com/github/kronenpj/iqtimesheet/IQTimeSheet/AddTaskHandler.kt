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
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnFocusChangeListener
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView.OnEditorActionListener
import kotlinx.android.synthetic.main.addtask.*

/**
 * Activity to provide an interface to add a task to the database.

 * @author Paul Kronenwetter <kronenpj@gmail.com>
 */
class AddTaskHandler : Activity() {

    private var textField: EditText? = null
    private var parentLabel: TextView? = null
    private var taskSpinner: Spinner? = null
    private var splitTask: CheckBox? = null
    private var percentLabel: EditText? = null
    private var percentSymbol: TextView? = null
    private var percentSlider: SeekBar? = null
    private var db: TimeSheetDbAdapter? = null
    // private var parents: Array<String>? = null

    /** Called when the activity is first created.  */
    // @Override
    // public void onCreate(Bundle savedInstanceState) {
    // super.onCreate(savedInstanceState);
    // Log.d(TAG, "In onCreate.");
    // }

    /**
     * Called when the activity is first created.
     */
    public override fun onResume() {
        super.onResume()
        Log.d(TAG, "In onResume.")

        showTaskAdd()

        db = TimeSheetDbAdapter(this)

        val items = db!!.fetchParentTasks()

        taskSpinner!!.adapter = ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items)

        percentSlider!!.max = 100
        // TODO: Retrieve from database or default to 100 if none.
        percentSlider!!.progress = 100
        percentLabel!!.setText("100")
    }

    internal fun showTaskAdd() {
        Log.d(TAG, "Changing to addtask layout.")
        setContentView(R.layout.addtask)

        textField = findViewById(R.id.EditTask) as EditText
        val child = arrayOf(findViewById(R.id.ChangeTask) as Button,
                findViewById(R.id.CancelEdit) as Button)
        parentLabel = findViewById(R.id.ParentLabel) as TextView
        taskSpinner = findViewById(R.id.TaskSpinner) as Spinner
        splitTask = findViewById(R.id.SplitTask) as CheckBox
        percentLabel = findViewById(R.id.PercentLabel) as EditText
        percentSymbol = findViewById(R.id.PercentSymbol) as TextView
        percentSlider = findViewById(R.id.PercentSlider) as SeekBar

        splitTask!!.setOnClickListener(mCheckBoxListener)

        percentSlider!!.setOnSeekBarChangeListener(mSeekBarListener)
        percentLabel!!.onFocusChangeListener = mTextListener
        percentLabel!!.setOnEditorActionListener(mEditorListener)

        for (aChild in child) {
            try {
                aChild.setOnClickListener(mButtonListener)
            } catch (e: NullPointerException) {
                Log.e(TAG, "NullPointerException adding listener to button.")
            }
        }
    }

    /**
     * This method is what is registered with the button to cause an action to
     * occur when it is pressed.
     */
    private val mButtonListener = OnClickListener { v ->
        // Perform action chosen by the user.

        val item = (v as Button).text.toString()
        if (item.equals("cancel", ignoreCase = true)) {
            setResult(Activity.RESULT_CANCELED, Intent().setAction(item))
        } else {
            val result = textField!!.text.toString()
            val mIntent = Intent()
            mIntent.putExtra("split", splitTask!!.isChecked)
            if (splitTask!!.isChecked) {
                mIntent.putExtra("parent", taskSpinner!!.selectedItem as String)
                mIntent.putExtra("percentage", percentSlider!!.progress)
            }
            mIntent.action = result
            setResult(Activity.RESULT_OK, mIntent)
        }
        finish()
    }

    /**
     * This method is what is registered with the checkbox to cause an action to
     * occur when it is pressed.
     */
    private val mCheckBoxListener = OnClickListener { v ->
        // Perform action on selected list item.

        if ((v as CheckBox).isChecked) {
            parentLabel!!.visibility = View.VISIBLE
            taskSpinner!!.visibility = View.VISIBLE
            percentLabel!!.visibility = View.VISIBLE
            percentSymbol!!.visibility = View.VISIBLE
            percentSlider!!.visibility = View.VISIBLE
        } else {
            parentLabel!!.visibility = View.INVISIBLE
            taskSpinner!!.visibility = View.INVISIBLE
            percentLabel!!.visibility = View.INVISIBLE
            percentSymbol!!.visibility = View.INVISIBLE
            percentSlider!!.visibility = View.INVISIBLE
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
            percentLabel!!.setText(progress.toString())
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
                var temp = Integer.valueOf((v as TextView).text
                        .toString())!!
                if (temp > 100) temp = 100
                if (temp < 0) temp = 0
                percentSlider!!.progress = temp
            } catch (e: NumberFormatException) {
                percentLabel!!.setText(percentSlider!!
                        .progress.toString())
            }
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
            percentSlider!!.progress = temp
        } catch (e: NumberFormatException) {
            percentLabel!!.setText(percentSlider!!.progress.toString())
        }

        v.clearFocus()
        true
    }

    companion object {
        private val TAG = "AddTaskHandler"
    }
}