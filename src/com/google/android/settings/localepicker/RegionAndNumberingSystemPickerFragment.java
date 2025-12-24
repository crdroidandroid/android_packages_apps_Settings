package com.google.android.settings.localepicker;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.app.LocaleStore;
import com.android.settings.SetupWizardUtils;
import com.android.settings.accessibility.AccessibilitySetupWizardUtils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;

import com.google.android.setupdesign.GlifPreferenceLayout;
import com.google.android.setupdesign.template.HeaderMixin;
import com.google.android.setupdesign.util.ThemeHelper;
import com.google.common.collect.ImmutableList;
import com.android.settings.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RegionAndNumberingSystemPickerFragment extends DashboardFragment {
    public static final String EXTRA_TARGET_LOCALE = "extra_target_locale";
    public static final String EXTRA_IS_NUMBERING_SYSTEM = "extra_is_numbering_system";
    private static final ImmutableList REGION_SEARCH_SUPPORTED_LANGUAGES =
            ImmutableList.builder()
                    .add((Object) "ar")
                    .add((Object) "en")
                    .add((Object) "es")
                    .add((Object) "fr")
                    .build();
    private static final String TAG = "RegionAndNumberingSystemPickerFragment";
    private static final String KEY_PREFERENCE_SYSTEM_LOCALE_LIST = "system_locale_list";
    private static final String KEY_PREFERENCE_SYSTEM_LOCALE_SUGGESTED_LIST =
            "system_locale_suggested_list";
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.suw_language_region_picker);
    private Activity mActivity;
    private boolean mIsNumberingMode;
    private LocaleStore.LocaleInfo mLocaleInfo;
    private SystemLocaleSuggestedListPreferenceController mSuggestedListPreferenceController;
    private SystemLocaleAllListPreferenceController mSystemLocaleAllListPreferenceController;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.REGION_PICKER_IN_SUW;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mActivity = getActivity();
        if (mActivity == null || mActivity.isFinishing()) {
            Log.d(TAG, "onCreate, no activity or activity is finishing");
            return;
        }
        applySuwTheme();
        ((DividerPreference) getPreferenceScreen().findPreference("category_divider"))
                .setVisible(false);
        if (mLocaleInfo == null) {
            Log.d(TAG, "onCreate, can not get localeInfo");
        } else if (mIsNumberingMode || !isRegionSearchSupported()) {
            ((SearchBarPreference) getPreferenceScreen().findPreference("search_bar_button"))
                    .setVisible(false);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((SearchBarPreferenceController) use(SearchBarPreferenceController.class))
                .setFragment(this);
        ((SearchBarPreferenceController) use(SearchBarPreferenceController.class))
                .setLocaleInfo(mLocaleInfo);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container,
            @NonNull Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String string;
        super.onViewCreated(view, savedInstanceState);
        if (view instanceof GlifPreferenceLayout) {
            GlifPreferenceLayout glifPreferenceLayout = (GlifPreferenceLayout) view;
            String string2 =
                    getContext().getString(com.android.settings.R.string.region_picker_sub_title);
            boolean z = mIsNumberingMode;
            if (z) {
                string2 = null;
            }
            if (z) {
                string = mLocaleInfo.getFullNameNative();
            } else {
                string = getContext().getString(com.android.settings.R.string.region_picker_title);
            }
            Drawable drawable = getContext().getDrawable(R.drawable.ic_suw_region_picker);
            ((HeaderMixin) glifPreferenceLayout.getMixin(HeaderMixin.class))
                    .getTextView()
                    .setVisibility(0);
            AccessibilitySetupWizardUtils.updateGlifPreferenceLayout(
                    getContext(), glifPreferenceLayout, string, string2, drawable);
        }
    }

    @Override
    public RecyclerView onCreateRecyclerView(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        if (container instanceof GlifPreferenceLayout) {
            return ((GlifPreferenceLayout) container)
                    .onCreateRecyclerView(inflater, container, savedInstanceState);
        }
        return super.onCreateRecyclerView(inflater, container, savedInstanceState);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.suw_language_region_picker;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    private List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        Bundle args = getArguments();
        mLocaleInfo = (LocaleStore.LocaleInfo) args.getSerializable(EXTRA_TARGET_LOCALE);
        mIsNumberingMode = args.getBoolean(EXTRA_IS_NUMBERING_SYSTEM);
        mSuggestedListPreferenceController =
                new SystemLocaleSuggestedListPreferenceController(
                        context,
                        KEY_PREFERENCE_SYSTEM_LOCALE_SUGGESTED_LIST,
                        mLocaleInfo,
                        mIsNumberingMode);
        mSystemLocaleAllListPreferenceController =
                new SystemLocaleAllListPreferenceController(
                        context, KEY_PREFERENCE_SYSTEM_LOCALE_LIST, mLocaleInfo, mIsNumberingMode);
        LocaleSelectedListener localeSelectedListener =
                new LocaleSelectedListener() {
                    @Override
                    public void onLocaleSelected(LocaleStore.LocaleInfo localeInfo) {
                        Intent intent = new Intent();
                        intent.putExtra("localeInfo", (Serializable) localeInfo);
                        mActivity.setResult(-1, intent);
                        mActivity.finish();
                    }
                };
        mSuggestedListPreferenceController.setLocaleSelectedListener(localeSelectedListener);
        mSystemLocaleAllListPreferenceController.setLocaleSelectedListener(localeSelectedListener);
        mSuggestedListPreferenceController.setFragment(this);
        mSystemLocaleAllListPreferenceController.setFragment(this);
        controllers.add(mSuggestedListPreferenceController);
        controllers.add(mSystemLocaleAllListPreferenceController);
        return controllers;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mActivity != null && requestCode == 0 && resultCode == -1) {
            if (data != null) {
                Serializable serializable =
                        (LocaleStore.LocaleInfo) data.getSerializableExtra("localeInfo");
                Intent intent2 = new Intent();
                intent2.putExtra("localeInfo", serializable);
                mActivity.setResult(-1, intent2);
            }
            mActivity.finish();
        }
    }

    private boolean isRegionSearchSupported() {
        String languageTag = mLocaleInfo.getLocale().toLanguageTag();
        if (REGION_SEARCH_SUPPORTED_LANGUAGES.contains(languageTag)) {
            return true;
        }
        Log.d(TAG, "Region search is not supported for " + languageTag);
        return false;
    }

    private void applySuwTheme() {
        if (ThemeHelper.shouldApplyGlifExpressiveStyle(mActivity)) {
            mActivity.setTheme(com.android.settings.R.style.SettingsPreferenceTheme_SetupWizard);
            ThemeHelper.trySetSuwTheme(mActivity);
        } else {
            Activity activity = mActivity;
            activity.setTheme(SetupWizardUtils.getTheme(activity, getIntent()));
            mActivity.setTheme(com.android.settings.R.style.SettingsPreferenceTheme_SetupWizard);
            ThemeHelper.trySetDynamicColor(mActivity);
        }
    }
}
