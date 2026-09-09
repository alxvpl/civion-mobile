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

| variant |      application id      |    certificate     |                property                |
|---------|--------------------------|--------------------|----------------------------------------|
| debug   | `nl.civion.mobile.debug` | debug keystore     | `civion.google.oauth.clientId.debug`   |
| release | `nl.civion.mobile`       | CIVION release key | `civion.google.oauth.clientId.release` |

Scope: `https://mail.google.com/` — the only scope that grants IMAP and SMTP (XOAUTH2) access. It is
a Google *restricted* scope.

Redirect URI: `<applicationId>:/oauth2redirect`. No manifest entry is needed here; `legacy:common`
already declares AppAuth's `RedirectUriReceiverActivity` with `android:scheme="${applicationId}"`.

Client ids are not committed. Put them in `~/.gradle/gradle.properties`.

For the current debug OAuth client in Google Auth Platform:

- application type: Android
- package: `nl.civion.mobile.debug`
- audience: External / Testing
- Custom URI scheme: enabled
- every Gmail account used during testing must be listed under Audience -> Test users

The OAuth client ID remains local Gradle configuration and must not be committed. Google Cloud
audience and custom-URI settings are service-side configuration and don't require a new APK.

## Windows build helper

On the CIVION development workstation, run:

        powershell -ExecutionPolicy Bypass -File app-civion\tools\build-civion-mobile.ps1

It is the one build procedure; the self-hosted CI runner calls the same script, so a CI artifact
and a hand-built one are produced the same way. Output goes to `-OutDir`, else `CIVION_OUT_DIR`,
else a repository-relative `out\`: the APK as `CIVION-Mobile-<version>.<n>.apk`, beside its
`BUILD-INFO-<n>.txt` (commit, tree state, footprint, publication state, SHA-256), the Gradle log,
`guard-<n>.txt` and a row in `build-history.csv`.

Before it builds, the script re-measures what this fork has changed against the upstream base and
fails on anything not recorded — see `$EngineHooks` and `$IntegrationPoints` in the script itself.
The measured perimeter is everything outside `app-civion\` and `feature\civion\`.
