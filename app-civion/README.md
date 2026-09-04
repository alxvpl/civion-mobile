# CIVION Mobile (`app-civion`)

Third white-label application module of this tree, alongside `app-k9mail` and `app-thunderbird`.

- component: **CIVION Mobile** `0.1.0-alpha` (a build/component artifact identifier, not a CIVION product release)
- application id: `nl.civion.mobile` (`nl.civion.mobile.debug` for debug builds)
- composition: `app-civion` → `app-common` → existing Thunderbird/K-9 mail runtime
- OAuth: CIVION's own Google client, or none. `CivionOAuthConfigurationFactory` builds the Gmail
  configuration from `BuildConfig.GOOGLE_OAUTH_CLIENT_ID`, which comes from the Gradle properties
  `civion.google.oauth.clientId.debug` / `.release`. No K-9 or Thunderbird OAuth client ID is ever
  used. With no client id configured the factory returns an empty map and account setup falls back
  to password authentication.
- no telemetry, no funding/billing, no migration-from-other-app features
- no CIVION domain functionality: no candidate extraction, no Core access, no server, no AI

Engine modules (`legacy:*`, `mail:*`, `backend:*`, `core:*`, `feature:*`) are not modified by this
module. The only upstream file touched is `settings.gradle.kts`, to register the module.

Build:

    ./gradlew :app-civion:assembleDebug

Release signing is configured from CIVION-owned Gradle properties (`civion.release.*`); see
`build.gradle.kts`. Upstream `SigningType` is deliberately left untouched.

## Google OAuth

A Google OAuth client of type *Android* is bound to one package name and one signing certificate, so
the debug and release application ids need separate registrations:

| variant | application id       | certificate           | property                                |
|---------|----------------------|-----------------------|-----------------------------------------|
| debug   | `nl.civion.mobile.debug` | debug keystore    | `civion.google.oauth.clientId.debug`    |
| release | `nl.civion.mobile`   | CIVION release key    | `civion.google.oauth.clientId.release`  |

Scope: `https://mail.google.com/` — the only scope that grants IMAP and SMTP (XOAUTH2) access. It is
a Google *restricted* scope.

Redirect URI: `<applicationId>:/oauth2redirect`. No manifest entry is needed here; `legacy:common`
already declares AppAuth's `RedirectUriReceiverActivity` with `android:scheme="${applicationId}"`.

Client ids are not committed. Put them in `~/.gradle/gradle.properties`.
