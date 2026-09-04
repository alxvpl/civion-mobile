package nl.civion.mobile.auth

import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory

/**
 * CIVION Mobile ships no OAuth provider configuration.
 *
 * The upstream K-9 and Thunderbird factories carry client IDs registered to those projects. Reusing
 * them would place CIVION traffic under someone else's OAuth registration, which is not permitted.
 * Until CIVION owns its own registrations the factory returns an empty map: providers that require
 * OAuth are not offered, and account setup uses IMAP/SMTP with a password or app password.
 */
class CivionOAuthConfigurationFactory : OAuthConfigurationFactory {
    override fun createConfigurations(): Map<List<String>, OAuthConfiguration> = emptyMap()
}
