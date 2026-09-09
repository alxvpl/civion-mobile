plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "nl.civion.mobile.integration.impl"
}

/*
 * The CIVION integration layer. Compiled into the integrated edition only.
 *
 * Everything that talks to CIVION Core belongs here and nowhere else: this module's package is
 * what the build's APK gate looks for, and finding it in a standalone artifact fails that
 * build. Adding integration code to a module the standalone edition also depends on would put
 * it in the standalone APK without anything noticing.
 *
 * There is no Core protocol yet. What exists is the boundary and the contract; the behaviour
 * arrives here later without any other module changing.
 */

dependencies {
    api(projects.feature.civion.core)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
