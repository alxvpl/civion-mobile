package nl.civion.mobile.adapter.account

import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.settings.api.BackgroundAccountRemover
import nl.civion.mobile.core.account.CivionAccount
import nl.civion.mobile.core.account.CivionAccounts

/**
 * [CivionAccounts] over the Thunderbird account store.
 *
 * The engine owns accounts; this reads them and hands back CIVION's own [CivionAccount], so the
 * upstream account type stops here. Nothing is cached: the store is asked each time, because an
 * account can be added or removed while a caller holds a reference and a stale list would send
 * the user to an account that is gone.
 */
internal class ThunderbirdAccounts(
    private val accountManager: LegacyAccountManager,
    private val accountRemover: BackgroundAccountRemover,
) : CivionAccounts {

    override fun all(): List<CivionAccount> = accountManager.getAccounts().map { account ->
        CivionAccount(
            uuid = account.uuid,
            isSetupComplete = account.isFinishedSetup,
        )
    }

    override fun removeUnfinished() {
        accountManager.getAccounts()
            .filterNot { it.isFinishedSetup }
            .forEach { accountRemover.removeAccountAsync(it.uuid) }
    }
}
