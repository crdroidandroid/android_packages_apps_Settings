package com.google.android.settings.localepicker;

import android.content.Context;
import android.util.AttributeSet;

import com.android.internal.app.LocaleStore;

import com.google.android.setupdesign.items.Item;

public class LocaleItem extends Item {
    private boolean mIsSuggested;
    private LocaleStore.LocaleInfo mLocaleInfo;

    public LocaleItem() {
        this.mIsSuggested = false;
    }

    public LocaleItem(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsSuggested = false;
    }

    public void setLocaleInfo(LocaleStore.LocaleInfo localeInfo) {
        this.mLocaleInfo = localeInfo;
    }

    public LocaleStore.LocaleInfo getLocaleInfo() {
        return this.mLocaleInfo;
    }

    public void setSuggestedState(boolean z) {
        this.mIsSuggested = z;
    }

    public boolean isSuggested() {
        return this.mIsSuggested;
    }
}
