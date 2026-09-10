plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

/*
 * Android Mail's own mark.
 *
 * The Bolt theme takes a logo as a Compose resource, and the only two it ships are K-9's and
 * Thunderbird's. Onboarding and account setup draw it, so with neither replaced the first screen
 * a new user sees carries the logo of a different product.
 *
 * A Compose resource cannot come from an Android `res/` directory, which is where the accepted
 * icon lives, so it is carried here instead. That is the whole reason this module exists; it has
 * no code.
 */

kotlin {
    android {
        namespace = "nl.civion.mobile.brand"
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}

/*
 * Public, so app-civion can name the logo when it composes the theme, and generated under a
 * CIVION package rather than one derived from the module path.
 */
compose.resources {
    publicResClass = true
    packageOfResClass = "nl.civion.mobile.brand.resources"
    generateResClass = always
}
