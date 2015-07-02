/*
 * Copyright (C) 2014 Slimroms
 * Modifyed for 2015 crDroid Android project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class InputMethodsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "KeyboardInputSettings";

    private static final int DLG_KEYBOARD_ROTATION = 0;

    private static final String KEYBOARD_ROTATION_TIMEOUT = "keyboard_rotation_timeout";

    private static final int KEYBOARD_ROTATION_TIMEOUT_DEFAULT = 5000; // 5s

    private ListPreference mKeyboardRotationTimeout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.input_methods_settings);

        boolean showCmImeSwitcher = getResources().getBoolean(
                com.android.internal.R.bool.config_show_cmIMESwitcher);
        if (!showCmImeSwitcher) {
            Preference pref = findPreference(Settings.System.STATUS_BAR_IME_SWITCHER);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }

        mKeyboardRotationTimeout = (ListPreference) findPreference(KEYBOARD_ROTATION_TIMEOUT);
        mKeyboardRotationTimeout.setOnPreferenceChangeListener(this);
        updateRotationTimeout(Settings.System.getInt(
                getContentResolver(), Settings.System.KEYBOARD_ROTATION_TIMEOUT,
                KEYBOARD_ROTATION_TIMEOUT_DEFAULT));

    }

    public void updateRotationTimeout(int timeout) {
        if (timeout == 0) {
            timeout = KEYBOARD_ROTATION_TIMEOUT_DEFAULT;
        }
        mKeyboardRotationTimeout.setValue(Integer.toString(timeout));
        mKeyboardRotationTimeout.setSummary(
            getString(R.string.keyboard_rotation_timeout_summary,
            mKeyboardRotationTimeout.getEntry()));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mKeyboardRotationTimeout) {
            int timeout = Integer.parseInt((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEYBOARD_ROTATION_TIMEOUT, timeout);
            updateRotationTimeout(timeout);
            return true;
        }
        return false;
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        InputMethodsSettings getOwner() {
            return (InputMethodsSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_KEYBOARD_ROTATION:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.attention)
                    .setMessage(R.string.keyboard_rotation_dialog)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }

}
