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
package com.android.settings.deviceinfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.core.AbstractPreferenceController;

public class KernelVersionPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_KERNEL_VERSION = "kernel_version";
    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private static final String LOG_TAG = "KernelVersionPreferenceController";

    public KernelVersionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(DeviceInfoUtils.getFormattedKernelVersion());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_KERNEL_VERSION;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_KERNEL_VERSION)) {
            return false;
        }
        preference.setSummary(getFullKernelVersion());
        return false;
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
