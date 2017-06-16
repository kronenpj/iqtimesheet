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

/**
 * @author (classes and interfaces only, required)
 * *
 * @version (classes and interfaces only, required. See footnote 1)
 * *
 * @param       (methods and constructors only)
 * *
 * @return (methods only)
 * *
 * @exception (@throws is a synonym added in Javadoc 1.2)
 * *
 * @see  @ deprecated
 */
package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.TextView

/**
 * @author kronenpj
 */
class AboutDialog : AppCompatActivity() {

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "In onCreate.")

        setContentView(R.layout.about)
        val version = findViewById(R.id.version) as TextView
        val aboutText = findViewById(R.id.abouttext) as TextView
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // TODO: Make into string.xml reference.
        version.text = "IQTimeSheet ${BuildConfig.VERSION_NAME} (${BuildConfig.GitHash} ${BuildConfig.BuildDate})"
        aboutText.setText(R.string.about_summary)
        Log.d(TAG, "Falling out of onCreate.")
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> return super
                    .onOptionsItemSelected(menuItem)
        }
    }

    companion object {
        private val TAG = "AboutDialog"
    }
}
