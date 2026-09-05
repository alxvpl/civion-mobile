plugins {
    id(ThunderbirdPlugins.App.androidCompose)
    alias(libs.plugins.tb.app.versioning)
}

val testCoverageEnabled = providers
    .gradleProperty("testCoverageEnabled")
    .isPresent

/**
 * CIVION Mobile release signing.
 *
 * Deliberately NOT using `createSigningConfig(project, SigningType...)` from the upstream build
 * plugin: `SigningType` is a closed upstream enum (k9/tb only) and extending it would mean editing
 * upstream build logic. The configuration below is self-contained and reads CIVION-owned
 * properties, so no Thunderbird or K-9 signing material is ever referenced.
 *
 * Provide, e.g. in `~/.gradle/gradle.properties`:
 *   civion.release.storeFile=/path/to/civion-release.jks
 *   civion.release.storePassword=...
 *   civion.release.keyAlias=civion
 *   civion.release.keyPassword=...
 */
/**
 * CIVION-owned Google OAuth client ids, per application id.
 *
 * A Google OAuth client of type Android is bound to one package name and one signing certificate,
 * so the debug application id (`nl.civion.mobile.debug`, debug keystore) and the release one
 * (`nl.civion.mobile`, CIVION release key) need separate registrations.
 *
 * Provide, e.g. in `~/.gradle/gradle.properties` — never in the repository:
 *   civion.google.oauth.clientId.debug=<id>.apps.googleusercontent.com
 *   civion.google.oauth.clientId.release=<id>.apps.googleusercontent.com
 *
 * Absent, the app simply offers no OAuth provider, exactly as before.
 */
val googleOAuthClientIdDebug = providers
    .gradleProperty("civion.google.oauth.clientId.debug")
    .getOrElse("")
val googleOAuthClientIdRelease = providers
    .gradleProperty("civion.google.oauth.clientId.release")
    .getOrElse("")

val civionStoreFile = providers.gradleProperty("civion.release.storeFile")
val civionStorePassword = providers.gradleProperty("civion.release.storePassword")
val civionKeyAlias = providers.gradleProperty("civion.release.keyAlias")
val civionKeyPassword = providers.gradleProperty("civion.release.keyPassword")
val hasCivionSigning = civionStoreFile.isPresent &&
    civionStorePassword.isPresent &&
    civionKeyAlias.isPresent &&
    civionKeyPassword.isPresent

android {
    namespace = "nl.civion.mobile"

    defaultConfig {
        applicationId = "nl.civion.mobile"
        testApplicationId = "nl.civion.mobile.tests"

        versionCode = 1
        versionName = "0.1.0"
        versionNameSuffix = "-alpha"

        buildConfigField("String", "CLIENT_INFO_APP_NAME", "\"CIVION Mobile\"")
    }

    androidResources {
        // Same locale coverage as Thunderbird for Android; keep in sync with "supported_languages".
        localeFilters += listOf(
            "ar",
            "be",
            "bg",
            "br",
            "ca",
            "co",
            "cs",
            "cy",
            "da",
            "de",
            "el",
            "en",
            "en-rGB",
            "eo",
            "es",
            "et",
            "eu",
            "fa",
            "fi",
            "fr",
            "fy",
            "ga",
            "gd",
            "gl",
            "hr",
            "hu",
            "in",
            "is",
            "it",
            "iw",
            "ja",
            "ko",
            "lt",
            "lv",
            "nb",
            "nl",
            "nn",
            "pl",
            "pt-rBR",
            "pt-rPT",
            "ro",
            "ru",
            "sk",
            "sl",
            "sq",
            "sr",
            "sv",
            "ta-rIN",
            "tr",
            "uk",
            "vi",
            "zh-rCN",
            "zh-rTW",
        )
    }

    signingConfigs {
        if (hasCivionSigning) {
            create("release") {
                storeFile = file(civionStoreFile.get())
                storePassword = civionStorePassword.get()
                keyAlias = civionKeyAlias.get()
                keyPassword = civionKeyPassword.get()
            }
        } else {
            logger.warn("CIVION release signing config not created: civion.release.* properties are absent")
        }
    }

    buildTypes {
        release {
            if (hasCivionSigning) {
                signingConfig = signingConfigs.getByName("release")
            }

            buildConfigField(
                "String",
                "GOOGLE_OAUTH_CLIENT_ID",
                "\"$googleOAuthClientIdRelease\"",
            )

            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        debug {
            applicationIdSuffix = ".debug"

            buildConfigField(
                "String",
                "GOOGLE_OAUTH_CLIENT_ID",
                "\"$googleOAuthClientIdDebug\"",
            )

            enableUnitTestCoverage = testCoverageEnabled
            enableAndroidTestCoverage = testCoverageEnabled

            isMinifyEnabled = false
        }
    }

    packaging {
        jniLibs {
            excludes += listOf("kotlin/**")
        }

        resources {
            excludes += listOf(
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "kotlin/**",
                "DebugProbesKt.bin",
            )
        }
    }
}

dependencies {
    implementation(projects.appCommon)
    implementation(projects.core.ui.compose.common)
    implementation(projects.core.ui.legacy.theme2.thunderbird)
    implementation(projects.feature.launcher)
    implementation(projects.feature.mail.message.list.api)
    implementation(projects.feature.mail.message.list.internal)
    implementation(projects.feature.mail.message.reader.api)

    implementation(projects.legacy.core)
    implementation(projects.legacy.ui.legacy)

    implementation(projects.core.featureflag)

    implementation(projects.feature.autodiscovery.api)
    implementation(projects.feature.account.settings.impl)

    // No Google Play billing, no donation flow.
    implementation(projects.feature.funding.noop)

    // No migration from a previously installed app.
    implementation(projects.feature.migration.launcher.noop)
    implementation(projects.feature.onboarding.migration.noop)

    implementation(projects.feature.thundermail.api)
    implementation(projects.feature.thundermail.thunderbird)

    // No telemetry.
    implementation(projects.feature.telemetry.noop)

    implementation(projects.feature.widget.messageList)
    implementation(projects.feature.widget.messageListGlance)
    implementation(projects.feature.widget.shortcut)
    implementation(projects.feature.widget.unread)

    implementation(libs.androidx.work.runtime)

    debugImplementation(projects.backend.demo)
    debugImplementation(projects.feature.autodiscovery.demo)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
