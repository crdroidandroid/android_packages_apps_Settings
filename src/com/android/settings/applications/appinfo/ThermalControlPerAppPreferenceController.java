/*
 * Copyright (C) 2019 The PixelExperience Project
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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.core.BasePreferenceController;

import java.util.List;
import java.util.Arrays;

import com.android.internal.util.thermal.ThermalController;

public class ThermalControlPerAppPreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener{

    private final PackageManager mPackageManager;
    private String mPackageName;
    private ListPreference mPref;
    private final String[] hideApps = {"com.android.settings", "com.android.documentsui",
        "com.android.fmradio", "com.caf.fmradio", "com.android.stk",
        "com.google.android.calculator", "com.google.android.calendar",
        "com.google.android.deskclock", "com.google.android.contacts",
        "com.google.android.apps.messaging", "com.google.android.googlequicksearchbox"};

    public ThermalControlPerAppPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPackageManager = context.getPackageManager();
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    @Override
    public int getAvailabilityStatus() {
        if (TextUtils.isEmpty(mPackageName) || mPackageManager.getLaunchIntentForPackage(mPackageName) == null ||
                isLauncherApp(mPackageName) || !ThermalController.isAvailable(mContext) ||
                Arrays.asList(hideApps).contains(mPackageName)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    private boolean isLauncherApp(String packageName) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo res = mPackageManager.resolveActivity(intent, 0);
        if (res.activityInfo != null && res.activityInfo.packageName.equals(packageName)) {
            return true;
        }
        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPref = (ListPreference) screen.findPreference(getPreferenceKey());
        mPref.setValue(Integer.toString(ThermalController.getProfile(mPackageName, mContext)));
        mPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int profile = Integer.parseInt((String) newValue);
        ThermalController.setProfile(mPackageName, profile, mContext);
        refreshSummary(preference);
        return true;
    }

    @Override
    public CharSequence getSummary() {
        int value = ThermalController.getProfile(mPackageName, mContext);
        int index = mPref.findIndexOfValue(Integer.toString(value));
        return mPref.getEntries()[index];
    }

}
