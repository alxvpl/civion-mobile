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
        localeFilters += listOf(
            "en",
            "bg",
            "nl",
            "de",
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

            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        debug {
            applicationIdSuffix = ".debug"

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
    implementation(projects.core.ui.legacy.theme2.k9mail)
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
    implementation(projects.feature.thundermail.k9mail)

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
