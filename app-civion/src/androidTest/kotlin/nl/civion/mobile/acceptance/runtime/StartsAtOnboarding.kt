package nl.civion.mobile.acceptance.runtime

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Starting the application the way the user does, and seeing where it lands.
 *
 * The launcher icon opens `MainActivity`, which resolves the start-up route and finishes
 * immediately, so the screen the user actually ends up on is a different activity. Watching for
 * that activity - rather than inspecting a router, a binding or an intent - is what makes the
 * tests here acceptance tests rather than restatements of the implementation.
 */
internal object StartsAtOnboarding {

    /** Onboarding is the launcher feature's screen. It is where a start with no account belongs. */
    const val ONBOARDING_ACTIVITY = "app.k9mail.feature.launcher.FeatureLauncherActivity"

    /** Where a start with a usable account belongs, and where a start without one must not go. */
    const val MESSAGE_LIST_ACTIVITY = "com.fsck.k9.activity.MessageHomeActivity"

    private const val START_TIMEOUT_MILLIS = 60_000L

    /**
     * Launches the application as the launcher icon does and returns the screen it settles on,
     * or `null` if [activityClassName] never appeared within the timeout.
     *
     * The caller is given the activity so it can be finished; leaving it up would make the next
     * test start from a screen rather than from a cold launch.
     */
    fun launchAndAwait(activityClassName: String): Activity? {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(activityClassName, null, false)

        try {
            instrumentation.targetContext.startLauncherIntent()
            return instrumentation.waitForMonitorWithTimeout(monitor, START_TIMEOUT_MILLIS)
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    /**
     * Whether [activityClassName] comes up on its own within a short window.
     *
     * Used to show that a screen is *not* reached. It cannot prove that in general - no finite
     * wait can - but the start it is racing against has already completed by the time this is
     * asked, so the screen would have to appear late for this to be wrong.
     */
    fun appearsShortly(activityClassName: String, millis: Long = 3_000L): Boolean {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(activityClassName, null, false)

        return try {
            instrumentation.waitForMonitorWithTimeout(monitor, millis) != null
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun Context.startLauncherIntent() {
        val intent = requireNotNull(packageManager.getLaunchIntentForPackage(packageName)) {
            "$packageName declares no launcher intent, so there is no way for a user to start it."
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    /** Leaves nothing on screen for the next test to inherit. */
    fun Instrumentation.finishOnUiThread(activity: Activity) {
        runOnMainSync { activity.finish() }
        waitForIdleSync()
    }
}
