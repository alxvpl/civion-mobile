package nl.civion.mobile.startup

import android.content.SharedPreferences
import kotlin.test.Test
import net.thunderbird.core.preference.display.visualSettings.message.list.DisplayMessageListSettings
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListPreferencesManager
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * The product defaults are starting values, not rules: each applied once per install, through the
 * engine's own preferences manager so the screens see it at once, and never again - so a value
 * the user changes back in Settings is left alone, even when a later default arrives.
 */
class CivionProductDefaultsTest {

    private val editor = mock<SharedPreferences.Editor>()
    private val applied = mock<SharedPreferences>()
    private val manager = mock<MessageListPreferencesManager>()

    private val defaults = CivionProductDefaults(manager, lazyOf(applied))

    private fun given(previewApplied: Boolean, backgroundApplied: Boolean, current: DisplayMessageListSettings) {
        whenever(applied.getBoolean(CivionProductDefaults.KEY_PREVIEW_LINES_APPLIED, false)).thenReturn(previewApplied)
        whenever(applied.getBoolean(CivionProductDefaults.KEY_UNREAD_BACKGROUND_APPLIED, false))
            .thenReturn(backgroundApplied)
        whenever(applied.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(manager.getConfig()).thenReturn(current)
    }

    @Test
    fun `should apply both defaults and remember them on a fresh install`() {
        given(previewApplied = false, backgroundApplied = false, current = DisplayMessageListSettings())

        defaults.applyOnce()

        verify(manager).save(DisplayMessageListSettings(previewLines = 2, isUseBackgroundAsUnreadIndicator = true))
        verify(editor).putBoolean(CivionProductDefaults.KEY_PREVIEW_LINES_APPLIED, true)
        verify(editor).putBoolean(CivionProductDefaults.KEY_UNREAD_BACKGROUND_APPLIED, true)
        verify(editor).apply()
    }

    @Test
    fun `should change nothing but the defaults`() {
        val current = DisplayMessageListSettings(isShowContactPicture = false)
        given(previewApplied = false, backgroundApplied = false, current = current)

        defaults.applyOnce()

        verify(manager).save(current.copy(previewLines = 2, isUseBackgroundAsUnreadIndicator = true))
    }

    @Test
    fun `should apply only the default that is new, keeping the user's value for the other`() {
        val current = DisplayMessageListSettings(previewLines = 3)
        given(previewApplied = true, backgroundApplied = false, current = current)

        defaults.applyOnce()

        verify(manager).save(current.copy(isUseBackgroundAsUnreadIndicator = true))
    }

    @Test
    fun `should leave the settings alone once every default is applied`() {
        given(previewApplied = true, backgroundApplied = true, current = DisplayMessageListSettings())

        defaults.applyOnce()

        verify(manager, never()).save(any())
        verify(applied, never()).edit()
    }
}
