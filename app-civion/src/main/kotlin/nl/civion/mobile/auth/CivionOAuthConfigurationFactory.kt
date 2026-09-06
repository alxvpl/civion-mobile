package nl.civion.mobile.auth

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import java.net.URLEncoder
import java.security.MessageDigest
import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import nl.civion.mobile.BuildConfig

/**
 * OAuth provider configuration for CIVION Mobile.
 *
 * CIVION never uses the K-9 or Thunderbird OAuth client IDs: those are registered to those projects.
 * The client ids here are CIVION's own and are supplied at build time from Gradle properties, so
 * they are never committed to the repository and the debug and release application ids can carry
 * their own registrations.
 *
 * When a provider has no client id configured it is simply absent from the map. That is the
 * pre-OAuth behaviour: `GetAutoDiscovery.cleanAuthenticationTypes` then strips OAuth2 from the
 * offered authentication types for that provider and setup falls back to password authentication.
 * A build without a client id is therefore degraded for that provider, never broken.
 */
class CivionOAuthConfigurationFactory(
    private val context: Context,
) : OAuthConfigurationFactory {

    override fun createConfigurations(): Map<List<String>, OAuthConfiguration> {
        return buildMap {
            googleConfiguration()?.let { put(it.first, it.second) }
            microsoftConfiguration()?.let { put(it.first, it.second) }
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

    /**
     * Microsoft: personal accounts (outlook.com, hotmail.com, live.com) and Microsoft 365 mailboxes,
     * over IMAP and SMTP.
     *
     * Basic authentication is no longer accepted by Microsoft, so without this configuration such an
     * account cannot be added at all — autodiscovery resolves the servers and then setup dead-ends
     * on a password the server will refuse.
     *
     * `common` as the tenant covers both personal and organisational accounts, matching an app
     * registration that supports both.
     *
     * The redirect uri uses the `msauth://<package>/<url-encoded base64 SHA-1 of the signing
     * certificate>` form Microsoft requires for Android clients. It is derived from the running
     * application's own signature rather than hard-coded, so a build signed by a different key
     * cannot silently present a redirect uri that Microsoft will reject — the same class of failure
     * that cost a day on the Google side. `legacy:common` already declares the `msauth` intent
     * filter with `android:host="${applicationId}"`, so no manifest change is needed here.
     */
    private fun microsoftConfiguration(): Pair<List<String>, OAuthConfiguration>? {
        val clientId = BuildConfig.MICROSOFT_OAUTH_CLIENT_ID
        if (clientId.isEmpty()) return null

        val redirectUri = microsoftRedirectUri() ?: return null

        return listOf(
            "outlook.office365.com",
            "smtp.office365.com",
            "smtp-mail.outlook.com",
        ) to OAuthConfiguration(
            clientId = clientId,
            scopes = listOf(
                "profile",
                "openid",
                "email",
                "https://outlook.office.com/IMAP.AccessAsUser.All",
                "https://outlook.office.com/SMTP.Send",
                "offline_access",
            ),
            authorizationEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
            tokenEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token",
            redirectUri = redirectUri,
        )
    }

    /**
     * `msauth://<package name>/<url-encoded base64 SHA-1 of this build's signing certificate>`.
     *
     * Returns null when the signature cannot be read, which leaves Microsoft unconfigured rather
     * than offering a redirect uri that cannot work.
     */
    private fun microsoftRedirectUri(): String? {
        val signature = signingCertificate() ?: return null
        val sha1 = MessageDigest.getInstance("SHA-1").digest(signature)
        val encoded = URLEncoder.encode(Base64.encodeToString(sha1, Base64.NO_WRAP), "UTF-8")

        return "msauth://${context.packageName}/$encoded"
    }

    @Suppress("DEPRECATION")
    private fun signingCertificate(): ByteArray? {
        val packageManager = context.packageManager
        val packageName = context.packageName

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                info.signatures?.firstOrNull()?.toByteArray()
            }
        }.getOrNull()
    }
}
