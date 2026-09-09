package nl.civion.mobile.acceptance

import nl.civion.mobile.acceptance.AcceptanceCoverage.Automated
import nl.civion.mobile.acceptance.AcceptanceCoverage.Pending
import nl.civion.mobile.acceptance.AcceptanceRunner.ANDROID_RUNTIME
import nl.civion.mobile.acceptance.AcceptanceRunner.ARTIFACT_GATE
import nl.civion.mobile.acceptance.AcceptanceRunner.HUMAN_WITH_PROVIDER
import nl.civion.mobile.acceptance.AcceptanceRunner.JVM
import nl.civion.mobile.acceptance.AcceptanceRunner.LOCAL_MAIL_SERVER
import nl.civion.mobile.acceptance.AcceptanceTier.FAST
import nl.civion.mobile.acceptance.AcceptanceTier.NIGHTLY
import nl.civion.mobile.acceptance.AcceptanceTier.RELEASE

/**
 * What Android Mail is held to.
 *
 * Most of this is honestly marked [Pending]: the mail engine is Thunderbird's and works, but
 * nothing here proves it still works after a change we made. Writing the inventory down as code
 * is what turns that from an unknown into a measured number, and what stops a journey from being
 * quietly dropped instead of quietly failing.
 *
 * A [Pending] entry is a debt with a named cost, not a wish. It says which runner it needs, so
 * the order to build runners in can be read off the list rather than guessed at.
 */
object CivionAcceptance {

    val journeys: List<AcceptanceJourney> = listOf(

        // ---------------------------------------------------------------- start-up ---

        AcceptanceJourney(
            id = "first-launch",
            title = "A first launch, with no account set up, reaches onboarding",
            tier = FAST,
            coverage = Automated(JVM, "nl.civion.mobile.startup.CivionStartupRouterBehaviourTest"),
        ),
        AcceptanceJourney(
            id = "account-persistence",
            title = "The account and folder the user was last in survive a restart",
            tier = FAST,
            coverage = Automated(ANDROID_RUNTIME, "nl.civion.mobile.navigation.CivionNavigationStateTest"),
        ),
        AcceptanceJourney(
            id = "restart-process-recreation",
            title = "A restart returns to the last used account, at its Inbox",
            tier = NIGHTLY,
            coverage = Pending(
                ANDROID_RUNTIME,
                "The decision is covered; the launch it ends in is not. Needs MessageHomeActivity " +
                    "under an Android runtime, which no harness starts yet.",
            ),
        ),

        // ----------------------------------------------------------------- account ---

        AcceptanceJourney(
            id = "account-add",
            title = "An account can be added, and a duplicate address is refused",
            tier = NIGHTLY,
            coverage = Pending(
                LOCAL_MAIL_SERVER,
                "Setup runs autodiscovery and then connects. Needs a deterministic IMAP/SMTP " +
                    "server in-process; none exists in this tree.",
            ),
        ),
        AcceptanceJourney(
            id = "account-remove",
            title = "An account can be removed, and nothing of it is left behind",
            tier = NIGHTLY,
            coverage = Pending(
                ANDROID_RUNTIME,
                "Removal is asynchronous and reaches storage. Needs an Android runtime with the " +
                    "account store; no harness starts one yet.",
            ),
        ),
        AcceptanceJourney(
            id = "google-oauth",
            title = "A Gmail account signs in over OAuth and can read and send mail",
            tier = RELEASE,
            coverage = Pending(
                HUMAN_WITH_PROVIDER,
                "Google will not issue tokens to a robot on a registered Android client. Stays a " +
                    "human check; the build gate covers what can be checked without one, that the " +
                    "client id is compiled in and the APK carries the registered certificate.",
            ),
        ),

        // -------------------------------------------------------------------- mail ---

        AcceptanceJourney(
            id = "inbox",
            title = "The Inbox lists the messages the server has",
            tier = NIGHTLY,
            coverage = Pending(LOCAL_MAIL_SERVER, "Needs a deterministic IMAP server."),
        ),
        AcceptanceJourney(
            id = "sync",
            title = "Synchronisation brings new messages down and reflects remote changes",
            tier = NIGHTLY,
            coverage = Pending(LOCAL_MAIL_SERVER, "Needs a deterministic IMAP server."),
        ),
        AcceptanceJourney(
            id = "folders",
            title = "The folder list is fetched and a folder can be opened",
            tier = NIGHTLY,
            coverage = Pending(LOCAL_MAIL_SERVER, "Needs a deterministic IMAP server."),
        ),
        AcceptanceJourney(
            id = "message-open",
            title = "A message opens and renders",
            tier = NIGHTLY,
            coverage = Pending(LOCAL_MAIL_SERVER, "Needs a deterministic IMAP server."),
        ),
        AcceptanceJourney(
            id = "read-unread",
            title = "Read and unread state is set locally and reaches the server",
            tier = NIGHTLY,
            coverage = Pending(LOCAL_MAIL_SERVER, "Needs a deterministic IMAP server."),
        ),
        AcceptanceJourney(
            id = "flag",
            title = "A message can be flagged, and the flag survives a sync",
            tier = NIGHTLY,
            coverage = Pending(LOCAL_MAIL_SERVER, "Needs a deterministic IMAP server."),
        ),
        AcceptanceJourney(
            id = "move-delete",
            title = "A message can be moved between folders and deleted",
            tier = NIGHTLY,
            coverage = Pending(LOCAL_MAIL_SERVER, "Needs a deterministic IMAP server."),
        ),
        AcceptanceJourney(
            id = "compose-send",
            title = "A message can be composed and sent",
            tier = NIGHTLY,
            coverage = Pending(LOCAL_MAIL_SERVER, "Needs a deterministic SMTP server."),
        ),
        AcceptanceJourney(
            id = "receive",
            title = "A message that arrives on the server reaches the user",
            tier = NIGHTLY,
            coverage = Pending(LOCAL_MAIL_SERVER, "Needs a deterministic IMAP server."),
        ),
        AcceptanceJourney(
            id = "attachments",
            title = "An attachment can be opened and saved",
            tier = NIGHTLY,
            coverage = Pending(
                ANDROID_RUNTIME,
                "Opening an attachment leaves the application through the platform. Needs an " +
                    "Android runtime.",
            ),
        ),
        AcceptanceJourney(
            id = "notifications",
            title = "A new message notifies, and the notification opens its own account",
            tier = NIGHTLY,
            coverage = Pending(
                ANDROID_RUNTIME,
                "The recorded hook changes which intent is built. Needs an Android runtime to " +
                    "read the resulting PendingIntent.",
            ),
        ),
        AcceptanceJourney(
            id = "drawer",
            title = "The drawer lists accounts and folders, and accounts can be reordered",
            tier = NIGHTLY,
            coverage = Pending(
                ANDROID_RUNTIME,
                "The drawer is Compose and is constructed by MessageHomeActivity. Needs an " +
                    "Android runtime; the reorder use case itself is covered upstream-side by " +
                    "MoveAccountTest.",
            ),
        ),

        // ----------------------------------------------------------------- editions ---

        AcceptanceJourney(
            id = "standalone-artifact",
            title = "The standalone artifact carries no CIVION integration code",
            tier = FAST,
            coverage = Automated(ARTIFACT_GATE, "build-civion-mobile.ps1: edition gate, -Edition standalone"),
        ),
        AcceptanceJourney(
            id = "integrated-artifact",
            title = "The integrated artifact carries the CIVION integration layer",
            tier = FAST,
            coverage = Automated(ARTIFACT_GATE, "build-civion-mobile.ps1: edition gate, -Edition integrated"),
        ),
        AcceptanceJourney(
            id = "integration-failure-isolation",
            title = "The mail client works when the CIVION integration is absent or unusable",
            tier = FAST,
            coverage = Automated(JVM, "nl.civion.mobile.edition.EditionModuleTest"),
        ),
    )

    /** The journeys a given tier is expected to run. */
    fun inTier(tier: AcceptanceTier): List<AcceptanceJourney> = journeys.filter { it.tier == tier }

    /** What is actually checked today. */
    fun automated(): List<AcceptanceJourney> = journeys.filter { it.isAutomated }

    /** What is not, with the reason attached. */
    fun pending(): List<AcceptanceJourney> = journeys.filterNot { it.isAutomated }

    /**
     * A one-line-per-journey report, for a build log.
     */
    fun report(): String = buildString {
        appendLine("CIVION Mobile acceptance contract: ${automated().size}/${journeys.size} automated")
        AcceptanceTier.entries.forEach { tier ->
            val inTier = inTier(tier)
            if (inTier.isEmpty()) return@forEach
            appendLine("  $tier (${inTier.count { it.isAutomated }}/${inTier.size})")
            inTier.forEach { journey ->
                val mark = if (journey.isAutomated) "ok     " else "pending"
                appendLine("    $mark ${journey.id} [${journey.coverage.runner}]")
            }
        }
    }
}
