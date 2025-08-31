/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.deviceinfo.storage;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SettingsSpinnerAdapter;
import com.android.settingslib.widget.SettingsSpinnerPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shows a spinner for users to select a storage volume.
 */
public class StorageSelectionPreferenceController extends BasePreferenceController implements
        AdapterView.OnItemSelectedListener {

    @VisibleForTesting
    SettingsSpinnerPreference mSpinnerPreference;
    @VisibleForTesting
    StorageAdapter mStorageAdapter;

    private final List<StorageEntry> mStorageEntries = new ArrayList<>();

    /** The interface for spinner selection callback. */
    public interface OnItemSelectedListener {
        /** Callbacked when the spinner selection is changed. */
        void onItemSelected(StorageEntry storageEntry);
    }
    private OnItemSelectedListener mOnItemSelectedListener;

    public StorageSelectionPreferenceController(Context context, String key) {
        super(context, key);

        mStorageAdapter = new StorageAdapter(context);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    /** Set the storages in the spinner. */
    public void setStorageEntries(List<StorageEntry> storageEntries) {
        mStorageAdapter.clear();
        mStorageEntries.clear();
        if (storageEntries == null || storageEntries.isEmpty()) {
            return;
        }
        Collections.sort(storageEntries);
        mStorageEntries.addAll(storageEntries);
        mStorageAdapter.addAll(storageEntries);

        if (mSpinnerPreference != null) {
            mSpinnerPreference.setVisible(mStorageAdapter.getCount() > 1);
        }
    }

    /** set selected storage in the spinner. */
    public void setSelectedStorageEntry(StorageEntry selectedStorageEntry) {
        if (mSpinnerPreference == null || !mStorageEntries.contains(selectedStorageEntry)) {
            return;
        }
        mSpinnerPreference.setSelection(mStorageAdapter.getPosition(selectedStorageEntry));
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mSpinnerPreference = screen.findPreference(getPreferenceKey());
        mSpinnerPreference.setAdapter(mStorageAdapter);
        mSpinnerPreference.setOnItemSelectedListener(this);
        mSpinnerPreference.setVisible(mStorageAdapter.getCount() > 1);
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        if (mOnItemSelectedListener == null) {
            return;
        }
        mOnItemSelectedListener.onItemSelected(
                (StorageEntry) mSpinnerPreference.getSelectedItem());
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // Do nothing.
    }

    @VisibleForTesting
    class StorageAdapter extends SettingsSpinnerAdapter<StorageEntry> {

        StorageAdapter(Context context) {
            super(context);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = getDefaultView(position, view, parent);
            }
            TextView textView = extractTextView(view);
            textView.setText(getItem(position).getDescription());
            return view;
        }

        @Override
        public View getDropDownView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = getDefaultDropDownView(position, view, parent);
            }
            TextView textView = extractTextView(view);
            textView.setText(getItem(position).getDescription());
            return view;
        }

        private TextView extractTextView(View root) {
            if (root instanceof TextView) {
                return (TextView) root;
            }
            // Try common ids first
            int titleId = root.getResources().getIdentifier(
                    "title", "id", "com.android.settingslib");
            TextView tv = titleId != 0 ? root.findViewById(titleId) : null;
            if (tv == null) {
                tv = root.findViewById(android.R.id.text1);
            }
            if (tv == null) {
                // Fallback: recursively find the first TextView
                tv = findFirstTextView(root);
            }
            if (tv == null) {
                throw new IllegalStateException("Spinner item view must contain a TextView");
            }
            return tv;
        }

        private TextView findFirstTextView(View v) {
            if (v instanceof TextView) return (TextView) v;
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    TextView tv = findFirstTextView(vg.getChildAt(i));
                    if (tv != null) return tv;
                }
            }
            return null;
        }
    }
}
