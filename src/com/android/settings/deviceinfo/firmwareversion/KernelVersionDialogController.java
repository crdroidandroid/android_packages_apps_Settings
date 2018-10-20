/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo.firmwareversion;

import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;

import com.android.settings.R;
import com.android.settingslib.DeviceInfoUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class KernelVersionDialogController implements View.OnClickListener {

    @VisibleForTesting
    static int KERNEL_VERSION_VALUE_ID = R.id.kernel_version_value;

    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private static final String LOG_TAG = "KernelVersionPreferenceController";
    private boolean fullKernelVersion = false;

    private final FirmwareVersionDialogFragment mDialog;

    public KernelVersionDialogController(FirmwareVersionDialogFragment dialog) {
        mDialog = dialog;
    }

    @Override
    public void onClick(View v) {
        if (fullKernelVersion) {
            mDialog.setText(KERNEL_VERSION_VALUE_ID,
                    DeviceInfoUtils.getFormattedKernelVersion(mDialog.getContext()));
            fullKernelVersion = false;
        } else {
            mDialog.setText(KERNEL_VERSION_VALUE_ID,
                    getFullKernelVersion());
            fullKernelVersion = true;
        }
    }

    /**
     * Updates kernel version to the dialog.
     */
    public void initialize() {
        registerClickListener();
        mDialog.setText(KERNEL_VERSION_VALUE_ID,
                DeviceInfoUtils.getFormattedKernelVersion(mDialog.getContext()));
    }

    private void registerClickListener() {
        mDialog.registerClickListener(KERNEL_VERSION_VALUE_ID, this /* listener */);
    }

    private String getFullKernelVersion() {
        String procVersionStr;
        try {
            procVersionStr = readLine(FILENAME_PROC_VERSION);
            return procVersionStr;
        } catch (IOException e) {
            Log.e(LOG_TAG,
            "IO Exception when getting kernel version for Device Info screen", e);
            return "Unavailable";
        }
    }

    /**
     * Reads a line from the specified file.
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }
}
