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

## Editions

One product, one application id, two editions, separated by which modules are compiled in:

|   edition    |         integration layer          |              Gradle task              |
|--------------|------------------------------------|---------------------------------------|
| `standalone` | `:feature:civion:integration:noop` | `:app-civion:assembleStandaloneDebug` |
| `integrated` | `:feature:civion:integration:impl` | `:app-civion:assembleIntegratedDebug` |

Both carry `nl.civion.mobile` and the same signing identity, so the integrated edition installs
over the standalone one as an upgrade and the Google OAuth client — bound to one package name and
one certificate — keeps working for both. There is deliberately no `applicationIdSuffix`: an id
that differed per edition would be a different application to Android, to Google and to the user's
data.

The separation is a compile-time dependency boundary, not a runtime flag, a Koin binding or
anything the shrinker does. The build script proves it against the built artifact: the dex must
carry that edition's marker package and none of the other one's.

`versionCode` is the number of commits reachable from `HEAD` — the same for a given commit
whoever builds it, and ordered, which matters because both editions share one application id.

Build:

        ./gradlew :app-civion:assembleStandaloneDebug

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

Pass `-Edition integrated` for the other edition; `-Variant release` for a release build.

It is the one build procedure; the self-hosted CI runner calls the same script, so a CI artifact
and a hand-built one are produced the same way. Output goes to `-OutDir`, else `CIVION_OUT_DIR`,
else a repository-relative `out\`: the APK as
`CIVION-Mobile-<version>.<build>-<edition>[-<variant>]-<short sha>.apk`, beside its
`BUILD-INFO-*.txt` (edition, build number, commit, tree state, footprint, publication state,
SHA-256), the Gradle log, `guard-*.txt` and a row in `build-history.csv`.

The name is not a counter over what happens to be in the output directory — that made identity a
property of a directory, so a fresh CI workspace started again at 1 and produced an artifact
claiming a number an earlier, different build already carried. The build number comes from git
and the short sha names the commit exactly.

Before it builds, the script re-measures what this fork has changed against the upstream base and
fails on anything not recorded — see `$EngineHooks` and `$IntegrationPoints` in the script itself.
The measured perimeter is everything outside `app-civion\` and `feature\civion\`.

It also checks the adapter boundary (no Thunderbird import in a CIVION module other than
`feature:civion:adapter`), and, after building, that the artifact is the edition it claims and
that its `versionCode` matches the build number in its name.
