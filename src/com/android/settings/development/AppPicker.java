/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.development;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import com.android.settings.SettingsActivity;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppPicker extends SettingsActivity {
    public static final String EXTRA_REQUESTIING_PERMISSION
            = "com.android.settings.extra.REQUESTIING_PERMISSION";
    public static final String EXTRA_DEBUGGABLE = "com.android.settings.extra.DEBUGGABLE";
    public static final String EXTRA_NON_SYSTEM = "com.android.settings.extra.NON_SYSTEM";
    public static final String EXTRA_INCLUDE_NOTHING = "com.android.settings.extra.INCLUDE_NOTHING";

    public static final int RESULT_NO_MATCHING_APPS = -2;

    @Override
    protected void onCreate(Bundle icicle) {
        final Intent intent = getIntent();

        String permissionName = getIntent().getStringExtra(EXTRA_REQUESTIING_PERMISSION);
        boolean debuggableOnly = getIntent().getBooleanExtra(EXTRA_DEBUGGABLE, false);
        boolean nonSystemOnly = getIntent().getBooleanExtra(EXTRA_NON_SYSTEM, false);
        boolean includeNothing = getIntent().getBooleanExtra(EXTRA_INCLUDE_NOTHING, true);

        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS,
                AppPickerListFragment.withArgs(permissionName, debuggableOnly, nonSystemOnly,
                        includeNothing));

        super.onCreate(icicle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return AppPickerListFragment.class.getName().equals(fragmentName);
    }

    private void handleBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            super.onBackPressed();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    public static class AppPickerListFragment extends ListFragment {
        private static final String EXTRA_PERMISSION_NAME = "extra_permission_name";
        private static final String EXTRA_DEBUGGABLE_ONLY = "extra_debuggable_only";
        private static final String EXTRA_NON_SYSTEM_ONLY = "extra_non_system_only";
        private static final String EXTRA_INCLUDE_NOTHING = "extra_include_nothing";

        private String mPermissionName = "";
        private boolean mDebuggableOnly;
        private boolean mNonSystemOnly;
        private boolean mIncludeNothing;

        private AppListAdapter mAdapter;

        public static Bundle withArgs(String permissionName,
                boolean debuggableOnly, boolean nonSystemOnly, boolean includeNothing) {
            final Bundle args = new Bundle(4);
            args.putString(EXTRA_PERMISSION_NAME, permissionName);
            args.putBoolean(EXTRA_DEBUGGABLE_ONLY, debuggableOnly);
            args.putBoolean(EXTRA_NON_SYSTEM_ONLY, nonSystemOnly);
            args.putBoolean(EXTRA_INCLUDE_NOTHING, includeNothing);
            return args;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                mPermissionName = getArguments().getString(EXTRA_PERMISSION_NAME);
                mDebuggableOnly = getArguments().getBoolean(EXTRA_DEBUGGABLE_ONLY);
                mNonSystemOnly = getArguments().getBoolean(EXTRA_NON_SYSTEM_ONLY);
                mIncludeNothing = getArguments().getBoolean(EXTRA_INCLUDE_NOTHING);
            }
            mAdapter = new AppListAdapter(requireContext());
            if (mAdapter.getCount() <= 0) {
                requireActivity().setResult(RESULT_NO_MATCHING_APPS);
                requireActivity().finish();
            } else {
                setListAdapter(mAdapter);
            }
        }

        @Override
        public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
            super.onListItemClick(l, v, position, id);
            MyApplicationInfo app = mAdapter.getItem(position);
            Intent intent = new Intent();
            if (app.info != null) intent.setAction(app.info.packageName);
            requireActivity().setResult(RESULT_OK, intent);
            requireActivity().finish();
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getListView().setNestedScrollingEnabled(true);
        }

        class MyApplicationInfo {
            ApplicationInfo info;
            CharSequence label;
        }

        private final class AppListAdapter extends ArrayAdapter<MyApplicationInfo> {
            private final List<MyApplicationInfo> mPackageInfoList =
                    new ArrayList<MyApplicationInfo>();
            private final LayoutInflater mInflater;

            public AppListAdapter(Context context) {
                super(context, 0);
                mInflater = (LayoutInflater) context.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                List<ApplicationInfo> pkgs = context.getPackageManager().getInstalledApplications(
                        0);
                for (int i = 0; i < pkgs.size(); i++) {
                    ApplicationInfo ai = pkgs.get(i);
                    if (ai.uid == Process.SYSTEM_UID) {
                        continue;
                    }

                    // Filter out apps that are not debuggable if required.
                    if (mDebuggableOnly) {
                        // On a user build, we only allow debugging of apps that
                        // are marked as debuggable.  Otherwise (for platform development)
                        // we allow all apps.
                        if ((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0
                                && "user".equals(Build.TYPE)) {
                            continue;
                        }
                    }

                    // Filter out apps that are system apps if requested
                    if (mNonSystemOnly && ai.isSystemApp()) {
                        continue;
                    }

                    // Filter out apps that do not request the permission if required.
                    if (mPermissionName != null) {
                        boolean requestsPermission = false;
                        try {
                            PackageInfo pi = getContext().getPackageManager().getPackageInfo(
                                    ai.packageName, PackageManager.GET_PERMISSIONS);
                            if (pi.requestedPermissions == null) {
                                continue;
                            }
                            for (String requestedPermission : pi.requestedPermissions) {
                                if (requestedPermission.equals(mPermissionName)) {
                                    requestsPermission = true;
                                    break;
                                }
                            }
                            if (!requestsPermission) {
                                continue;
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            continue;
                        }
                    }

                    MyApplicationInfo info = new MyApplicationInfo();
                    info.info = ai;
                    info.label = info.info.loadLabel(getContext().getPackageManager()).toString();
                    mPackageInfoList.add(info);
                }
                Collections.sort(mPackageInfoList, sDisplayNameComparator);
                if (mIncludeNothing) {
                    MyApplicationInfo info = new MyApplicationInfo();
                    info.label = context.getText(com.android.settingslib.R.string.no_application);
                    mPackageInfoList.add(0, info);
                }
                addAll(mPackageInfoList);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // A ViewHolder keeps references to children views to avoid unnecessary calls
                // to findViewById() on each row.
                AppViewHolder holder = AppViewHolder.createOrRecycle(mInflater, convertView);
                convertView = holder.rootView;
                MyApplicationInfo info = getItem(position);
                holder.appName.setText(info.label);
                if (info.info != null) {
                    holder.appIcon.setImageDrawable(
                            info.info.loadIcon(getContext().getPackageManager()));
                    holder.summary.setText(info.info.packageName);
                } else {
                    holder.appIcon.setImageDrawable(null);
                    holder.summary.setText("");
                }
                holder.disabled.setVisibility(View.GONE);
                holder.widget.setVisibility(View.GONE);
                return convertView;
            }
        }

        private final static Comparator<MyApplicationInfo> sDisplayNameComparator
                = new Comparator<MyApplicationInfo>() {
            public final int
            compare(MyApplicationInfo a, MyApplicationInfo b) {
                return collator.compare(a.label, b.label);
            }

            private final Collator collator = Collator.getInstance();
        };
    }
}
