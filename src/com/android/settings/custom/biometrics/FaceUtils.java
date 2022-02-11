package com.android.settings.custom.biometrics;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.util.custom.faceunlock.FaceUnlockUtils;

public final class FaceUtils {
    private static final String TAG = "FaceUtils";

    public static boolean isFaceUnlockSupported() {
        return FaceUnlockUtils.isFaceUnlockSupported();
    }

    public static boolean isFaceDisabledByAdmin(Context context) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        try {
            if (devicePolicyManager.getPasswordQuality(null) > 32768) {
                return true;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "isFaceDisabledByAdmin error:", e);
        }
        if ((devicePolicyManager.getKeyguardDisabledFeatures(null) & 128) != 0) {
            return true;
        }
        return false;
    }
}
