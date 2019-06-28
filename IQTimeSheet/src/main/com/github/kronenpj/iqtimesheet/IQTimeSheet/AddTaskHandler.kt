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

    private var db: TimeSheetDbAdapter? = null

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

        TaskSpinner.adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_item, items)

        PercentSlider.max = 100
        // TODO: Retrieve from database or default to 100 if none.
        PercentSlider.progress = 100
        PercentLabel.setText("100")
    }

    internal fun showTaskAdd() {
        Log.d(TAG, "Changing to addtask layout.")
        setContentView(R.layout.addtask)

        val child = arrayOf(ChangeTask, CancelEdit)

        SplitTask.setOnClickListener(mCheckBoxListener)

        PercentSlider.setOnSeekBarChangeListener(mSeekBarListener)
        PercentLabel.onFocusChangeListener = mTextListener
        PercentLabel.setOnEditorActionListener(mEditorListener)

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
            setResult(RESULT_CANCELED, Intent().setAction(item))
        } else {
            val result = EditTask.text.toString()
            val mIntent = Intent()
            mIntent.putExtra("split", SplitTask.isChecked)
            if (SplitTask.isChecked) {
                mIntent.putExtra("parent", TaskSpinner.selectedItem as String)
                mIntent.putExtra("percentage", PercentSlider.progress)
            }
            mIntent.action = result
            setResult(RESULT_OK, mIntent)
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
            ParentLabel.visibility = View.VISIBLE
            TaskSpinner.visibility = View.VISIBLE
            PercentLabel.visibility = View.VISIBLE
            PercentSymbol.visibility = View.VISIBLE
            PercentSlider.visibility = View.VISIBLE
        } else {
            ParentLabel.visibility = View.INVISIBLE
            TaskSpinner.visibility = View.INVISIBLE
            PercentLabel.visibility = View.INVISIBLE
            PercentSymbol.visibility = View.INVISIBLE
            PercentSlider.visibility = View.INVISIBLE
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
            PercentLabel.setText(progress.toString())
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
                        .toString())
                if (temp > 100) temp = 100
                if (temp < 0) temp = 0
                PercentSlider.progress = temp
            } catch (e: NumberFormatException) {
                PercentLabel.setText(PercentSlider
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
            var temp = Integer.valueOf(v.text.toString())
            if (temp > 100) temp = 100
            if (temp < 0) temp = 0
            PercentSlider.progress = temp
        } catch (e: NumberFormatException) {
            PercentLabel.setText(PercentSlider.progress.toString())
        }

        v.clearFocus()
        true
    }

    companion object {
        private const val TAG = "AddTaskHandler"
    }
}