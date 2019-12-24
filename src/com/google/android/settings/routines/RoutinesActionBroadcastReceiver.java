package com.google.android.settings.routines;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

public class RoutinesActionBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "com.google.android.settings.routines.RoutinesActionBroadcastReceiver";

    public void onReceive(Context context, Intent intent) {
        if ("com.google.android.settings.routines.RoutinesActionBroadcastReceiver.RINGER_MODE_SILENCE_ACTION".equals(intent.getAction())) {
            handleRingerModeSilenceAction(context);
            return;
        }
        String str = TAG;
        Log.w(str, "Unknown action for RoutinesActionBroadcastReceiver: " + intent.getAction());
    }

    private void handleRingerModeSilenceAction(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(AudioManager.class);
        if (audioManager == null) {
            Log.d(TAG, "AudioManager was null. Not able to handleRingerModeSilenceAction.");
        } else {
            audioManager.setRingerModeInternal(0);
        }
    }
}
