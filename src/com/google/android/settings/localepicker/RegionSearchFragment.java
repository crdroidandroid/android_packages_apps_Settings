package com.google.android.settings.localepicker;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;

import com.google.android.setupdesign.GlifPreferenceLayout;
import com.google.android.setupdesign.template.FloatingBackButtonMixin;
import com.android.settings.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes3.dex */
public class RegionSearchFragment extends DashboardFragment implements TextWatcher {
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.suw_region_search);
    public static final String EXTRA_TARGET_LOCALE = "extra_target_locale";
    public static final String EXTRA_IS_NUMBERING_SYSTEM = "extra_is_numbering_system";
    private static final String TAG = "RegionSearchFragment";
    private static final String KEY_PREFERENCE_SYSTEM_LOCALE_LIST = "system_locale_list";
    private static final String KEY_PREFERENCE_SYSTEM_LOCALE_SUGGESTED_LIST =
            "system_locale_suggested_list";
    private Activity mActivity;
    private AppCompatImageView mBackIcon;
    private AppCompatImageView mClearSearchQueryButton;
    private boolean mIsNumberingMode;
    private LocaleStore.LocaleInfo mLocaleInfo;
    private List<LocaleStore.LocaleInfo> mLocaleOptions;
    private List<LocaleStore.LocaleInfo> mOriginalLocaleInfos;
    private CharSequence mPrefix;
    private RecyclerView mRecyclerView;
    private AppCompatEditText mSearchActionBarText;
    private SearchFilter mSearchFilter = null;
    private SystemLocaleSuggestedListPreferenceController mSuggestedListPreferenceController;
    private SystemLocaleAllListPreferenceController mSystemLocaleAllListPreferenceController;

    @Override
    public void afterTextChanged(Editable s) {}

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.REGION_SEARCH_IN_SUW;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mActivity = getActivity();
        if (mActivity == null || mActivity.isFinishing()) {
            Log.d(TAG, "onCreate, no activity or activity is finishing");
            return;
        }
        if (mSystemLocaleAllListPreferenceController != null) {
            mOriginalLocaleInfos =
                    mSystemLocaleAllListPreferenceController.getSupportedLocaleList();
            mOriginalLocaleInfos.addAll(
                    mSystemLocaleAllListPreferenceController.getSuggestedLocaleList());
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container,
            @NonNull Bundle savedInstanceState) {
        mBackIcon = (AppCompatImageView) mActivity.findViewById(R.id.back_icon);
        mBackIcon.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        FeatureFactory.getFeatureFactory()
                                .getMetricsFeatureProvider()
                                .action(
                                        getContext(),
                                        SettingsEnums
                                                .ACTION_NO_PREFERRED_REGION_AFTER_SEARCH_REGION_IN_SUW,
                                        new Pair[0]);
                        mActivity.finish();
                    }
                });
        mSearchActionBarText =
                (AppCompatEditText) mActivity.findViewById(R.id.search_action_bar_text);
        mSearchActionBarText.addTextChangedListener(this);
        mSearchActionBarText.requestFocus();
        mClearSearchQueryButton =
                (AppCompatImageView) mActivity.findViewById(R.id.clear_search_query);
        mClearSearchQueryButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        clearLanguageSearchText();
                    }
                });
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(com.android.settings.R.id.recycler_view);
        if (view instanceof GlifPreferenceLayout) {
            GlifPreferenceLayout glifPreferenceLayout = (GlifPreferenceLayout) view;
            if (glifPreferenceLayout.getMixin(FloatingBackButtonMixin.class) != null) {
                ((FloatingBackButtonMixin)
                                glifPreferenceLayout.getMixin(FloatingBackButtonMixin.class))
                        .setVisibility(8);
            }
            LinearLayout linearLayout =
                    (LinearLayout)
                            glifPreferenceLayout.findManagedViewById(
                                    com.google.android.setupdesign.R.id.sud_layout_header);
            if (linearLayout != null) {
                linearLayout.setVisibility(8);
            }
            View findViewById =
                    glifPreferenceLayout.findViewById(
                            com.google.android.setupdesign.R.id.sud_landscape_content_area);
            if (findViewById != null) {
                findViewById.setPadding(0, 0, 0, 0);
            }
            ScrollView scrollView =
                    (ScrollView)
                            glifPreferenceLayout.findManagedViewById(
                                    com.google.android.setupdesign.R.id.sud_header_scroll_view);
            if (scrollView != null) {
                scrollView.setContentDescription(
                        getContext().getString(R.string.search_region_page));
            }
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

    private void filterSearch(String str) {
        if (mSearchFilter == null) {
            mSearchFilter = new SearchFilter();
        }
        if (mOriginalLocaleInfos == null) {
            Log.w(TAG, "Locales haven't loaded completely yet, so nothing can be filtered");
        } else {
            mSearchFilter.filter(str);
        }
    }

    class SearchFilter extends Filter {
        private SearchFilter() {}

        @Override
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            Filter.FilterResults filterResults = new Filter.FilterResults();
            mPrefix = charSequence;
            if (TextUtils.isEmpty(charSequence)) {
                filterResults.values = mOriginalLocaleInfos;
                filterResults.count = mOriginalLocaleInfos.size();
                return filterResults;
            }
            ArrayList<LocaleStore.LocaleInfo> arrayList = new ArrayList<>(mOriginalLocaleInfos);
            Locale locale = Locale.getDefault();
            String normalizeForSearch =
                    LocaleHelper.normalizeForSearch(charSequence.toString(), locale);
            ArrayList<LocaleStore.LocaleInfo> arrayList2 = new ArrayList<>();
            int size = arrayList.size();
            int i = 0;
            while (i < size) {
                Object obj = arrayList.get(i);
                i++;
                LocaleStore.LocaleInfo localeInfo = (LocaleStore.LocaleInfo) obj;
                String normalizeForSearch2 =
                        LocaleHelper.normalizeForSearch(
                                localeInfo.getFullNameInUiLanguage(), locale);
                if (wordMatches(
                                LocaleHelper.normalizeForSearch(
                                        localeInfo.getFullNameNative(), locale),
                                normalizeForSearch)
                        || wordMatches(normalizeForSearch2, normalizeForSearch)) {
                    if (!arrayList2.contains(localeInfo)) {
                        arrayList2.add(localeInfo);
                    }
                }
            }
            filterResults.values = arrayList2;
            filterResults.count = arrayList2.size();
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (mSystemLocaleAllListPreferenceController == null
                    || mSuggestedListPreferenceController == null) {
                Log.d(TAG, "publishResults(), can not get preference.");
                return;
            }
            mLocaleOptions = (ArrayList<LocaleStore.LocaleInfo>) results.values;
            if (mRecyclerView != null) {
                mRecyclerView.post(() -> mRecyclerView.scrollToPosition(0));
            }
            mSystemLocaleAllListPreferenceController.onSearchListChanged(mLocaleOptions, mPrefix);
            mSuggestedListPreferenceController.onSearchListChanged(mLocaleOptions, mPrefix);
        }

        private boolean wordMatches(String valueText, String prefixString) {
            if (valueText == null) {
                return false;
            }
            if (valueText.startsWith(prefixString)) {
                return true;
            }
            Pattern pattern = Pattern.compile("^.*?\\((.*)");
            Matcher matcher = pattern.matcher(valueText);
            if (matcher.find()) {
                String region = matcher.group(1);
                return region.startsWith(prefixString);
            }
            return false;
        }
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.suw_region_search;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    private List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mLocaleInfo = (LocaleStore.LocaleInfo) extras.getSerializable(EXTRA_TARGET_LOCALE);
            mIsNumberingMode = extras.getBoolean(EXTRA_IS_NUMBERING_SYSTEM);
        }
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
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mClearSearchQueryButton != null) {
            mClearSearchQueryButton.setVisibility(s.length() == 0 ? 8 : 0);
        }
        filterSearch(s.toString());
    }

    private void clearLanguageSearchText() {
        Editable text;
        if (mSearchActionBarText == null || (text = mSearchActionBarText.getText()) == null) {
            return;
        }
        text.clear();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mActivity != null) {
            if (requestCode == 0 && resultCode == -1) {
                Serializable serializable =
                        (LocaleStore.LocaleInfo) data.getSerializableExtra("localeInfo");
                Intent intent = new Intent();
                intent.putExtra("localeInfo", serializable);
                mActivity.setResult(-1, intent);
            }
            mActivity.finish();
        }
    }
}
