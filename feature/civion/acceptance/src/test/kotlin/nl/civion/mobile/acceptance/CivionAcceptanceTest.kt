package nl.civion.mobile.acceptance

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test

/**
 * That the contract says something, and says it about the right journeys.
 *
 * An inventory of what is tested is worth exactly as much as the guarantee that it is complete
 * and not lying. These are the checks that give it that: nothing required may be missing,
 * nothing may claim coverage without naming what backs it, and nothing may excuse itself from
 * coverage without saying what is missing.
 *
 * The point is not that the numbers are good. Most of this inventory is pending. The point is
 * that dropping a journey, or quietly downgrading one, fails here instead of going unnoticed.
 */
class CivionAcceptanceTest {

    /**
     * The journeys the product is required to keep working. Adding one here before it exists in
     * the inventory is the intended way to introduce a new requirement: the build then fails
     * until someone has decided its tier, its runner, and whether it is covered.
     */
    private val required = setOf(
        "first-launch",
        "product-identity",
        "account-add",
        "account-remove",
        "google-oauth",
        "account-persistence",
        "inbox",
        "sync",
        "folders",
        "drawer",
        "message-open",
        "read-unread",
        "flag",
        "move-delete",
        "compose-send",
        "receive",
        "attachments",
        "notifications",
        "restart-process-recreation",
        "standalone-artifact",
        "integrated-artifact",
        "integration-failure-isolation",
    )

    @Test
    fun `should cover every required journey`() {
        val present = CivionAcceptance.journeys.map { it.id }.toSet()

        assertThat(required - present).isEmpty()
    }

    @Test
    fun `should not invent journeys nobody asked for`() {
        val present = CivionAcceptance.journeys.map { it.id }.toSet()

        assertThat(present - required).isEmpty()
    }

    @Test
    fun `should name each journey once`() {
        val ids = CivionAcceptance.journeys.map { it.id }

        assertThat(ids.size).isEqualTo(ids.toSet().size)
    }

    /**
     * A claim of coverage has to be followable. No evidence, or a blank entry among it, is a
     * journey marked done by someone who did not do it.
     */
    @Test
    fun `should back every claim of coverage with named evidence`() {
        val unbacked = CivionAcceptance.automated()
            .map { it to (it.coverage as AcceptanceCoverage.Automated).evidence }
            .filter { (_, evidence) -> evidence.isEmpty() || evidence.any { it.isBlank() } }

        assertThat(unbacked.map { (journey, _) -> journey.id }).isEmpty()
    }

    /**
     * Evidence is named once. The same test listed twice reads as two independent checks.
     */
    @Test
    fun `should not list the same evidence twice for one journey`() {
        val duplicated = CivionAcceptance.automated()
            .map { it to (it.coverage as AcceptanceCoverage.Automated).evidence }
            .filter { (_, evidence) -> evidence.size != evidence.toSet().size }

        assertThat(duplicated.map { (journey, _) -> journey.id }).isEmpty()
    }

    /**
     * And a journey that is not covered has to say what is missing, so the inventory doubles as
     * the order in which harnesses are worth building.
     */
    @Test
    fun `should say what each pending journey is waiting for`() {
        val unexplained = CivionAcceptance.pending()
            .filter { (it.coverage as AcceptanceCoverage.Pending).blockedBy.isBlank() }

        assertThat(unexplained.map { it.id }).isEmpty()
    }

    /**
     * A journey that needs a person cannot be in a tier that runs unattended. Putting one there
     * makes that tier permanently red or, worse, quietly skipped.
     */
    @Test
    fun `should keep journeys that need a person out of the unattended tiers`() {
        val misplaced = CivionAcceptance.journeys.filter {
            it.coverage.runner == AcceptanceRunner.HUMAN_WITH_PROVIDER && it.tier != AcceptanceTier.RELEASE
        }

        assertThat(misplaced.map { it.id }).isEmpty()
    }

    /**
     * The fast tier runs on every push, so nothing in it may need a mail server or a device.
     * A journey that does belongs in a slower tier until a runner for it exists.
     */
    @Test
    fun `should keep the fast tier to runners that run on every push`() {
        val tooSlow = CivionAcceptance.inTier(AcceptanceTier.FAST).filter {
            it.coverage.runner == AcceptanceRunner.LOCAL_MAIL_SERVER ||
                it.coverage.runner == AcceptanceRunner.HUMAN_WITH_PROVIDER
        }

        assertThat(tooSlow.map { it.id }).isEmpty()
    }

    /**
     * The fast tier is the one that actually gates a push, so it has to be genuinely checked.
     * A pending journey sitting in it would make the tier look green while checking nothing.
     */
    @Test
    fun `should let nothing sit in the fast tier unchecked`() {
        val pendingInFast = CivionAcceptance.inTier(AcceptanceTier.FAST).filterNot { it.isAutomated }

        assertThat(pendingInFast.map { it.id }).isEmpty()
    }
}
