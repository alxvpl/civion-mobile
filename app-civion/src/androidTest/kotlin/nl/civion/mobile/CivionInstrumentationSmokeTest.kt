package nl.civion.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That there is an Android runtime to test on at all.
 *
 * This is infrastructure evidence, not product coverage. It deliberately says nothing about
 * mail: what it proves is that Gradle can provision the managed device, boot it, install both
 * the application and the instrumentation APK, start a test process inside the application, and
 * shut the device down again — the sequence every Android-runtime journey in
 * `feature:civion:acceptance` is waiting on.
 *
 * `assembleAndroidTest` proves none of that. It proves an APK can be built. Everything that
 * actually goes wrong with an emulator goes wrong afterwards.
 */
@RunWith(AndroidJUnit4::class)
class CivionInstrumentationSmokeTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    /**
     * Two packages, both installed: the application under test, and the test APK driving it.
     * If either were missing the run would not have reached this line, but asserting it is what
     * distinguishes "the test ran" from "the test ran against the right thing".
     */
    @Test
    fun bothApplicationAndTestPackageAreInstalled() {
        val target = instrumentation.targetContext.packageName
        val test = instrumentation.context.packageName

        assertEquals(BuildConfig.APPLICATION_ID, target)
        assertEquals("nl.civion.mobile.tests", test)
        assertNotEquals(target, test)
    }

    /**
     * The application object is CIVION's, which means the real Application started in this
     * process rather than a default one. Anything a later journey does — resolving a binding,
     * opening a screen — depends on that having happened.
     */
    @Test
    fun theApplicationUnderTestIsCivionMobile() {
        val application = instrumentation.targetContext.applicationContext

        assertTrue(
            "Expected a CivionApp, got ${application.javaClass.name}",
            application is CivionApp,
        )
    }

    /**
     * The device is a real one, running the API level the managed device declares. A run that
     * silently landed on some other image would still pass everything above.
     */
    @Test
    fun theDeviceIsTheApiLevelTheManagedDeviceDeclares() {
        assertEquals(36, android.os.Build.VERSION.SDK_INT)
    }

    /**
     * And it is the edition this variant was built as. The editions differ by compiled-in code,
     * so an instrumentation run has to be able to tell which one it is looking at before any
     * journey can assert edition-specific behaviour.
     */
    @Test
    fun theEditionUnderTestIsTheOneThisVariantWasBuiltAs() {
        assertTrue(
            "Unexpected edition '${BuildConfig.CIVION_EDITION}'",
            BuildConfig.CIVION_EDITION in setOf("standalone", "integrated"),
        )
    }
}
