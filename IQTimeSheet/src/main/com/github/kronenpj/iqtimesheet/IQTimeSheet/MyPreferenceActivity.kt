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
 * @param (methods and constructors only)
 * *
 * @return (methods only)
 * *
 * @exception (@throws is a synonym added in Javadoc 1.2)
 * * @ deprecated  (see How and When To Deprecate APIs)
 * *
 * @see
 */
package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.os.Bundle
import android.preference.PreferenceActivity

/**
 * @author kronenpj
 */
class MyPreferenceActivity : PreferenceActivity() {

    public override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)

        // Display the fragment as the main content.
        fragmentManager.beginTransaction().replace(android.R.id.content,
                MyPreferenceFragment()).commit()
    }

    companion object {
        private const val TAG = "MyPreferenceActivity"
    }
}
