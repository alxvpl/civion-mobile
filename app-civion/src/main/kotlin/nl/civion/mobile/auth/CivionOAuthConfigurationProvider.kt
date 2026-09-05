package nl.civion.mobile.auth

import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import net.thunderbird.core.logging.Logger
import nl.civion.mobile.BuildConfig

/**
 * CIVION's own [OAuthConfigurationProvider], replacing the upstream in-memory one.
 *
 * The upstream provider resolves a hostname by exact map lookup. That is correct for the host names
 * autodiscovery normally produces, and it fails silently for everything else: a miss becomes
 * `AuthorizationIntentResult.NotSupported`, which the account setup renders as "OAuth 2.0 is
 * currently not supported with this provider" — the same message whether no configuration exists at
 * all or the hostname simply did not match a key.
 *
 * Two things are added here, both inside `app-civion` and without touching any engine module.
 *
 * 1. A suffix match for Google's mail hosts. Google serves IMAP and SMTP under several host
 *    spellings, and a manually configured or provider-supplied host does not have to be one of the
 *    four names the configuration is keyed by. The root domains `gmail.com`, `googlemail.com` and
 *    `google.com`, and their real subdomains, resolve to the Google configuration.
 * 2. Debug-build logging of every lookup, under the tag `civion-oauth`. A miss logs the hostname it
 *    was asked for and the keys it knows, which is the fact needed to tell the two failure causes
 *    apart. An empty hostname is logged distinctly, because a lookup for `""` means the OAuth screen
 *    was opened without its state being initialised — a different defect entirely, and one that no
 *    configuration change can fix.
 */
class CivionOAuthConfigurationProvider(
    factory: OAuthConfigurationFactory,
    private val logger: Logger,
) : OAuthConfigurationProvider {

    private val byHostname: Map<String, OAuthConfiguration> = buildMap {
        for ((hostnames, configuration) in factory.createConfigurations()) {
            for (hostname in hostnames) {
                put(hostname.lowercase(), configuration)
            }
        }
    }

    override fun getConfiguration(hostname: String): OAuthConfiguration? {
        val key = hostname.trim().lowercase().removeSuffix(".")

        if (key.isEmpty()) {
            log("lookup with an EMPTY hostname — the OAuth screen was opened without an initialised state")
            return null
        }

        byHostname[key]?.let { configuration ->
            log("exact match for '$key'")
            return configuration
        }

        val googleConfiguration = if (GOOGLE_DOMAINS.any { domain -> key.matchesDomain(domain) }) {
            byHostname[GOOGLE_CANONICAL_HOST]
        } else {
            null
        }

        if (googleConfiguration != null) {
            log("suffix match for '$key' resolved to the Google configuration")
            return googleConfiguration
        }

        log("NO configuration for '$key'; known hosts: ${byHostname.keys.sorted()}")
        return null
    }

    private fun log(message: String) {
        if (BuildConfig.DEBUG) {
            logger.warn(tag = TAG) { message }
        }
    }

    private companion object {
        const val TAG = "civion-oauth"
        const val GOOGLE_CANONICAL_HOST = "imap.gmail.com"
        val GOOGLE_DOMAINS = listOf("gmail.com", "googlemail.com", "google.com")
    }
}

private fun String.matchesDomain(domain: String): Boolean {
    return this == domain || endsWith(".$domain")
}
