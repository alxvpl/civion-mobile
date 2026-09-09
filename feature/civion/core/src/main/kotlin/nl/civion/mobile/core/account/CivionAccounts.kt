package nl.civion.mobile.core.account

/**
 * A mail account, as CIVION Mobile's own layers refer to one.
 *
 * The uuid is the account's stable identity and the only part of it CIVION stores. Everything
 * else about an account — its address, servers, colour, folders, credentials — stays with the
 * Thunderbird engine, which owns accounts; carrying copies here would mean two versions of the
 * same account that can disagree.
 */
data class CivionAccount(
    val uuid: String,
    val isSetupComplete: Boolean,
)

/**
 * The accounts on the device.
 *
 * Implemented in `feature:civion:adapter` over the engine's account store. Declared here so
 * that CIVION code can ask about accounts without naming an upstream type, and can be tested
 * without one.
 */
interface CivionAccounts {

    /**
     * Every account, finished or not.
     *
     * An account whose setup never completed is included and marked, rather than filtered out:
     * some callers must open one, and others must clean it up, and both need to see it.
     */
    fun all(): List<CivionAccount>

    /**
     * Asks for every account whose setup never finished to be removed.
     *
     * Returns as soon as the removal is requested; the accounts are gone some time afterwards.
     * An interrupted setup leaves an account behind that cannot be opened and must not be
     * offered, so whatever decides where to send the user is also the natural place to ask for
     * the leftovers to go.
     */
    fun removeUnfinished()
}

/**
 * The accounts that can actually be opened.
 */
fun CivionAccounts.usable(): List<CivionAccount> = all().filter { it.isSetupComplete }
