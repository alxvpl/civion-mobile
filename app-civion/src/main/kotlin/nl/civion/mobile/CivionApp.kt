package nl.civion.mobile

import net.thunderbird.app.common.FeatureFlagApplication
import org.koin.core.module.Module

class CivionApp : FeatureFlagApplication() {
    /**
     * Feature-flag registry slot, not a display name.
     *
     * This value is used only as the `app` attribute of the feature-flag evaluation context, where
     * it selects a slot of
     * [net.thunderbird.core.featureflag.model.FlagRegistryOverride]. That type resolves exactly two
     * names, `k9` and `thunderbird`; any other value makes `catalog.overrides[app]` null, so no
     * variant override can ever be read and the app runs on bare catalog defaults.
     *
     * CIVION Mobile occupies the Thunderbird slot, which is where
     * [nl.civion.mobile.featureflag.CivionOverrides] supplies CIVION's own flag values. The user
     * facing application name comes from `AppNameProvider` and is unaffected.
     */
    override val appName: String = "thunderbird"
    override val appVersion: String = BuildConfig.VERSION_NAME

    override fun provideAppModule(): Module = appModule
}
