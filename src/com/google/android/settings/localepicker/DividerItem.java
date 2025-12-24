package com.google.android.settings.localepicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.setupdesign.R;
import com.google.android.setupdesign.items.AbstractItem;
import com.google.android.setupdesign.util.LayoutStyler;
import com.google.android.setupdesign.util.ThemeHelper;

public class DividerItem extends AbstractItem {
    private boolean mEnabled;
    private int mLayoutRes;
    private boolean mShouldApplyGlifExpressiveStyle;
    private boolean mVisible;

    @Override
    public boolean isGroupDivider() {
        return true;
    }

    public DividerItem() {
        this.mEnabled = true;
        this.mVisible = true;
        this.mLayoutRes = getDefaultLayoutResource();
    }

    public DividerItem(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mEnabled = true;
        this.mVisible = true;
        this.mShouldApplyGlifExpressiveStyle = ThemeHelper.shouldApplyGlifExpressiveStyle(context);
        TypedArray obtainStyledAttributes =
                context.obtainStyledAttributes(attributeSet, R.styleable.SudItem);
        this.mEnabled =
                obtainStyledAttributes.getBoolean(R.styleable.SudItem_android_enabled, true);
        this.mLayoutRes =
                obtainStyledAttributes.getResourceId(
                        R.styleable.SudItem_android_layout, getDefaultLayoutResource());
        this.mVisible =
                obtainStyledAttributes.getBoolean(R.styleable.SudItem_android_visible, true);
        obtainStyledAttributes.recycle();
    }

    protected int getDefaultLayoutResource() {
        return com.android.settings.R.layout.divider_item;
    }

    @Override
    public int getCount() {
        return isVisible() ? 1 : 0;
    }

    @Override
    public boolean isEnabled() {
        return this.mEnabled;
    }

    @Override
    public int getLayoutResource() {
        return this.mLayoutRes;
    }

    public void setVisible(boolean z) {
        if (this.mVisible == z) {
            return;
        }
        this.mVisible = z;
        if (!z) {
            notifyItemRangeRemoved(0, 1);
        } else {
            notifyItemRangeInserted(0, 1);
        }
    }

    public boolean isVisible() {
        return this.mVisible;
    }

    @Override
    public void onBindView(View view) {
        if (this.mShouldApplyGlifExpressiveStyle) {
            return;
        }
        LayoutStyler.applyPartnerCustomizationLayoutPaddingStyle(view);
    }
}
