/*
 * Copyright (C) 2019 RevengeOS
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

package com.android.settings.fuelgauge.smartcharging;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.crdroid.settings.preferences.CustomSeekBarPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen for Smart charging
 */
public class SmartChargingSettings extends DashboardFragment implements OnPreferenceChangeListener {

    private static final String TAG = "SmartChargingSettings";
    private static final String KEY_SMART_CHARGING_LEVEL = "smart_charging_level";
    private static final String KEY_SMART_CHARGING_RESUME_LEVEL = "smart_charging_resume_level";

    private CustomSeekBarPreference mSmartChargingLevel;
    private CustomSeekBarPreference mSmartChargingResumeLevel;

    private int mSmartChargingLevelDefaultConfig;
    private int mSmartChargingResumeLevelDefaultConfig;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mSmartChargingLevelDefaultConfig = getResources().getInteger(
                com.android.internal.R.integer.config_smartChargingBatteryLevel);

        mSmartChargingResumeLevelDefaultConfig = getResources().getInteger(
                com.android.internal.R.integer.config_smartChargingBatteryResumeLevel);
        mSmartChargingLevel = (CustomSeekBarPreference) findPreference(KEY_SMART_CHARGING_LEVEL);
        int currentLevel = Settings.System.getInt(getContentResolver(),
            Settings.System.SMART_CHARGING_LEVEL, mSmartChargingLevelDefaultConfig);
        mSmartChargingLevel.setValue(currentLevel);
        mSmartChargingLevel.setOnPreferenceChangeListener(this);
        mSmartChargingResumeLevel = (CustomSeekBarPreference) findPreference(KEY_SMART_CHARGING_RESUME_LEVEL);
        int currentResumeLevel = Settings.System.getInt(getContentResolver(),
            Settings.System.SMART_CHARGING_RESUME_LEVEL, mSmartChargingResumeLevelDefaultConfig);
        if (currentResumeLevel >= currentLevel) currentResumeLevel = currentLevel -1;
        mSmartChargingResumeLevel.setValue(currentResumeLevel);
        mSmartChargingResumeLevel.setOnPreferenceChangeListener(this);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.smart_charging;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CRDROID_SETTINGS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mSmartChargingLevel) {
            int smartChargingLevel = (Integer) objValue;
            int mChargingResumeLevel = Settings.System.getInt(getContentResolver(),
                     Settings.System.SMART_CHARGING_RESUME_LEVEL, mSmartChargingResumeLevelDefaultConfig);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SMART_CHARGING_LEVEL, smartChargingLevel);
            if (smartChargingLevel <= mChargingResumeLevel) {
                mSmartChargingResumeLevel.setValue(smartChargingLevel - 1);
                Settings.System.putInt(getContentResolver(),
                    Settings.System.SMART_CHARGING_RESUME_LEVEL, smartChargingLevel - 1);
            }
            return true;
        } else if (preference == mSmartChargingResumeLevel) {
            int smartChargingResumeLevel = (Integer) objValue;
            int mChargingLevel = Settings.System.getInt(getContentResolver(),
                     Settings.System.SMART_CHARGING_LEVEL, mSmartChargingLevelDefaultConfig);
               Settings.System.putInt(getContentResolver(),
                    Settings.System.SMART_CHARGING_RESUME_LEVEL, smartChargingResumeLevel);
            return true;
        } else {
            return false;
        }
    }
}
