package nl.civion.mobile.adapter

import assertk.assertThat
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.settings.api.BackgroundAccountRemover
import nl.civion.mobile.adapter.account.ThunderbirdAccounts
import nl.civion.mobile.core.account.CivionAccounts
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.mockito.kotlin.mock

/**
 * That every CIVION contract is actually bound to its Thunderbird implementation.
 *
 * The contracts are what CIVION's own layers resolve; an unbound one is a crash on the first
 * screen that asks for it, and nothing about the source of a contract and its implementation
 * living in different modules makes that visible at compile time. Resolving the real module
 * here is what does.
 */
class CivionAdapterModuleTest {

    @Test
    fun `should bind accounts to the Thunderbird account store`() {
        val koin = koinApplication {
            modules(engineStandIn, civionAdapterModule)
        }.koin

        assertThat(koin.get<CivionAccounts>()).isInstanceOf<ThunderbirdAccounts>()
    }

    /** The upstream bindings the adapter is built from. */
    private val engineStandIn = module {
        single<LegacyAccountManager> { mock<LegacyAccountManager>() }
        single<BackgroundAccountRemover> { mock<BackgroundAccountRemover>() }
    }
}
