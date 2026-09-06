package nl.civion.mobile

import app.k9mail.feature.widget.shortcut.LauncherShortcutActivity
import com.fsck.k9.AppConfig
import com.fsck.k9.DefaultAppConfig
import com.fsck.k9.activity.MessageCompose
import net.thunderbird.app.common.appCommonModule
import org.koin.android.ext.koin.androidApplication
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import nl.civion.mobile.auth.CivionOAuthConfigurationFactory
import nl.civion.mobile.auth.CivionOAuthConfigurationProvider
import nl.civion.mobile.dev.developmentModuleAdditions
import nl.civion.mobile.feature.featureModule
import nl.civion.mobile.featureflag.civionFeatureFlagModule
import nl.civion.mobile.provider.providerModule
import nl.civion.mobile.widget.provider.MessageListWidgetProvider
import nl.civion.mobile.widget.provider.UnreadWidgetProvider
import nl.civion.mobile.widget.widgetModule
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.override

private val civionOAuthProviderOverrideModule = module {
    single<OAuthConfigurationProvider> {
        CivionOAuthConfigurationProvider(
            factory = get(),
            logger = get(),
        )
    }.override()
}

val appModule = module {
    includes(civionFeatureFlagModule)
    includes(appCommonModule)

    includes(widgetModule)
    includes(featureModule)
    includes(providerModule)

    single(named("ClientInfoAppName")) { BuildConfig.CLIENT_INFO_APP_NAME }
    single(named("ClientInfoAppVersion")) { BuildConfig.VERSION_NAME }
    single<AppConfig> { appConfig }
    single<OAuthConfigurationFactory> { CivionOAuthConfigurationFactory(context = androidApplication()) }

    developmentModuleAdditions()

    // Must be included last so the upstream OAuthConfigurationProvider has
    // already been registered before this targeted CIVION override is loaded.
    includes(civionOAuthProviderOverrideModule)
}

val appConfig = DefaultAppConfig(
    componentsToDisable = listOf(
        MessageCompose::class.java,
        LauncherShortcutActivity::class.java,
        UnreadWidgetProvider::class.java,
        MessageListWidgetProvider::class.java,
    ),
)