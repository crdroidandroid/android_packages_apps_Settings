/*
 * Copyright (C) 2020 Yet Another AOSP Project
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

package com.android.settings.sound;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.sound.CustomVibrationPreferenceController;
import com.android.settings.Utils;
import com.android.settingslib.core.AbstractPreferenceController;

import com.crdroid.settings.preferences.CustomSeekBarPreference;

/**
 * This class allows choosing a vibration pattern while ringing
 */
public class CustomVibrationPreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY = "custom_vibration_pattern";
    private static final String KEY_CUSTOM_VIB1 = "custom_vibration_pattern1";
    private static final String KEY_CUSTOM_VIB2 = "custom_vibration_pattern2";
    private static final String KEY_CUSTOM_VIB3 = "custom_vibration_pattern3";

    private CustomSeekBarPreference mCustomVib1;
    private CustomSeekBarPreference mCustomVib2;
    private CustomSeekBarPreference mCustomVib3;

    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .build();

    private static final int[] SEVEN_ELEMENTS_VIBRATION_AMPLITUDE = {
        0, // No delay before starting
        255, // Vibrate full amplitude
        0, // No amplitude while waiting
        255,
        0,
        255,
        0,
    };

    public CustomVibrationPreferenceController(Context context) {
        super(context);
    }

    private static class VibrationEffectProxy {
        public VibrationEffect createWaveform(long[] timings, int[] amplitudes, int repeat) {
            return VibrationEffect.createWaveform(timings, amplitudes, repeat);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mCustomVib1 = (CustomSeekBarPreference) screen.findPreference(KEY_CUSTOM_VIB1);
        mCustomVib2 = (CustomSeekBarPreference) screen.findPreference(KEY_CUSTOM_VIB2);
        mCustomVib3 = (CustomSeekBarPreference) screen.findPreference(KEY_CUSTOM_VIB3);
        updateCustomVibPreferences();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCustomVib1) {
            updateCustomVib(0, (Integer) newValue);
            return true;
        } else if (preference == mCustomVib2) {
            updateCustomVib(1, (Integer) newValue);
            return true;
        } else if (preference == mCustomVib3) {
            updateCustomVib(2, (Integer) newValue);
            return true;
        }
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return Utils.isVoiceCapable(mContext);
    }

    private void updateCustomVibPreferences() {
        String value = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN);
        if (value != null) {
            String[] customPattern = value.split(",", 3);
            mCustomVib1.setValue(Integer.parseInt(customPattern[0]));
            mCustomVib2.setValue(Integer.parseInt(customPattern[1]));
            mCustomVib3.setValue(Integer.parseInt(customPattern[2]));
        } else { // set default
            mCustomVib1.setValue(0);
            mCustomVib2.setValue(800);
            mCustomVib3.setValue(800);
            Settings.System.putString(mContext.getContentResolver(),
                    Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN, "0,800,800");
        }
        mCustomVib1.setOnPreferenceChangeListener(this);
        mCustomVib2.setOnPreferenceChangeListener(this);
        mCustomVib3.setOnPreferenceChangeListener(this);
    }

    private void updateCustomVib(int index, int value) {
        String[] customPattern = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN).split(",", 3);
        customPattern[index] = String.valueOf(value);
        Settings.System.putString(mContext.getContentResolver(),
                Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN, String.join(
                ",", customPattern[0], customPattern[1], customPattern[2]));
        previewPattern();
    }

    private void previewPattern() {
        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        VibrationEffect effect;
        VibrationEffectProxy vibrationEffectProxy = new VibrationEffectProxy();
        String[] customVib = Settings.System.getString(
                mContext.getContentResolver(),
                Settings.System.CUSTOM_RINGTONE_VIBRATION_PATTERN).split(",", 3);
        long[] customVibPattern = {
            0, // No delay before starting
            Long.parseLong(customVib[0]), // How long to vibrate
            400, // Delay
            Long.parseLong(customVib[1]), // How long to vibrate
            400, // Delay
            Long.parseLong(customVib[2]), // How long to vibrate
            400, // How long to wait before vibrating again
        };
        effect = vibrationEffectProxy.createWaveform(customVibPattern,
                SEVEN_ELEMENTS_VIBRATION_AMPLITUDE, -1);
        vibrator.vibrate(effect, VIBRATION_ATTRIBUTES);
    }

}
