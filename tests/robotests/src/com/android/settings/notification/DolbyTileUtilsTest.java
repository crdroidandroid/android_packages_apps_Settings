/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.settingslib.drawer.Tile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class DolbyTileUtilsTest {

    @Test
    public void isDolbyTile_matchesPackageName() {
        final Tile tile = mock(Tile.class);
        when(tile.getPackageName()).thenReturn("com.vendor.dolby.atmos");

        assertThat(DolbyTileUtils.isDolbyTile(tile)).isTrue();
    }

    @Test
    public void isDolbyTile_matchesComponentName() {
        final Tile tile = mock(Tile.class);
        when(tile.getPackageName()).thenReturn("com.vendor.audio");
        when(tile.getComponentName()).thenReturn("com.vendor.audio.DolbyActivity");

        assertThat(DolbyTileUtils.isDolbyTile(tile)).isTrue();
    }

    @Test
    public void isDolbyTile_withoutDolby_returnsFalse() {
        final Tile tile = mock(Tile.class);
        when(tile.getPackageName()).thenReturn("com.vendor.audio");
        when(tile.getComponentName()).thenReturn("com.vendor.audio.AudioActivity");

        assertThat(DolbyTileUtils.isDolbyTile(tile)).isFalse();
    }

    @Test
    public void findDolbyTile_returnsFirstMatchingTile() {
        final Tile nonDolbyTile = mock(Tile.class);
        when(nonDolbyTile.getPackageName()).thenReturn("com.vendor.audio");
        when(nonDolbyTile.getComponentName()).thenReturn("com.vendor.audio.AudioActivity");

        final Tile firstDolbyTile = mock(Tile.class);
        when(firstDolbyTile.getPackageName()).thenReturn("com.vendor.dolby.one");

        final Tile secondDolbyTile = mock(Tile.class);
        when(secondDolbyTile.getPackageName()).thenReturn("com.vendor.dolby.two");

        assertThat(DolbyTileUtils.findDolbyTile(
                Arrays.asList(nonDolbyTile, firstDolbyTile, secondDolbyTile)))
                .isSameInstanceAs(firstDolbyTile);
    }
}
