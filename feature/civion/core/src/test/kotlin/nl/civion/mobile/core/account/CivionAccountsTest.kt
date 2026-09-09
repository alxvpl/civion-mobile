package nl.civion.mobile.core.account

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test

class CivionAccountsTest {

    @Test
    fun `should offer only accounts that finished setup`() {
        val accounts = FakeAccounts(
            CivionAccount(uuid = "finished", isSetupComplete = true),
            CivionAccount(uuid = "interrupted", isSetupComplete = false),
            CivionAccount(uuid = "also-finished", isSetupComplete = true),
        )

        assertThat(accounts.usable().map { it.uuid }).containsExactly("finished", "also-finished")
    }

    @Test
    fun `should offer nothing while setup has never been completed`() {
        val accounts = FakeAccounts(CivionAccount(uuid = "interrupted", isSetupComplete = false))

        assertThat(accounts.usable()).isEmpty()
    }

    private class FakeAccounts(private vararg val accounts: CivionAccount) : CivionAccounts {
        override fun all(): List<CivionAccount> = accounts.toList()
        override fun removeUnfinished() = Unit
    }
}
