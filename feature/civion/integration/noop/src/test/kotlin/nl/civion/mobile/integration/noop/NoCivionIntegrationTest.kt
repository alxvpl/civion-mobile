package nl.civion.mobile.integration.noop

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import kotlin.test.Test
import nl.civion.mobile.core.integration.CivionEdition

class NoCivionIntegrationTest {

    @Test
    fun `should report the standalone edition`() {
        assertThat(NoCivionIntegration.edition).isEqualTo(CivionEdition.STANDALONE)
    }

    @Test
    fun `should never report itself as available`() {
        assertThat(NoCivionIntegration.isAvailable()).isFalse()
    }
}
