package nl.civion.mobile.startup

import assertk.assertThat
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import net.thunderbird.app.common.startup.DefaultStartupRouter
import net.thunderbird.app.common.startup.StartupRouter
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.settings.api.BackgroundAccountRemover
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.mockito.kotlin.mock

/**
 * That the CIVION router is the one actually bound.
 *
 * A Koin override survives an upstream merge, but it can stop *acting* without failing to
 * compile — upstream renaming the binding, or reaching the router some other way, would leave
 * the override registered against nothing while start-up quietly reverts to upstream
 * behaviour. On a device that shows up as "the app forgot which account I was in", which is
 * not a symptom anyone traces back to dependency injection. This test is what makes that a
 * red build instead.
 *
 * It cannot catch upstream abandoning the binding altogether; only the running application
 * can reveal that.
 */
class StartupModuleTest {

    @Test
    fun `should bind the CIVION router over the upstream one`() {
        val koin = koinApplication {
            modules(upstreamStandIn, civionStartupRouterOverrideModule)
        }.koin

        assertThat(koin.get<StartupRouter>()).isInstanceOf<CivionStartupRouter>()
    }

    @Test
    fun `should leave the upstream router bound when the override is absent`() {
        val koin = koinApplication {
            modules(upstreamStandIn)
        }.koin

        assertThat(koin.get<StartupRouter>()).isInstanceOf<DefaultStartupRouter>()
    }

    /**
     * Stands in for `appCommonStartupModule`: the upstream binding CIVION overrides, and the
     * two dependencies both routers are built from.
     */
    private val upstreamStandIn = module {
        single<LegacyAccountManager> { mock<LegacyAccountManager>() }
        single<BackgroundAccountRemover> { mock<BackgroundAccountRemover>() }
        single<StartupRouter> { DefaultStartupRouter(get(), get()) }
    }
}
