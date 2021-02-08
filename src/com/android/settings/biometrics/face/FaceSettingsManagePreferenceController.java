package com.android.settings.biometrics.face;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import com.android.settings.custom.biometrics.FaceUtils;

public class FaceSettingsManagePreferenceController extends BasePreferenceController {

    public static final String KEY = "security_settings_face_manage_category";

    public FaceSettingsManagePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    public FaceSettingsManagePreferenceController(Context context) {
        this(context, KEY);
    }

    @Override
    public int getAvailabilityStatus() {
        return FaceUtils.isFaceUnlockSupported() ? UNSUPPORTED_ON_DEVICE : AVAILABLE;
    }
}