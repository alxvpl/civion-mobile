package nl.civion.mobile.navigation

import assertk.assertThat
import assertk.assertions.isEmpty
import kotlin.test.Test
import nl.civion.mobile.acceptance.AcceptanceCoverage
import nl.civion.mobile.acceptance.CivionAcceptance

/**
 * That the tests the acceptance contract points at in this module are really there.
 *
 * See the counterpart in app-civion: the contract names evidence as a string so one inventory
 * can span modules, and this is what stops a renamed or deleted test from leaving behind a
 * claim that reads exactly like a true one.
 */
class AcceptanceEvidenceTest {

    @Test
    fun `should find every test this module is named for`() {
        val missing = CivionAcceptance.automated()
            .flatMap { (it.coverage as AcceptanceCoverage.Automated).evidence }
            .filter { it.startsWith("nl.civion.mobile.navigation.") }
            .filterNot { runCatching { Class.forName(it) }.isSuccess }

        assertThat(missing).isEmpty()
    }
}
