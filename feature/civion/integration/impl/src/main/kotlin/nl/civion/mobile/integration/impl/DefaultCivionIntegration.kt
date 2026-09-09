package nl.civion.mobile.integration.impl

import nl.civion.mobile.core.integration.CivionEdition
import nl.civion.mobile.core.integration.CivionIntegration

/**
 * The CIVION integration layer of the integrated edition.
 *
 * Present, and that is all it claims so far: [isAvailable] reports whether the integration can
 * be used at the moment it is asked, and with no Core protocol implemented yet there is nothing
 * it could reach. It answers `false` rather than `true`, because a caller must be able to trust
 * the answer — a build that says it can reach CIVION Core and then cannot is worse than one
 * that says it cannot.
 *
 * When the Core protocol lands, this is where it goes, and [isAvailable] starts answering from
 * the connection instead of from a constant. Nothing outside this module changes.
 */
class DefaultCivionIntegration : CivionIntegration {
    override val edition: CivionEdition = CivionEdition.INTEGRATED
    override fun isAvailable(): Boolean = false
}
