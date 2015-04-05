package com.android.settings.crdroid;

import android.os.Bundle;
import android.content.ContentResolver;
import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.widget.Toast;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import com.android.internal.logging.MetricsLogger;

public class ScreenAndAnimations extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "ScreenAndAnimations";

    private static final String KEY_TOAST_ANIMATION = "toast_animation";

    private Context mContext;

    private ListPreference mToastAnimation;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.screen_and_animations);

        ContentResolver resolver = getActivity().getContentResolver();

        mContext = getActivity().getApplicationContext();

        // Toast Animations
        mToastAnimation = (ListPreference) findPreference(KEY_TOAST_ANIMATION);
        mToastAnimation.setSummary(mToastAnimation.getEntry());
        int CurrentToastAnimation = Settings.System.getInt(resolver,
                Settings.System.TOAST_ANIMATION, 1);
        mToastAnimation.setValueIndex(CurrentToastAnimation); //set to index of default value
        mToastAnimation.setSummary(mToastAnimation.getEntries()[CurrentToastAnimation]);
        mToastAnimation.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (preference == mToastAnimation) {
            int index = mToastAnimation.findIndexOfValue((String) objValue);
            Settings.System.putString(getContentResolver(), Settings.System.TOAST_ANIMATION, (String) objValue);
            mToastAnimation.setSummary(mToastAnimation.getEntries()[index]);
            Toast.makeText(mContext, "Toast Test", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
