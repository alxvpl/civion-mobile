plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "nl.civion.mobile.navigation"
}

dependencies {
    testImplementation(projects.feature.civion.acceptance)
    testImplementation(libs.robolectric)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
