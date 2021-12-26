package com.google.android.settings.overlay;

import com.android.settings.applications.GameSettingsFeatureProvider;
import com.google.android.settings.games.GameSettingsFeatureProviderGoogleImpl;

public final class FeatureFactoryImpl extends com.android.settings.overlay.FeatureFactoryImpl {
    private GameSettingsFeatureProvider mGameSettingsFeatureProvider;

    @Override
    public GameSettingsFeatureProvider getGameSettingsFeatureProvider() {
        if (mGameSettingsFeatureProvider == null) {
            mGameSettingsFeatureProvider = new GameSettingsFeatureProviderGoogleImpl();
        }
        return mGameSettingsFeatureProvider;
    }
}
