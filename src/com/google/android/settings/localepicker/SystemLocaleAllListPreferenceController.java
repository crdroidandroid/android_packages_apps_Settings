package com.google.android.settings.localepicker;

import android.content.Context;
import android.os.LocaleList;

import com.android.internal.app.LocaleCollectorBase;
import com.android.internal.app.LocaleStore;
import com.android.internal.app.SystemLocaleCollector;

public class SystemLocaleAllListPreferenceController
        extends LocalePickerBaseListPreferenceController {
    private static final String KEY_PREFERENCE_CATEGORY_ADD_LANGUAGE_ALL_SUPPORTED =
            "system_language_all_supported_category";
    private static final String KEY_PREFERENCE_SYSTEM_LOCALE_LIST = "system_locale_list";
    private LocaleList mExplicitLocales;
    private boolean mIsNumberingSystemMode;
    private LocaleStore.LocaleInfo mLocaleInfo;

    public SystemLocaleAllListPreferenceController(Context context, String str) {
        super(context, str);
    }

    public SystemLocaleAllListPreferenceController(
            Context context, String str, LocaleStore.LocaleInfo localeInfo, boolean z) {
        super(context, str);
        this.mLocaleInfo = localeInfo;
        this.mIsNumberingSystemMode = z;
    }

    public SystemLocaleAllListPreferenceController(
            Context context, String str, LocaleList localeList) {
        super(context, str);
        this.mExplicitLocales = localeList;
    }

    @Override
    protected String getPreferenceCategoryKey() {
        return KEY_PREFERENCE_CATEGORY_ADD_LANGUAGE_ALL_SUPPORTED;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PREFERENCE_SYSTEM_LOCALE_LIST;
    }

    @Override
    protected LocaleCollectorBase getLocaleCollectorController(Context context) {
        return new SystemLocaleCollector(context, getExplicitLocaleList());
    }

    @Override
    protected LocaleStore.LocaleInfo getParentLocale() {
        return this.mLocaleInfo;
    }

    @Override
    protected boolean isNumberingMode() {
        return this.mIsNumberingSystemMode;
    }

    @Override
    protected LocaleList getExplicitLocaleList() {
        return this.mExplicitLocales;
    }
}
