package com.google.android.settings.localepicker;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;

import com.android.settings.R;

public class DividerPreference extends Preference {
    public DividerPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setLayoutResource(R.layout.divider_preference);
    }
}
