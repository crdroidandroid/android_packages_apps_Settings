package com.android.settings.custom.biometrics.face;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.android.settings.biometrics.face.FaceEnrollIntroduction;
import com.android.settings.custom.biometrics.FaceUtils;

public class FaceEnrollActivity extends Activity {
    private byte[] mToken;
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (FaceUtils.isFaceUnlockSupported()) {
            if (mToken == null) {
                mToken = getIntent().getByteArrayExtra("hw_auth_token");
            }
            Intent faceIntroIntent = getFaceIntroIntent();
            faceIntroIntent.putExtra("for_redo", getIntent().getBooleanExtra("for_redo", false));
            faceIntroIntent.putExtra("hw_auth_token", mToken);
            if (getCallingActivity() != null) {
                faceIntroIntent.setFlags(33554432);
            }
            startActivity(faceIntroIntent);
        }
        finish();
    }

    private Intent getFaceIntroIntent() {
        Intent intent = new Intent(this, FaceEnrollIntroduction.class);
        intent.addFlags(268468224);
        return intent;
    }
}
