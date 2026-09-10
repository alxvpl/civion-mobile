package nl.civion.mobile.acceptance.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.civion.mobile.acceptance.AcceptanceCoverage
import nl.civion.mobile.acceptance.CivionAcceptance
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That the device tests the acceptance contract points at are really here.
 *
 * The counterparts in app-civion's unit tests and in feature:civion:navigation check the
 * evidence in the packages those modules own. Neither can see this one: instrumentation classes
 * are not on a JVM test classpath, so a contract entry naming a deleted or renamed device test
 * would go on reading like a true claim everywhere else. This is the side of that check that
 * only the device can run.
 */
@RunWith(AndroidJUnit4::class)
class AcceptanceEvidenceTest {

    private val ownedPackage = "nl.civion.mobile.acceptance.runtime."

    @Test
    fun everyDeviceTestTheContractNamesExists() {
        val missing = CivionAcceptance.automated()
            .flatMap { (it.coverage as AcceptanceCoverage.Automated).evidence }
            .filter { it.startsWith(ownedPackage) }
            .filterNot { runCatching { Class.forName(it) }.isSuccess }

        assertTrue("The acceptance contract names device tests that do not exist: $missing", missing.isEmpty())
    }

    /**
     * And that this suite is actually named by the contract, rather than running beside it.
     * A device suite nothing points at is not acceptance coverage; it is just tests.
     */
    @Test
    fun theContractNamesThisDeviceSuite() {
        val named = CivionAcceptance.automated()
            .flatMap { (it.coverage as AcceptanceCoverage.Automated).evidence }
            .count { it.startsWith(ownedPackage) }

        assertTrue("The acceptance contract names no device test at all.", named > 0)
    }
}
