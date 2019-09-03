package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import java.util.*

/**
 * @author kronenpj
 */
class MyPreferenceFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        val tzAnchor = findPreference("tz.anchor") as ListPreference
        tzAnchor.order = 70
        val timeZones = TimeZone.getAvailableIDs()
        tzAnchor.entries = timeZones
        tzAnchor.entryValues = timeZones
    }
}
