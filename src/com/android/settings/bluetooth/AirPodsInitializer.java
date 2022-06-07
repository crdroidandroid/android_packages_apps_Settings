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

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

public class AirPodsInitializer {

    static void startBatteryService(Context context, BluetoothDevice device) {
        if (AirPodsConstants.shouldBeAirPods(device)) {
            final Intent intent = new Intent(context, AirPodsBatteryService.class);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            context.startService(intent);
        }
    }

    static void stopBatteryService(Context context, BluetoothDevice device) {
        context.stopService(new Intent(context, AirPodsBatteryService.class));
    }

}
