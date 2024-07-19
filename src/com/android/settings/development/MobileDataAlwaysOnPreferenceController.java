/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import static android.content.pm.PackageManager.NameNotFoundException;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class MobileDataAlwaysOnPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String TAG = "MobileDataAlwaysOnPreferenceController";
    private static final String MOBILE_DATA_ALWAYS_ON = "mobile_data_always_on";
    private static final String SETTINGS_PROVIDER_PACKAGE = "com.android.providers.settings";

    @VisibleForTesting
    static final int SETTING_VALUE_ON = 1;
    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;

    public MobileDataAlwaysOnPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return MOBILE_DATA_ALWAYS_ON;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.MOBILE_DATA_ALWAYS_ON,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int mobileDataAlwaysOnMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.MOBILE_DATA_ALWAYS_ON, SETTING_VALUE_ON);

        ((TwoStatePreference) mPreference).setChecked(mobileDataAlwaysOnMode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();

        boolean enabledByDefault = true;
        try {
            final Resources res = mContext.getPackageManager().getResourcesForApplication(
                    SETTINGS_PROVIDER_PACKAGE);
            final int resId = res.getIdentifier("def_mobile_data_always_on", "bool",
                    SETTINGS_PROVIDER_PACKAGE);
            if (resId != 0) {
                enabledByDefault = res.getBoolean(resId);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Failed to get resources from settingsprovider", e);
        }
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.MOBILE_DATA_ALWAYS_ON,
                enabledByDefault ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        ((TwoStatePreference) mPreference).setChecked(true);
    }
}
