plugins {
    id(ThunderbirdPlugins.Library.jvm)
}

/*
 * The acceptance contract: which journeys Android Mail is held to, how each one is checked, and
 * which are not checked yet.
 *
 * It is code rather than a document so that it can be wrong in a way that fails a build. A list
 * in a document drifts silently; this one is compared against the required journeys, against the
 * tiers that actually run, and against the evidence each claim names.
 *
 * No dependencies, deliberately: the inventory is read by tests in modules that cannot see each
 * other, so it must be reachable from all of them and tied to none.
 */

dependencies {
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
