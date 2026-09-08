package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.LegacyAccountDto

internal class MoveAccountTest {

    @Test
    fun `should move the account to the requested position`() = runTest {
        val accountManager = FakeLegacyAccountDtoManager(accounts = accounts())
        val testSubject = MoveAccount(accountManager, UnconfinedTestDispatcher(testScheduler))

        testSubject(accountUuid = UUID_A, toPosition = 2)

        assertThat(accountManager.movedAccounts).containsExactly(UUID_A to 2)
    }

    @Test
    fun `should clamp a position past the end of the list`() = runTest {
        val accountManager = FakeLegacyAccountDtoManager(accounts = accounts())
        val testSubject = MoveAccount(accountManager, UnconfinedTestDispatcher(testScheduler))

        testSubject(accountUuid = UUID_A, toPosition = 7)

        assertThat(accountManager.movedAccounts).containsExactly(UUID_A to 2)
    }

    @Test
    fun `should do nothing when the account no longer exists`() = runTest {
        val accountManager = FakeLegacyAccountDtoManager(accounts = accounts())
        val testSubject = MoveAccount(accountManager, UnconfinedTestDispatcher(testScheduler))

        testSubject(accountUuid = UUID_GONE, toPosition = 1)

        assertThat(accountManager.movedAccounts).isEmpty()
    }

    private fun accounts(): List<LegacyAccountDto> =
        listOf(UUID_A, UUID_B, UUID_C).map { LegacyAccountDto(uuid = it) }

    private companion object {
        const val UUID_A = "11111111-1111-4111-8111-111111111111"
        const val UUID_B = "22222222-2222-4222-8222-222222222222"
        const val UUID_C = "33333333-3333-4333-8333-333333333333"
        const val UUID_GONE = "44444444-4444-4444-8444-444444444444"
    }
}
