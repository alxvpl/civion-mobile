package nl.civion.mobile.startup

import android.app.Activity
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import net.thunderbird.app.common.startup.StartupRouter
import nl.civion.mobile.core.account.CivionAccount
import nl.civion.mobile.core.account.CivionAccounts
import org.mockito.kotlin.mock

/**
 * What the router does, rather than what it decides.
 *
 * Only the path that hands back to upstream is exercised here. The other one ends in
 * `MessageHomeActivity.launch`, which needs an Android runtime, so it is covered where the
 * acceptance contract says it is rather than faked into a unit test that would prove nothing.
 *
 * The upstream path is the one worth pinning at this level anyway: it is what a first launch
 * takes, and what every start takes when the recorded account has gone. A CIVION override that
 * quietly swallowed it would leave a user with no accounts staring at a screen for an account
 * that does not exist, instead of at onboarding.
 */
class CivionStartupRouterBehaviourTest {

    @Test
    fun `should leave a first launch entirely to upstream`() {
        val upstream = RecordingRouter()
        val router = CivionStartupRouter(
            accounts = FakeAccounts(),
            upstream = upstream,
            recordedAccountUuid = { null },
        )

        router.routeToNextScreen(mock<Activity>())

        assertThat(upstream.routed).isEqualTo(1)
    }

    @Test
    fun `should hand back to upstream when the recorded account has been removed`() {
        val accounts = FakeAccounts(CivionAccount(uuid = "still-here", isSetupComplete = true))
        val upstream = RecordingRouter()
        val router = CivionStartupRouter(
            accounts = accounts,
            upstream = upstream,
            recordedAccountUuid = { "removed-since" },
        )

        router.routeToNextScreen(mock<Activity>())

        assertThat(upstream.routed).isEqualTo(1)
    }

    /**
     * Cleaning up half-finished accounts is upstream's, and on the path CIVION defers it must
     * stay upstream's. Doing it here as well would remove them twice.
     */
    @Test
    fun `should not clean up accounts itself when it defers`() {
        val accounts = FakeAccounts()
        val router = CivionStartupRouter(
            accounts = accounts,
            upstream = RecordingRouter(),
            recordedAccountUuid = { null },
        )

        router.routeToNextScreen(mock<Activity>())

        assertThat(accounts.removalRequested).isFalse()
    }

    @Test
    fun `should treat an account that never finished setup as not there`() {
        val accounts = FakeAccounts(CivionAccount(uuid = "interrupted", isSetupComplete = false))
        val upstream = RecordingRouter()
        val router = CivionStartupRouter(
            accounts = accounts,
            upstream = upstream,
            recordedAccountUuid = { "interrupted" },
        )

        router.routeToNextScreen(mock<Activity>())

        assertThat(upstream.routed).isEqualTo(1)
        assertThat(accounts.removalRequested).isFalse()
    }

    @Test
    fun `should offer only accounts that finished setup as start targets`() {
        val accounts = FakeAccounts(
            CivionAccount(uuid = "finished", isSetupComplete = true),
            CivionAccount(uuid = "interrupted", isSetupComplete = false),
        )

        val target = selectStartupAccount(
            recordedAccountUuid = "finished",
            usableAccountUuids = accounts.all().filter { it.isSetupComplete }.map { it.uuid }.toSet(),
        )

        assertThat(target == "finished").isTrue()
    }

    private class RecordingRouter : StartupRouter {
        var routed = 0
            private set

        override fun routeToNextScreen(activity: Activity) {
            routed++
        }
    }

    private class FakeAccounts(private vararg val accounts: CivionAccount) : CivionAccounts {
        var removalRequested = false
            private set

        override fun all(): List<CivionAccount> = accounts.toList()

        override fun removeUnfinished() {
            removalRequested = true
        }
    }
}
