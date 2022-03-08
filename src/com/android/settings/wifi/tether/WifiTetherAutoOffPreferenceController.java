/*
 * Copyright (C) 2022 Project Kaleidoscope
 * Copyright (C) 2022 FlamingoOS Project
 * Copyright (C) 2024 crDroid Android Project
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

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.repository.WifiHotspotRepository;

public class WifiTetherAutoOffPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private final WifiManager mWifiManager;
    @VisibleForTesting
    boolean mNeedShutdownSecondarySap;

    private ListPreference mPreference;

    public WifiTetherAutoOffPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        WifiHotspotRepository wifiHotspotRepository = FeatureFactory.getFeatureFactory()
                .getWifiFeatureProvider().getWifiHotspotRepository();
        if (wifiHotspotRepository.isSpeedFeatureAvailable() && wifiHotspotRepository.isDualBand()) {
            mNeedShutdownSecondarySap = true;
        }
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateDisplay();
    }

    private long getAutoOffTimeout() {
        final SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        final boolean settingsOn = softApConfiguration.isAutoShutdownEnabled();
        return settingsOn ? softApConfiguration.getShutdownTimeoutMillis() : 0;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final long timeout;
        try {
            timeout = Long.parseLong((String) newValue);
        } catch (NumberFormatException e) {
            return false;
        }
        final SoftApConfiguration.Builder configBuilder =
                new SoftApConfiguration.Builder(mWifiManager.getSoftApConfiguration());
        setShutdownTimeout(configBuilder, timeout);
        if (mNeedShutdownSecondarySap) {
            configBuilder.setBridgedModeOpportunisticShutdownEnabled(timeout > 0);
        }
        return mWifiManager.setSoftApConfiguration(configBuilder.build());
    }

    public void updateConfig(SoftApConfiguration.Builder builder) {
        if (builder == null) return;
        final long timeout = getAutoOffTimeout();
        setShutdownTimeout(builder, timeout);
    }

    private void setShutdownTimeout(SoftApConfiguration.Builder builder, long timeout) {
        builder.setAutoShutdownEnabled(timeout > 0);
        if (timeout > 0) {
            builder.setShutdownTimeoutMillis(timeout);
        }
        updateDisplay(timeout);
    }

    public void updateDisplay() {
        updateDisplay(getAutoOffTimeout());
    }

    private void updateDisplay(long timeout) {
        if (mPreference != null) {
            mPreference.setValue(String.valueOf(timeout));
            if (timeout > 0) {
                int index = mPreference.findIndexOfValue(String.valueOf(timeout));
                mPreference.setSummary(mPreference.getEntries()[index]);
            } else {
                mPreference.setSummary(mContext.getResources().getString(
                    R.string.wifi_hotspot_auto_off_summary));
            }
        }
    }
}
