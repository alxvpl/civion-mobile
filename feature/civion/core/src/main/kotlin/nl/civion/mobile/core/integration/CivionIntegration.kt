package nl.civion.mobile.core.integration

/**
 * Which edition of Android Mail this build is.
 *
 * One product, one application id, two editions. They are not two applications: the integrated
 * edition is an upgrade of the standalone one on the same device, over the same data, signed
 * with the same key. What separates them is which code was compiled in, not who they claim to
 * be.
 */
enum class CivionEdition {
    /** A complete mail client, carrying no CIVION integration code at all. */
    STANDALONE,

    /** The same mail client, plus the CIVION integration layer. */
    INTEGRATED,
}

/**
 * The CIVION integration layer, as the rest of Android Mail sees it.
 *
 * Bound in every build, because the mail client must not have to know whether it is running in
 * an edition that has an integration. The standalone edition binds an implementation that says
 * so and does nothing; the integrated edition binds the real one. Nothing above this contract
 * branches on the edition — it asks [isAvailable] and carries on either way, which is also what
 * keeps a failing integration from taking the mail client with it.
 */
interface CivionIntegration {

    /** The edition this build was compiled as. */
    val edition: CivionEdition

    /**
     * Whether the integration can be used right now.
     *
     * Always `false` in the standalone edition, where there is nothing to use. In the integrated
     * edition it answers for the moment it is asked, not for the build: an integration that is
     * present can still be unreachable, unconfigured or failing, and callers must treat that the
     * same way they treat its absence.
     */
    fun isAvailable(): Boolean
}
