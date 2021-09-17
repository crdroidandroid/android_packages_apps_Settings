/*
Copyright (c) 2021 The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.android.settings.deviceinfo;

import android.content.Context;
import android.telephony.ims.ImsMmTelManager;
import android.util.Log;

import com.android.ims.FeatureConnector;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;

public class ImsConnector implements FeatureConnector.Listener<ImsManager> {
    private static final String TAG = ImsConnector.class.getSimpleName();

    private Context mContext;
    private int mSlotId;
    private FeatureConnector<ImsManager> mConnector;
    private ImsMmTelManager.RegistrationCallback mRegistrationCallback;
    private ImsManager mImsManager;

    public ImsConnector(Context context, int slotId,
                         ImsMmTelManager.RegistrationCallback callback) {
        mContext = context.getApplicationContext();
        mSlotId = slotId;
        mRegistrationCallback = callback;
        mConnector = ImsManager.getConnector(mContext, mSlotId, TAG, this,
                mContext.getMainExecutor());
    }

    public void connect() {
        if (null != mConnector) {
            mConnector.connect();
        }
    }

    public void disconnect() {
        if (null != mConnector) {
            mConnector.disconnect();
        }
    }

    @Override
    public void connectionReady(ImsManager manager) throws ImsException {
        mImsManager = manager;
        registerListener();
    }

    @Override
    public void connectionUnavailable(int reason) {
        unregisterListener();
    }

    private void registerListener() {
        if (null == mImsManager) {
            Log.e(TAG, "registerListener: mImsManager is null");
            return;
        }

        try {
            mImsManager.addRegistrationCallback(mRegistrationCallback,
                    mContext.getMainExecutor());
            Log.d(TAG, "registerListener: add callback for mSlotId = " + mSlotId
                    + " mImsManager = " + mImsManager);
        } catch (ImsException e) {
            Log.e(TAG, "registerListener: ", e);
        }
    }

    private void unregisterListener() {
        if (null == mImsManager) {
            Log.e(TAG, "unregisterListener: mImsManager is null");
            return;
        }

        mImsManager.removeRegistrationListener(mRegistrationCallback);
        Log.d(TAG, "unregisterListener: remove ims registration callback for mSlotId = "
                + mSlotId + " mImsManager = " + mImsManager);
    }
}