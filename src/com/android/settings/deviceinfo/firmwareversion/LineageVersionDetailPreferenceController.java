/*
 * Copyright (C) 2019-2022 The LineageOS Project
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

import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class LineageVersionDetailPreferenceController extends BasePreferenceController {

    private static final String TAG = "lineageVersionDialogCtrl";

    private static final String KEY_LINEAGE_VERSION_PROP = "ro.modversion";

    public LineageVersionDetailPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public CharSequence getSummary() {
        return SystemProperties.get(KEY_LINEAGE_VERSION_PROP,
                mContext.getString(R.string.unknown));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }
}
