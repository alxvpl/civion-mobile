package nl.civion.mobile.navigation

import android.content.Context
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Where the user was, across a restart.
 *
 * This class is what two of the recorded engine hooks exist to feed, and until now nothing
 * exercised it. Its whole purpose is to survive process death, which is exactly the property
 * that cannot be observed by reading the code: an implementation that kept the value in memory
 * would look identical and pass every compile-time check.
 *
 * Robolectric gives a real SharedPreferences on a real Context, so a write here is a write to
 * the same store the application uses.
 */
@RunWith(RobolectricTestRunner::class)
class CivionNavigationStateTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `should record nothing before the user has been anywhere`() {
        assertThat(CivionNavigationState.lastActiveAccountUuid(context)).isNull()
        assertThat(CivionNavigationState.lastFolderId(context, "account-1")).isNull()
    }

    @Test
    fun `should give back the account and folder it was told about`() {
        CivionNavigationState.recordActiveAccount(context, "account-1", folderId = 42L)

        assertThat(CivionNavigationState.lastActiveAccountUuid(context)).isEqualTo("account-1")
        assertThat(CivionNavigationState.lastFolderId(context, "account-1")).isEqualTo(42L)
    }

    /**
     * The folder is remembered per account, not globally. Switching to another account and back
     * has to return to where each of them was left, which is the whole point of keying it.
     */
    @Test
    fun `should keep a folder per account rather than one folder overall`() {
        CivionNavigationState.recordActiveAccount(context, "account-1", folderId = 11L)
        CivionNavigationState.recordActiveAccount(context, "account-2", folderId = 22L)

        assertThat(CivionNavigationState.lastFolderId(context, "account-1")).isEqualTo(11L)
        assertThat(CivionNavigationState.lastFolderId(context, "account-2")).isEqualTo(22L)
        assertThat(CivionNavigationState.lastActiveAccountUuid(context)).isEqualTo("account-2")
    }

    @Test
    fun `should follow the user to a new folder within the same account`() {
        CivionNavigationState.recordActiveAccount(context, "account-1", folderId = 11L)
        CivionNavigationState.recordActiveAccount(context, "account-1", folderId = 12L)

        assertThat(CivionNavigationState.lastFolderId(context, "account-1")).isEqualTo(12L)
    }

    /**
     * A read from a fresh Context is what a restart looks like from this class's side: the
     * process is gone, and only what reached the preferences file is still there.
     */
    @Test
    fun `should survive a restart`() {
        CivionNavigationState.recordActiveAccount(context, "account-1", folderId = 42L)

        val afterRestart: Context = RuntimeEnvironment.getApplication()

        assertThat(CivionNavigationState.lastActiveAccountUuid(afterRestart)).isEqualTo("account-1")
        assertThat(CivionNavigationState.lastFolderId(afterRestart, "account-1")).isEqualTo(42L)
    }

    /**
     * An account with nothing recorded must not inherit another account's folder. Returning one
     * would open a list belonging to a different account.
     */
    @Test
    fun `should not answer for an account it was never told about`() {
        CivionNavigationState.recordActiveAccount(context, "account-1", folderId = 42L)

        assertThat(CivionNavigationState.lastFolderId(context, "account-unknown")).isNull()
    }

    /**
     * An empty uuid is not an account. Recording one would make the next start resolve an
     * account that cannot be found and lose the one that was actually there.
     */
    @Test
    fun `should refuse an empty account uuid`() {
        CivionNavigationState.recordActiveAccount(context, "account-1", folderId = 42L)

        CivionNavigationState.recordActiveAccount(context, "", folderId = 99L)

        assertThat(CivionNavigationState.lastActiveAccountUuid(context)).isEqualTo("account-1")
    }
}
