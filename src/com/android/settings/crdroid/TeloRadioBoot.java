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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.os.UserHandle;

public class TeloRadioBoot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean mTeloRadioPrefEnable = Settings.System.getIntForUser(context.getContentResolver(), Settings.System.TELO_RADIO_ENABLED, 0, UserHandle.USER_CURRENT) == 1;
        if (!mTeloRadioPrefEnable)
            return;
                
        // Launch Service
        Intent serviceIntent = new Intent();
        serviceIntent.setAction("com.android.phone.TeloRadioService");
        context.startService(serviceIntent);
    }
}
