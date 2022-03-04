/*
 * Copyright (C) 2017 The Android Open Source Project
 *           (C) 2022 Project Kaleidoscope
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

package com.android.settings.wifi.tether;

import static com.android.settings.AllInOneTetherSettings.DEDUP_POSTFIX;

import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.SoftApConfiguration;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;

import java.util.ArrayList;
import java.util.Arrays;

public class WifiTetherApBandPreferenceController extends WifiTetherBasePreferenceController {

    private static final String TAG = "WifiTetherApBandPref";
    private static final String PREF_KEY = "wifi_tether_network_ap_band";

    private static final int[][] sBands = {
        {SoftApConfiguration.BAND_2GHZ},
        {SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_5GHZ},
        {SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_6GHZ},
        {SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_60GHZ},
        {SoftApConfiguration.BAND_2GHZ,
            SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_5GHZ},
        {SoftApConfiguration.BAND_2GHZ,
            SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_6GHZ}
    };

    private String[] mBandEntries;
    private String[] mBandSummaries;
    private int mBandIndex;

    public WifiTetherApBandPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        updatePreferenceEntries();
    }

    @Override
    public void updateDisplay() {
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config == null) {
            mBandIndex = 0;
            Log.d(TAG, "Updating band index to BAND_2GHZ because no config");
        } else {
            mBandIndex = getIndexOfBands(getBands(config));
            Log.d(TAG, "Updating band index to " + mBandIndex);
        }
        ListPreference preference =
                (ListPreference) mPreference;
        preference.setEntries(mBandSummaries);
        preference.setEntryValues(mBandEntries);

        preference.setValue(Integer.toString(mBandIndex));
        preference.setSummary(getConfigSummary());
    }

    String getConfigSummary() {
        final String index = Integer.toString(mBandIndex);
        for (int i = 0; i < mBandEntries.length; i++) {
            if (TextUtils.equals(index, mBandEntries[i])) {
                return mBandSummaries[i];
            }
        }
        return "";
    }

    @Override
    public String getPreferenceKey() {
        return FeatureFlagUtils.isEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE)
                ? PREF_KEY + DEDUP_POSTFIX : PREF_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mBandIndex = Integer.parseInt((String) newValue);
        Log.d(TAG, "Band preference changed, updating band index to " + mBandIndex);
        preference.setSummary(getConfigSummary());
        mListener.onTetherConfigUpdated(this);
        return true;
    }

    @VisibleForTesting
    void updatePreferenceEntries() {
        mBandEntries = getSupportedApBandEntries();
        mBandSummaries = getSupportedApBandSummaries();
    }

    private String[] getSupportedApBandEntries() {
        ArrayList<String> bands = new ArrayList<>();
        if (mWifiManager.is24GHzBandSupported()) {
            bands.add("0");
        }
        if (mWifiManager.is5GHzBandSupported()) {
            bands.add("1");
        }
        if (mWifiManager.is6GHzBandSupported()) {
            bands.add("2");
        }
        if (mWifiManager.is60GHzBandSupported()) {
            bands.add("3");
        }
        // device supports dual-ap
        if (mWifiManager.isBridgedApConcurrencySupported()) {
            if (mWifiManager.is24GHzBandSupported() && mWifiManager.is5GHzBandSupported()) {
                bands.add("4");
            }
            if (mWifiManager.is24GHzBandSupported() && mWifiManager.is6GHzBandSupported()) {
                bands.add("5");
            }
        }
        return bands.toArray(new String[bands.size()]);
    }

    private String[] getSupportedApBandSummaries() {
        ArrayList<String> bands = new ArrayList<>();
        Resources res = mContext.getResources();
        if (mWifiManager.is24GHzBandSupported()) {
            bands.add(res.getString(R.string.wifi_band_24ghz));
        }
        if (mWifiManager.is5GHzBandSupported()) {
            bands.add(res.getString(R.string.wifi_band_5ghz));
        }
        if (mWifiManager.is6GHzBandSupported()) {
            bands.add(res.getString(R.string.wifi_band_6ghz));
        }
        if (mWifiManager.is60GHzBandSupported()) {
            bands.add(res.getString(R.string.wifi_band_60ghz));
        }
        // device supports dual-ap
        if (mWifiManager.isBridgedApConcurrencySupported()) {
            if (mWifiManager.is24GHzBandSupported() && mWifiManager.is5GHzBandSupported()) {
                bands.add(res.getString(R.string.wifi_band_24ghz_and_5ghz));
            }
            if (mWifiManager.is24GHzBandSupported() && mWifiManager.is6GHzBandSupported()) {
                bands.add(res.getString(R.string.wifi_band_24ghz_and_6ghz));
            }
        }
        return bands.toArray(new String[bands.size()]);
    }

    private int getIndexOfBands(int[] bands) {
        for (int i = 0; i < sBands.length; i++) {
            if (Arrays.equals(bands, sBands[i])) {
                return i;
            }
        }
        return -1;
    }

    private int[] getBands(SoftApConfiguration config) {
        SparseIntArray channels = config.getChannels();
        int[] bands = new int[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            bands[i] = channels.keyAt(i);
        }
        return bands;
    }

    /**
     * Setup the bands setting to the SoftAp Configuration
     *
     * @param builder The builder to build the SoftApConfiguration.
     */
    public void setupBands(SoftApConfiguration.Builder builder) {
        if (builder == null) {
            return;
        }
        if (mBandIndex != -1) {
            builder.setBands(sBands[mBandIndex]);
        }
    }
}
