plugins {
    id(ThunderbirdPlugins.App.androidCompose)
    alias(libs.plugins.tb.app.versioning)
}

val testCoverageEnabled = providers
    .gradleProperty("testCoverageEnabled")
    .isPresent

/*
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
/*
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

/*
 * CIVION-owned Microsoft (Entra) client ids, per application id.
 *
 * Registered as an Android platform application, which binds the client to the package name and the
 * signing certificate exactly as Google does. The redirect uri is derived at runtime from the
 * application's own signature, so it can never drift from the key that signed the build.
 *
 *   civion.microsoft.oauth.clientId.debug=<application (client) id>
 *   civion.microsoft.oauth.clientId.release=<application (client) id>
 *
 * Absent, the application simply offers no Microsoft OAuth provider.
 */
val microsoftOAuthClientIdDebug = providers
    .gradleProperty("civion.microsoft.oauth.clientId.debug")
    .getOrElse("")
val microsoftOAuthClientIdRelease = providers
    .gradleProperty("civion.microsoft.oauth.clientId.release")
    .getOrElse("")
val googleOAuthClientIdRelease = providers
    .gradleProperty("civion.google.oauth.clientId.release")
    .getOrElse("")

/*
 * CIVION Mobile debug signing.
 *
 * A Google OAuth client of type Android is bound to one package name AND one signing certificate.
 * The default Android debug keystore is per user account, so a build made by the owner and a build
 * made by the CI runner (a different Windows account) are signed by different certificates and only
 * one of them can match the registered OAuth client.
 *
 * Point every machine at the same keystore to remove that divergence, e.g. in
 * `~/.gradle/gradle.properties`:
 *   civion.debug.storeFile=F:/civion-keys/civion-debug.jks
 *   civion.debug.storePassword=android
 *   civion.debug.keyAlias=androiddebugkey
 *   civion.debug.keyPassword=android
 *
 * Absent, Gradle's own default debug keystore is used, exactly as before.
 */
val civionDebugStoreFile = providers.gradleProperty("civion.debug.storeFile")
val civionDebugStorePassword = providers.gradleProperty("civion.debug.storePassword").getOrElse("android")
val civionDebugKeyAlias = providers.gradleProperty("civion.debug.keyAlias").getOrElse("androiddebugkey")
val civionDebugKeyPassword = providers.gradleProperty("civion.debug.keyPassword").getOrElse("android")

val civionStoreFile = providers.gradleProperty("civion.release.storeFile")
val civionStorePassword = providers.gradleProperty("civion.release.storePassword")
val civionKeyAlias = providers.gradleProperty("civion.release.keyAlias")
val civionKeyPassword = providers.gradleProperty("civion.release.keyPassword")
val hasCivionSigning = civionStoreFile.isPresent &&
    civionStorePassword.isPresent &&
    civionKeyAlias.isPresent &&
    civionKeyPassword.isPresent

/*
 * Build identity: the number of commits reachable from HEAD.
 *
 * It has to be the same for the same commit no matter who builds it or where the output goes.
 * It used to be derived from the APK files already sitting in the output directory, which meant
 * a fresh CI workspace started again at 1 and produced a second artifact claiming a number an
 * earlier, different build already had.
 *
 * Commit count is the one git number that cannot go backwards here. Counting only the commits
 * since the upstream base would be smaller and prettier, but it moves when the base does;
 * counting the whole ancestry grows whether upstream is merged or rebased onto, which is what
 * versionCode needs. Both editions share one applicationId, so Android will refuse to install
 * an edition whose versionCode is lower than the installed one — the number has to be ordered,
 * not merely unique.
 *
 * `-Pcivion.build.number` lets the build script pass the value it already computed for the
 * artifact name, so the name and the versionCode inside the APK cannot disagree. Without it,
 * Gradle works this out itself, which is what keeps an IDE build honest too.
 *
 * A checkout without git history is a build whose identity cannot be established, so it fails
 * here rather than silently producing an artifact that claims to be some other build.
 */
val civionBuildNumber: Int = run {
    val override = providers.gradleProperty("civion.build.number").orNull?.trim()?.toIntOrNull()
    if (override != null && override > 0) return@run override

    val counted = runCatching {
        providers.exec {
            workingDir = rootDir
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText.get().trim().toIntOrNull()
    }.getOrNull()

    counted?.takeIf { it > 0 } ?: throw GradleException(
        "Could not establish the CIVION Mobile build number: 'git rev-list --count HEAD' in " +
            "$rootDir produced no usable count. Build from a full git checkout (a shallow " +
            "clone is not enough), or pass -Pcivion.build.number=<n> explicitly.",
    )
}

android {
    namespace = "nl.civion.mobile"

    defaultConfig {
        applicationId = "nl.civion.mobile"
        testApplicationId = "nl.civion.mobile.tests"

        versionCode = civionBuildNumber
        versionName = "0.1.0"
        versionNameSuffix = "-alpha"

        buildConfigField("String", "CLIENT_INFO_APP_NAME", "\"CIVION Mobile\"")
        buildConfigField("int", "CIVION_BUILD_NUMBER", "$civionBuildNumber")
    }

    /*
     * Two editions of one product, not two products.
     *
     * Both carry the same applicationId, are signed by the same key and read the same data, so
     * the integrated edition installs over the standalone one as an upgrade and the Google
     * OAuth client — which is bound to exactly one package name and one certificate — keeps
     * working for both. No second OAuth registration, and deliberately no applicationIdSuffix:
     * an id that differed per edition would be a different application to Android, to Google
     * and to the user's data.
     *
     * What separates them is the dependency graph below. The standalone edition never links
     * :feature:civion:integration:impl, so no integration class can reach its APK — which the
     * build script proves against the built artifact rather than trusting it.
     */
    flavorDimensions += listOf("edition")
    productFlavors {
        create("standalone") {
            dimension = "edition"
            buildConfigField("String", "CIVION_EDITION", "\"standalone\"")
        }

        create("integrated") {
            dimension = "edition"
            buildConfigField("String", "CIVION_EDITION", "\"integrated\"")
        }
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
        if (civionDebugStoreFile.isPresent) {
            getByName("debug") {
                storeFile = file(civionDebugStoreFile.get())
                storePassword = civionDebugStorePassword
                keyAlias = civionDebugKeyAlias
                keyPassword = civionDebugKeyPassword
            }
        }

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

            buildConfigField(
                "String",
                "MICROSOFT_OAUTH_CLIENT_ID",
                "\"$microsoftOAuthClientIdRelease\"",
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

            buildConfigField(
                "String",
                "MICROSOFT_OAUTH_CLIENT_ID",
                "\"$microsoftOAuthClientIdDebug\"",
            )

            enableUnitTestCoverage = testCoverageEnabled
            enableAndroidTestCoverage = testCoverageEnabled

            isMinifyEnabled = false
        }
    }

    /*
     * The Android runtime the acceptance contract has been waiting for.
     *
     * Several journeys in `feature:civion:acceptance` are pending on ANDROID_RUNTIME: the
     * decisions around them are unit-tested, but what they end in — an activity launching, a
     * notification's PendingIntent, an attachment leaving the app through the platform — cannot
     * be observed without a device. This is that device, declared so Gradle provisions, boots,
     * installs, runs and shuts it down as part of a task rather than as something a person sets
     * up by hand and remembers differently next time.
     *
     * AOSP ATD: an automated-test image with the Play services, UI and setup wizard stripped
     * out. It boots faster and, more importantly, boots the same way every time, which is what
     * a test runtime needs and what a device meant for a human to look at is not.
     *
     * `require64Bit` because only the x86_64 image is installed; without it the emulator may
     * resolve a 32-bit variant that is not there and fail at provisioning rather than here.
     *
     * The device is declared once, at the module level, and applies to every variant. The task
     * names are generated per variant — see `:app-civion:tasks --group verification`.
     */
    @Suppress("UnstableApiUsage")
    testOptions {
        managedDevices {
            localDevices {
                create("pixelApi36") {
                    device = "Pixel 6"
                    apiLevel = 36
                    systemImageSource = "aosp-atd"
                    require64Bit = true
                }
            }
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
    implementation(projects.core.android.account)
    implementation(projects.core.ui.compose.common)
    implementation(projects.core.ui.legacy.theme2.thunderbird)
    implementation(projects.feature.launcher)
    implementation(projects.feature.mail.message.list.api)
    implementation(projects.feature.mail.message.list.internal)
    implementation(projects.feature.mail.message.reader.api)

    implementation(projects.legacy.core)
    implementation(projects.legacy.ui.legacy)

    implementation(projects.core.common)
    implementation(projects.core.featureflag)

    implementation(projects.feature.civion.adapter)
    implementation(projects.feature.civion.core)
    "standaloneImplementation"(projects.feature.civion.integration.noop)
    "integratedImplementation"(projects.feature.civion.integration.impl)
    implementation(projects.feature.civion.navigation)

    implementation(projects.feature.autodiscovery.api)
    implementation(projects.feature.account.settings.api)
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

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)

    testImplementation(projects.feature.civion.acceptance)
    testImplementation(libs.mockito.kotlin)

    debugImplementation(projects.backend.demo)
    debugImplementation(projects.feature.autodiscovery.demo)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
