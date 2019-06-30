/*
 * Copyright (C) 2018 Projekt Substratum
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

package com.android.settings.development;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ForceAuthorizeSubstratumPreferenceController extends
        DeveloperOptionsPreferenceController implements
        Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES = "force_authorize_substratum_packages";

    final static int DISABLE_FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES = 0;
    final static int ENABLE_FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES = 1;

    public ForceAuthorizeSubstratumPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean mForceAuthorizeSubstratumPackages = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES,
                mForceAuthorizeSubstratumPackages ? ENABLE_FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES : DISABLE_FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int mForceAuthorizeSubstratumPackages = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES, 0);

        ((SwitchPreference) mPreference).setChecked(mForceAuthorizeSubstratumPackages != DISABLE_FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES, DISABLE_FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES);
        ((SwitchPreference) mPreference).setChecked(false);
    }
}
