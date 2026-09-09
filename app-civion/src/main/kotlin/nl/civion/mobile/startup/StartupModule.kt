package nl.civion.mobile.startup

import net.thunderbird.app.common.startup.DefaultStartupRouter
import net.thunderbird.app.common.startup.StartupRouter
import org.koin.dsl.module
import org.koin.dsl.override

/**
 * Rebinds [StartupRouter] to CIVION's, keeping upstream's as the one it defers to.
 *
 * The upstream router is constructed here rather than resolved, because resolving
 * `StartupRouter` from inside its own replacement would resolve the replacement.
 *
 * Include this module *after* the upstream one, so the binding being overridden already exists.
 */
internal val civionStartupRouterOverrideModule = module {
    single<StartupRouter> {
        CivionStartupRouter(
            accountManager = get(),
            accountRemover = get(),
            upstream = DefaultStartupRouter(get(), get()),
        )
    }.override()
}
