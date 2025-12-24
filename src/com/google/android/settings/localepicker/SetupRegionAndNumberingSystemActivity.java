package com.google.android.settings.localepicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.internal.app.LocaleStore;

import com.google.android.setupdesign.GlifRecyclerLayout;
import com.google.android.setupdesign.items.RecyclerItemAdapter;
import com.google.android.setupdesign.util.ThemeHelper;
import com.android.settings.R;

import java.io.Serializable;

public class SetupRegionAndNumberingSystemActivity extends Activity {
    private RecyclerItemAdapter mAdapter;
    private boolean mIsNumberingMode;
    private LocaleStore.LocaleInfo mLocaleInfo;

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
        setContentView(R.layout.setup_region_and_numbering_system_picker);
        setupContent();
    }

    private void setupContent() {
        String string;
        Bundle extras = getIntent().getExtras();
        mLocaleInfo = (LocaleStore.LocaleInfo) extras.getSerializable("extra_target_locale");
        mIsNumberingMode = extras.getBoolean("extra_is_numbering_system");
        GlifRecyclerLayout glifRecyclerLayout =
                (GlifRecyclerLayout)
                        findViewById(R.id.setup_wizard_region_and_numbering_system_picker_layout);
        if (mIsNumberingMode) {
            string = mLocaleInfo.getFullNameNative();
        } else {
            string =
                    getApplicationContext()
                            .getString(com.android.settings.R.string.region_picker_title);
        }
        String string2 =
                getApplicationContext()
                        .getString(com.android.settings.R.string.region_picker_sub_title);
        if (mIsNumberingMode) {
            string2 = "";
        }
        glifRecyclerLayout.setHeaderText(string);
        glifRecyclerLayout.setDescriptionText(string2);
        mAdapter = (RecyclerItemAdapter) glifRecyclerLayout.getAdapter();
        LocaleSelectedListener localeSelectedListener =
                new LocaleSelectedListener() {
                    @Override
                    public void onLocaleSelected(LocaleStore.LocaleInfo localeInfo) {
                        Intent intent = new Intent();
                        intent.putExtra("localeInfo", (Serializable) localeInfo);
                        setResult(-1, intent);
                        finish();
                    }
                };
        SetupLocalePickerListController setupLocalePickerListController =
                new SetupLocalePickerListController(
                        getApplicationContext(), this, mLocaleInfo, mIsNumberingMode);
        setupLocalePickerListController.setLocaleSelectedListener(localeSelectedListener);
        setupLocalePickerListController.displayScreen(mAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == -1) {
            if (data != null) {
                LocaleStore.LocaleInfo serializableExtra =
                        (LocaleStore.LocaleInfo) data.getSerializableExtra("localeInfo");
                Intent intent2 = new Intent();
                intent2.putExtra("localeInfo", (Serializable) serializableExtra);
                setResult(-1, intent2);
            }
            finish();
        }
    }
}
