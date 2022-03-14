/*
 * Copyright (C) 2022 Project Kaleidoscope
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

package com.android.settings.wifi.tether.preference;

import android.content.Context;
import android.widget.SeekBar;
import android.util.AttributeSet;
import android.view.View;

import com.android.settings.R;
import com.android.settings.SeekBarDialogPreference;

public class WifiTetherClientLimitPreference extends SeekBarDialogPreference implements
        SeekBar.OnSeekBarChangeListener {

    private Context mContext;
    private SeekBar mSeekBar;
    private int mValue;
    private int mMin;
    private int mMax;

    public WifiTetherClientLimitPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        setText(getSummaryForValue(progress + mMin));
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = getSeekBar(view);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(mMax - mMin);
        mSeekBar.setProgress(mValue - mMin);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            setValue(mSeekBar.getProgress() + mMin, true);
        }
    }

    private String getSummaryForValue(int value) {
        return mContext.getResources().getQuantityString(
            R.plurals.wifi_hotspot_client_limit_summary, value, value);
    }

    public void setMin(int min) {
        mMin = min;
    }

    public void setMax(int max) {
        mMax = max;
    }

    public void setValue(int value, boolean callListener) {
        if (value == 0) value = mMax;
        mValue = value;
        String summary = getSummaryForValue(value);
        setSummary(summary);
        setText(summary);
        if (callListener) callChangeListener(value);
    }

    public int getValue() {
        return mValue;
    }
}
