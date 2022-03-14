/*
 * Copyright (C) 2022 Project Kaleidoscope
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

import android.annotation.NonNull;
import android.content.Context;
import android.net.MacAddress;
import android.net.wifi.SoftApCapability;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.util.FeatureFlagUtils;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.wifi.tether.WifiTetherBasePreferenceController;

import java.util.List;

public class WifiTetherClientManagerPreferenceController extends WifiTetherBasePreferenceController
        implements WifiManager.SoftApCallback {

    public static final String DEDUP_POSTFIX = "_2";
    public static final String PREF_KEY = "wifi_tether_client_manager";

    private boolean mSupportForceDisconnect;

    public WifiTetherClientManagerPreferenceController(Context context,
            WifiTetherBasePreferenceController.OnTetherConfigUpdateListener listener) {
        super(context, listener);

        mWifiManager.registerSoftApCallback(context.getMainExecutor(), this);
    }

    @Override
    public String getPreferenceKey() {
        return FeatureFlagUtils.isEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE)
                ? PREF_KEY + DEDUP_POSTFIX : PREF_KEY;
    }

    @Override
    public void onCapabilityChanged(@NonNull SoftApCapability softApCapability) {
        mSupportForceDisconnect =
                softApCapability.areFeaturesSupported(
                    SoftApCapability.SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT);
        mWifiManager.unregisterSoftApCallback(this);
        updateDisplay();
    }

    @Override
    public void updateDisplay() {
        if (mPreference != null) {
            if (mSupportForceDisconnect) {
                mPreference.setSummary(R.string.wifi_hotspot_client_manager_summary);
            } else {
                mPreference.setSummary(R.string.wifi_hotspot_client_manager_list_only_summary);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    public void updateConfig(SoftApConfiguration.Builder builder) {
        if (builder == null || !mSupportForceDisconnect) return;
        final SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        final int maxNumberOfClients = softApConfiguration.getMaxNumberOfClients();
        final List<MacAddress> blockedClientList = softApConfiguration.getBlockedClientList();
        builder.setMaxNumberOfClients(maxNumberOfClients)
                .setBlockedClientList(blockedClientList);
    }
}
