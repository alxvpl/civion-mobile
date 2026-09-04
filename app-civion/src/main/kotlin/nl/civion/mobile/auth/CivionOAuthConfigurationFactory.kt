package nl.civion.mobile.auth

import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import nl.civion.mobile.BuildConfig

/**
 * OAuth provider configuration for CIVION Mobile.
 *
 * CIVION never uses the K-9 or Thunderbird OAuth client IDs: those are registered to those projects.
 * The client id here is CIVION's own and is supplied at build time from the Gradle properties
 * `civion.google.oauth.clientId.debug` / `.release`, so it is never committed to the repository and
 * the debug and release application ids can carry their own registrations.
 *
 * When no client id is configured the factory returns an empty map. That is the pre-OAuth behaviour:
 * `GetAutoDiscovery.cleanAuthenticationTypes` then strips OAuth2 from the offered authentication
 * types and account setup falls back to password authentication. A build without a client id is
 * therefore degraded, never broken.
 */
class CivionOAuthConfigurationFactory : OAuthConfigurationFactory {

    override fun createConfigurations(): Map<List<String>, OAuthConfiguration> {
        return buildMap {
            googleConfiguration()?.let { put(it.first, it.second) }
        }
    }

    /**
     * Google/Gmail over IMAP and SMTP.
     *
     * `https://mail.google.com/` is the only scope that grants IMAP and SMTP (XOAUTH2) access; the
     * narrower gmail.* scopes are API-only and do not work for a mail client. It is a restricted
     * scope, which is what makes the Google Cloud registration a governed step rather than a detail.
     *
     * The redirect uri follows the reverse-DNS custom scheme Google uses for Android clients and
     * must match the package name registered for the client. `legacy:common` already declares
     * AppAuth's `RedirectUriReceiverActivity` with `android:scheme="${applicationId}"`, so the
     * scheme resolves per variant and no manifest change is needed in `app-civion`.
     */
    private fun googleConfiguration(): Pair<List<String>, OAuthConfiguration>? {
        val clientId = BuildConfig.GOOGLE_OAUTH_CLIENT_ID
        if (clientId.isEmpty()) return null

        return listOf(
            "imap.gmail.com",
            "imap.googlemail.com",
            "smtp.gmail.com",
            "smtp.googlemail.com",
        ) to OAuthConfiguration(
            clientId = clientId,
            scopes = listOf("https://mail.google.com/"),
            authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
            tokenEndpoint = "https://oauth2.googleapis.com/token",
            redirectUri = "${BuildConfig.APPLICATION_ID}:/oauth2redirect",
        )
    }
}
