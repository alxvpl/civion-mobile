package nl.civion.mobile.acceptance.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import nl.civion.mobile.acceptance.runtime.StartsAtOnboarding.finishOnUiThread
import nl.civion.mobile.navigation.CivionNavigationState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Journey `first-launch`: a first launch, with no account set up, reaches onboarding.
 *
 * The router-level unit test shows which branch is chosen. This shows what the user gets: the
 * application is started through its launcher intent, and the screen that comes up is
 * onboarding. Between the two lies everything a decision cannot cover - Koin actually starting,
 * the feature-flag state resolving, MainActivity handing over and finishing, the launcher
 * feature bringing up its screen - and a break in any of it leaves a user with no account
 * staring at a splash screen or at a message list for an account that does not exist.
 *
 * CIVION's start-up override is the reason this needs saying on a device at all: it is the one
 * that decides where a start lands, and it replaced upstream's.
 */
@RunWith(AndroidJUnit4::class)
class FirstLaunchTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    /**
     * A first launch means nothing recorded. The state is CIVION's own, and clearing it here
     * rather than trusting a fresh install is what stops this test from depending on whether
     * another test ran before it.
     */
    @Before
    fun clearRecordedStartupState() {
        val context = instrumentation.targetContext
        context.getSharedPreferences("civion_navigation_state", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        assertNotNull("Test setup", context)
    }

    @Test
    fun aFirstLaunchWithNoAccountReachesOnboarding() {
        val onboarding = StartsAtOnboarding.launchAndAwait(StartsAtOnboarding.ONBOARDING_ACTIVITY)

        assertNotNull(
            "A launch with no account set up did not reach onboarding " +
                "(${StartsAtOnboarding.ONBOARDING_ACTIVITY}) within the timeout.",
            onboarding,
        )

        instrumentation.finishOnUiThread(onboarding!!)
    }

    /**
     * And does not reach the message list.
     *
     * Landing there with no account is the failure this behaviour exists to prevent: a list that
     * can never have contents, for an account that was never added.
     */
    @Test
    fun aFirstLaunchDoesNotReachTheMessageList() {
        val onboarding = StartsAtOnboarding.launchAndAwait(StartsAtOnboarding.ONBOARDING_ACTIVITY)
        assertNotNull("Precondition: the launch has settled on onboarding", onboarding)

        val reachedMessageList = StartsAtOnboarding.appearsShortly(StartsAtOnboarding.MESSAGE_LIST_ACTIVITY)

        assertFalse(
            "A launch with no account set up opened the message list " +
                "(${StartsAtOnboarding.MESSAGE_LIST_ACTIVITY}).",
            reachedMessageList,
        )

        instrumentation.finishOnUiThread(onboarding!!)
    }

    /**
     * Nothing was recorded by a start that never reached an account. The next start has to be a
     * first launch too, rather than one that resolves an account the user never added.
     */
    @Test
    fun aFirstLaunchRecordsNoAccountToReturnTo() {
        val onboarding = StartsAtOnboarding.launchAndAwait(StartsAtOnboarding.ONBOARDING_ACTIVITY)
        assertNotNull("Precondition: the launch has settled on onboarding", onboarding)

        val recorded = CivionNavigationState.lastActiveAccountUuid(instrumentation.targetContext)

        org.junit.Assert.assertNull(
            "A launch that reached onboarding recorded '$recorded' as the account to return to.",
            recorded,
        )

        instrumentation.finishOnUiThread(onboarding!!)
    }
}
