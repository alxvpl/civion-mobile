# CIVION Mobile

CIVION Mobile is the Android mail client of the CIVION personal administration system.

The current `0.1.0-alpha` milestone focuses on a reliable, independently branded mobile email client that can be used as the mobile mail edge for CIVION. The application has its own Android package, branding, OAuth configuration and build path while reusing a mature open-source mail engine underneath.

## Current status

**Component version:** `0.1.0-alpha`  
**Android application ID:** `nl.civion.mobile`  
**Debug application ID:** `nl.civion.mobile.debug`  
**Active development branch:** `civion/mobile-0.1.0-alpha`

The current alpha provides the core mobile mail workflow: account setup, mailbox access, message reading and the existing mail capabilities supplied by the underlying Android mail engine.

Gmail authentication is handled with CIVION-owned Google OAuth configuration. CIVION does not reuse Thunderbird or K-9 OAuth client credentials.

## Role inside CIVION

CIVION Mobile is a client and edge application. It is not the canonical store for CIVION operational data.

Its current responsibility is mobile email access. Future CIVION-specific capabilities can build on this client boundary, including controlled hand-off of mail-derived information into the wider CIVION system, while keeping canonical records and governance in CIVION Core.

The `0.1.0-alpha` build does **not** yet provide full CIVION domain integration, Core synchronization, AI processing, candidate extraction or server-backed administration workflows.

## What is CIVION?

CIVION is a modular personal administration system designed to bring together operational records, public-administration procedures, contracts, mail processing and a unified user interface.

The main modules are:

- **Core** — canonical operational records and shared registries
- **Civic** — institutions, public administration, rights, obligations and procedures
- **Contracts** — providers, contracts, terms, renewals and consumer routes
- **Mail** — mail ingestion, analysis and controlled extraction of candidate information
- **Console** — the main human-facing CIVION interface

CIVION Mobile extends that product family to Android. Its present development track is deliberately narrower: first make the mobile mail client stable and usable, then add CIVION-specific integration through explicit module boundaries.

## Gmail OAuth

CIVION Mobile uses its own Google OAuth client configuration.

For the current debug build:

- application type: Android
- package: `nl.civion.mobile.debug`
- audience: External / Testing
- custom URI scheme: enabled
- Gmail accounts used during testing must be registered as Google Auth Platform test users

OAuth client IDs are local build configuration and are not committed to the repository.

The relevant Gradle properties are:

```properties
civion.google.oauth.clientId.debug=...
civion.google.oauth.clientId.release=...
```

The OAuth scope used for Gmail IMAP/SMTP access is `https://mail.google.com/`.

## Build

Standard debug build:

```bash
./gradlew :app-civion:assembleDebug
```

On the current CIVION Windows development workstation, the helper script can be used:

```powershell
powershell -ExecutionPolicy Bypass -File app-civion\tools\build-civion-mobile.ps1
```

The helper configures the local JDK/Android SDK environment, disables Gradle file-system watching for the current workspace, writes a build log and copies the resulting APK to the configured CIVION output location together with its SHA-256.

## Repository layout

CIVION-specific Android application code lives primarily under:

```text
app-civion/
```

Important CIVION-owned areas include:

```text
app-civion/src/main/
app-civion/src/debug/
app-civion/src/release/
app-civion/tools/
```

The repository also contains the upstream mail-engine source tree required to build the application. That code remains present because CIVION Mobile is currently maintained as a white-label Android application on top of that engine rather than as a thin binary dependency.

## Technical foundation

CIVION Mobile is based on the open-source Thunderbird for Android / K-9 Mail codebase. This gives the project a mature Android mail implementation for protocols, storage, account handling and message workflows while CIVION maintains its own application identity and product direction.

Thunderbird and K-9 Mail are upstream projects; they are not CIVION products and their branding, services, support channels and release process do not apply to CIVION Mobile.

CIVION-specific changes are intended to remain isolated from the reusable mail-engine layers wherever practical.

## License and upstream attribution

This repository retains the licensing and attribution requirements of its upstream open-source code. See [`LICENSE`](LICENSE) and the relevant source headers and notices in the repository.

CIVION Mobile is an independent CIVION project and is not an official Mozilla, Thunderbird or K-9 Mail release.
