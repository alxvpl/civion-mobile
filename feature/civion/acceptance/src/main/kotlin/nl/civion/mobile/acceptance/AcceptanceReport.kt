package nl.civion.mobile.acceptance

/**
 * Prints the contract's own status.
 *
 * Run by `:feature:civion:acceptance:acceptanceReport`, so the covered/pending numbers come out
 * of the inventory rather than being counted by hand into a report that then drifts from it.
 */
fun main() {
    print(CivionAcceptance.report())
}
