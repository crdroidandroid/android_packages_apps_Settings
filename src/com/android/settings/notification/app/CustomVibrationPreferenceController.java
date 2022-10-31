/*
 * Copyright (C) 2022 Yet Another AOSP Project
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

package com.android.settings.notification.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedSwitchPreference;

import com.crdroid.settings.preferences.CustomSeekBarPreference;

public class CustomVibrationPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};
    private static final int[] VIBRATION_AMPLITUDE = {0, 255, 0, 255};
    private static final int VIBRATE_PATTERN_MAXLEN = 8 * 2 + 1; // up to eight bumps
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .build();

    private static final String KEY = "custom_vibrate";
    private static final String KEY1 = "custom_vibrate1";
    private static final String KEY2 = "custom_vibrate2";
    private static final String KEY3 = "custom_vibrate3";
    private static final String CATEGORY_KEY = "custom_vibrate_seek_bars";
    private final Vibrator mVibrator;
    private final long[] mDefaultPattern;

    private RestrictedSwitchPreference mPreference;
    private CustomSeekBarPreference mSeekBar1;
    private CustomSeekBarPreference mSeekBar2;
    private CustomSeekBarPreference mSeekBar3;
    private PreferenceCategory mBarsCategory;

    public CustomVibrationPreferenceController(Context context, NotificationBackend backend) {
        super(context, backend);
        mVibrator = context.getSystemService(Vibrator.class);
        mDefaultPattern = getLongArray(context.getResources(),
                com.android.internal.R.array.config_defaultNotificationVibePattern,
                VIBRATE_PATTERN_MAXLEN,
                DEFAULT_VIBRATE_PATTERN);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY);
        mPreference.setOnPreferenceChangeListener(this);
        mSeekBar1 = screen.findPreference(KEY1);
        mSeekBar1.setOnPreferenceChangeListener(this);
        mSeekBar2 = screen.findPreference(KEY2);
        mSeekBar2.setOnPreferenceChangeListener(this);
        mSeekBar3 = screen.findPreference(KEY3);
        mSeekBar3.setOnPreferenceChangeListener(this);
        mBarsCategory = screen.findPreference(CATEGORY_KEY);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable() || mChannel == null) {
            if (mBarsCategory != null)
                mBarsCategory.setVisible(false);
            return false;
        }
        final boolean avail = checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT)
                && mChannel.shouldVibrate()
                && mChannel.getVibrationPattern() == null
                && !isDefaultChannel()
                && mVibrator != null
                && mVibrator.hasVibrator();
        if (mBarsCategory != null && !avail)
            mBarsCategory.setVisible(false);
        return avail;
    }

    @Override
    boolean isIncludedInFilter() {
        return mPreferenceFilter.contains(NotificationChannel.EDIT_VIBRATION);
    }

    public void updateState(Preference preference) {
        if (mChannel == null) return;
        if (mPreference == null) mPreference = (RestrictedSwitchPreference) preference;
        mPreference.setDisabledByAdmin(mAdmin);
        mPreference.setEnabled(!mPreference.isDisabledByAdmin());
        final boolean isChecked = mChannel.shouldVibrate() &&
                mChannel.getCustomVibrationPattern() != null;
        mPreference.setChecked(isChecked);
        mBarsCategory.setVisible(isChecked);
        if (isChecked) getValues();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int pref = 0;
        if (preference == mPreference) {
            Boolean value = (Boolean) newValue;
            mBarsCategory.setVisible(value);
            if (value) getValues();
            else setValues(false, 0, 0);
            return true;
        } else if (preference == mSeekBar1) {
            pref = 1;
        } else if (preference == mSeekBar2) {
            pref = 2;
        } else if (preference == mSeekBar3) {
            pref = 3;
        }
        return setValues(mPreference.isChecked(), pref,
                ((Integer) newValue).longValue());
    }

    private boolean setValues(boolean enabled, int pref, long val) {
        mBarsCategory.setVisible(enabled);
        if (enabled) {
            final long val1 = pref == 1 ? val : mSeekBar1.getValue();
            final long val2 = pref == 2 ? val : mSeekBar2.getValue();
            final long val3 = pref == 3 ? val : mSeekBar3.getValue();
            if (val1 == 0 && val2 == 0 && val3 == 0)
                return false;
            final long[] pattern = {0, val1, val2, val3};
            mChannel.setCustomVibrationPattern(pattern);
            previewPattern(pattern);
        } else {
            mChannel.setCustomVibrationPattern(null);
        }
        saveChannel();
        return true;
    }

    private void getValues() {
        long[] pattern = mChannel.getCustomVibrationPattern();
        if (pattern == null) pattern = mDefaultPattern;
        if (pattern.length < 4) pattern = new long[] {0, 100, 150, 100};
        mSeekBar1.setValue(Math.round(pattern[1]));
        mSeekBar2.setValue(Math.round(pattern[2]));
        mSeekBar3.setValue(Math.round(pattern[3]));
    }

    private void previewPattern(long[] pattern) {
        VibrationEffect effect = VibrationEffect.createWaveform(
                pattern, VIBRATION_AMPLITUDE, -1);
        mVibrator.vibrate(effect, VIBRATION_ATTRIBUTES);
    }

    private static long[] getLongArray(Resources resources, int resId, int maxLength, long[] def) {
        int[] ar = resources.getIntArray(resId);
        if (ar == null) {
            return def;
        }
        final int len = ar.length > maxLength ? maxLength : ar.length;
        long[] out = new long[len];
        for (int i = 0; i < len; i++) {
            out[i] = ar[i];
        }
        return out;
    }
}
