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
 * @version (classes and interfaces only, required. See footnote 1)
 * @param (methods and constructors only)
 * @return (methods only)
 * @exception (@throws is a synonym added in Javadoc 1.2)
 * @ deprecated  (see How and When To Deprecate APIs)
 * @see
 */
package com.googlecode.iqapps.IQTimeSheet;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.List;
import java.util.TimeZone;

/**
 * @author kronenpj
 */
public class MyPreferenceActivity extends PreferenceActivity {
    private static final String TAG = "MyPreferenceActivity";

    //	@Override
    //	public void onCreate(Bundle savedInstanceState) {
    //		super.onCreate(savedInstanceState);
    //
    //		// Load the preferences from an XML resource
    //		addPreferencesFromResource(R.xml.preferences);
    //
    //		ListPreference tzAnchor = (ListPreference) findPreference("tz.anchor");
    //		tzAnchor.setOrder(70);
    //		String[] timeZones = TimeZone.getAvailableIDs();
    //		tzAnchor.setEntries(timeZones);
    //		tzAnchor.setEntryValues(timeZones);
    //    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new MyPreferenceFragment()).commit();
    }

    //    @Override
    //    public void onBuildHeaders(List<Header> target) {
    //        loadHeadersFromResource(R.xml.headers_preference, target);
    //    }

    //    @Override
    //    protected boolean isValidFragment(String fragmentName) {
    //        return MyPreferenceActivity.class.getName().equals(fragmentName) ||
    //                MyPreferenceFragment.class.getName().equals(fragmentName);
    //    }
}
