/*
 * Copyright (C) 2014 crDroid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.crdroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import org.cyanogenmod.internal.logging.CMMetricsLogger;

public class RecentsPanelSettings extends SettingsPreferenceFragment implements
             Preference.OnPreferenceChangeListener {

    private static final String TAG = "RecentPanelSettings";

    private static final String CATEGORY_STOCK_RECENTS = "recent_panel";
    private static final String CATEGORY_OMNI_RECENTS = "omni_recents";

    private static final String SHOW_CLEAR_ALL_RECENTS = "show_clear_all_recents";
    private static final String RECENTS_CLEAR_ALL_LOCATION = "recents_clear_all_location";
    private static final String RECENTS_USE_OMNISWITCH = "recents_use_omniswitch";
    private static final String OMNISWITCH_START_SETTINGS = "omniswitch_start_settings";

    public static final String OMNISWITCH_PACKAGE_NAME = "org.omnirom.omniswitch";
    public static Intent INTENT_OMNISWITCH_SETTINGS = new Intent(Intent.ACTION_MAIN)
           .setClassName(OMNISWITCH_PACKAGE_NAME, OMNISWITCH_PACKAGE_NAME + ".SettingsActivity");

    private ListPreference mRecentsClearAllLocation;
    private Preference mOmniSwitchSettings;
    private PreferenceCategory mOmniSwitch;
    private SwitchPreference mRecentsClearAll;
    private SwitchPreference mRecentsUseOmniSwitch;

    private boolean mOmniSwitchInitCalled;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.recents_panel_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mRecentsUseOmniSwitch = (SwitchPreference)
                prefSet.findPreference(RECENTS_USE_OMNISWITCH);

        try {
            mRecentsUseOmniSwitch.setChecked(Settings.System.getInt(resolver,
                                  Settings.System.RECENTS_USE_OMNISWITCH) == 1);
            mOmniSwitchInitCalled = true;
        } catch(SettingNotFoundException e){
            // if the settings value is unset
        }
        mRecentsUseOmniSwitch.setOnPreferenceChangeListener(this);

        mOmniSwitchSettings = (Preference)
                prefSet.findPreference(OMNISWITCH_START_SETTINGS);
        mOmniSwitchSettings.setEnabled(mRecentsUseOmniSwitch.isChecked());

        mOmniSwitch = (PreferenceCategory) findPreference(CATEGORY_OMNI_RECENTS);
        if (!Utils.isPackageInstalled(getActivity(), OMNISWITCH_PACKAGE_NAME)) {
            prefSet.removePreference(mOmniSwitch);
        }

        mRecentsClearAll = (SwitchPreference) prefSet.findPreference(SHOW_CLEAR_ALL_RECENTS);
        mRecentsClearAll.setChecked(Settings.System.getIntForUser(resolver,
            Settings.System.SHOW_CLEAR_ALL_RECENTS, 1, UserHandle.USER_CURRENT) == 1);
        mRecentsClearAll.setOnPreferenceChangeListener(this);

        mRecentsClearAllLocation = (ListPreference) prefSet.findPreference(RECENTS_CLEAR_ALL_LOCATION);
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.RECENTS_CLEAR_ALL_LOCATION, 0, UserHandle.USER_CURRENT);
        mRecentsClearAllLocation.setValue(String.valueOf(location));
        mRecentsClearAllLocation.setOnPreferenceChangeListener(this);
        updateRecentsLocation(location);
        updateDisableStockRecents();
    }

    @Override
    protected int getMetricsCategory() {
        return CMMetricsLogger.APPLICATION;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mOmniSwitchSettings){
            startActivity(INTENT_OMNISWITCH_SETTINGS);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mRecentsClearAll) {
            boolean show = (Boolean) objValue;
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.SHOW_CLEAR_ALL_RECENTS, show ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mRecentsClearAllLocation) {
            int location = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.RECENTS_CLEAR_ALL_LOCATION, location, UserHandle.USER_CURRENT);
            updateRecentsLocation(location);
            return true;
        } else if (preference == mRecentsUseOmniSwitch) {
            boolean value = (Boolean) objValue;

            if (value && !mOmniSwitchInitCalled){
                openOmniSwitchFirstTimeWarning();
                mOmniSwitchInitCalled = true;
            }

            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.RECENTS_USE_OMNISWITCH, value ? 1 : 0, UserHandle.USER_CURRENT);
            mOmniSwitchSettings.setEnabled(value);
            updateDisableStockRecents();
        } else {
            return false;
        }
        return true;
    }

    private void updateDisableStockRecents() {
        boolean enabled = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.RECENTS_USE_OMNISWITCH, 0) == 1;

        updateRecentsOptions(!enabled);
    }

    private void updateRecentsOptions(boolean enabled) {
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final PreferenceCategory stockRecentsCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_STOCK_RECENTS);

        if (stockRecentsCategory != null) {
            stockRecentsCategory.setEnabled(enabled);
        }
    }

    private void openOmniSwitchFirstTimeWarning() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.omniswitch_first_time_title))
                .setMessage(getResources().getString(R.string.omniswitch_first_time_message))
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                }).show();
    }

    private void updateRecentsLocation(int value) {
        ContentResolver resolver = getContentResolver();
        Resources res = getResources();
        int summary = -1;

        Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, value);

        if (value == 0) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 0);
            summary = R.string.recents_clear_all_location_bottom_right;
        } else if (value == 1) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 1);
            summary = R.string.recents_clear_all_location_bottom_left;
        }
        if (mRecentsClearAllLocation != null && summary != -1) {
            mRecentsClearAllLocation.setSummary(res.getString(summary));
        }
    }
}
