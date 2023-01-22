//
// Copyright (C) 2022 The Project Mia
//
// SPDX-License-Identifier: Apache-2.0
//

package com.android.settings.fuelgauge.batterydata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public final class BatteryDataBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "BatteryDataBroadcastReceiver";

    public boolean mFetchBatteryUsageData;

    @Override
    public void onReceive(Context context, Intent intent) {
        String batteryData = intent.getAction();
        switch (batteryData) {
            // Fetch device usage data
            case "settings.intelligence.battery.action.FETCH_BATTERY_USAGE_DATA":
                mFetchBatteryUsageData = true;
                BatteryDataFetchService.enqueueWork(context);
                break;
            // Fetch bluetooth device usage data
            case "settings.intelligence.battery.action.FETCH_BLUETOOTH_BATTERY_DATA":
                try {
                    BluetoothBatteryDataFetch.returnBluetoothDevices(context, intent);
                } catch (Exception e) {
                    Log.e(TAG, "returnBluetoothDevices() error: ", e);
                }
                break;
            default:
                break;
        }
    }
}
