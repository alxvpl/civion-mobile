# CIVION Mobile (`app-civion`)

Third white-label application module of this tree, alongside `app-k9mail` and `app-thunderbird`.

- component: **CIVION Mobile** `0.1.0-alpha` (a build/component artifact identifier, not a CIVION product release)
- application id: `nl.civion.mobile` (`nl.civion.mobile.debug` for debug builds)
- composition: `app-civion` → `app-common` → existing Thunderbird/K-9 mail runtime
- OAuth: none. `CivionOAuthConfigurationFactory` returns an empty configuration map, so no K-9 or
  Thunderbird OAuth client ID is ever used. Accounts are IMAP/SMTP with password or app password.
- no telemetry, no funding/billing, no migration-from-other-app features
- no CIVION domain functionality: no candidate extraction, no Core access, no server, no AI

Engine modules (`legacy:*`, `mail:*`, `backend:*`, `core:*`, `feature:*`) are not modified by this
module. The only upstream file touched is `settings.gradle.kts`, to register the module.

Build:

    ./gradlew :app-civion:assembleDebug

Release signing is configured from CIVION-owned Gradle properties (`civion.release.*`); see
`build.gradle.kts`. Upstream `SigningType` is deliberately left untouched.
