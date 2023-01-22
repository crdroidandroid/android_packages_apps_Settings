//
// Copyright (C) 2022 The Project Mia
//
// SPDX-License-Identifier: Apache-2.0
//

package com.android.settings.fuelgauge.batterydata;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class BluetoothBatteryDataFetch {

    private static final String TAG = "BluetoothBatteryDataFetch";

    @VisibleForTesting
    public static LocalBluetoothManager mLocalBluetoothManager;
    private static Context context;
    private static Intent intent;

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    public static ContentValues wrapBluetoothData(
            Context context, CachedBluetoothDevice cachedBluetoothDevice,
            boolean nonbuds) {
        BluetoothDevice device = cachedBluetoothDevice.getDevice();

        ContentValues contentValues = new ContentValues();
        contentValues.put("type", device.getType());
        contentValues.put("name", emptyIfNull(device.getName()));
        contentValues.put("alias", emptyIfNull(device.getAlias()));
        contentValues.put("address", emptyIfNull(device.getAddress()));
        contentValues.put("batteryLevel", device.getBatteryLevel());

        putStringMetadata(contentValues, "hardwareVersion", device.getMetadata(
                BluetoothDevice.METADATA_HARDWARE_VERSION));
        putStringMetadata(contentValues, "batteryLevelRight", device.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY));
        putStringMetadata(contentValues, "batteryLevelLeft", device.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY));
        putStringMetadata(contentValues, "batteryLevelCase", device.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY));
        putStringMetadata(contentValues, "batteryChargingRight", device.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING));
        putStringMetadata(contentValues, "batteryChargingLeft", device.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING));
        putStringMetadata(contentValues, "batteryChargingCase", device.getMetadata(
                BluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING));
        putStringMetadata(contentValues, "batteryChargingMain", device.getMetadata(
                BluetoothDevice.METADATA_MAIN_CHARGING));
        if (nonbuds) {
            putStringMetadata(contentValues, "deviceIconMain", device.getMetadata(
                    BluetoothDevice.METADATA_MAIN_ICON));
            putStringMetadata(contentValues, "deviceIconCase", device.getMetadata(
                    BluetoothDevice.METADATA_UNTETHERED_CASE_ICON));
            putStringMetadata(contentValues, "deviceIconLeft", device.getMetadata(
                    BluetoothDevice.METADATA_UNTETHERED_LEFT_ICON));
            putStringMetadata(contentValues, "deviceIconRight", device.getMetadata(
                    BluetoothDevice.METADATA_UNTETHERED_RIGHT_ICON));
        }
        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass != null) {
            contentValues.put("bluetoothClass", marshall(bluetoothClass));
        }
        return contentValues;
    }

    private static byte[] marshall(Parcelable parcelable) {
        Parcel obtain = Parcel.obtain();
        parcelable.writeToParcel(obtain, 0);
        byte[] marshall = obtain.marshall();
        obtain.recycle();
        return marshall;
    }

    private static void putStringMetadata(
            ContentValues contentValues, String key, byte[] value) {
        if (value == null || value.length == 0) {
            return;
        }
        contentValues.put(key, new String(value));
    }

    public static void returnBluetoothDevices(Context context, Intent intent) {
        AsyncTask.execute(() -> returnBluetoothDevicesInner(context, intent));
    }

    public static void returnBluetoothDevicesInner(Context context, Intent intent) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(Intent.EXTRA_RESULT_RECEIVER);
        if (resultReceiver == null) {
            Log.w(TAG, "No result receiver found from intent");
            return;
        }
        if (mLocalBluetoothManager == null) {
            mLocalBluetoothManager = LocalBluetoothManager.getInstance(context, null);
        }
        BluetoothAdapter adapter = context.getSystemService(BluetoothManager.class).getAdapter();
        if (adapter == null || !adapter.isEnabled() || mLocalBluetoothManager == null) {
            Log.w(TAG, "BluetoothAdapter not present or not enabled");
            resultReceiver.send(1, null);
            return;
        }
        sendAndFilterBluetoothData(context, resultReceiver, mLocalBluetoothManager,
                intent.getBooleanExtra("extra_fetch_icon", false));
    }

    public static void sendAndFilterBluetoothData(Context context,
            ResultReceiver resultReceiver,
            LocalBluetoothManager localBluetoothManager,
            boolean cache) {
        long start = System.currentTimeMillis();
        Collection<CachedBluetoothDevice> cachedDevicesCopy =
                localBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();
        Log.d(TAG, "cachedDevices:" + cachedDevicesCopy);
        if (cachedDevicesCopy == null || cachedDevicesCopy.isEmpty()) {
            resultReceiver.send(0, Bundle.EMPTY);
            return;
        }
        List<CachedBluetoothDevice> connectedDevices = cachedDevicesCopy.stream()
                .filter(CachedBluetoothDevice::isConnected)
                .collect(Collectors.toList());
        Log.d(TAG, "Connected devices:" + connectedDevices);
        if (connectedDevices.isEmpty()) {
            resultReceiver.send(0, Bundle.EMPTY);
            return;
        }
        ArrayList<ContentValues> bluetoothWrapDataListKey = new ArrayList<>();
        ArrayList<BluetoothDevice> bluetoothParcelableList = new ArrayList<>();
        connectedDevices.forEach(cachedBluetoothDevice -> {
            BluetoothDevice device = cachedBluetoothDevice.getDevice();
            bluetoothParcelableList.add(device);
            try {
                bluetoothWrapDataListKey.add(
                        wrapBluetoothData(context, cachedBluetoothDevice, cache));
            } catch (Exception e) {
                Log.e(TAG, "Wrap bluetooth data failed: " + device, e);
            }
        });
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("bluetoothParcelableListKey", bluetoothParcelableList);
        if (!bluetoothWrapDataListKey.isEmpty()) {
            bundle.putParcelableArrayList("bluetoothWrapDataListKey", bluetoothWrapDataListKey);
        }
        resultReceiver.send(0, bundle);
        Log.d(TAG, String.format("Send and filter bluetooth data size=%d in %d/ms",
                bluetoothWrapDataListKey.size(), (System.currentTimeMillis() - start)));
    }
}
