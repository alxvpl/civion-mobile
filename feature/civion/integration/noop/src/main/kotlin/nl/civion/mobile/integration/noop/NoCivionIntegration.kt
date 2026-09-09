package nl.civion.mobile.integration.noop

import nl.civion.mobile.core.integration.CivionEdition
import nl.civion.mobile.core.integration.CivionIntegration

/**
 * The standalone edition has no CIVION integration.
 *
 * It answers the contract rather than leaving it unbound, so nothing above has to know which
 * edition it is running in, and a caller that forgets to check [isAvailable] gets a truthful
 * "no" instead of a missing binding.
 */
object NoCivionIntegration : CivionIntegration {
    override val edition: CivionEdition = CivionEdition.STANDALONE
    override fun isAvailable(): Boolean = false
}
