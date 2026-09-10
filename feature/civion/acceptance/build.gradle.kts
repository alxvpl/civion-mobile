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

/*
 * The contract, read out loud.
 *
 * Anything that quotes a coverage number - a report, a commit message, a status update - should
 * take it from here. A number typed in by hand is right once.
 */
tasks.register<JavaExec>("acceptanceReport") {
    group = "verification"
    description = "Prints which acceptance journeys are covered and which are pending."
    mainClass.set("nl.civion.mobile.acceptance.AcceptanceReportKt")
    classpath = sourceSets["main"].runtimeClasspath
}
