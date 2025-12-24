package com.google.android.settings.localepicker;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.app.LocaleCollectorBase;
import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.internal.app.SystemLocaleCollector;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.setupdesign.items.IItem;
import com.google.android.setupdesign.items.RecyclerItemAdapter;
import com.google.android.setupdesign.items.SectionItem;
import com.google.android.setupdesign.util.ThemeHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SetupLocalePickerListController implements RecyclerItemAdapter.OnItemSelectedListener {
    private Activity mActivity;
    private RecyclerItemAdapter mAdapter;
    private SectionItem mAllListGroup;
    private Context mContext;
    private SectionItem mDividerItem;
    private boolean mIsCountryMode;
    private boolean mIsNumberingSystemMode;
    private LocaleSelectedListener mListener;
    private Set<LocaleStore.LocaleInfo> mLocaleList;
    private List<LocaleStore.LocaleInfo> mLocaleOptions;
    private LocaleStore.LocaleInfo mParentLocale;
    private String mPskuString;
    private SectionItem mSearchBarItem;
    private SectionItem mSuggestedListGroup;
    private static final Map sPskuMap =
            new ImmutableMap.Builder()
                    .put("UVZ", 2)
                    .put("UAT", 2)
                    .put("UTM", 2)
                    .put("UGS", 2)
                    .put("AJP", 1)
                    .build();
    private static final ImmutableList REGION_SEARCH_SUPPORTED_LANGUAGES =
            ImmutableList.builder()
                    .add((Object) "ar")
                    .add((Object) "en")
                    .add((Object) "es")
                    .add((Object) "fr")
                    .build();
    private static final String TAG = "SetupLocalePickerListController";
    private boolean mIsFromSearch = false;
    private List<LocaleStore.LocaleInfo> mSuggestedLocaleOptions = new ArrayList<>();

    public SetupLocalePickerListController(
            Context context, Activity activity, LocaleStore.LocaleInfo localeInfo, boolean z) {
        mPskuString = "";
        mParentLocale = localeInfo;
        mIsNumberingSystemMode = z;
        mLocaleList =
                LocaleStore.getLevelLocales(
                        context, new HashSet(), (LocaleStore.LocaleInfo) null, true);
        mContext = context;
        mActivity = activity;
        mLocaleOptions = new ArrayList<>(mLocaleList.size());
    }

    void setLocaleSelectedListener(LocaleSelectedListener localeSelectedListener) {
        mListener = localeSelectedListener;
    }

    void setPsku(String str) {
        mPskuString = str;
    }

    protected void displayScreen(RecyclerItemAdapter recyclerItemAdapter) {
        mAdapter = recyclerItemAdapter;
        mSuggestedListGroup = (SectionItem) recyclerItemAdapter.findItemById(R.id.suggested_list);
        mAllListGroup = (SectionItem) mAdapter.findItemById(R.id.all_list);
        mSearchBarItem = (SectionItem) mAdapter.findItemById(R.id.setup_search_bar_section);
        mDividerItem = (SectionItem) mAdapter.findItemById(R.id.setup_divider_section);
        if (mSearchBarItem != null && (mIsNumberingSystemMode || !isRegionSearchSupported())) {
            ((SearchBarItem) mSearchBarItem.getItemAt(0)).setVisible(false);
        }
        if (mDividerItem != null
                && (ThemeHelper.shouldApplyGlifExpressiveStyle(mActivity)
                        || AccessibilityStateUtils.isTtsEnabled(mContext.getContentResolver()))) {
            ((DividerItem) mDividerItem.getItemAt(0)).setVisible(false);
        }
        updateScreen();
        mAdapter.setOnItemSelectedListener(this);
    }

    private void updateScreen() {
        mIsFromSearch = false;
        updateHeaders();
        updateGroupItems();
    }

    private void updateHeaders() {
        String string;
        if (mParentLocale != null) {
            mSuggestedListGroup.setHeaderTitle(
                    mContext.getString(com.android.settings.R.string.suggested_locales_title));
            SectionItem sectionItem = mAllListGroup;
            if (mIsNumberingSystemMode) {
                string =
                        mContext.getString(
                                com.android.settings.R.string.all_supported_numbering_system_title);
            } else {
                string =
                        mContext.getString(
                                com.android.settings.R.string.all_supported_locales_regions_title);
            }
            sectionItem.setHeaderTitle(string);
            if (ThemeHelper.shouldApplyGlifExpressiveStyle(mActivity)) {
                return;
            }
            mSuggestedListGroup
                    .getHeader()
                    .setTitleColor(
                            mContext.getColor(R.color.primary_text_disable_only_material_light));
            mAllListGroup
                    .getHeader()
                    .setTitleColor(
                            mContext.getColor(R.color.primary_text_disable_only_material_light));
            return;
        }
        if (ThemeHelper.shouldApplyGlifExpressiveStyle(mActivity)) {
            mSuggestedListGroup.setHeaderTitle("");
            mAllListGroup.setHeaderTitle("");
        } else {
            mSuggestedListGroup.removeChild(mSuggestedListGroup.getHeader());
            mAllListGroup.removeChild(mAllListGroup.getHeader());
        }
    }

    protected void updateGroupItems() {
        List<LocaleStore.LocaleInfo> suggestedLocaleListByPsku;
        List<LocaleStore.LocaleInfo> sortedLocaleList;
        if (mParentLocale != null) {
            mIsCountryMode = true;
            mLocaleList =
                    getLocaleCollectorController(mContext)
                            .getSupportedLocaleList(mParentLocale, false, mIsCountryMode);
            mLocaleOptions = new ArrayList<>(mLocaleList.size());
            suggestedLocaleListByPsku = getSortedLocaleList(getSuggestedLocaleList());
            sortedLocaleList = getSortedLocaleList(getSupportedLocaleList());
        } else if (AccessibilityStateUtils.isTtsEnabled(mContext.getContentResolver())) {
            suggestedLocaleListByPsku = getTtsLocaleList();
            sortedLocaleList = new ArrayList<>();
        } else {
            LocaleStore.updateSimCountries(mContext);
            mLocaleList =
                    LocaleStore.getLevelLocales(
                            mContext, new HashSet(), (LocaleStore.LocaleInfo) null, true);
            if (LocaleStore.isSimOrNwCountryAvailable()) {
                suggestedLocaleListByPsku = getSortedLocaleList(getLanguageSuggestedLocaleList());
                sortedLocaleList = getSortedLocaleList(getSupportedLocaleList());
            } else {
                suggestedLocaleListByPsku = getSuggestedLocaleListByPsku();
                sortedLocaleList = getSortedLocaleList(getSupportedLocaleListByPsku());
            }
        }
        setupItem(suggestedLocaleListByPsku, sortedLocaleList);
    }

    protected void onSearchListChanged(
            @NonNull List<LocaleStore.LocaleInfo> newList, @Nullable CharSequence prefix) {
        mSuggestedListGroup.clear();
        mAllListGroup.clear();
        List<LocaleStore.LocaleInfo> allSupportedLocaleList = getAllSupportedLocaleList();
        if (prefix == null || prefix.toString().isEmpty()) {
            updateScreen();
            return;
        }
        List<LocaleStore.LocaleInfo> sortedSuggestedRegionFromSearchList =
                getSortedSuggestedRegionFromSearchList(prefix, newList, allSupportedLocaleList);
        Collections.sort(
                sortedSuggestedRegionFromSearchList,
                Comparator.comparing(localeInfo -> localeInfo.getLocale().getDisplayName()));
        setupItemWithSearchResult(sortedSuggestedRegionFromSearchList);
    }

    private void setupItemWithSearchResult(List<LocaleStore.LocaleInfo> localeInfoList) {
        if (mSuggestedListGroup.getHeader() != null) {
            mSuggestedListGroup.removeChild(mSuggestedListGroup.getHeader());
        }
        if (mAllListGroup.getHeader() != null) {
            mAllListGroup.removeChild(mAllListGroup.getHeader());
        }
        mIsFromSearch = true;
        localeInfoList.stream()
                .forEach(
                        locale -> {
                            LocaleItem localeItem = new LocaleItem();
                            String fullCountryNameNative =
                                    mIsCountryMode
                                            ? locale.getFullCountryNameNative()
                                            : locale.getFullNameNative();
                            localeItem.setSuggestedState(true);
                            localeItem.setTitle(fullCountryNameNative);
                            localeItem.setLocaleInfo(locale);
                            mSuggestedListGroup.addChild(localeItem);
                        });
    }

    protected void setupItem(
            List<LocaleStore.LocaleInfo> list, List<LocaleStore.LocaleInfo> list2) {
        Log.d(TAG, "setupItem: isNumberingMode = " + mIsNumberingSystemMode);
        list.stream()
                .forEach(
                        locale -> {
                            LocaleItem localeItem = new LocaleItem();
                            mSuggestedListGroup.addChild(localeItem);
                            localeItem.setSuggestedState(true);
                            localeItem.setTitle(
                                    mIsCountryMode
                                            ? locale.getFullCountryNameNative()
                                            : locale.getFullNameNative());
                            localeItem.setLocaleInfo(locale);
                        });
        list2.stream()
                .forEach(
                        locale -> {
                            LocaleItem localeItem = new LocaleItem();
                            mAllListGroup.addChild(localeItem);
                            localeItem.setSuggestedState(false);
                            localeItem.setTitle(
                                    mIsCountryMode
                                            ? locale.getFullCountryNameNative()
                                            : locale.getFullNameNative());
                            localeItem.setLocaleInfo(locale);
                        });
        if (mSuggestedListGroup.getCount() == 0) {
            mSuggestedListGroup.clear();
        }
        if (mAllListGroup.getCount() == 0) {
            mAllListGroup.clear();
        }
    }

    @Override
    public void onItemSelected(IItem iItem) {
        if (iItem instanceof SearchBarItem) {
            Bundle extras = mActivity.getIntent().getExtras();
            extras.putSerializable("extra_target_locale", mParentLocale);
            extras.putBoolean("extra_is_numbering_system", mParentLocale.hasNumberingSystems());
            Intent intent =
                    new Intent("com.google.android.settings.localepicker.SETUP_REGION_SEARCH");
            intent.putExtras(extras);
            mActivity.startActivityForResult(intent, 0);
            return;
        }
        LocaleItem localeItem = (LocaleItem) iItem;
        if (mParentLocale == null) {
            FeatureFactory.getFeatureFactory()
                    .getMetricsFeatureProvider()
                    .action(
                            mContext,
                            localeItem.isSuggested()
                                    ? SettingsEnums
                                            .ACTION_CHOOSE_PREFERRED_LANGUAGE_FROM_SUGGESTED_LIST_IN_SUW
                                    : SettingsEnums
                                            .ACTION_CHOOSE_PREFERRED_LANGUAGE_FROM_ALL_LIST_IN_SUW,
                            new Pair[0]);
            Settings.Global.putInt(
                    mContext.getContentResolver(),
                    "is_suggested_locale",
                    localeItem.isSuggested() ? 1 : 0);
        } else if (!mIsNumberingSystemMode) {
            if (mIsFromSearch) {
                FeatureFactory.getFeatureFactory()
                        .getMetricsFeatureProvider()
                        .action(
                                mContext,
                                SettingsEnums.ACTION_CHOOSE_REGION_AFTER_SEARCH_REGION_IN_SUW,
                                new Pair[0]);
            } else {
                FeatureFactory.getFeatureFactory()
                        .getMetricsFeatureProvider()
                        .action(
                                mContext,
                                localeItem.isSuggested()
                                        ? SettingsEnums
                                                .ACTION_CHOOSE_PREFERRED_REGION_FROM_SUGGESTED_LIST_IN_SUW
                                        : SettingsEnums
                                                .ACTION_CHOOSE_PREFERRED_REGION_FROM_ALL_LIST_IN_SUW,
                                new Pair[0]);
            }
        }
        LocaleStore.LocaleInfo localeInfo = localeItem.getLocaleInfo();
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
            Settings.Global.putInt(mContext.getContentResolver(), "is_locale_set", 1);
            return;
        }
        if (localeInfo.getParent() == null) {
            LocalePicker.updateLocales(new LocaleList(localeInfo.getLocale()));
        }
        Bundle extras2 = mActivity.getIntent().getExtras();
        extras2.putSerializable("extra_target_locale", localeInfo);
        extras2.putBoolean("extra_is_numbering_system", localeInfo.hasNumberingSystems());
        Intent intent2 = new Intent("com.google.android.settings.localepicker.REGIONAND_PICKER");
        intent2.putExtras(extras2);
        mActivity.startActivityForResult(intent2, 0);
    }

    protected void initSuggestedListSectionItem(SectionItem sectionItem) {
        mSuggestedListGroup = sectionItem;
    }

    protected void initAllListSectionItem(SectionItem sectionItem) {
        mAllListGroup = sectionItem;
    }

    protected void initLocaleList(Set set) {
        mLocaleList = set;
    }

    protected SectionItem getSuggestedListSectionItem() {
        return mSuggestedListGroup;
    }

    protected SectionItem getAllListSectionItem() {
        return mAllListGroup;
    }

    protected List<LocaleStore.LocaleInfo> getTtsLocaleList() {
        for (String str : getTtsLocalesFromStringArray()) {
            mSuggestedLocaleOptions.add(LocaleStore.getLocaleInfo(Locale.forLanguageTag(str)));
        }
        return mSuggestedLocaleOptions;
    }

    private String[] getTtsLocalesFromStringArray() {
        return mContext.getResources().getStringArray(R.array.tts_locales);
    }

    private List<LocaleStore.LocaleInfo> getAllSupportedLocaleList() {
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            mLocaleOptions.addAll(mLocaleList);
        } else {
            Log.d(TAG, "Can not get locales because the locale list is null or empty.");
        }
        return mLocaleOptions;
    }

    protected List<LocaleStore.LocaleInfo> getSuggestedLocaleListByPsku() {
        for (String str : getSuggestedLocalesFromStringArray()) {
            mSuggestedLocaleOptions.add(LocaleStore.getLocaleInfo(Locale.forLanguageTag(str)));
        }
        return mSuggestedLocaleOptions;
    }

    protected List<LocaleStore.LocaleInfo> getSupportedLocaleListByPsku() {
        List<LocaleStore.LocaleInfo> arrayList = new ArrayList<>();
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
            arrayList.addAll(hashMap.values());
            return arrayList;
        }
        Log.d(TAG, "Can not get supported locales because the locale list is null or empty.");
        return arrayList;
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

    protected List<LocaleStore.LocaleInfo> getLanguageSuggestedLocaleList() {
        List<LocaleStore.LocaleInfo> arrayList = new ArrayList<>();
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
            arrayList.addAll(hashMap.values());
            return arrayList;
        }
        Log.d(TAG, "Can not get suggested locales because the locale list is null or empty.");
        return arrayList;
    }

    protected List<LocaleStore.LocaleInfo> getSuggestedLocaleList() {
        List<LocaleStore.LocaleInfo> arrayList = new ArrayList<>();
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            arrayList.addAll(
                    (Collection)
                            mLocaleList.stream()
                                    .filter(localeInfo -> localeInfo.isSuggested())
                                    .collect(Collectors.toList()));
            return arrayList;
        }
        Log.d(TAG, "Can not get suggested locales because the locale list is null or empty.");
        return arrayList;
    }

    protected List<LocaleStore.LocaleInfo> getSupportedLocaleList() {
        List<LocaleStore.LocaleInfo> arrayList = new ArrayList<>();
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            arrayList.addAll(
                    (Collection)
                            mLocaleList.stream()
                                    .filter(localeInfo -> !localeInfo.isSuggested())
                                    .collect(Collectors.toList()));
            return arrayList;
        }
        Log.d(TAG, "Can not get supported locales because the locale list is null or empty.");
        return arrayList;
    }

    private List<LocaleStore.LocaleInfo> getSortedSuggestedRegionFromSearchList(
            CharSequence charSequence,
            List<LocaleStore.LocaleInfo> list,
            List<LocaleStore.LocaleInfo> list2) {
        List<LocaleStore.LocaleInfo> arrayList = new ArrayList<>();
        if (charSequence == null || charSequence.toString().isEmpty()) {
            return getSortedLocaleList(list2);
        }
        Iterator it = list.iterator();
        while (it.hasNext()) {
            LocaleStore.LocaleInfo localeInfo = (LocaleStore.LocaleInfo) it.next();
            if (list2.contains(localeInfo)) {
                arrayList.add(localeInfo);
            }
        }
        return getSortedLocaleList(arrayList);
    }

    protected List<LocaleStore.LocaleInfo> getSortedLocaleList(List<LocaleStore.LocaleInfo> list) {
        Collections.sort(
                list, new LocaleHelper.LocaleInfoComparator(Locale.getDefault(), mIsCountryMode));
        return list;
    }

    private LocaleCollectorBase getLocaleCollectorController(Context context) {
        return new SystemLocaleCollector(context, (LocaleList) null);
    }

    private boolean isRegionSearchSupported() {
        String languageTag = mParentLocale.getLocale().toLanguageTag();
        if (REGION_SEARCH_SUPPORTED_LANGUAGES.contains(languageTag)) {
            return true;
        }
        Log.d(TAG, "Region search is not supported for " + languageTag);
        return false;
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
                        + mIsNumberingSystemMode);
        return mLocaleList.size() == 1
                || isSystemLocale
                || localeInfo.isSuggested()
                || (z && !hasNumberingSystems)
                || mIsNumberingSystemMode;
    }
}
