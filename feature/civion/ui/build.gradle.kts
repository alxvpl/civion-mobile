plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "nl.civion.mobile.ui"
}

/*
 * Android Mail's own surfaces.
 *
 * The second module allowed to know about Thunderbird, and for the same kind of reason as
 * `feature:civion:adapter`: a screen that replaces an upstream one has to implement the upstream
 * interface the host constructs, render data the engine owns, and use the shared theme. Pretending
 * otherwise would mean an adapter that exists only to copy display types through, which buys
 * nothing and costs a layer.
 *
 * The rule that still holds, and that the build guard checks: everything else CIVION owns - core,
 * navigation, integration, brand - stays free of upstream types.
 */

dependencies {
    api(projects.feature.navigation.drawer.api)

    implementation(projects.core.android.account)
    implementation(projects.core.ui.theme.api)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)
    implementation(projects.feature.search.implLegacy)
    implementation(projects.legacy.ui.folder)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
