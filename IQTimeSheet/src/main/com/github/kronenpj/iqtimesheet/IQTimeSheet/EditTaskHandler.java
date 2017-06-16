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

package com.github.kronenpj.iqtimesheet.IQTimeSheet;

import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to provide an interface to change a task name and, potentially,
 * other attributes of a task.
 *
 * @author Paul Kronenwetter <kronenpj@gmail.com>
 */
public class EditTaskHandler extends AppCompatActivity {
    private static final String TAG = "EditTaskHandler";
    private EditText textField;
    private String oldData;
    private Spinner taskSpinner;
    private CheckBox splitTask;
    private TextView parentLabel;
    private TextView percentLabel;
    private SeekBar percentSlider;
    private TextView percentSymbol;
    private TimeSheetDbAdapter db;
    private Cursor parents;
    int oldSplitState = 0;
    long oldParent;
    int oldPercentage = 100;
    String[] items;

    /** Called when the activity is first created. */
    // @Override
    // public void onCreate(Bundle savedInstanceState) {
    // super.onCreate(savedInstanceState);
    // Log.d(TAG, "In onCreate.");
    // showTaskEdit();
    // }

    /**
     * Called when the activity is resumed or created.
     */
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "In onResume.");

        db = new TimeSheetDbAdapter(this);
        try {
            db.open();
        } catch (SQLException e) {
            Log.i(TAG, "Database open failed.");
            finish();
        }

        showTaskEdit();

        parents = db.fetchParentTasks();
        startManagingCursor(parents);
        items = new String[parents.getCount()];
        parents.moveToFirst();
        int i = 0;
        while (!parents.isAfterLast()) {
            items[i] = parents.getString(1);
            parents.moveToNext();
            i++;
        }

        taskSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items));

        percentSlider.setMax(100);
        percentSlider.setProgress(100);
        percentLabel.setText(R.string.DefaultPercentage);

        try {
            oldParent = db.getSplitTaskParent(oldData);
            oldPercentage = db.getSplitTaskPercentage(oldData);
            oldSplitState = db.getSplitTaskFlag(oldData);

            splitTask.setChecked(oldSplitState == 1);
            // TODO: There must be a better way to find a string in the spinner.
            String parentName = db.getTaskNameByID(oldParent);
            Log.d(TAG, "showTaskEdit: trying to find: " + parentName);
            i = 0;
            for (int j = 0; j < items.length; j++) {
                Log.d(TAG, "showTaskEdit: " + j + " / " + items[j]);
                if (items[j].equalsIgnoreCase(parentName)) {
                    i = j;
                    break;
                }
            }
            Log.d(TAG, "showTaskEdit: trying to select: " + i + " / "
                    + items[i]);
            taskSpinner.setSelection(i);
            percentSlider.setProgress(oldPercentage);
            percentLabel.setText(String.valueOf(oldPercentage));

            if (oldSplitState == 1) {
                parentLabel.setVisibility(View.VISIBLE);
                taskSpinner.setVisibility(View.VISIBLE);
                percentLabel.setVisibility(View.VISIBLE);
                percentSymbol.setVisibility(View.VISIBLE);
                percentSlider.setVisibility(View.VISIBLE);
            }
        } catch (CursorIndexOutOfBoundsException e) {
            Log.i(TAG, "showTaskEdit: " + e.toString());
            if (oldData == null)
                Log.d(TAG, "showTaskEdit: oldData is null");
            if (db == null)
                Log.d(TAG, "showTaskEdit: db is null");
        }
    }

    void showTaskEdit() {
        Log.d(TAG, "Changing to addtask layout.");
        setContentView(R.layout.addtask);
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
        }

        textField = (EditText) findViewById(R.id.EditTask);
        parentLabel = (TextView) findViewById(R.id.ParentLabel);
        percentLabel = (EditText) findViewById(R.id.PercentLabel);
        percentSymbol = (TextView) findViewById(R.id.PercentSymbol);
        percentSlider = (SeekBar) findViewById(R.id.PercentSlider);
        splitTask = (CheckBox) findViewById(R.id.SplitTask);
        taskSpinner = (Spinner) findViewById(R.id.TaskSpinner);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            oldData = extras.getString("taskName");
            textField.setText(oldData);
        }

        splitTask.setOnClickListener(mCheckBoxListener);

        percentSlider.setOnSeekBarChangeListener(mSeekBarListener);
        percentLabel.setOnFocusChangeListener(mTextListener);
        percentLabel.setOnEditorActionListener(mEditorListener);

        Button[] child = new Button[]{(Button) findViewById(R.id.ChangeTask),
                (Button) findViewById(R.id.CancelEdit)};

        for (Button aChild : child) {
            try {
                aChild.setOnClickListener(mButtonListener);
            } catch (NullPointerException e) {
                Log.e(TAG, "NullPointerException adding listener to button.");
            }
        }

        // Look for tasks that are split children of this task and list them.
        long parentTaskID = db.getTaskIDByName(oldData);
        // Log.v(TAG, "Parent item: " + parentTaskID + " (" + db.getTaskNameByID(parentTaskID) + ")");
        Long[] children = db.fetchChildTasks(parentTaskID);
        if (children.length > 0) {
            splitTask.setVisibility(View.GONE);
            ListView myChildList = (ListView) findViewById(R.id.childlist);
            List<String> childNames = new ArrayList<>(children.length);
            for (Long childID : children) {
                childNames.add(db.getTaskNameByID(childID));
                // Log.v(TAG, "  Child item: " + childID + " (" + db.getTaskNameByID(childID) + ")");
            }
            myChildList.setAdapter(new MyArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, childNames));
            // TODO: Find some way to be able to edit these items and still be able to return here.
            // myChildList.setOnItemClickListener(mChildListener);
            myChildList.setVisibility(View.VISIBLE);
        }
    }

    /**
     * This method is what is registered with the button to cause an action to
     * occur when it is pressed.
     */
    private OnClickListener mButtonListener = new OnClickListener() {
        public void onClick(View v) {
            // Perform action on selected list item.

            String item = ((Button) v).getText().toString();
            if (item.equalsIgnoreCase("cancel")) {
                setResult(RESULT_CANCELED, (new Intent()).setAction(item));
            } else {
                String result = textField.getText().toString();
                Intent intent = new Intent();
                intent.setAction(result);
                intent.putExtra("oldTaskName", oldData);
                // TODO: Test case to make sure this doesn't clobber split in
                // parent tasks, where it == 2.
                if (oldSplitState != 2) {
                    intent.putExtra("split", splitTask.isChecked() ? 1 : 0);
                } else {
                    intent.putExtra("split", oldSplitState);
                }
                intent.putExtra("oldSplit", oldSplitState);
                if (splitTask.isChecked()) {
                    intent.putExtra("parent",
                            (String) taskSpinner.getSelectedItem());
                    intent.putExtra("oldParent", oldParent);
                    intent.putExtra("percentage", percentSlider.getProgress());
                    intent.putExtra("oldPercentage", oldPercentage);
                }
                setResult(RESULT_OK, intent);
            }
            closeCursorDB();
            finish();
        }
    };

    /**
     * This method is what is registered with the button to cause an action to
     * occur when it is pressed.
     */
    private AdapterView.OnItemClickListener mChildListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            // Perform action on selected list item.

            String item = ((TextView) v).getText().toString();
            Log.d(TAG, "Edit (child) task");
            Intent intent = new Intent(getApplicationContext(),
                    EditTaskHandler.class);
            intent.putExtra("taskName", ((TextView) v).getText().toString());
            try {
                startActivityForResult(intent, ActivityCodes.TASKEDIT.ordinal());
            } catch (RuntimeException e) {
                Toast.makeText(getApplicationContext(), "RuntimeException",
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, e.getLocalizedMessage());
                Log.e(TAG, "RuntimeException caught.");
            }

            // TODO: This finishes the entire activity, need to find a better way
            // to display/edit this. Maybe another (duplicate) activity?`1
            finish();
        }
    };

    /**
     * Attempts to close both the cursor and the database connection.
     */
    private void closeCursorDB() {
        try {
            parents.close();
        } catch (SQLException e) {
            Log.i(TAG, "Cursor close: " + e.toString());
        }
        try {
            db.close();
        } catch (SQLException e) {
            Log.i(TAG, "Database close: " + e.toString());
        }
    }

    /**
     * This method is what is registered with the checkbox to cause an action to
     * occur when it is pressed.
     */
    private OnClickListener mCheckBoxListener = new OnClickListener() {
        public void onClick(View v) {
            // Perform action on selected list item.

            if (((CheckBox) v).isChecked()) {
                parentLabel.setVisibility(View.VISIBLE);
                taskSpinner.setVisibility(View.VISIBLE);
                percentLabel.setVisibility(View.VISIBLE);
                percentSymbol.setVisibility(View.VISIBLE);
                percentSlider.setVisibility(View.VISIBLE);
            } else {
                parentLabel.setVisibility(View.GONE);
                taskSpinner.setVisibility(View.GONE);
                percentLabel.setVisibility(View.GONE);
                percentSymbol.setVisibility(View.GONE);
                percentSlider.setVisibility(View.GONE);
            }
        }
    };

    /**
     * This method is registered with the percent slider to cause an action to
     * occur when it is changed.
     */
    private OnSeekBarChangeListener mSeekBarListener = new OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            // percentLabel.setText(String.valueOf(seekBar.getProgress()));
            percentLabel.setText(String.valueOf(progress));
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            seekBar.requestFocus();
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    /**
     * This method is registered with the percent slider to cause an action to
     * occur when it is changed.
     */
    private OnFocusChangeListener mTextListener = new OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                try {
                    int temp = Integer.valueOf(((TextView) v).getText()
                            .toString());
                    if (temp > 100) temp = 100;
                    if (temp < 0) temp = 0;
                    percentSlider.setProgress(temp);
                } catch (NumberFormatException e) {
                    percentLabel.setText(String.valueOf(percentSlider
                            .getProgress()));
                }
            }
        }
    };

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home: {
                finish();
                return true;
            }
            default:
                return super
                        .onOptionsItemSelected(menuItem);
        }
    }

    /**
     * This method is registered with the percent label to cause an action to
     * occur when it is changed.
     */
    private OnEditorActionListener mEditorListener = new OnEditorActionListener() {
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            try {
                int temp = Integer.valueOf(v.getText().toString());
                if (temp > 100) temp = 100;
                if (temp < 0) temp = 0;
                percentSlider.setProgress(temp);
            } catch (NumberFormatException e) {
                percentLabel.setText(String.valueOf(percentSlider.getProgress()));
            }
            v.clearFocus();
            return true;
        }
    };
}