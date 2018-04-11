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

import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.SimpleCursorAdapter
import android.widget.TextView

import java.util.Locale

/**
 * An extension of the SimpleCursorAdapter to allow a list item to properly
 * display the desired data on the second line. In this case it's the number of
 * hours an entry represents.

 * @author Paul Kronenwetter <kronenpj></kronenpj>@gmail.com>
 */
class ReportCursorAdapter
/**
 * @param context
 * *
 * @param layout
 * *
 * @param c
 * *
 * @param from
 * *
 * @param to
 */
(context: Context, layout: Int, c: Cursor,
 from: Array<String>, to: IntArray) : SimpleCursorAdapter(context, layout, c, from, to) {

    override fun bindView(view: View, context: Context, cursor: Cursor) {

        // TODO: Remove the extraneous tempString and temp declarations and
        // assignments.
        var t = view.findViewById(android.R.id.text1) as TextView
        val tempString = cursor.getString(cursor.getColumnIndex("task"))
        t.text = tempString
        // FIXME: Horrible hack to get the list visible in a dark theme.
        // I've invested hours trying to figure out the proper way to fix this.
        t.setTextColor(Color.WHITE)

        t = view.findViewById(android.R.id.text2) as TextView
        val temp = cursor.getFloat(cursor.getColumnIndex("total"))
        Log.d(TAG, "bindView: task: " + tempString + ", total: "
                + String.format(Locale.US, "%1.2f hours", temp))
        t.text = String.format(Locale.US, "%1.2f hours", temp)
        // FIXME: Horrible hack to get the list visible in a dark theme.
        // I've invested hours trying to figure out the proper way to fix this.
        t.setTextColor(Color.WHITE)
    }

    companion object {
        private val TAG = "ReportCursorAdapter"
    }
}
