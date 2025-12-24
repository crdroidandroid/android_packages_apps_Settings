package com.google.android.settings.localepicker;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;

import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.setupdesign.GlifRecyclerLayout;
import com.google.android.setupdesign.items.RecyclerItemAdapter;
import com.google.android.setupdesign.template.FloatingBackButtonMixin;
import com.google.android.setupdesign.template.HeaderMixin;
import com.google.android.setupdesign.util.ThemeHelper;
import com.android.settings.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetupRegionSearchActivity extends Activity implements TextWatcher {
    private RecyclerItemAdapter mAdapter;
    private AppCompatImageView mBackIcon;
    private AppCompatImageView mClearSearchQueryButton;
    private boolean mIsNumberingMode;
    private LocaleStore.LocaleInfo mLocaleInfo;
    private List mLocaleOptions;
    private List mOriginalLocaleInfos;
    private CharSequence mPrefix;
    private AppCompatEditText mSearchActionBarText;
    private SearchFilter mSearchFilter = null;
    private SetupLocalePickerListController mSetupLocalePickerListController;

    @Override
    public void afterTextChanged(Editable editable) {}

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

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
        setContentView(R.layout.setup_search_region);
        setupContent();
    }

    private void setupContent() {
        View findManagedViewById;
        Bundle extras = getIntent().getExtras();
        this.mLocaleInfo = (LocaleStore.LocaleInfo) extras.getSerializable("extra_target_locale");
        this.mIsNumberingMode = extras.getBoolean("extra_is_numbering_system");
        GlifRecyclerLayout glifRecyclerLayout =
                (GlifRecyclerLayout) findViewById(R.id.setup_wizard_region_search_layout);
        ((HeaderMixin) glifRecyclerLayout.getMixin(HeaderMixin.class))
                .getTextView()
                .setVisibility(8);
        if (glifRecyclerLayout.getMixin(FloatingBackButtonMixin.class) != null) {
            ((FloatingBackButtonMixin) glifRecyclerLayout.getMixin(FloatingBackButtonMixin.class))
                    .setVisibility(8);
        }
        LinearLayout linearLayout =
                (LinearLayout)
                        glifRecyclerLayout.findManagedViewById(
                                com.google.android.setupdesign.R.id.sud_layout_header);
        if (linearLayout != null) {
            linearLayout.setVisibility(8);
        }
        ViewGroup viewGroup =
                (ViewGroup) findViewById(com.google.android.setupdesign.R.id.suc_layout_status);
        if (viewGroup != null) {
            View inflate =
                    getLayoutInflater().inflate(R.layout.search_bar_layout, (ViewGroup) null);
            int childCount = viewGroup.getChildCount();
            if (viewGroup instanceof FrameLayout) {
                if (!ThemeHelper.shouldApplyGlifExpressiveStyle(getApplicationContext())
                        && (findManagedViewById =
                                        glifRecyclerLayout.findManagedViewById(
                                                com.google.android.setupdesign.R.id
                                                        .sud_landscape_content_area))
                                != null) {
                    findManagedViewById.setPadding(0, 0, 0, 0);
                }
                final View childAt = viewGroup.getChildAt(0);
                final FrameLayout.LayoutParams layoutParams =
                        (FrameLayout.LayoutParams) childAt.getLayoutParams();
                final View findViewById = inflate.findViewById(R.id.suw_locale_picker_search_bar);
                findViewById.post(
                        new Runnable() {
                            @Override
                            public final void run() {
                                layoutParams.topMargin =
                                        findViewById.getBottom() + (findViewById.getHeight() / 4);
                                childAt.setLayoutParams(layoutParams);
                            }
                        });
                viewGroup.addView(inflate, childCount);
            } else {
                int i = childCount + 1;
                final View[] viewArr = new View[i];
                viewArr[0] = inflate;
                int i2 = 0;
                while (i2 < childCount) {
                    int i3 = i2 + 1;
                    viewArr[i3] = viewGroup.getChildAt(i2);
                    i2 = i3;
                }
                viewGroup.removeAllViews();
                View findViewById2 =
                        viewArr[2].findViewById(
                                com.google.android.setupdesign.R.id.sud_landscape_content_area);
                if (findViewById2 != null) {
                    findViewById2.setPadding(
                            findViewById2.getPaddingStart(),
                            0,
                            findViewById2.getPaddingEnd(),
                            findViewById2.getPaddingBottom());
                }
                final LinearLayout.LayoutParams layoutParams2 =
                        (LinearLayout.LayoutParams) viewArr[2].getLayoutParams();
                final View findViewById3 = inflate.findViewById(R.id.suw_locale_picker_search_bar);
                findViewById3.post(
                        new Runnable() {
                            @Override
                            public final void run() {
                                layoutParams2.bottomMargin = (findViewById3.getHeight() * 3) / 2;
                                viewArr[2].setLayoutParams(layoutParams2);
                            }
                        });
                for (int i4 = 0; i4 < i; i4++) {
                    viewGroup.addView(viewArr[i4], i4);
                }
                ScrollView scrollView =
                        (ScrollView)
                                findViewById(
                                        com.google.android.setupdesign.R.id.sud_header_scroll_view);
                if (scrollView != null) {
                    scrollView.setContentDescription(
                            getApplicationContext().getString(R.string.search_region_page));
                }
            }
        }
        AppCompatImageView appCompatImageView = (AppCompatImageView) findViewById(R.id.back_icon);
        this.mBackIcon = appCompatImageView;
        if (appCompatImageView != null) {
            appCompatImageView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public final void onClick(View view) {
                            FeatureFactory.getFeatureFactory()
                                    .getMetricsFeatureProvider()
                                    .action(
                                            getApplicationContext(),
                                            SettingsEnums
                                                    .ACTION_NO_PREFERRED_REGION_AFTER_SEARCH_REGION_IN_SUW,
                                            new Pair[0]);
                            finish();
                        }
                    });
        }
        AppCompatEditText appCompatEditText =
                (AppCompatEditText) findViewById(R.id.search_action_bar_text);
        this.mSearchActionBarText = appCompatEditText;
        if (appCompatEditText != null) {
            appCompatEditText.addTextChangedListener(this);
            this.mSearchActionBarText.requestFocus();
        }
        AppCompatImageView appCompatImageView2 =
                (AppCompatImageView) findViewById(R.id.clear_search_query);
        this.mClearSearchQueryButton = appCompatImageView2;
        if (appCompatImageView2 != null) {
            appCompatImageView2.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public final void onClick(View view) {
                            clearLanguageSearchText();
                        }
                    });
        }
        this.mAdapter = (RecyclerItemAdapter) glifRecyclerLayout.getAdapter();
        LocaleSelectedListener localeSelectedListener =
                new LocaleSelectedListener() {
                    @Override
                    public void onLocaleSelected(LocaleStore.LocaleInfo localeInfo) {
                        Intent intent = new Intent();
                        intent.putExtra("localeInfo", (Serializable) localeInfo);
                        SetupRegionSearchActivity.this.setResult(-1, intent);
                        SetupRegionSearchActivity.this.finish();
                    }
                };
        SetupLocalePickerListController setupLocalePickerListController =
                new SetupLocalePickerListController(
                        getApplicationContext(), this, this.mLocaleInfo, this.mIsNumberingMode);
        this.mSetupLocalePickerListController = setupLocalePickerListController;
        setupLocalePickerListController.setLocaleSelectedListener(localeSelectedListener);
        this.mSetupLocalePickerListController.displayScreen(this.mAdapter);
        SetupLocalePickerListController setupLocalePickerListController2 =
                this.mSetupLocalePickerListController;
        if (setupLocalePickerListController2 != null) {
            List supportedLocaleList = setupLocalePickerListController2.getSupportedLocaleList();
            this.mOriginalLocaleInfos = supportedLocaleList;
            supportedLocaleList.addAll(
                    this.mSetupLocalePickerListController.getSuggestedLocaleList());
        }
    }

    private void filterSearch(String str) {
        if (this.mSearchFilter == null) {
            this.mSearchFilter = new SearchFilter();
        }
        if (this.mOriginalLocaleInfos == null) {
            Log.w(
                    "SetupRegionSearchActivity",
                    "Locales haven't loaded completely yet, so nothing can be filtered");
        } else {
            this.mSearchFilter.filter(str);
        }
    }

    class SearchFilter extends Filter {
        private SearchFilter() {}

        @Override
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            Filter.FilterResults filterResults = new Filter.FilterResults();
            SetupRegionSearchActivity.this.mPrefix = charSequence;
            if (TextUtils.isEmpty(charSequence)) {
                filterResults.values = SetupRegionSearchActivity.this.mOriginalLocaleInfos;
                filterResults.count = SetupRegionSearchActivity.this.mOriginalLocaleInfos.size();
                return filterResults;
            }
            ArrayList arrayList =
                    new ArrayList(SetupRegionSearchActivity.this.mOriginalLocaleInfos);
            Locale locale = Locale.getDefault();
            String normalizeForSearch =
                    LocaleHelper.normalizeForSearch(charSequence.toString(), locale);
            ArrayList arrayList2 = new ArrayList();
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
        protected void publishResults(
                CharSequence charSequence, Filter.FilterResults filterResults) {
            if (SetupRegionSearchActivity.this.mSetupLocalePickerListController == null) {
                Log.d("SetupRegionSearchActivity", "publishResults(), can not get item.");
                return;
            }
            SetupRegionSearchActivity.this.mLocaleOptions = (ArrayList) filterResults.values;
            SetupRegionSearchActivity.this.mSetupLocalePickerListController.onSearchListChanged(
                    SetupRegionSearchActivity.this.mLocaleOptions,
                    SetupRegionSearchActivity.this.mPrefix);
        }

        private boolean wordMatches(String str, String str2) {
            if (str == null) {
                return false;
            }
            if (str.startsWith(str2)) {
                return true;
            }
            Matcher matcher = Pattern.compile("^.*?\\((.*)").matcher(str);
            if (matcher.find()) {
                return matcher.group(1).startsWith(str2);
            }
            return false;
        }
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        AppCompatImageView appCompatImageView = this.mClearSearchQueryButton;
        if (appCompatImageView != null) {
            appCompatImageView.setVisibility(charSequence.length() == 0 ? 8 : 0);
        }
        filterSearch(charSequence.toString());
    }

    private void clearLanguageSearchText() {
        Editable text;
        AppCompatEditText appCompatEditText = this.mSearchActionBarText;
        if (appCompatEditText == null || (text = appCompatEditText.getText()) == null) {
            return;
        }
        text.clear();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == -1) {
            if (data != null) {
                LocaleStore.LocaleInfo serializableExtra =
                        (LocaleStore.LocaleInfo) data.getSerializableExtra("localeInfo");
                Intent intent = new Intent();
                intent.putExtra("localeInfo", (Serializable) serializableExtra);
                setResult(-1, intent);
            }
            finish();
        }
    }
}
