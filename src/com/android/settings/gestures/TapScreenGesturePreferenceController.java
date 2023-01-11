/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.gestures;

import static android.provider.Settings.Secure.DOZE_TAP_SCREEN_GESTURE;

import android.annotation.UserIdInt;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.AmbientDisplayConfiguration;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.PrimarySwitchPreference;

public class TapScreenGesturePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final String SECURE_KEY = DOZE_TAP_SCREEN_GESTURE;
    private static final String AMBIENT_SECURE_KEY = "doze_tap_gesture_ambient";

    private AmbientDisplayConfiguration mAmbientConfig;
    @UserIdInt
    private final int mUserId;

    private PrimarySwitchPreference mPreference;
    private SettingObserver mSettingObserver;

    public TapScreenGesturePreferenceController(Context context, String key) {
        super(context, key);
        mUserId = UserHandle.myUserId();
        mAmbientConfig = new AmbientDisplayConfiguration(context);
    }

    @Override
    public int getAvailabilityStatus() {
        // No hardware support for this Gesture
        if (!getAmbientConfig().tapSensorAvailable()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mSettingObserver = new SettingObserver(mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final ContentResolver resolver = mContext.getContentResolver();
        final boolean enabled =
                Settings.Secure.getInt(resolver, SECURE_KEY, 1) == 1;
        String summary;
        if (enabled) {
            summary = mContext.getString(R.string.gesture_setting_on) + " ("
                    + (Settings.Secure.getInt(resolver, AMBIENT_SECURE_KEY, 0) == 1
                    ? mContext.getString(R.string.gesture_wake_ambient)
                    : mContext.getString(R.string.gesture_wake)) + ")";
        } else {
            summary = mContext.getString(R.string.gesture_setting_off);
        }
        preference.setSummary(summary);
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public CharSequence getSummary() {
        return super.getSummary();
    }

    @Override
    public boolean isChecked() {
        return getAmbientConfig().tapGestureEnabled(mUserId);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        boolean success = Settings.Secure.putInt(mContext.getContentResolver(),
                SECURE_KEY, isChecked ? 1 : 0);
        SystemProperties.set("persist.sys.tap_gesture", isChecked ? "1" : "0");
        return success;
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
