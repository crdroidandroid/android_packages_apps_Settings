/*
 * Copyright (C) 2020 Yet Another AOSP Project
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

package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import com.crdroid.settings.preferences.CustomSeekBarPreference;

/**
 * This class allows choosing a vibration pattern while ringing
 */
public class VibrationPatternPreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_VIB_PATTERN = "vibration_pattern";
    private static final String KEY_CUSTOM_VIB_CATEGORY = "custom_vibration_pattern";
    private static final String KEY_CUSTOM_VIB1 = "custom_vibration_pattern1";
    private static final String KEY_CUSTOM_VIB2 = "custom_vibration_pattern2";
    private static final String KEY_CUSTOM_VIB3 = "custom_vibration_pattern3";

    private ListPreference mVibPattern;
    private PreferenceCategory mCustomVibCategory;
    private CustomSeekBarPreference mCustomVib1;
    private CustomSeekBarPreference mCustomVib2;
    private CustomSeekBarPreference mCustomVib3;

    public VibrationPatternPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VIB_PATTERN;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mVibPattern = (ListPreference) screen.findPreference(KEY_VIB_PATTERN);
        int vibPattern = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.RINGTONE_VIBRATION_PATTERN, 0);
        mVibPattern.setValueIndex(vibPattern);
        mVibPattern.setSummary(mVibPattern.getEntries()[vibPattern]);
        mVibPattern.setOnPreferenceChangeListener(this);

        mCustomVibCategory = (PreferenceCategory) screen.findPreference(KEY_CUSTOM_VIB_CATEGORY);
        mCustomVib1 = (CustomSeekBarPreference) screen.findPreference(KEY_CUSTOM_VIB1);
        mCustomVib2 = (CustomSeekBarPreference) screen.findPreference(KEY_CUSTOM_VIB2);
        mCustomVib3 = (CustomSeekBarPreference) screen.findPreference(KEY_CUSTOM_VIB3);
        updateCustomVibVisibility(vibPattern == 5);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVibPattern) {
            int vibPattern = Integer.valueOf((String) newValue);
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.RINGTONE_VIBRATION_PATTERN, vibPattern);
            mVibPattern.setSummary(mVibPattern.getEntries()[vibPattern]);
            updateCustomVibVisibility(vibPattern == 5);
            return true;
        } else if (preference == mCustomVib1) {
            updateCustomVib(0, (Integer) newValue);
            return true;
        } else if (preference == mCustomVib2) {
            updateCustomVib(1, (Integer) newValue);
            return true;
        } else if (preference == mCustomVib3) {
            updateCustomVib(2, (Integer) newValue);
            return true;
        }
        return false;
    }

    private void updateCustomVibVisibility(boolean show) {
        mCustomVibCategory.setVisible(show);
        mCustomVib1.setVisible(show);
        mCustomVib2.setVisible(show);
        mCustomVib3.setVisible(show);
        if (show) updateCustomVibPreferences();
    }

    private void updateCustomVibPreferences() {
        String value = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN);
        if (value != null) {
            String[] customPattern = value.split(",", 3);
            mCustomVib1.setValue(Integer.parseInt(customPattern[0]));
            mCustomVib2.setValue(Integer.parseInt(customPattern[1]));
            mCustomVib3.setValue(Integer.parseInt(customPattern[2]));
        } else { // set default
            mCustomVib1.setValue(0);
            mCustomVib2.setValue(800);
            mCustomVib3.setValue(800);
            Settings.System.putString(mContext.getContentResolver(),
                    Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN, "0,800,800");
        }
        mCustomVib1.setOnPreferenceChangeListener(this);
        mCustomVib2.setOnPreferenceChangeListener(this);
        mCustomVib3.setOnPreferenceChangeListener(this);
    }

    private void updateCustomVib(int index, int value) {
        String[] customPattern = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN).split(",", 3);
        customPattern[index] = String.valueOf(value);
        Settings.System.putString(mContext.getContentResolver(),
                Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN, String.join(
                ",", customPattern[0], customPattern[1], customPattern[2]));
    }
}
