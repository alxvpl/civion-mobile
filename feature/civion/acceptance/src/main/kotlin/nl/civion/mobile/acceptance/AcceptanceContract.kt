package nl.civion.mobile.acceptance

/**
 * When a journey is checked.
 *
 * The tiers are about cost, not importance. A journey is placed in the cheapest tier that can
 * actually run it, because a check that only runs when someone remembers to run it is not a
 * check.
 */
enum class AcceptanceTier {
    /** Runs on every push, in minutes. Unit tests and checks against the built artifact. */
    FAST,

    /** Runs unattended, nightly. Slower harnesses: a local mail server, an emulator. */
    NIGHTLY,

    /** Runs before a release. Includes what needs a real device or a real provider. */
    RELEASE,
}

/**
 * What has to be available for a journey to be checked at all.
 *
 * This is the thing that decides a journey's tier and, for most of the inventory, why it is not
 * automated yet: the runner does not exist. Naming the runner separately from the tier keeps
 * "we have not written this test" apart from "this cannot be a fast test".
 */
enum class AcceptanceRunner {
    /** A plain unit test. No Android, no network, no device. */
    JVM,

    /** Robolectric, or a device. Needed where the Android framework itself is the subject. */
    ANDROID_RUNTIME,

    /** A deterministic IMAP/SMTP server in the same process as the test. */
    LOCAL_MAIL_SERVER,

    /** A check made against a built APK rather than against source. */
    ARTIFACT_GATE,

    /** A person, signing in to a real provider. Cannot be automated and should not be faked. */
    HUMAN_WITH_PROVIDER,
}

/**
 * Whether a journey is actually checked, and by what.
 */
sealed interface AcceptanceCoverage {

    val runner: AcceptanceRunner

    /**
     * Checked. [evidence] names what does the checking - a test class, or a gate in the build
     * script - so a claim here can be followed to the thing that backs it.
     */
    data class Automated(
        override val runner: AcceptanceRunner,
        val evidence: String,
    ) : AcceptanceCoverage

    /**
     * Not checked yet. [blockedBy] says what is missing, in terms of what would have to be
     * built, never as a restatement of the journey.
     */
    data class Pending(
        override val runner: AcceptanceRunner,
        val blockedBy: String,
    ) : AcceptanceCoverage
}

/**
 * One thing Android Mail has to keep doing.
 */
data class AcceptanceJourney(
    val id: String,
    val title: String,
    val tier: AcceptanceTier,
    val coverage: AcceptanceCoverage,
) {
    val isAutomated: Boolean get() = coverage is AcceptanceCoverage.Automated
}
