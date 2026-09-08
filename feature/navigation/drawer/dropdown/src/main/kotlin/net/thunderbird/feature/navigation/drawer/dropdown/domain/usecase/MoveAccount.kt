package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase

/**
 * Puts an account at a new position in the account order.
 *
 * The order itself is the account manager's: it is the order accounts are stored and returned in,
 * so it survives a restart, a new account is appended to the end without a position of its own,
 * and a deleted account simply leaves the list. Nothing here keeps a second copy of that order.
 *
 * Runs off the main thread because moving an account rewrites the account settings storage.
 */
internal class MoveAccount(
    private val accountManager: LegacyAccountDtoManager,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : UseCase.MoveAccount {

    override suspend fun invoke(accountUuid: String, toPosition: Int) {
        withContext(coroutineContext) {
            val accounts = accountManager.getAccounts()
            val account = accounts.firstOrNull { it.uuid == accountUuid } ?: return@withContext

            // The account is removed before being re-inserted, so the last valid position is one
            // less than the number of accounts. Clamping here keeps a stale index from the drawer
            // — an account removed while the list was on screen — from being an exception.
            val position = toPosition.coerceIn(0, accounts.lastIndex)

            accountManager.moveAccount(account, position)
        }
    }
}
