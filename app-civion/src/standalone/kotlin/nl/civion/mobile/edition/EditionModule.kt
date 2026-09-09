package nl.civion.mobile.edition

import nl.civion.mobile.core.integration.CivionIntegration
import nl.civion.mobile.integration.noop.NoCivionIntegration
import org.koin.core.module.Module

/**
 * Standalone edition wiring.
 *
 * The integrated edition has a file of the same name in its own source set, and only one of the
 * two is ever compiled. That is why `appModule` can wire the edition without naming one: the
 * choice is made by which source set the flavour selects, not by a branch at runtime.
 */
fun Module.civionEditionAdditions() {
    single<CivionIntegration> { NoCivionIntegration }
}
