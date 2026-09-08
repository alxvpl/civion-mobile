package net.thunderbird.app.common.startup

import android.app.Activity
import app.k9mail.feature.launcher.FeatureLauncherActivity
import app.k9mail.feature.launcher.FeatureLauncherTarget
import com.fsck.k9.activity.MessageHomeActivity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.settings.api.BackgroundAccountRemover
import nl.civion.mobile.navigation.CivionNavigationState

interface StartupRouter {
    fun routeToNextScreen(activity: Activity)
}

class DefaultStartupRouter(
    private val accountManager: LegacyAccountManager,
    private val accountRemover: BackgroundAccountRemover,
) : StartupRouter {
    override fun routeToNextScreen(activity: Activity) {
        val accounts = accountManager.getAccounts()
        deleteIncompleteAccounts(accounts)

        val hasAccountSetup = accounts.any { it.isFinishedSetup }
        if (!hasAccountSetup) {
            FeatureLauncherActivity.launch(activity, FeatureLauncherTarget.Onboarding)
        } else {
            val lastAccountUuid = resolveLastUsedAccount(activity, accounts)
            if (lastAccountUuid != null) {
                MessageHomeActivity.launch(activity, lastAccountUuid)
            } else {
                MessageHomeActivity.launch(activity)
            }
        }
    }

    /**
     * The account to open on a normal start: the one the user last used.
     *
     * Returns `null` when there is nothing recorded yet, or when the recorded account has since
     * been deleted; the caller then falls back to the plain upstream start, which is the safe
     * behaviour we want in exactly those cases.
     *
     * This deliberately resolves the account only. Where inside that account to land is decided
     * further down by [MessageHomeActivity], which opens an account at its default folder — the
     * Inbox. A cold start goes to the Inbox even if the account was last left in Sent; returning
     * to the last folder is a separate rule that applies only when switching accounts inside a
     * running application.
     */
    private fun resolveLastUsedAccount(activity: Activity, accounts: List<LegacyAccount>): String? {
        val lastAccountUuid = CivionNavigationState.lastActiveAccountUuid(activity) ?: return null

        return lastAccountUuid.takeIf { uuid ->
            accounts.any { it.uuid == uuid && it.isFinishedSetup }
        }
    }

    private fun deleteIncompleteAccounts(accounts: List<LegacyAccount>) {
        accounts.filter { !it.isFinishedSetup }.forEach {
            accountRemover.removeAccountAsync(it.uuid)
        }
    }
}
