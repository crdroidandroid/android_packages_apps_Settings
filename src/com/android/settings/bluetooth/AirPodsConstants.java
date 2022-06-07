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
import android.os.ParcelUuid;

import java.util.HashSet;
import java.util.Set;

class AirPodsConstants {

    static final int MANUFACTURER_ID = 0x004C;
    static final int MANUFACTURER_MAGIC = 0x07;

    private static final Set<ParcelUuid> UUIDS = new HashSet<>();

    static {
        UUIDS.add(ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"));
        UUIDS.add(ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74"));
    }

    static boolean shouldBeAirPods(BluetoothDevice device) {
        for (ParcelUuid uuid : device.getUuids()) {
            if (AirPodsConstants.UUIDS.contains(uuid)) {
                return true;
            }
        }

        return false;
    }

}
