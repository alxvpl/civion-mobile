package nl.civion.mobile.ui.search

import android.app.Activity
import net.thunderbird.feature.search.legacy.LocalMessageSearch

/**
 * Shows the results of a search the Search screen has put together.
 *
 * The results are upstream's message list, opened on the search; that list lives in a module
 * this one must not depend on, so the application binds the way there.
 */
interface CivionSearchNavigator {
    fun showResults(activity: Activity, search: LocalMessageSearch)
}
