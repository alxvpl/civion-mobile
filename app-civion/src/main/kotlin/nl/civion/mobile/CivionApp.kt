package nl.civion.mobile

import net.thunderbird.app.common.FeatureFlagApplication
import org.koin.core.module.Module

class CivionApp : FeatureFlagApplication() {
    override val appName: String = "civion"
    override val appVersion: String = BuildConfig.VERSION_NAME

    override fun provideAppModule(): Module = appModule
}
