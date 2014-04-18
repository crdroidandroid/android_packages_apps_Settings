/*
 * Copyright (C) 2012 TeloKang project
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

package com.android.settings.crdroid;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.IWindowManager;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.Arrays;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class TeloRadioSettings extends SettingsPreferenceFragment implements CompoundButton.OnCheckedChangeListener,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "TeloRadioSettings";

    private static final String KEY_TELO_RADIO_ENABLED = "telo_radio_enable";
    private static final String KEY_TELO_RADIO_LTE = "telo_radio_lte";
    private static final String KEY_TELO_RADIO_2G_WIFI = "telo_radio_2g_wifi";
    private static final String KEY_TELO_RADIO_2G_SCREENOFF = "telo_radio_2g_screenoff_enable";
    private static final String KEY_TELO_RADIO_2G_SCREENOFF_TIME = "telo_radio_2g_screenoff_timeout";
    private static final String KEY_TELO_RADIO_GO3G_UNLOCK = "telo_radio_go_3g_unlock";

    private Switch mEnabledSwitch;
    private boolean mEnabledPref;

    private CheckBoxPreference mLTEPref;
    private CheckBoxPreference m2GWifiPref;
    private CheckBoxPreference m2GScreenOffPref;
    private ListPreference m2GScreenOffTime;
    private CheckBoxPreference mGo3GUnlock;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        mEnabledSwitch = new Switch(activity);

        final int padding = activity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        mEnabledSwitch.setPaddingRelative(0, 0, padding, 0);
        mEnabledSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        final Activity activity = getActivity();
        final Context context = getActivity();
        activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(mEnabledSwitch, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL | Gravity.END));
        mEnabledSwitch.setChecked(Settings.System.getInt(context.getContentResolver(),
                Settings.System.TELO_RADIO_ENABLED, 0) == 1);
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.telo_radio_settings);

        final ContentResolver resolver = getContentResolver();

        mLTEPref = (CheckBoxPreference) findPreference(KEY_TELO_RADIO_LTE);
        mLTEPref.setChecked((Settings.System.getInt(resolver, Settings.System.TELO_RADIO_LTE, 0) == 1));

        m2GWifiPref = (CheckBoxPreference) findPreference(KEY_TELO_RADIO_2G_WIFI);
        m2GWifiPref.setChecked((Settings.System.getInt(resolver, Settings.System.TELO_RADIO_2G_WIFI, 0) == 1));

        m2GScreenOffPref = (CheckBoxPreference) findPreference(KEY_TELO_RADIO_2G_SCREENOFF);
        m2GScreenOffPref.setChecked((Settings.System.getInt(resolver, Settings.System.TELO_RADIO_2G_SCREENOFF, 0) == 1));

        m2GScreenOffTime = (ListPreference) findPreference(KEY_TELO_RADIO_2G_SCREENOFF_TIME);
        m2GScreenOffTime.setOnPreferenceChangeListener(this);
        long value2GScreenOffTime = Settings.System.getLong(resolver, Settings.System.TELO_RADIO_2G_SCREENOFF_TIME, 600000L);
        m2GScreenOffTime.setValue(String.valueOf(value2GScreenOffTime));
        update2GScreenOffTimeSummary(value2GScreenOffTime);

        mGo3GUnlock = (CheckBoxPreference) findPreference(KEY_TELO_RADIO_GO3G_UNLOCK);
        mGo3GUnlock.setChecked((Settings.System.getInt(resolver, Settings.System.TELO_RADIO_GO3G_UNLOCK, 0) == 1));
        updateDependency();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateDependency() {
        ContentResolver resolver = getContentResolver();
        mEnabledPref = Settings.System.getInt(resolver, Settings.System.TELO_RADIO_ENABLED, 0) == 1;

        mLTEPref.setEnabled(mEnabledPref);
        m2GWifiPref.setEnabled(mEnabledPref);
        m2GScreenOffPref.setEnabled(mEnabledPref);
        m2GScreenOffTime.setEnabled(mEnabledPref);
        mGo3GUnlock.setEnabled(mEnabledPref);
    }


    public boolean onPreferenceChange(Preference preference, Object newValue) {
                ContentResolver resolver = getContentResolver();

        if (preference == m2GScreenOffTime) {
            long value = Integer.valueOf((String) newValue);
            update2GScreenOffTimeSummary(value);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getContentResolver();
        boolean value;
        if (preference == m2GWifiPref) {
            value = m2GWifiPref.isChecked();
            Settings.System.putInt(resolver, Settings.System.TELO_RADIO_2G_WIFI, value ? 1 : 0);
        } else if (preference == mLTEPref) {
            value = mLTEPref.isChecked();
            Settings.System.putInt(resolver, Settings.System.TELO_RADIO_LTE, value ? 1 : 0);
            return true;
        } else if (preference == m2GScreenOffPref) {
            value = m2GScreenOffPref.isChecked();
            Settings.System.putInt(resolver, Settings.System.TELO_RADIO_2G_SCREENOFF, value ? 1 : 0);
            return true;
        } else if (preference == mGo3GUnlock) {
            value = mGo3GUnlock.isChecked();
            Settings.System.putInt(resolver, Settings.System.TELO_RADIO_GO3G_UNLOCK, value ? 1 : 0);
            return true;
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mEnabledSwitch) {

            boolean value = ((Boolean)isChecked).booleanValue();
            final Context context = getActivity();
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.TELO_RADIO_ENABLED,
                    value ? 1 : 0);

            Intent serviceIntent = new Intent();
            serviceIntent.setAction("com.android.phone.TeloRadioService");
            if (isChecked)
                getActivity().getApplicationContext().startService(serviceIntent);
            else
                getActivity().getApplicationContext().stopService(serviceIntent);
            
        }
        updateDependency();
    }

    private void update2GScreenOffTimeSummary(long value) {
        try {
            m2GScreenOffTime.setSummary(m2GScreenOffTime.getEntries()[m2GScreenOffTime.findIndexOfValue("" + value)]);
            Settings.System.putLong(getContentResolver(), Settings.System.TELO_RADIO_2G_SCREENOFF_TIME, value);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }
}
