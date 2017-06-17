package com.github.kronenpj.iqtimesheet.IQTimeSheet;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

import java.util.TimeZone;

/**
 * @author kronenpj
 */
public class MyPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ListPreference tzAnchor = (ListPreference) findPreference("tz.anchor");
        tzAnchor.setOrder(70);
        String[] timeZones = TimeZone.getAvailableIDs();
        tzAnchor.setEntries(timeZones);
        tzAnchor.setEntryValues(timeZones);
    }
}
