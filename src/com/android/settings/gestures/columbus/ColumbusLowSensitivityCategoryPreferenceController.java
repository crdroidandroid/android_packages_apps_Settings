package com.android.settings.gestures.columbus;

import android.content.Context;
import com.android.settings.core.BasePreferenceController;
import android.os.SystemProperties;

public class ColumbusLowSensitivityCategoryPreferenceController extends BasePreferenceController {
    public ColumbusLowSensitivityCategoryPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return SystemProperties.getBoolean("persist.columbus.use_ap_sensor", true) ? UNSUPPORTED_ON_DEVICE : AVAILABLE;
    }
}