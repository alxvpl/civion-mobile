package nl.civion.mobile.edition

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import kotlin.test.Test
import nl.civion.mobile.BuildConfig
import nl.civion.mobile.core.integration.CivionEdition
import nl.civion.mobile.core.integration.CivionIntegration
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * That the edition wired into the graph is the edition this build actually is.
 *
 * `civionEditionAdditions()` exists twice, once per flavour source set, and which copy is
 * compiled is decided by the flavour rather than by anything visible in the shared source. That
 * is what keeps the integration out of the standalone APK, and it is also what makes a mistake
 * invisible: swap the two files, or point a flavour at the wrong source set, and everything
 * still compiles and runs - as the other edition.
 *
 * This test runs for both flavours and compares what was wired against what the build says it
 * is, so the two cannot drift apart silently.
 */
class EditionModuleTest {

    @Test
    fun `should wire the edition this variant was built as`() {
        val expected = when (BuildConfig.CIVION_EDITION) {
            "standalone" -> CivionEdition.STANDALONE
            "integrated" -> CivionEdition.INTEGRATED
            else -> error("Unknown edition '${BuildConfig.CIVION_EDITION}'")
        }

        val koin = koinApplication {
            modules(module { civionEditionAdditions() })
        }.koin

        assertThat(koin.get<CivionIntegration>().edition).isEqualTo(expected)
    }

    /**
     * Nothing above the contract branches on the edition; it asks and carries on. Both editions
     * therefore have to answer, and the standalone one has to answer "no" rather than fail.
     */
    @Test
    fun `should answer availability rather than leave it unbound`() {
        val koin = koinApplication {
            modules(module { civionEditionAdditions() })
        }.koin

        val available = koin.get<CivionIntegration>().isAvailable()

        if (BuildConfig.CIVION_EDITION == "standalone") {
            assertThat(available).isFalse()
        }
    }
}
