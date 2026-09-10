package nl.civion.mobile.acceptance.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import nl.civion.mobile.acceptance.runtime.StartsAtOnboarding.finishOnUiThread
import nl.civion.mobile.core.integration.CivionIntegration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Journey `integration-failure-isolation`: the mail client works when the CIVION integration is
 * absent or unusable.
 *
 * The unit test shows that the contract is bound. That is not the journey. The journey is that a
 * user whose integration cannot be used still has a working mail client, and the only way to say
 * that is to start the application with the integration in that state and watch it arrive
 * somewhere usable.
 *
 * The integration is read out of the running application's own Koin graph, not constructed here,
 * so the state asserted is the state the client is actually running with.
 */
@RunWith(AndroidJUnit4::class)
class IntegrationIsolationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val integration: CivionIntegration
        get() = GlobalContext.get().get()

    @Test
    fun theClientStartsWhileTheIntegrationCannotBeUsed() {
        assertFalse(
            "Precondition: this build's integration reports itself usable, so this run says " +
                "nothing about what happens when it is not.",
            integration.isAvailable(),
        )

        val screen = StartsAtOnboarding.launchAndAwait(StartsAtOnboarding.ONBOARDING_ACTIVITY)

        assertNotNull(
            "The client did not reach a usable screen while the CIVION integration was " +
                "unusable. A mail client must not depend on the integration to start.",
            screen,
        )

        instrumentation.finishOnUiThread(screen!!)
    }

    /**
     * And the integration is still bound while unusable, rather than missing.
     *
     * An unbound contract fails at the first thing that asks for it, which is a different and
     * worse failure than one that answers "no": callers are written to carry on when the answer
     * is no, and cannot carry on when there is no answer.
     */
    @Test
    fun theIntegrationAnswersRatherThanBeingAbsent() {
        val edition = integration.edition

        assertNotNull(
            "The CIVION integration contract resolved to nothing at runtime.",
            edition,
        )
    }
}
