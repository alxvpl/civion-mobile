plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "nl.civion.mobile.adapter"
}

/*
 * The one place CIVION Mobile is allowed to know about Thunderbird.
 *
 * Every upstream type CIVION needs is resolved here and handed on as a `feature:civion:core`
 * type. Nothing upstream leaves this module: no LegacyAccount, no MessageReference, no
 * MessageStore, no type from com.fsck.k9, net.thunderbird or app.k9mail appears in a signature
 * that CIVION UI, storage or intelligence can see.
 *
 * That is what confines an upstream package rename, a moved class or a changed DTO to this
 * module instead of letting it travel through the codebase.
 *
 * Upstream dependencies belong here. Add them here, not in the modules that consume the
 * contracts.
 */

dependencies {
    api(projects.feature.civion.core)

    implementation(projects.core.android.account)
    implementation(projects.feature.account.settings.api)
    implementation(projects.feature.mail.account.api)

    testImplementation(libs.mockito.kotlin)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
