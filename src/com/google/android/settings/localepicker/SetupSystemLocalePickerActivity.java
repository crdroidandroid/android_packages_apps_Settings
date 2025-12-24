package com.google.android.settings.localepicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ScrollView;

import com.android.internal.app.LocaleStore;

import com.google.android.setupdesign.GlifRecyclerLayout;
import com.google.android.setupdesign.items.RecyclerItemAdapter;
import com.google.android.setupdesign.template.HeaderMixin;
import com.google.android.setupdesign.util.ThemeHelper;
import com.android.settings.R;

import java.io.Serializable;

public class SetupSystemLocalePickerActivity extends Activity {
    private RecyclerItemAdapter mAdapter;

    @Override
    protected void onCreate(Bundle bundle) {
        applySuwTheme();
        super.onCreate(bundle);
        setupLayout();
    }

    private void applySuwTheme() {
        setTheme(R.style.Theme_Settings_LocalePicker);
        if (ThemeHelper.shouldApplyGlifExpressiveStyle(getApplicationContext())) {
            if (ThemeHelper.trySetSuwTheme(this)) {
                return;
            }
            setTheme(ThemeHelper.getSuwDefaultTheme(getApplicationContext()));
            ThemeHelper.trySetDynamicColor(this);
            return;
        }
        ThemeHelper.applyTheme(this);
        ThemeHelper.trySetDynamicColor(this);
    }

    private void setupLayout() {
        setContentView(R.layout.setup_language_picker);
        setupContent();
    }

    private void setupContent() {
        GlifRecyclerLayout glifRecyclerLayout =
                (GlifRecyclerLayout) findViewById(R.id.setup_wizard_language_picker_layout);
        ((HeaderMixin) glifRecyclerLayout.getMixin(HeaderMixin.class))
                .getTextView()
                .setVisibility(8);
        this.mAdapter = (RecyclerItemAdapter) glifRecyclerLayout.getAdapter();
        LocaleSelectedListener localeSelectedListener =
                new LocaleSelectedListener() {
                    @Override
                    public void onLocaleSelected(LocaleStore.LocaleInfo localeInfo) {
                        int i =
                                Settings.Global.getInt(
                                        getApplicationContext().getContentResolver(),
                                        "is_suggested_locale",
                                        0);
                        Intent intent = new Intent();
                        intent.putExtra("localeInfo", (Serializable) localeInfo);
                        intent.putExtra("EXTRA_IS_SUGGESTED_LOCALE", i);
                        setResult(-1, intent);
                        finish();
                    }
                };
        ScrollView scrollView =
                (ScrollView)
                        findViewById(com.google.android.setupdesign.R.id.sud_header_scroll_view);
        if (scrollView != null) {
            scrollView.setContentDescription(
                    getApplicationContext().getString(R.string.language_picker_page));
        }
        SetupLocalePickerListController setupLocalePickerListController =
                new SetupLocalePickerListController(getApplicationContext(), this, null, false);
        setupLocalePickerListController.setLocaleSelectedListener(localeSelectedListener);
        String string = getIntent().getExtras().getString("extra_psku");
        if (string == null) {
            Log.d("SetupSystemLocalePickerActivity", "No suggested PSKU");
            string = "";
        }
        setupLocalePickerListController.setPsku(string);
        setupLocalePickerListController.displayScreen(this.mAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == -1) {
            if (data != null) {
                int i3 =
                        Settings.Global.getInt(
                                getApplicationContext().getContentResolver(),
                                "is_suggested_locale",
                                0);
                LocaleStore.LocaleInfo serializableExtra =
                        (LocaleStore.LocaleInfo) data.getSerializableExtra("localeInfo");
                Intent intent = new Intent();
                intent.putExtra(
                        "EXTRA_SELECTED_LOCALE", serializableExtra.getLocale().toLanguageTag());
                intent.putExtra("EXTRA_IS_SUGGESTED_LOCALE", i3);
                setResult(-1, intent);
            }
            finish();
        }
    }
}
