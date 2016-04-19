package com.galdiuz.manga.other;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class FloatListPreference extends ListPreference {
    public FloatListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatListPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean persistString(String value) {
        return value != null && persistFloat(Float.valueOf(value));
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if(getSharedPreferences().contains(getKey())) {
            float floatValue = getPersistedFloat(0);
            return String.valueOf(floatValue);
        }
        else {
            return defaultReturnValue;
        }
    }
}
