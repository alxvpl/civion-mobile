plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "nl.civion.mobile.integration.noop"
}

/*
 * The standalone edition's answer to "is there a CIVION integration?": no.
 *
 * This module exists so that the standalone build can satisfy the contract without linking a
 * single line of integration code. It is what makes the separation physical rather than a
 * runtime decision: standalone does not depend on :feature:civion:integration:impl at all, so
 * those classes cannot be in its APK, whatever a feature flag, a Koin binding or a shrinker
 * does or does not do.
 *
 * Keep it empty of behaviour. Anything real belongs in impl.
 */

dependencies {
    api(projects.feature.civion.core)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
