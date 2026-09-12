package nl.civion.mobile.provider

import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import com.fsck.k9.preferences.FilePrefixProvider
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.core.ui.theme.api.ThemeProvider
import nl.civion.mobile.ui.settings.CivionSettingsNavigator
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.binds
import org.koin.dsl.module

internal val providerModule = module {
    single {
        CivionAppNameProvider(androidContext())
    } binds arrayOf(AppNameProvider::class, BrandNameProvider::class, FilePrefixProvider::class)

    single<ThemeProvider> { CivionThemeProvider() }

    single<FeatureThemeProvider> { CivionFeatureThemeProvider() }

    single<CivionSettingsNavigator> { CivionSettingsNavigatorImpl() }

    single<NotificationIconResourceProvider> {
        CivionAppNotificationIconProvider()
    }
}
