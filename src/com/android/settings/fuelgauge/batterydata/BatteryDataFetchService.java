//
// Copyright (C) 2022 The Project Mia
//
// SPDX-License-Identifier: Apache-2.0
//

package com.android.settings.fuelgauge.batterydata;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.BatteryStatsManager;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.util.List;

public class BatteryDataFetchService extends JobIntentService {

    private static final String TAG = "BatteryDataFetchService";
    private static final Intent JOB_INTENT = new Intent("action.LOAD_BATTERY_USAGE_DATA");

    public static void enqueueWork(final Context context) {
        AsyncTask.execute(() -> {
            loadUsageDataSafely(context);
        });
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        loadUsageDataSafely(this);
    }

    private static void loadUsageDataSafely(Context context) {
        try {
            loadUsageData(context);
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail load usage data:" + e);
        }
    }

    private static void loadUsageData(Context context) {
        BatteryUsageStats batteryUsageStats = context
                .getSystemService(BatteryStatsManager.class)
                .getBatteryUsageStats(new BatteryUsageStatsQuery.Builder()
                .includeBatteryHistory()
                .build());
    }
}
