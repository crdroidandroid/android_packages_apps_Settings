/*
 * Copyright (C) 2019 The MoKee Open Source Project
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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AirPodsBatteryInitializer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
            final int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            handleA2dpStateChanged(context, state, device);
        }
    }

    private void handleA2dpStateChanged(Context context, int state, BluetoothDevice device) {
        if (state == BluetoothProfile.STATE_CONNECTED) {
            AirPodsInitializer.startBatteryService(context, device);
        } else if (state == BluetoothProfile.STATE_DISCONNECTING ||
                state == BluetoothProfile.STATE_DISCONNECTED) {
            AirPodsInitializer.stopBatteryService(context, device);
        }
    }

}
