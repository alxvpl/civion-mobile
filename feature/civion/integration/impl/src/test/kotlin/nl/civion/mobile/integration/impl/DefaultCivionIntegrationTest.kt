package nl.civion.mobile.integration.impl

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import kotlin.test.Test
import nl.civion.mobile.core.integration.CivionEdition

class DefaultCivionIntegrationTest {

    @Test
    fun `should report the integrated edition`() {
        assertThat(DefaultCivionIntegration().edition).isEqualTo(CivionEdition.INTEGRATED)
    }

    /**
     * Being compiled in is not the same as being usable. There is no CIVION Core protocol yet,
     * so the honest answer is that the integration cannot be used - and callers must be able to
     * trust that answer, because they are expected to carry on without it.
     */
    @Test
    fun `should not claim to be usable while there is nothing to reach`() {
        assertThat(DefaultCivionIntegration().isAvailable()).isFalse()
    }
}
