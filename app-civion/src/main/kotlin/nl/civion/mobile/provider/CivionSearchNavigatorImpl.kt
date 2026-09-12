package nl.civion.mobile.provider

import android.app.Activity
import com.fsck.k9.activity.MessageHomeActivity
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import nl.civion.mobile.ui.search.CivionSearchNavigator

/** Search results are upstream's message list, opened on the search CIVION's screen put together. */
internal class CivionSearchNavigatorImpl : CivionSearchNavigator {
    override fun showResults(activity: Activity, search: LocalMessageSearch) {
        MessageHomeActivity.actionDisplaySearch(activity, search, false, false)
    }
}
