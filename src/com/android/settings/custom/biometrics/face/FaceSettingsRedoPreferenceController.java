package com.android.settings.custom.biometrics.face;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.face.Face;
import android.hardware.face.FaceManager;
import android.util.Log;
import android.widget.Toast;
import androidx.preference.Preference;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.biometrics.face.FaceSettings;
import com.android.settings.custom.biometrics.FaceUtils;
import java.util.List;

public class FaceSettingsRedoPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceClickListener {
    static final String KEY = "security_settings_face_redo_face_scan";
    private static final String TAG = "FaceSettings/Redo";
    private SettingsActivity mActivity;
    private final Context mContext;
    private final FaceManager mFaceManager;
    private final FaceManager.RemovalCallback mRemovalCallback;
    private int mUserId;

    @Override 
    public String getPreferenceKey() {
        return KEY;
    }

    public FaceSettingsRedoPreferenceController(Context context, String str) {
        super(context, str);
        mRemovalCallback = new FaceManager.RemovalCallback() {
            @Override
            public void onRemovalError(Face face, int i, CharSequence charSequence) {
                Log.e(FaceSettingsRedoPreferenceController.TAG, "Unable to remove face: " + face.getBiometricId() + " error: " + i + " " + ((Object) charSequence));
                Toast.makeText(mContext, charSequence, 0).show();
            }

            @Override
            public void onRemovalSucceeded(Face face, int i) {
                if (i == 0) {
                    Log.v(FaceSettingsRedoPreferenceController.TAG, "onRemovalSucceeded ");
                    Intent intent = new Intent("com.android.settings.intent.action.FACE_ENROLL");
                    intent.putExtra("for_face", true);
                    intent.putExtra("for_redo", true);
                    intent.addFlags(268435456);
                    mContext.startActivity(intent);
                    return;
                }
                Log.v(FaceSettingsRedoPreferenceController.TAG, "Remaining: " + i);
            }
        };
        mContext = context;
        mFaceManager = (FaceManager) context.getSystemService(FaceManager.class);
    }

    public FaceSettingsRedoPreferenceController(Context context) {
        this(context, KEY);
    }

    public void setUserId(int i) {
        mUserId = i;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!FaceSettings.isFaceHardwareDetected(mContext) ||
              !mFaceManager.hasEnrolledTemplates(mUserId)) {
            preference.setEnabled(false);
        } else {
            preference.setEnabled(true);
            preference.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return FaceUtils.isFaceUnlockSupported() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        showFaceRedoWarningDialog();
        return true;
    }

    public void setActivity(SettingsActivity settingsActivity) {
        mActivity = settingsActivity;
    }

    private void deleteFace() {
        List enrolledFaces = mFaceManager.getEnrolledFaces(mUserId);
        if (enrolledFaces.isEmpty()) {
            Log.e(TAG, "No faces");
            return;
        }
        if (enrolledFaces.size() > 1) {
            Log.e(TAG, "Multiple enrollments: " + enrolledFaces.size());
        }
        mFaceManager.remove((Face) enrolledFaces.get(0), mUserId, mRemovalCallback);
    }

    void showFaceRedoWarningDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.security_settings_face_unlock_redo_face_scan_title)
            .setMessage(R.string.face_redo_warning_msg)
            .setPositiveButton(R.string.face_redo_confirm_btn, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    deleteFace();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                }
            });
        builder.create().show();
    }
}
