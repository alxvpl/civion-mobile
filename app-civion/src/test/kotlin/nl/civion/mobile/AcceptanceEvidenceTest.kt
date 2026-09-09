package nl.civion.mobile

import assertk.assertThat
import assertk.assertions.isEmpty
import kotlin.test.Test
import nl.civion.mobile.acceptance.AcceptanceCoverage
import nl.civion.mobile.acceptance.CivionAcceptance

/**
 * That the tests the acceptance contract points at are really there.
 *
 * The contract names its evidence as a string, which is what lets one inventory describe
 * journeys covered in modules that cannot see each other. The cost of a string is that deleting
 * or renaming a test leaves the contract claiming a coverage that no longer exists, and claiming
 * it in exactly the confident tone of a claim that does.
 *
 * Each module checks the evidence in the packages it owns. This one owns the start-up and
 * edition journeys.
 */
class AcceptanceEvidenceTest {

    private val ownedPackages = listOf(
        "nl.civion.mobile.startup.",
        "nl.civion.mobile.edition.",
        "nl.civion.mobile.reader.",
    )

    @Test
    fun `should find every test this module is named for`() {
        val missing = CivionAcceptance.automated()
            .mapNotNull { (it.coverage as AcceptanceCoverage.Automated).evidence.takeIf { e -> e.isOwned() } }
            .filterNot { it.isLoadable() }

        assertThat(missing).isEmpty()
    }

    private fun String.isOwned(): Boolean = ownedPackages.any { startsWith(it) }

    private fun String.isLoadable(): Boolean =
        runCatching { Class.forName(this) }.isSuccess
}
