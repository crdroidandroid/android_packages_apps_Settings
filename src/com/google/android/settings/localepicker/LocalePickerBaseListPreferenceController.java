package com.google.android.settings.localepicker;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.app.LocaleCollectorBase;
import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.setupdesign.util.ItemStyler;
import com.google.android.setupdesign.util.ThemeHelper;
import com.google.common.collect.ImmutableMap;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class LocalePickerBaseListPreferenceController extends BasePreferenceController {
    private static final int GLOBAL_SUGGESTED_LOCALES = 0;
    protected static final String INTENT_LOCALE_KEY = "localeInfo";
    private static final String IS_LOCALE_SETUP_ONCE = "is_locale_set";
    protected static final String IS_SUGGESTED_LOCALE = "is_suggested_locale";
    private static final int JAPAN_SUGGESTED_LOCALES = 1;
    private static final String KEY_SUGGESTED = "suggested";
    private static final String KEY_SUPPORTED = "supported";
    protected static final int REQUEST_LOCALE_PICKER = 0;
    private static final String TAG = "LocalePickerBaseListPreference";
    private static final int UNITED_STATES_SUGGESTED_LOCALES = 2;
    private static final Map sPskuMap =
            new ImmutableMap.Builder()
                    .put("UVZ", 2)
                    .put("UAT", 2)
                    .put("UTM", 2)
                    .put("UGS", 2)
                    .put("AJP", 1)
                    .build();
    private DashboardFragment mFragment;
    private boolean mIsCountryMode;
    private boolean mIsSuggestedCategory;
    private LocaleSelectedListener mListener;
    private Set<LocaleStore.LocaleInfo> mLocaleList;
    private List<LocaleStore.LocaleInfo> mLocaleOptions;
    private LocaleStore.LocaleInfo mParentLocale;
    private PreferenceCategory mPreferenceCategory;
    private Map<String, Preference> mPreferences;
    private String mPskuString;
    private boolean mShouldApplyGlifExpressiveStyle;
    private List<LocaleStore.LocaleInfo> mSuggestedLocaleOptions;

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return super.getBackgroundWorkerClass();
    }

    protected abstract LocaleList getExplicitLocaleList();

    @Override
    public IntentFilter getIntentFilter() {
        return super.getIntentFilter();
    }

    protected abstract LocaleCollectorBase getLocaleCollectorController(Context context);

    protected abstract LocaleStore.LocaleInfo getParentLocale();

    protected abstract String getPreferenceCategoryKey();

    @Override
    public int getSliceHighlightMenuRes() {
        return super.getSliceHighlightMenuRes();
    }

    @Override
    public boolean hasAsyncUpdate() {
        return super.hasAsyncUpdate();
    }

    protected abstract boolean isNumberingMode();

    @Override
    public boolean isPublicSlice() {
        return super.isPublicSlice();
    }

    @Override
    public boolean isSliceable() {
        return super.isSliceable();
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return super.useDynamicSliceSummary();
    }

    public LocalePickerBaseListPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mPskuString = "";
        mLocaleList =
                LocaleStore.getLevelLocales(
                        context, new HashSet(), (LocaleStore.LocaleInfo) null, true);
        mLocaleOptions = new ArrayList<>(mLocaleList.size());
        mSuggestedLocaleOptions = new ArrayList<>();
        mPreferences = new ArrayMap<>();
        mShouldApplyGlifExpressiveStyle = ThemeHelper.shouldApplyGlifExpressiveStyle(context);
    }

    void setLocaleSelectedListener(LocaleSelectedListener localeSelectedListener) {
        mListener = localeSelectedListener;
    }

    void setPsku(String str) {
        mPskuString = str;
    }

    public void setFragment(DashboardFragment dashboardFragment) {
        mFragment = dashboardFragment;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory =
                (PreferenceCategory) screen.findPreference(getPreferenceCategoryKey());
        mIsSuggestedCategory = getPreferenceCategoryKey().contains(KEY_SUGGESTED);
        updatePreferences();
    }

    private void updatePreferences() {
        List<LocaleStore.LocaleInfo> supportedLocaleListByPsku;
        List<LocaleStore.LocaleInfo> sortedLocaleList;
        List<LocaleStore.LocaleInfo> supportedLocaleList;

        if (mPreferenceCategory == null) {
            Log.d(TAG, "updatePreferences, mPreferenceCategory is null");
            return;
        }

        mParentLocale = getParentLocale();
        if (mParentLocale != null) {
            mIsCountryMode = true;
            mLocaleList =
                    getLocaleCollectorController(mContext)
                            .getSupportedLocaleList(mParentLocale, false, mIsCountryMode);
            mLocaleOptions = new ArrayList<>(mLocaleList.size());
            if (!getPreferenceCategoryKey().contains(KEY_SUGGESTED)) {
                mPreferenceCategory.setTitle(
                        mContext.getString(
                                com.android.settings.R.string.all_supported_locales_regions_title));
            }
            if (mIsSuggestedCategory) {
                supportedLocaleList = getSuggestedLocaleList();
            } else {
                supportedLocaleList = getSupportedLocaleList();
            }
            sortedLocaleList = getSortedLocaleList(supportedLocaleList);
        } else {
            mPreferenceCategory.setTitle("");
            if (mIsSuggestedCategory) {
                if (AccessibilityStateUtils.isTtsEnabled(mContext.getContentResolver())) {
                    sortedLocaleList = getTtsLocaleList();
                } else {
                    LocaleStore.updateSimCountries(mContext);
                    mLocaleList =
                            LocaleStore.getLevelLocales(
                                    mContext, new HashSet(), (LocaleStore.LocaleInfo) null, true);
                    if (LocaleStore.isSimOrNwCountryAvailable()) {
                        sortedLocaleList = getSortedLocaleList(getLanguageSuggestedLocaleList());
                    } else {
                        sortedLocaleList = getSuggestedLocaleListByPsku();
                    }
                }
            } else {
                if (AccessibilityStateUtils.isTtsEnabled(mContext.getContentResolver())) {
                    supportedLocaleListByPsku = new ArrayList<>();
                } else {
                    LocaleStore.updateSimCountries(mContext);
                    mLocaleList =
                            LocaleStore.getLevelLocales(
                                    mContext, new HashSet(), (LocaleStore.LocaleInfo) null, true);
                    if (LocaleStore.isSimOrNwCountryAvailable()) {
                        supportedLocaleListByPsku = getSupportedLocaleList();
                    } else {
                        mSuggestedLocaleOptions.addAll(getSuggestedLocaleListByPsku());
                        supportedLocaleListByPsku = getSupportedLocaleListByPsku();
                    }
                }
                sortedLocaleList = getSortedLocaleList(supportedLocaleListByPsku);
            }
        }

        final Map<String, Preference> existingPreferences = mPreferences;
        mPreferences = new ArrayMap<>();
        setupPreference(sortedLocaleList, existingPreferences);

        for (Preference pref : existingPreferences.values()) {
            mPreferenceCategory.removePreference(pref);
        }
    }

    public void onSearchListChanged(
            @NonNull List<LocaleStore.LocaleInfo> newList, @Nullable CharSequence prefix) {
        mPreferenceCategory.removeAll();
        mPreferences.clear();
        final Map<String, Preference> existingPreferences = mPreferences;
        List<LocaleStore.LocaleInfo> allSupportedLocaleList = getAllSupportedLocaleList();
        if (prefix == null || prefix.toString().isEmpty()) {
            updatePreferences();
            return;
        }
        List<LocaleStore.LocaleInfo> sortedSuggestedRegionFromSearchList =
                getSortedSuggestedRegionFromSearchList(prefix, newList, allSupportedLocaleList);
        Collections.sort(
                sortedSuggestedRegionFromSearchList,
                Comparator.comparing(localeInfo -> localeInfo.getLocale().getDisplayName()));
        setupPreferenceWithSearchResult(sortedSuggestedRegionFromSearchList, existingPreferences);
    }

    private List<LocaleStore.LocaleInfo> getSortedSuggestedRegionFromSearchList(
            @Nullable CharSequence prefix,
            List<LocaleStore.LocaleInfo> listOptions,
            List<LocaleStore.LocaleInfo> listSuggested) {
        List<LocaleStore.LocaleInfo> searchItem = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) {
            return getSortedLocaleList(listSuggested);
        }

        for (LocaleStore.LocaleInfo option : listOptions) {
            if (listSuggested.contains(option)) {
                searchItem.add(option);
            }
        }
        return getSortedLocaleList(searchItem);
    }

    private void setupPreference(
            List<LocaleStore.LocaleInfo> localeInfoList,
            Map<String, Preference> existingPreferences) {
        Log.d(TAG, "setupPreference: isNumberingMode = " + isNumberingMode());
        if (isNumberingMode() && getPreferenceCategoryKey().contains(KEY_SUPPORTED)) {
            mPreferenceCategory.setTitle(
                    mContext.getString(
                            com.android.settings.R.string.all_supported_numbering_system_title));
        }
        localeInfoList.stream()
                .forEach(
                        locale -> {
                            Preference pref = existingPreferences.remove(locale.getId());
                            if (pref == null) {
                                pref =
                                        new Preference(mContext) {
                                            @Override
                                            public void onBindViewHolder(
                                                    PreferenceViewHolder preferenceViewHolder) {
                                                super.onBindViewHolder(preferenceViewHolder);
                                                if (mShouldApplyGlifExpressiveStyle) {
                                                    ItemStyler
                                                            .applyPartnerCustomizationLayoutMarginStyle(
                                                                    preferenceViewHolder
                                                                            .findViewById(
                                                                                    android.R.id
                                                                                            .title));
                                                }
                                            }
                                        };
                                mPreferenceCategory.addPreference(pref);
                            }
                            pref.setTitle(
                                    mIsCountryMode
                                            ? locale.getFullCountryNameNative()
                                            : locale.getFullNameNative());
                            pref.setKey(locale.toString());
                            pref.setOnPreferenceClickListener(
                                    clickedPref -> {
                                        if (mParentLocale == null) {
                                            FeatureFactory.getFeatureFactory()
                                                    .getMetricsFeatureProvider()
                                                    .action(
                                                            mContext,
                                                            mIsSuggestedCategory
                                                                    ? SettingsEnums
                                                                            .ACTION_CHOOSE_PREFERRED_LANGUAGE_FROM_SUGGESTED_LIST_IN_SUW
                                                                    : SettingsEnums
                                                                            .ACTION_CHOOSE_PREFERRED_LANGUAGE_FROM_ALL_LIST_IN_SUW,
                                                            new Pair[0]);
                                            Settings.Global.putInt(
                                                    mContext.getContentResolver(),
                                                    IS_SUGGESTED_LOCALE,
                                                    mIsSuggestedCategory ? 1 : 0);
                                        } else if (!isNumberingMode()) {
                                            FeatureFactory.getFeatureFactory()
                                                    .getMetricsFeatureProvider()
                                                    .action(
                                                            mContext,
                                                            mIsSuggestedCategory
                                                                    ? SettingsEnums
                                                                            .ACTION_CHOOSE_PREFERRED_REGION_FROM_SUGGESTED_LIST_IN_SUW
                                                                    : SettingsEnums
                                                                            .ACTION_CHOOSE_PREFERRED_REGION_FROM_ALL_LIST_IN_SUW,
                                                            new Pair[0]);
                                        }
                                        switchFragment(locale);
                                        return true;
                                    });
                            mPreferences.put(locale.getId(), pref);
                        });
        mPreferenceCategory.setVisible(mPreferenceCategory.getPreferenceCount() > 0);
    }

    private void setupPreferenceWithSearchResult(
            List<LocaleStore.LocaleInfo> localeInfoList,
            Map<String, Preference> existingPreferences) {
        if (!mIsSuggestedCategory) {
            localeInfoList.stream()
                    .forEach(
                            locale -> {
                                Preference preference = existingPreferences.remove(locale.getId());
                                if (preference == null) {
                                    preference =
                                            new Preference(mContext) {
                                                @Override
                                                public void onBindViewHolder(
                                                        PreferenceViewHolder preferenceViewHolder) {
                                                    super.onBindViewHolder(preferenceViewHolder);
                                                    if (mShouldApplyGlifExpressiveStyle) {
                                                        ItemStyler
                                                                .applyPartnerCustomizationLayoutMarginStyle(
                                                                        preferenceViewHolder
                                                                                .findViewById(
                                                                                        android.R.id
                                                                                                .title));
                                                    }
                                                }
                                            };
                                    mPreferenceCategory.addPreference(preference);
                                }
                                preference.setTitle(
                                        mIsCountryMode
                                                ? locale.getFullCountryNameNative()
                                                : locale.getFullNameNative());
                                preference.setKey(locale.toString());
                                preference.setOnPreferenceClickListener(
                                        clickedPref -> {
                                            FeatureFactory.getFeatureFactory()
                                                    .getMetricsFeatureProvider()
                                                    .action(
                                                            mContext,
                                                            SettingsEnums
                                                                    .ACTION_CHOOSE_REGION_AFTER_SEARCH_REGION_IN_SUW,
                                                            new Pair[0]);
                                            switchFragment(locale);
                                            return true;
                                        });
                                mPreferences.put(locale.getId(), preference);
                            });
            mPreferenceCategory.setTitle("");
            mPreferenceCategory.setVisible(mPreferenceCategory.getPreferenceCount() > 0);
            return;
        }
        mPreferenceCategory.setVisible(false);
    }

    private List<LocaleStore.LocaleInfo> getAllSupportedLocaleList() {
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            mLocaleOptions.addAll(mLocaleList);
        } else {
            Log.d(TAG, "Can not get locales because the locale list is null or empty.");
        }
        return mLocaleOptions;
    }

    private List<LocaleStore.LocaleInfo> getSuggestedLocaleListByPsku() {
        for (String str : getSuggestedLocalesFromStringArray()) {
            mSuggestedLocaleOptions.add(LocaleStore.getLocaleInfo(Locale.forLanguageTag(str)));
        }
        return mSuggestedLocaleOptions;
    }

    private String[] getSuggestedLocalesFromStringArray() {
        int intValue = ((Integer) sPskuMap.getOrDefault(mPskuString, 0)).intValue();
        if (intValue == 1) {
            return mContext.getResources().getStringArray(R.array.japan_locales);
        }
        if (intValue == 2) {
            return mContext.getResources().getStringArray(R.array.united_states_locales);
        }
        return mContext.getResources().getStringArray(R.array.global_locales);
    }

    private List<LocaleStore.LocaleInfo> getTtsLocaleList() {
        for (String str : getTtsLocalesFromStringArray()) {
            mSuggestedLocaleOptions.add(LocaleStore.getLocaleInfo(Locale.forLanguageTag(str)));
        }
        return mSuggestedLocaleOptions;
    }

    private String[] getTtsLocalesFromStringArray() {
        return mContext.getResources().getStringArray(R.array.tts_locales);
    }

    private List<LocaleStore.LocaleInfo> getSupportedLocaleListByPsku() {
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            Map<Locale, LocaleStore.LocaleInfo> hashMap = new HashMap<>(mLocaleList.size());
            for (LocaleStore.LocaleInfo localeInfo : mLocaleList) {
                if (!isPskuSuggestedLocale(localeInfo)) {
                    if (localeInfo.getLocale().getCountry().isEmpty()) {
                        if (localeInfo.isTranslated()) {
                            hashMap.put(localeInfo.getLocale(), localeInfo);
                        }
                    } else if (localeInfo.isTranslated()) {
                        hashMap.put(
                                localeInfo.getParent(),
                                LocaleStore.getLocaleInfo(localeInfo.getParent()));
                    }
                }
            }
            mLocaleOptions.addAll(hashMap.values());
        } else {
            Log.d(TAG, "Can not get supported locales because the locale list is null or empty.");
        }
        return mLocaleOptions;
    }

    private boolean isPskuSuggestedLocale(LocaleStore.LocaleInfo localeInfo) {
        for (LocaleStore.LocaleInfo localeInfo2 : mSuggestedLocaleOptions) {
            if (localeInfo2.getLocale().equals(localeInfo.getLocale())) {
                return true;
            }
            if (localeInfo2.getLocale().getCountry().isEmpty()
                    && !localeInfo.getLocale().getCountry().isEmpty()
                    && localeInfo2.getLocale().equals(localeInfo.getParent())) {
                return true;
            }
        }
        return false;
    }

    protected List<LocaleStore.LocaleInfo> getLanguageSuggestedLocaleList() {
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            HashMap hashMap = new HashMap(mLocaleList.size());
            for (LocaleStore.LocaleInfo localeInfo : mLocaleList) {
                if (localeInfo.isSuggested()) {
                    Locale build =
                            new Locale.Builder()
                                    .setLocale(localeInfo.getLocale().stripExtensions())
                                    .build();
                    hashMap.put(build, LocaleStore.getLocaleInfo(build));
                }
            }
            mLocaleOptions.addAll(hashMap.values());
        } else {
            Log.d(TAG, "Can not get suggested locales because the locale list is null or empty.");
        }
        return mLocaleOptions;
    }

    protected List<LocaleStore.LocaleInfo> getSuggestedLocaleList() {
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            mLocaleOptions.addAll(
                    (Collection)
                            mLocaleList.stream()
                                    .filter(localeInfo -> localeInfo.isSuggested())
                                    .collect(Collectors.toList()));
        } else {
            Log.d(TAG, "Can not get suggested locales because the locale list is null or empty.");
        }
        return mLocaleOptions;
    }

    protected List<LocaleStore.LocaleInfo> getSupportedLocaleList() {
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            mLocaleOptions.addAll(
                    (Collection)
                            mLocaleList.stream()
                                    .filter(localeInfo -> !localeInfo.isSuggested())
                                    .collect(Collectors.toList()));
        } else {
            Log.d(TAG, "Can not get supported locales because the locale list is null or empty.");
        }
        return mLocaleOptions;
    }

    private List<LocaleStore.LocaleInfo> getSortedLocaleList(
            List<LocaleStore.LocaleInfo> sortedLocaleList) {
        Collections.sort(
                sortedLocaleList,
                new LocaleHelper.LocaleInfoComparator(Locale.getDefault(), mIsCountryMode));
        return sortedLocaleList;
    }

    private void switchFragment(LocaleStore.LocaleInfo localeInfo) {
        if (isFinalSelectedLocale(localeInfo)) {
            if (localeInfo.getLocale().getCountry().isEmpty()) {
                localeInfo =
                        (LocaleStore.LocaleInfo)
                                LocaleStore.getLevelLocales(
                                                mContext, new HashSet(), localeInfo, true)
                                        .iterator()
                                        .next();
            }
            mListener.onLocaleSelected(localeInfo);
            LocalePicker.updateLocales(new LocaleList(localeInfo.getLocale()));
            Settings.Global.putInt(mContext.getContentResolver(), IS_LOCALE_SETUP_ONCE, 1);
            return;
        }
        if (localeInfo.getParent() == null) {
            LocalePicker.updateLocales(new LocaleList(localeInfo.getLocale()));
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable("extra_target_locale", localeInfo);
        bundle.putBoolean("extra_is_numbering_system", localeInfo.hasNumberingSystems());
        new SubSettingLauncher(mContext)
                .setDestination(RegionAndNumberingSystemPickerFragment.class.getCanonicalName())
                .setSourceMetricsCategory(0)
                .setExtras(
                        mFragment.getActivity().getIntent() != null
                                ? mFragment.getActivity().getIntent().getExtras()
                                : null)
                .setArguments(bundle)
                .setResultListener(mFragment, 0)
                .launch();
    }

    private boolean isFinalSelectedLocale(LocaleStore.LocaleInfo localeInfo) {
        boolean isSystemLocale = localeInfo.isSystemLocale();
        boolean z = localeInfo.getParent() != null;
        boolean hasNumberingSystems = localeInfo.hasNumberingSystems();
        mLocaleList = LocaleStore.getLevelLocales(mContext, new HashSet(), localeInfo, true);
        Log.d(
                TAG,
                "isFinalSelectedLocale: isSystemLocale = "
                        + isSystemLocale
                        + ", isRegionLocale = "
                        + z
                        + ", mayHaveDifferentNumberingSystem = "
                        + hasNumberingSystems
                        + ", isSuggested = "
                        + localeInfo.isSuggested()
                        + ", isNumberingMode = "
                        + isNumberingMode());
        return mLocaleList.size() == 1
                || isSystemLocale
                || localeInfo.isSuggested()
                || (z && !hasNumberingSystems)
                || isNumberingMode();
    }
}
