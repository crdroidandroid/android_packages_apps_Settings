package com.google.android.settings.localepicker;

import android.R;
import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

import com.google.android.setupdesign.util.ItemStyler;
import com.google.android.setupdesign.util.ThemeHelper;

public class ExpressivePreferenceCategory extends PreferenceCategory {
    private boolean mShouldApplyGlifExpressiveStyle;

    public ExpressivePreferenceCategory(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mShouldApplyGlifExpressiveStyle = ThemeHelper.shouldApplyGlifExpressiveStyle(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        if (this.mShouldApplyGlifExpressiveStyle) {
            ItemStyler.applyPartnerCustomizationLayoutMarginStyle(
                    preferenceViewHolder.findViewById(R.id.title));
        }
    }
}
