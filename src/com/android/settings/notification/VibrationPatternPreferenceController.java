/*
 * Copyright (C) 2022 crDroid Android Project
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

package com.android.settings.notification;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * This class allows choosing a vibration pattern while ringing
 */
public class VibrationPatternPreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_VIB_PATTERN = "ringtone_vibration_pattern";

    private ListPreference mVibPattern;

    private static final String[] mKeys = {"pattern_dzzz_dzzz", "pattern_dzzz_da", "pattern_mm_mm_mm",
            "pattern_da_da_dzzz", "pattern_da_dzzz_da"};

    private static final long[] DZZZ_DZZZ_VIBRATION_PATTERN = {
            0, // No delay before starting
            1000, // How long to vibrate
            1000, // How long to wait before vibrating again
            1000, // How long to vibrate
            1000, // How long to wait before vibrating again
    };

    private static final long[] DZZZ_DA_VIBRATION_PATTERN = {
            0, // No delay before starting
            500, // How long to vibrate
            200, // Delay
            70, // How long to vibrate
            720, // How long to wait before vibrating again
    };

    private static final long[] MM_MM_MM_VIBRATION_PATTERN = {
            0, // No delay before starting
            300, // How long to vibrate
            400, // Delay
            300, // How long to vibrate
            400, // Delay
            300, // How long to vibrate
            1400, // How long to wait before vibrating again
    };

    private static final long[] DA_DA_DZZZ_VIBRATION_PATTERN = {
            0, // No delay before starting
            70, // How long to vibrate
            80, // Delay
            70, // How long to vibrate
            180, // Delay
            600,  // How long to vibrate
            1050, // How long to wait before vibrating again
    };

    private static final long[] DA_DZZZ_DA_VIBRATION_PATTERN = {
            0, // No delay before starting
            80, // How long to vibrate
            200, // Delay
            600, // How long to vibrate
            150, // Delay
            60,  // How long to vibrate
            1050, // How long to wait before vibrating again
    };

    private static final int[] SEVEN_ELEMENTS_VIBRATION_AMPLITUDE = {
            0, // No delay before starting
            255, // Vibrate full amplitude
            0, // No amplitude while waiting
            255,
            0,
            255,
            0,
    };

    private static final int[] FIVE_ELEMENTS_VIBRATION_AMPLITUDE = {
            0, // No delay before starting
            255, // Vibrate full amplitude
            0, // No amplitude while waiting
            255,
            0,
    };

    private final AudioAttributes mAudioAttributesRingtone = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .build();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private Vibrator mVibrator;

    public VibrationPatternPreferenceController(Context context) {
        super(context);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null && !mVibrator.hasVibrator()) {
            mVibrator = null;
        }
    }

    @Override
    public boolean isAvailable() {
        return mVibrator != null;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VIB_PATTERN;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mVibPattern = (ListPreference) screen.findPreference(KEY_VIB_PATTERN);
        int vibPattern = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.RINGTONE_VIBRATION_PATTERN, 0, UserHandle.USER_CURRENT);
        mVibPattern.setValueIndex(vibPattern);
        mVibPattern.setSummary(mVibPattern.getEntries()[vibPattern]);
        mVibPattern.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int vibPattern = Integer.valueOf((String) newValue);
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.RINGTONE_VIBRATION_PATTERN, vibPattern, UserHandle.USER_CURRENT);
        mVibPattern.setSummary(mVibPattern.getEntries()[vibPattern]);
        mHandler.postDelayed(() -> {
            performVibrationDemo(vibPattern);
        }, 15);
        return true;
    }

    private void performVibrationDemo(int val) {
        VibrationEffect mDefaultVibrationEffect;
        switch (val) {
            case 1:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(DZZZ_DA_VIBRATION_PATTERN,
                        FIVE_ELEMENTS_VIBRATION_AMPLITUDE,
                        -1);
                break;
            case 2:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(MM_MM_MM_VIBRATION_PATTERN,
                        SEVEN_ELEMENTS_VIBRATION_AMPLITUDE,
                        -1);
                break;
            case 3:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(DA_DA_DZZZ_VIBRATION_PATTERN,
                        SEVEN_ELEMENTS_VIBRATION_AMPLITUDE,
                        -1);
                break;
            case 4:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(DA_DZZZ_DA_VIBRATION_PATTERN,
                        SEVEN_ELEMENTS_VIBRATION_AMPLITUDE,
                        -1);
                break;
            default:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(DZZZ_DZZZ_VIBRATION_PATTERN,
                        FIVE_ELEMENTS_VIBRATION_AMPLITUDE,
                        -1);
                break;
        }
        if (mVibrator != null) {
            mVibrator.vibrate(mDefaultVibrationEffect, mAudioAttributesRingtone);
        }
    }
}
