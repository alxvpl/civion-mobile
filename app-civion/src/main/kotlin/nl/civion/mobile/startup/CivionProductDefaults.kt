package nl.civion.mobile.startup

import android.content.Context
import android.content.SharedPreferences
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListPreferencesManager

/**
 * The settings Android Mail ships with, rather than the ones the engine ships with.
 *
 * Upstream's defaults are K-9's and Thunderbird's, and a few of them decide how the product
 * looks the first time it is opened. They are ordinary preferences: fully implemented, exposed
 * in Settings, and the user may change any of them. All that is decided here is where they
 * start.
 *
 * Each default is applied once per install and never again, so a value the user changes back is
 * left alone. Whether it has been applied is remembered in CIVION's own small preferences file,
 * not in the engine's storage: the engine writes its whole settings group, defaults included, on
 * its own first run, so "the key is absent" is not a usable signal - and nothing of CIVION's
 * belongs in Thunderbird's storage anyway.
 *
 * The value goes through the engine's own preferences manager rather than straight into
 * storage, for the same reason: the manager holds the configuration the screens read and it
 * persists what it holds, so a value written behind its back is neither shown on this start nor
 * safe from being written over by upstream's next save.
 */
internal class CivionProductDefaults(
    private val messageListPreferences: MessageListPreferencesManager,
    private val appliedPreferences: Lazy<SharedPreferences>,
) {

    constructor(context: Context, messageListPreferences: MessageListPreferencesManager) : this(
        messageListPreferences = messageListPreferences,
        appliedPreferences = lazy {
            context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        },
    )

    fun applyOnce() {
        val applied = appliedPreferences.value
        if (applied.getBoolean(KEY_PREVIEW_LINES_APPLIED, false)) return

        val current = messageListPreferences.getConfig()
        messageListPreferences.save(current.copy(previewLines = CIVION_PREVIEW_LINES))

        applied.edit().putBoolean(KEY_PREVIEW_LINES_APPLIED, true).apply()
    }

    internal companion object {
        const val PREFERENCES_NAME = "civion_product_defaults"
        const val KEY_PREVIEW_LINES_APPLIED = "preview_lines_applied"

        /**
         * How many lines of the row the preview may take. The same text view carries the sender
         * first, so with upstream's two lines about one and a half are message text.
         *
         * Three: one more than upstream. The accepted Inbox change is that the list should tell
         * more of the message before it is opened. One extra line is the smallest step that
         * visibly does that - roughly two and a half lines of message text instead of one and a
         * half - while a row stays a row and the screen still shows a list. Four and more make
         * every row tall for the sake of the few messages that fill them.
         */
        const val CIVION_PREVIEW_LINES = 3
    }
}
