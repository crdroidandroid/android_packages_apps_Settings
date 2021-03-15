/*
 * Copyright (C) 2020 The LineageOS Project
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

package com.android.settings.display;

import static android.provider.Settings.System.PEAK_REFRESH_RATE;

import android.content.Context;
import android.provider.Settings;
import android.view.Display;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PeakRefreshRatePreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_PEAK_REFRESH_RATE = "peak_refresh_rate";

    private ListPreference mListPreference;

    public PeakRefreshRatePreferenceController(Context context) {
        super(context, KEY_PEAK_REFRESH_RATE);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_refresh_rate_controls) &&
                mListPreference != null && mListPreference.getEntries().length > 1
                        ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PEAK_REFRESH_RATE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mListPreference = screen.findPreference(getPreferenceKey());

        List<String> entries = new ArrayList<>(), values = new ArrayList<>();
        Display.Mode mode = mContext.getDisplay().getMode();
        Display.Mode[] modes = mContext.getDisplay().getSupportedModes();
        for (Display.Mode m : modes) {
            if (m.getPhysicalWidth() == mode.getPhysicalWidth() &&
                    m.getPhysicalHeight() == mode.getPhysicalHeight()) {
                entries.add(String.format("%.02fHz", m.getRefreshRate())
                        .replaceAll("[\\.,]00", ""));
                values.add(String.format(Locale.US, "%.02f", m.getRefreshRate()));
            }
        }
        mListPreference.setEntries(entries.toArray(new String[entries.size()]));
        mListPreference.setEntryValues(values.toArray(new String[values.size()]));

        super.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference) {
        final float defaultRefreshRate = (float) mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_defaultPeakRefreshRate);
        final float currentValue = Settings.System.getFloat(mContext.getContentResolver(),
                PEAK_REFRESH_RATE, defaultRefreshRate);
        int index = mListPreference.findIndexOfValue(
                String.format(Locale.US, "%.02f", currentValue));
        if (index < 0) index = 0;
        mListPreference.setValueIndex(index);
        mListPreference.setSummary(mListPreference.getEntries()[index]);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.System.putFloat(mContext.getContentResolver(), PEAK_REFRESH_RATE,
                Float.valueOf((String) newValue));
        updateState(preference);
        return true;
    }

}
