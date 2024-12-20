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

public class GestureNavigationCtsController extends TogglePreferenceController {

    private final boolean mDefaultEnabled;
    private final String mCtsPackage;

    public GestureNavigationCtsController(Context context, String key) {
        super(context, key);

        mDefaultEnabled = context.getResources().getBoolean(
                com.android.internal.R.bool.config_searchAllEntrypointsEnabledDefault);
        mCtsPackage = mContext.getResources().getString(
                com.android.internal.R.string.config_defaultContextualSearchPackageName);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SEARCH_ALL_ENTRYPOINTS_ENABLED, mDefaultEnabled ? 1 : 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SEARCH_ALL_ENTRYPOINTS_ENABLED, isChecked ? 1 : 0);
    }

    @Override
    public int getAvailabilityStatus() {
        PackageManager pm = mContext.getPackageManager();
        if (pm == null) {
            return UNSUPPORTED_ON_DEVICE;
        }
        try {
            ApplicationInfo ai = pm.getApplicationInfo(mCtsPackage, 0);
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
