package com.google.android.settings.localepicker;

import com.android.internal.app.LocaleStore;

public interface LocaleSelectedListener {
    void onLocaleSelected(LocaleStore.LocaleInfo localeInfo);
}
