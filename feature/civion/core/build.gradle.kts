plugins {
    id(ThunderbirdPlugins.Library.jvm)
}

/*
 * The CIVION Mobile contract layer.
 *
 * This module has no dependencies, and that is the point rather than an accident of it being
 * small. It holds the types CIVION Mobile's own layers speak in — UI, storage and, later,
 * intelligence — so that none of them has to name a Thunderbird type to do its work.
 *
 * Exactly one module is allowed to know about Thunderbird: `feature:civion:adapter`, which
 * implements these contracts over the upstream engine. Everything else consumes them.
 *
 * Do not add a dependency here. A dependency on Thunderbird would let upstream types travel
 * through the contracts into every consumer, which is the coupling this module exists to
 * prevent; and being a plain JVM module, it cannot reach Android either, which keeps the
 * contracts testable without a device. `app-civion/tools/build-civion-mobile.ps1` checks this
 * on every build.
 */

dependencies {
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
