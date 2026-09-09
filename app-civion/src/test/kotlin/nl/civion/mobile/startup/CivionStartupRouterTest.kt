package nl.civion.mobile.startup

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

/**
 * The rule that decides which account a start opens.
 *
 * `null` means "leave it to upstream", which is the safe answer whenever the recorded account
 * cannot actually be opened. Getting that wrong is the way this feature breaks a start: an
 * account that was deleted, or one whose setup was never finished, would send the user to a
 * screen for an account that is not there.
 */
class CivionStartupRouterTest {

    @Test
    fun `should defer to upstream when nothing was recorded`() {
        val target = selectStartupAccount(
            recordedAccountUuid = null,
            usableAccountUuids = setOf("account-1", "account-2"),
        )

        assertThat(target).isNull()
    }

    @Test
    fun `should open the recorded account when it is usable`() {
        val target = selectStartupAccount(
            recordedAccountUuid = "account-2",
            usableAccountUuids = setOf("account-1", "account-2"),
        )

        assertThat(target).isEqualTo("account-2")
    }

    @Test
    fun `should defer to upstream when the recorded account no longer exists`() {
        val target = selectStartupAccount(
            recordedAccountUuid = "account-removed",
            usableAccountUuids = setOf("account-1"),
        )

        assertThat(target).isNull()
    }

    @Test
    fun `should defer to upstream when no account has finished setup`() {
        val target = selectStartupAccount(
            recordedAccountUuid = "account-1",
            usableAccountUuids = emptySet(),
        )

        assertThat(target).isNull()
    }
}
