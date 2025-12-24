package com.google.android.settings.localepicker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.LocaleList;
import android.util.Log;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;

public class LocalePickerSetupWizardEventsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(
                intent.getAction(), "com.google.android.setupwizard.SETUP_WIZARD_FINISHED")) {
            Locale locale = LocaleList.getDefault().get(0);
            if (locale.getCountry().isEmpty()) {
                for (LocaleStore.LocaleInfo localeInfo :
                        LocaleStore.getLevelLocales(
                                context,
                                new HashSet<String>(),
                                (LocaleStore.LocaleInfo) null,
                                true)) {
                    if (sameLanguageAndScript(localeInfo.getLocale(), locale)) {
                        LocalePicker.updateLocales(
                                new LocaleList(
                                        ((LocaleStore.LocaleInfo)
                                                        LocaleStore.getLevelLocales(
                                                                        context,
                                                                        new HashSet<String>(),
                                                                        localeInfo,
                                                                        true)
                                                                .iterator()
                                                                .next())
                                                .getLocale()));
                        Log.d("SetupWizardEventsReceiver", "Update region");
                        return;
                    }
                }
            }
        }
    }

    private static boolean sameLanguageAndScript(Locale locale, Locale locale2) {
        String language = locale.getLanguage();
        String language2 = locale2.getLanguage();
        String script = locale.getScript();
        String script2 = locale2.getScript();
        if (!language.equals(language2)) {
            return false;
        }
        if (script.isEmpty() || script2.isEmpty()) {
            return true;
        }
        return script.equals(script2);
    }
}
