package nl.civion.mobile.startup

import android.app.Activity
import com.fsck.k9.activity.MessageHomeActivity
import net.thunderbird.app.common.startup.StartupRouter
import nl.civion.mobile.core.account.CivionAccounts
import nl.civion.mobile.core.account.usable
import nl.civion.mobile.navigation.CivionNavigationState

/**
 * Opens the account the user last used, on a normal start.
 *
 * This replaces the upstream `StartupRouter` binding through Koin instead of editing
 * `app-common`, which is built into K-9 and Thunderbird as well as CIVION Mobile. The
 * previous implementation of this behaviour patched `DefaultStartupRouter` directly; that
 * change was invisible to the engine-footprint guard, because `app-common` sits outside the
 * paths it measured. Binding here keeps the behaviour and removes the divergence.
 *
 * The override is deliberately narrow. CIVION decides exactly one thing — *which* account a
 * start lands in — and only when there is a usable recorded one. Everything else is
 * [upstream]'s: onboarding on a first run, the fallback when nothing is recorded, and whatever
 * upstream may add to a start later.
 *
 * Accounts are reached through [CivionAccounts] rather than through the engine's account
 * manager, so no upstream account type appears here. [MessageHomeActivity] does appear: it is
 * the screen being opened, and until CIVION owns that screen, launching it is what starting
 * the application means.
 *
 * Where inside the account the start lands is not decided here. [MessageHomeActivity] opens an
 * account at its default folder, so a cold start reaches the Inbox even when the account was
 * last left in Sent. Returning to the last folder is a separate rule that applies only when
 * switching accounts inside a running application.
 */
internal class CivionStartupRouter(
    private val accounts: CivionAccounts,
    private val upstream: StartupRouter,
    private val recordedAccountUuid: (Activity) -> String? = CivionNavigationState::lastActiveAccountUuid,
) : StartupRouter {

    override fun routeToNextScreen(activity: Activity) {
        val target = selectStartupAccount(
            recordedAccountUuid = recordedAccountUuid(activity),
            usableAccountUuids = accounts.usable().map { it.uuid }.toSet(),
        )

        if (target == null) {
            upstream.routeToNextScreen(activity)
            return
        }

        // Upstream removes half-finished accounts before it routes. On the path CIVION takes
        // over, upstream never runs, so that one side effect is reproduced here. Doing it after
        // the decision rather than before is safe and avoids removing twice: an account that is
        // not finished can never be selected, so the removal cannot change the outcome.
        accounts.removeUnfinished()

        MessageHomeActivity.launch(activity, target)
    }
}

/**
 * The account a start should open, or `null` to leave the decision to upstream.
 *
 * Separated from the router so the rule can be read and tested on its own: it is the only part
 * of the override that carries a decision, and it needs neither Android nor an account store.
 *
 * `null` is returned when nothing was recorded yet, and when the recorded account is no longer
 * usable — deleted between two runs, or never finished setup. Both cases must fall back to the
 * plain upstream start rather than to an account the user cannot open.
 */
internal fun selectStartupAccount(
    recordedAccountUuid: String?,
    usableAccountUuids: Set<String>,
): String? = recordedAccountUuid?.takeIf { it in usableAccountUuids }
