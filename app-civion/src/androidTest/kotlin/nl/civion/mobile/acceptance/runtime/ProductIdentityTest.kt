package nl.civion.mobile.acceptance.runtime

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.thunderbird.core.common.provider.AppNameProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Journey `product-identity`: the running application presents itself as CIVION Mobile.
 *
 * This product is a third persona inside a codebase that also builds K-9 Mail and Thunderbird
 * for Android, and it is composed rather than forked: which name the user sees is decided by a
 * handful of bindings and a resource override. Getting that wrong does not break a build - it
 * ships a product that calls itself something else, in its launcher entry, its notifications and
 * its settings.
 *
 * The name is read from the running application's own graph and from the package manager, which
 * is where the user reads it from too.
 */
@RunWith(AndroidJUnit4::class)
class ProductIdentityTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val appNameProvider: AppNameProvider
        get() = GlobalContext.get().get()

    @Test
    fun theApplicationCallsItselfCivionMobile() {
        assertEquals("CIVION Mobile", appNameProvider.appName)
    }

    /**
     * The label the launcher shows. It comes from the manifest and the resource override rather
     * than from the binding above, so the two can disagree, and the one the user sees is this.
     */
    @Test
    fun theLauncherEntryIsLabelledCivionMobile() {
        val context = instrumentation.targetContext
        val packageManager = context.packageManager
        val info = packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)

        val label = packageManager.getApplicationLabel(info).toString()

        assertEquals("CIVION Mobile", label)
    }

    /**
     * And it is not one of the other two products this codebase builds.
     *
     * Stated separately because the failure it guards against is composition falling back to an
     * upstream default, which produces a plausible name rather than an empty one.
     */
    @Test
    fun theApplicationDoesNotPresentItselfAsAnUpstreamProduct() {
        val names = setOf(
            appNameProvider.appName,
            instrumentation.targetContext.packageManager
                .getApplicationLabel(instrumentation.targetContext.applicationInfo)
                .toString(),
        )

        val upstreamNames = listOf("K-9 Mail", "Thunderbird")

        upstreamNames.forEach { upstream ->
            assertTrue(
                "The application presents itself as '$upstream'. It is CIVION Mobile.",
                names.none { it.contains(upstream, ignoreCase = true) },
            )
        }
    }
}
