package com.galdiuz.manga;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(App.PREFSNAME);

        addPreferencesFromResource(R.xml.preferences);
    }
}
