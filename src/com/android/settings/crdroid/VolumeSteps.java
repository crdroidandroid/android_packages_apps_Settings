/*
 * Copyright (C) 2015 The DU Project
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

package com.android.settings.crdroid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class VolumeSteps extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "VolumeSteps";

    // base map of all preference keys and the associated stream
    private static final Map<String, Integer> volume_map = new HashMap<String, Integer>();
    static {
        volume_map.put("volume_steps_alarm", new Integer(AudioManager.STREAM_ALARM));
        volume_map.put("volume_steps_dtmf", new Integer(AudioManager.STREAM_DTMF));
        volume_map.put("volume_steps_music", new Integer(AudioManager.STREAM_MUSIC));
        volume_map.put("volume_steps_notification", new Integer(AudioManager.STREAM_NOTIFICATION));
        volume_map.put("volume_steps_ring", new Integer(AudioManager.STREAM_RING));
        volume_map.put("volume_steps_system", new Integer(AudioManager.STREAM_SYSTEM));
        volume_map.put("volume_steps_voice_call", new Integer(AudioManager.STREAM_VOICE_CALL));
    }

    // entries to remove on non-telephony devices
    private static final Set<String> telephony_set = new HashSet<String>();
    static {
        telephony_set.add("volume_steps_dtmf");
        telephony_set.add("volume_steps_ring");
        telephony_set.add("volume_steps_voice_call");
    }

    // set of available pref keys after device configuration filter
    private Set<String> mAvailableKeys = new HashSet<String>();
    private AudioManager mAudioManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.volume_steps_settings);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        final PreferenceScreen prefScreen = getPreferenceScreen();
        mAvailableKeys = volume_map.keySet();

        // remove invalid audio stream prefs
        boolean isPhone = TelephonyManager.getDefault().getCurrentPhoneType() != TelephonyManager.PHONE_TYPE_NONE;

        if (!isPhone) {
            // remove telephony keys from available set
            mAvailableKeys.removeAll(telephony_set);
            for (String key : telephony_set) {
                Preference toRemove = prefScreen.findPreference(key);
                if (toRemove != null) {
                    prefScreen.removePreference(toRemove);
                }
            }
        }

        // initialize prefs: set listeners and update values
        for (String key : mAvailableKeys) {
            Preference pref = prefScreen.findPreference(key);
            if (pref == null || !(pref instanceof ListPreference)) {
                continue;
            }
            updateVolumeSteps(pref, mAudioManager.getStreamMaxVolume(volume_map.get(key)));
            pref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference.hasKey() && mAvailableKeys.contains(preference.getKey())) {
            updateVolumeSteps(preference, Integer.parseInt(objValue.toString()));
            return true;
        }
        return false;
    }

    private void updateVolumeSteps(Preference pref, int steps) {
        Settings.System.putInt(getContentResolver(), pref.getKey(), steps);
        pref.setSummary(String.valueOf(steps));
        mAudioManager.setStreamMaxVolume(volume_map.get(pref.getKey()), steps);
        Log.i(TAG, "Volume steps:" + pref.getKey() + "" + String.valueOf(steps));
    }
}
