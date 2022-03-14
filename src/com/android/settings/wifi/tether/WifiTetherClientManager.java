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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.MacAddress;
import android.net.TetheringManager;
import android.net.TetheredClient;
import android.net.wifi.SoftApCapability;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.wifi.tether.preference.WifiTetherClientLimitPreference;
import com.android.settingslib.widget.FooterPreference;

public class WifiTetherClientManager extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener,
        WifiManager.SoftApCallback, TetheringManager.TetheringEventCallback {

    private static final String TAG = "WifiTetherClientManager";

    private static final String PREF_KEY_CLIENT_LIMIT = "client_limit";
    private static final String PREF_KEY_BLOCKED_CLIENT_LIST = "blocked_client_list";
    private static final String PREF_KEY_CONNECTED_CLIENT_LIST = "connected_client_list";
    private static final String PREF_KEY_FOOTER = "footer";

    private WifiManager mWifiManager;
    private TetheringManager mTetheringManager;

    private WifiTetherClientLimitPreference mClientLimitPref;
    private PreferenceCategory mConnectedClientsPref;
    private PreferenceCategory mBlockedClientsPref;
    private FooterPreference mFooterPref;

    private boolean mSupportForceDisconnect;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWifiManager = getSystemService(WifiManager.class);
        mTetheringManager = getSystemService(TetheringManager.class);

        mWifiManager.registerSoftApCallback(getActivity().getMainExecutor(), this);

        addPreferencesFromResource(R.xml.hotspot_client_manager);

        getActivity().setTitle(R.string.wifi_hotspot_client_manager_title);

        mClientLimitPref = findPreference(PREF_KEY_CLIENT_LIMIT);
        mConnectedClientsPref = findPreference(PREF_KEY_CONNECTED_CLIENT_LIST);
        mBlockedClientsPref = findPreference(PREF_KEY_BLOCKED_CLIENT_LIST);
        mFooterPref = findPreference(PREF_KEY_FOOTER);

        mClientLimitPref.setOnPreferenceChangeListener(this);

        updateBlockedClients();
        updatePreferenceVisible();
    }

    @Override
    public void onCapabilityChanged(@NonNull SoftApCapability softApCapability) {
        mSupportForceDisconnect =
                softApCapability.areFeaturesSupported(
                    SoftApCapability.SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT);
        mWifiManager.unregisterSoftApCallback(this);

        if (mSupportForceDisconnect) {
            mClientLimitPref.setMin(1);
            mClientLimitPref.setMax(softApCapability.getMaxSupportedClients());
            final SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
            final int maxNumberOfClients = softApConfiguration.getMaxNumberOfClients();
            mClientLimitPref.setValue(maxNumberOfClients, false);
        }
        updatePreferenceVisible();
    }

    private void updatePreferenceVisible() {
        if (mBlockedClientsPref == null || mClientLimitPref == null ||
                mConnectedClientsPref == null || mFooterPref == null) return;
        boolean hasConnectedClient = mConnectedClientsPref.getPreferenceCount() > 0;
        boolean hasBlockedClient = mBlockedClientsPref.getPreferenceCount() > 0;
        mClientLimitPref.setVisible(mSupportForceDisconnect);
        mBlockedClientsPref.setVisible(mSupportForceDisconnect && hasBlockedClient);
        mConnectedClientsPref.setVisible(hasConnectedClient);
        mFooterPref.setVisible(!hasBlockedClient && !hasConnectedClient);
    }

    private void updateBlockedClients() {
        final SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        final List<MacAddress> blockedClientList = softApConfiguration.getBlockedClientList();
        mBlockedClientsPref.removeAll();
        for (MacAddress mac : blockedClientList) {
            BlockedClientPreference preference = new BlockedClientPreference(getActivity(), mac);
            preference.setOnPreferenceClickListener(this);
            mBlockedClientsPref.addPreference(preference);
        }
        updatePreferenceVisible();
    }

    @Override
    public void onClientsChanged(Collection<TetheredClient> clients) {
        mConnectedClientsPref.removeAll();
        for (TetheredClient client : clients) {
            if (client.getTetheringType() != TetheringManager.TETHERING_WIFI) {
                continue;
            }
            ConnectedClientPreference preference =
                new ConnectedClientPreference(getActivity(), client);
            preference.setOnPreferenceClickListener(this);
            mConnectedClientsPref.addPreference(preference);
        }
        updatePreferenceVisible();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mSupportForceDisconnect) {
            if (preference instanceof ConnectedClientPreference) {
                showBlockClientDialog(
                    ((ConnectedClientPreference)preference).getMacAddress(),
                    preference.getTitle());
                return true;
            } else if (preference instanceof BlockedClientPreference) {
                showUnblockClientDialog(((BlockedClientPreference)preference).getMacAddress());
                return true;
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mClientLimitPref) {
            int value = (int) newValue;
            SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
            SoftApConfiguration newSoftApConfiguration =
                    new SoftApConfiguration.Builder(softApConfiguration)
                            .setMaxNumberOfClients(value)
                            .build();
            return mWifiManager.setSoftApConfiguration(newSoftApConfiguration);
        }
        return false;
    }

    private void blockClient(MacAddress mac, boolean isBlock) {
        final SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        final List<MacAddress> blockedClientList = softApConfiguration.getBlockedClientList();
        if (isBlock) {
            if (blockedClientList.contains(mac)) return;
            blockedClientList.add(mac);
        } else {
            if (!blockedClientList.contains(mac)) return;
            blockedClientList.remove(mac);
        }
        SoftApConfiguration newSoftApConfiguration =
                new SoftApConfiguration.Builder(softApConfiguration)
                        .setBlockedClientList(blockedClientList)
                        .build();
        mWifiManager.setSoftApConfiguration(newSoftApConfiguration);
        updateBlockedClients();
    }

    private void showBlockClientDialog(MacAddress mac, CharSequence deviceName) {
        final Activity activity = getActivity();
        new AlertDialog.Builder(activity)
            .setTitle(R.string.wifi_hotspot_block_client_dialog_title)
            .setMessage(activity.getString(
                R.string.wifi_hotspot_block_client_dialog_text, deviceName))
            .setPositiveButton(android.R.string.ok,
                (dialog, which) -> {
                    blockClient(mac, true);
                })
            .setNegativeButton(android.R.string.cancel, null)
            .create().show();
    }

    private void showUnblockClientDialog(MacAddress mac) {
        final Activity activity = getActivity();
        new AlertDialog.Builder(activity)
            .setTitle(R.string.wifi_hotspot_unblock_client_dialog_title)
            .setMessage(activity.getString(
                R.string.wifi_hotspot_unblock_client_dialog_text, mac.toString()))
            .setPositiveButton(android.R.string.ok,
                (dialog, which) -> {
                    blockClient(mac, false);
                })
            .setNegativeButton(android.R.string.cancel, null)
            .create().show();
    }

    @Override
    public void onStart() {
        super.onStart();
        mTetheringManager.registerTetheringEventCallback(getActivity().getMainExecutor(), this);
    }

    @Override
    public void onStop() {
        mTetheringManager.unregisterTetheringEventCallback(this);
        super.onStop();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI_TETHER_SETTINGS;
    }

    private class ConnectedClientPreference extends Preference {
        private MacAddress mMacAddress;

        public ConnectedClientPreference(Context context, TetheredClient client) {
            super(context);
            mMacAddress = client.getMacAddress();

            String hostName = null;
            String macAddress = client.getMacAddress().toString();

            for (TetheredClient.AddressInfo addressInfo : client.getAddresses()) {
                if (!TextUtils.isEmpty(addressInfo.getHostname())) {
                    hostName = addressInfo.getHostname();
                    break;
                }
            }

            setKey(macAddress);
            if (!TextUtils.isEmpty(hostName)) {
                setTitle(hostName);
                setSummary(macAddress);
            } else {
                setTitle(macAddress);
            }
        }

        public MacAddress getMacAddress() {
            return mMacAddress;
        }
    }

    private class BlockedClientPreference extends Preference {
        private MacAddress mMacAddress;

        public BlockedClientPreference(Context context, MacAddress mac) {
            super(context);
            mMacAddress = mac;
            setKey(mac.toString());
            setTitle(mac.toString());
        }

        public MacAddress getMacAddress() {
            return mMacAddress;
        }
    }
}
