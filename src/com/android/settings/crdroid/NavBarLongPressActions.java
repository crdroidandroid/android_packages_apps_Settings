package com.android.settings.crdroid;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NavBarLongPressActions extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "NavBarLongPressActions";

    private static final String PREF_RECENT_LONG_PRESS = "recent_long_press";
    
    ListPreference mRecentLongPress;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.navbar_long_press_actions);
        PreferenceScreen prefScreen = getPreferenceScreen();

        mRecentLongPress = (ListPreference) findPreference(PREF_RECENT_LONG_PRESS);
        int longPress = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.NAVBAR_RECENT_LONG_PRESS, 0, UserHandle.USER_CURRENT);
        mRecentLongPress.setValue(String.valueOf(longPress));
        mRecentLongPress.setOnPreferenceChangeListener(this);
        updateLongPressMode(longPress);

    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mRecentLongPress) {
            int longPress = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NAVBAR_RECENT_LONG_PRESS,
                    longPress, UserHandle.USER_CURRENT);
            updateLongPressMode(longPress);
            return true;
        }

        return false;
    }

    private void updateLongPressMode(int value) {
        ContentResolver cr = getContentResolver();
        Resources res = getResources();
        int summary = -1;

        Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, value);

        if (value == 0) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 0);
            summary = R.string.recent_long_press_none;
        } else if (value == 1) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 1);
            summary = R.string.recent_long_press_last_app;
        } else if (value == 2) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 2);
            summary = R.string.recent_long_press_screenshot;
        } else if (value == 3) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 3);
            summary = R.string.recent_long_press_kill_app;
        } else if (value == 4) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 4);
            summary = R.string.recent_long_press_notif_panel;
        } else if (value == 5) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 5);
            summary = R.string.recent_long_press_qs_panel;
        } else if (value == 6) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 6);
            summary = R.string.recent_long_press_power_menu;
        }

        if (mRecentLongPress != null && summary != -1) {
            mRecentLongPress.setSummary(res.getString(summary));
        }
    }
}
