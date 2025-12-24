package com.google.android.settings.localepicker;

import android.os.Bundle;

import com.android.settings.SettingsActivity;
import com.android.settings.SetupWizardUtils;

import com.google.android.setupdesign.util.ThemeHelper;
import com.android.settings.R;

public class RegionSearchActivity extends SettingsActivity {
    @Override
    protected boolean isValidFragment(String str) {
        return RegionSearchFragment.class.getName().equals(str);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.locale_picker);
        applySuwTheme();
    }

    private void applySuwTheme() {
        if (ThemeHelper.shouldApplyGlifExpressiveStyle(this)) {
            setTheme(com.android.settings.R.style.SettingsPreferenceTheme_SetupWizard);
            ThemeHelper.trySetSuwTheme(this);
        } else {
            setTheme(SetupWizardUtils.getTheme(this, getIntent()));
            setTheme(com.android.settings.R.style.SettingsPreferenceTheme_SetupWizard);
            ThemeHelper.trySetDynamicColor(this);
        }
    }
}
