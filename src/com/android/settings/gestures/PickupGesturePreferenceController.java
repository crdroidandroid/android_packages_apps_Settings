/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.provider.Settings.Secure.DOZE_PICK_UP_GESTURE;

import android.annotation.UserIdInt;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.hardware.display.AmbientDisplayConfiguration;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.PrimarySwitchPreference;

public class PickupGesturePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final int ON = 1;
    private static final int OFF = 0;

    private final String SECURE_KEY = DOZE_PICK_UP_GESTURE;
    private static final String AMBIENT_SECURE_KEY = "doze_pick_up_gesture_ambient";

    private AmbientDisplayConfiguration mAmbientConfig;
    @UserIdInt
    private final int mUserId;
        private final boolean mDefault;

    private PrimarySwitchPreference mPreference;
    private SettingObserver mSettingObserver;

    public PickupGesturePreferenceController(Context context, String key) {
        super(context, key);
        mUserId = UserHandle.myUserId();
        mDefault = context.getResources().getBoolean(
                com.android.internal.R.bool.config_dozePickupGestureEnabled);
    }

    public PickupGesturePreferenceController setConfig(AmbientDisplayConfiguration config) {
        mAmbientConfig = config;
        return this;
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences prefs) {
        AmbientDisplayConfiguration ambientConfig = new AmbientDisplayConfiguration(context);
        return prefs.getBoolean(PickupGestureSettings.PREF_KEY_SUGGESTION_COMPLETE, false)
                || !ambientConfig.dozePickupSensorAvailable();
    }

    @Override
    public int getAvailabilityStatus() {
        // No hardware support for Pickup Gesture
        if (!getAmbientConfig().dozePickupSensorAvailable()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mSettingObserver = new SettingObserver(mPreference);
        updateState(mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final ContentResolver resolver = mContext.getContentResolver();
        final boolean enabled =
                Settings.Secure.getInt(resolver, SECURE_KEY, mDefault ? ON : OFF) == ON;
        String summary;
        if (enabled) {
            summary = mContext.getString(R.string.gesture_setting_on) + " ("
                    + (Settings.Secure.getInt(resolver, AMBIENT_SECURE_KEY, OFF) == ON
                    ? mContext.getString(R.string.gesture_wake_ambient)
                    : mContext.getString(R.string.gesture_wake)) + ")";
        } else {
            summary = mContext.getString(R.string.gesture_setting_off);
        }
        preference.setSummary(summary);
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_pick_up");
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean isChecked() {
        return getAmbientConfig().pickupGestureEnabled(mUserId);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY,
                isChecked ? ON : OFF);
    }

    private AmbientDisplayConfiguration getAmbientConfig() {
        if (mAmbientConfig == null) {
            mAmbientConfig = new AmbientDisplayConfiguration(mContext);
        }

        return mAmbientConfig;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return NO_RES;
    }

    @Override
    public void onStart() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
            mSettingObserver.onChange(false, null);
        }
    }

    @Override
    public void onStop() {
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    private class SettingObserver extends ContentObserver {
        private final Uri mUri = Settings.Secure.getUriFor(SECURE_KEY);
        private final Uri mAmbientUri = Settings.Secure.getUriFor(AMBIENT_SECURE_KEY);

        private final Preference mPreference;

        SettingObserver(Preference preference) {
            super(Handler.getMain());
            mPreference = preference;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(mUri, false, this);
            cr.registerContentObserver(mAmbientUri, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri == null || mUri.equals(uri) || mAmbientUri.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
