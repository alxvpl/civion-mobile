package nl.civion.mobile.adapter

import nl.civion.mobile.adapter.account.ThunderbirdAccounts
import nl.civion.mobile.core.account.CivionAccounts
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Binds the CIVION contracts to their Thunderbird implementations.
 *
 * These are additions, not overrides: no upstream binding is replaced, so this module can be
 * included at any point in the graph. Include it before anything that consumes the contracts.
 */
val civionAdapterModule: Module = module {
    single<CivionAccounts> {
        ThunderbirdAccounts(
            accountManager = get(),
            accountRemover = get(),
        )
    }
}
