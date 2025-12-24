package com.google.android.settings.localepicker;

import android.content.ContentResolver;
import android.provider.Settings;
import android.text.TextUtils;

import com.google.common.base.Splitter;

import java.util.HashSet;
import java.util.Set;

public abstract class AccessibilityStateUtils {
    private static void setEnabledAccessibilityServices(ContentResolver contentResolver, Set set) {
        String string =
                Settings.Secure.getString(contentResolver, "enabled_accessibility_services");
        if (TextUtils.isEmpty(string)) {
            return;
        }
        for (String str : Splitter.on(':').split(string)) {
            if (str.startsWith("com.google.") || str.startsWith("com.googlecode.")) {
                if (str.endsWith("TalkBackService")) {
                    set.add("TalkBackService");
                }
                if (str.endsWith("SelectToSpeakService")) {
                    set.add("SelectToSpeakService");
                }
            }
        }
    }

    public static boolean isTtsEnabled(ContentResolver contentResolver) {
        HashSet hashSet = new HashSet();
        setEnabledAccessibilityServices(contentResolver, hashSet);
        return hashSet.contains("TalkBackService") || hashSet.contains("SelectToSpeakService");
    }
}
