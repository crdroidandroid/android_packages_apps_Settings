/*
 * Copyright (C) 2024 Yet Another AOSP Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class GestureNavigationLongPressController extends TogglePreferenceController {

    private static final String GSA_PACKAGE = "com.google.android.googlequicksearchbox";

    public GestureNavigationLongPressController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NAVBAR_LONG_PRESS_GESTURE, 1) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NAVBAR_LONG_PRESS_GESTURE, isChecked ? 1 : 0);
    }

    @Override
    public int getAvailabilityStatus() {
        PackageManager pm = mContext.getPackageManager();
        if (pm == null) {
            return UNSUPPORTED_ON_DEVICE;
        }
        try {
            ApplicationInfo ai = pm.getApplicationInfo(GSA_PACKAGE, 0);
            if (ai.enabled && ai.isProduct()) {
                return AVAILABLE;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}
