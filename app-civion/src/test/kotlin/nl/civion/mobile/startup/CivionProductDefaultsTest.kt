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
 * The product default is a starting value, not a rule: applied once per install, through the
 * engine's own preferences manager so the screens see it at once, and never again - so a value
 * the user changes back in Settings is left alone.
 */
class CivionProductDefaultsTest {

    private val editor = mock<SharedPreferences.Editor>()
    private val applied = mock<SharedPreferences>()
    private val manager = mock<MessageListPreferencesManager>()

    private val defaults = CivionProductDefaults(manager, lazyOf(applied))

    @Test
    fun `should save three preview lines and remember it on the first run`() {
        whenever(applied.getBoolean(CivionProductDefaults.KEY_PREVIEW_LINES_APPLIED, false)).thenReturn(false)
        whenever(applied.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(manager.getConfig()).thenReturn(DisplayMessageListSettings(previewLines = 2))

        defaults.applyOnce()

        verify(manager).save(DisplayMessageListSettings(previewLines = 3))
        verify(editor).putBoolean(CivionProductDefaults.KEY_PREVIEW_LINES_APPLIED, true)
        verify(editor).apply()
    }

    @Test
    fun `should change nothing but the preview lines`() {
        whenever(applied.getBoolean(CivionProductDefaults.KEY_PREVIEW_LINES_APPLIED, false)).thenReturn(false)
        whenever(applied.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        val current = DisplayMessageListSettings(previewLines = 2, isShowContactPicture = false)
        whenever(manager.getConfig()).thenReturn(current)

        defaults.applyOnce()

        verify(manager).save(current.copy(previewLines = 3))
    }

    @Test
    fun `should leave the setting alone once applied`() {
        whenever(applied.getBoolean(CivionProductDefaults.KEY_PREVIEW_LINES_APPLIED, false)).thenReturn(true)

        defaults.applyOnce()

        verify(manager, never()).save(any())
        verify(applied, never()).edit()
    }
}
