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

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.util.FeatureFlagUtils;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.wifi.tether.WifiTetherBasePreferenceController;

/**
 * This controller helps to manage the state of hide SSID switch preference.
 */
public class WifiTetherHiddenSsidPreferenceController extends
        WifiTetherBasePreferenceController {

    public static final String DEDUP_POSTFIX = "_2";
    public static final String PREF_KEY = "wifi_tether_hidden_ssid";

    private boolean mHiddenSsid;

    public WifiTetherHiddenSsidPreferenceController(Context context,
            WifiTetherBasePreferenceController.OnTetherConfigUpdateListener listener) {
        super(context, listener);

        if (mWifiManager != null) {
            final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
            if (config != null) {
                mHiddenSsid = config.isHiddenSsid();
            }
        }
    }

    @Override
    public String getPreferenceKey() {
        return FeatureFlagUtils.isEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE)
                ? PREF_KEY + DEDUP_POSTFIX : PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        if (mPreference == null) {
            return;
        }
        ((SwitchPreferenceCompat) mPreference).setChecked(mHiddenSsid);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mHiddenSsid = (Boolean) newValue;
        if (mListener != null) {
            mListener.onTetherConfigUpdated(this);
        }
        return true;
    }

    public boolean isHiddenSsidEnabled() {
        return mHiddenSsid;
    }
}
