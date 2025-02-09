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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class GestureNavigationLongPressController extends TogglePreferenceController {

    private static final String GSA_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final String LENS_SHARE_ACTIVITY = "com.google.android.apps.search.lens.LensShareEntryPointActivity";
    private static final String LONGPRESS_KEY = "search_all_entrypoints_enabled";

    private Preference mLongPressPref;

    public GestureNavigationLongPressController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mLongPressPref = (Preference) screen.findPreference(LONGPRESS_KEY);
	    mLongPressPref.setEnabled(isChecked());
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NAVBAR_LONG_PRESS_GESTURE, 1) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mLongPressPref != null)
            mLongPressPref.setEnabled(isChecked);
        return Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NAVBAR_LONG_PRESS_GESTURE, isChecked ? 1 : 0);
    }

    public static boolean isAvailable(Context context) {
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return false;
        }
        try {
            if (!pm.getApplicationInfo(GSA_PACKAGE, 0).enabled) {
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        // telling real GSA apart from the google stub
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setComponent(new ComponentName(GSA_PACKAGE, LENS_SHARE_ACTIVITY));
        return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }

    @Override
    public int getAvailabilityStatus() {
        return isAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}
