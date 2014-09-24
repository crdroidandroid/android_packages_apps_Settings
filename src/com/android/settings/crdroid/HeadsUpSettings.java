package com.android.settings.crdroid;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.os.UserHandle;


import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

public class HeadsUpSettings extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener  {

    public static final String TAG = "HeadsUpSettings";

    // Default timeout for heads up snooze. 5 minutes.
    protected static final int DEFAULT_TIME_HEADS_UP_SNOOZE = 300000;

    private static final String PREF_HEADS_UP_MASTER_SWITCH = "heads_up_master_switch";
    private static final String PREF_HEADS_UP_EXPANDED = "heads_up_expanded";
    private static final String PREF_HEADS_UP_SNOOZE_TIME = "heads_up_snooze_time";
    private static final String PREF_HEADS_UP_TIME_OUT = "heads_up_time_out";
    private static final String PREF_HEADS_UP_SHOW_UPDATE = "heads_up_show_update";
    private static final String PREF_HEADS_UP_GRAVITY = "heads_up_gravity";
    private static final String PREF_HEADS_UP_EXCLUDE_FROM_LOCK_SCREEN = "heads_up_exclude_from_lock_screen";
    
    CheckBoxPreference mHeadsUpMasterSwitch;
    ListPreference mHeadsUpSnoozeTime;
    ListPreference mHeadsUpTimeOut;
    CheckBoxPreference mHeadsUpExpanded;
    CheckBoxPreference mHeadsUpShowUpdates;
    CheckBoxPreference mHeadsUpGravity;
    CheckBoxPreference mHeadsExcludeFromLockscreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.heads_up_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        PackageManager pm = getPackageManager();

        mHeadsUpMasterSwitch = (CheckBoxPreference) findPreference(PREF_HEADS_UP_MASTER_SWITCH);
        mHeadsUpMasterSwitch.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HEADS_UP_MASTER_SWITCH, 0, UserHandle.USER_CURRENT) == 1);
        mHeadsUpMasterSwitch.setOnPreferenceChangeListener(this);

        mHeadsUpExpanded = (CheckBoxPreference) findPreference(PREF_HEADS_UP_EXPANDED);
        mHeadsUpExpanded.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HEADS_UP_EXPANDED, 0, UserHandle.USER_CURRENT) == 1);
        mHeadsUpExpanded.setOnPreferenceChangeListener(this);

        mHeadsUpShowUpdates = (CheckBoxPreference) findPreference(PREF_HEADS_UP_SHOW_UPDATE);
        mHeadsUpShowUpdates.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HEADS_UP_SHOW_UPDATE, 0, UserHandle.USER_CURRENT) == 1);
        mHeadsUpShowUpdates.setOnPreferenceChangeListener(this);

        mHeadsUpGravity = (CheckBoxPreference) findPreference(PREF_HEADS_UP_GRAVITY);
        mHeadsUpGravity.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HEADS_UP_GRAVITY_BOTTOM, 0, UserHandle.USER_CURRENT) == 1);
        mHeadsUpGravity.setOnPreferenceChangeListener(this);

        mHeadsExcludeFromLockscreen = (CheckBoxPreference) findPreference(PREF_HEADS_UP_EXCLUDE_FROM_LOCK_SCREEN);
        mHeadsExcludeFromLockscreen.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HEADS_UP_EXCLUDE_FROM_LOCK_SCREEN, 0, UserHandle.USER_CURRENT) == 1);
        mHeadsExcludeFromLockscreen.setOnPreferenceChangeListener(this);

        mHeadsUpSnoozeTime = (ListPreference) findPreference(PREF_HEADS_UP_SNOOZE_TIME);
        mHeadsUpSnoozeTime.setOnPreferenceChangeListener(this);
        int headsUpSnoozeTime = Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_SNOOZE_TIME, DEFAULT_TIME_HEADS_UP_SNOOZE);
        mHeadsUpSnoozeTime.setValue(String.valueOf(headsUpSnoozeTime));
        updateHeadsUpSnoozeTimeSummary(headsUpSnoozeTime);

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            return;
        }

        int defaultTimeOut = systemUiResources.getInteger(systemUiResources.getIdentifier(
                    "com.android.systemui:integer/heads_up_notification_decay", null, null));
        mHeadsUpTimeOut = (ListPreference) findPreference(PREF_HEADS_UP_TIME_OUT);
        mHeadsUpTimeOut.setOnPreferenceChangeListener(this);
        int headsUpTimeOut = Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_NOTIFCATION_DECAY, defaultTimeOut);
        mHeadsUpTimeOut.setValue(String.valueOf(headsUpTimeOut));
        updateHeadsUpTimeOutSummary(headsUpTimeOut);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHeadsUpMasterSwitch) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HEADS_UP_MASTER_SWITCH,
                    (Boolean) newValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mHeadsUpSnoozeTime) {
            int headsUpSnoozeTime = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_SNOOZE_TIME,
                    headsUpSnoozeTime);
            updateHeadsUpSnoozeTimeSummary(headsUpSnoozeTime);
            return true;
        } else if (preference == mHeadsUpTimeOut) {
            int headsUpTimeOut = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_NOTIFCATION_DECAY,
                    headsUpTimeOut);
            updateHeadsUpTimeOutSummary(headsUpTimeOut);
            return true;
        } else if (preference == mHeadsUpExpanded) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HEADS_UP_EXPANDED,
                    (Boolean) newValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mHeadsUpShowUpdates) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HEADS_UP_SHOW_UPDATE,
                    (Boolean) newValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mHeadsUpGravity) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HEADS_UP_GRAVITY_BOTTOM,
                    (Boolean) newValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mHeadsExcludeFromLockscreen) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HEADS_UP_EXCLUDE_FROM_LOCK_SCREEN,
                    (Boolean) newValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    private void updateHeadsUpSnoozeTimeSummary(int value) {
        String summary = value != 0
                ? getResources().getString(R.string.heads_up_snooze_summary, value / 60 / 1000)
                : getResources().getString(R.string.heads_up_snooze_disabled_summary);
        mHeadsUpSnoozeTime.setSummary(summary);
    }

    private void updateHeadsUpTimeOutSummary(int value) {
        String summary = getResources().getString(R.string.heads_up_time_out_summary,
                value / 1000);
        if (value == 0) {
            mHeadsUpTimeOut.setSummary(
                    getResources().getString(R.string.heads_up_time_out_never_summary));
        } else {
            mHeadsUpTimeOut.setSummary(summary);
        }
    }
}
