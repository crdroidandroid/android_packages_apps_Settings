package com.google.android.settings.localepicker;

import android.content.Context;
import android.os.LocaleList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.app.LocaleCollectorBase;
import com.android.internal.app.LocaleStore;
import com.android.internal.app.SystemLocaleCollector;

public class SystemLocaleSuggestedListPreferenceController
        extends LocalePickerBaseListPreferenceController {
    private static final String KEY_PREFERENCE_CATEGORY_ADD_A_LANGUAGE_SUGGESTED =
            "system_language_suggested_category";
    private static final String KEY_PREFERENCE_SYSTEM_LOCALE_SUGGESTED_LIST =
            "system_locale_suggested_list";
    @Nullable private LocaleStore.LocaleInfo mLocaleInfo;
    private boolean mIsNumberingSystemMode;

    @Override
    protected @Nullable LocaleList getExplicitLocaleList() {
        return null;
    }

    public SystemLocaleSuggestedListPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    public SystemLocaleSuggestedListPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull LocaleStore.LocaleInfo parentLocale,
            boolean isNumberingSystemMode) {
        super(context, preferenceKey);
        this.mLocaleInfo = parentLocale;
        this.mIsNumberingSystemMode = isNumberingSystemMode;
    }

    @Override
    protected @NonNull String getPreferenceCategoryKey() {
        return KEY_PREFERENCE_CATEGORY_ADD_A_LANGUAGE_SUGGESTED;
    }

    @Override
    public @NonNull String getPreferenceKey() {
        return KEY_PREFERENCE_SYSTEM_LOCALE_SUGGESTED_LIST;
    }

    @Override
    protected LocaleCollectorBase getLocaleCollectorController(Context context) {
        return new SystemLocaleCollector(context, getExplicitLocaleList());
    }

    @Override
    protected @Nullable LocaleStore.LocaleInfo getParentLocale() {
        return this.mLocaleInfo;
    }

    @Override
    protected boolean isNumberingMode() {
        return this.mIsNumberingSystemMode;
    }
}
