package nl.civion.mobile.ui.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListPreferencesManager
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.core.preference.update

/** What the Settings screen shows (mockup screen 09). */
internal data class CivionSettingsState(
    val accounts: List<SettingsAccount> = emptyList(),
    val previewLines: Int = 0,
    val isUnreadBackground: Boolean = false,
    val density: UiDensity = UiDensity.Default,
    val isNotifyNewMail: Boolean = false,
    val theme: AppTheme = AppTheme.FOLLOW_SYSTEM,
    val appVersion: String = "",
)

internal data class SettingsAccount(
    val uuid: String,
    val email: String,
    /** The incoming protocol as the engine names it - "imap", "pop3" - shown in capitals. */
    val protocol: String,
    /** Minutes between automatic checks, or -1 for manual only. */
    val checkIntervalMinutes: Int,
)

/**
 * The Settings screen's reading and writing of the product's own settings.
 *
 * Every value is read from the engine's own managers and written back through them, so the
 * upstream settings screens and this one always agree; nothing is stored twice. The theme
 * takes effect on its own: upstream's ThemeManager watches the same setting.
 */
internal class CivionSettingsModel(
    private val accountManager: LegacyAccountDtoManager,
    private val generalSettings: GeneralSettingsManager,
    private val messageListPreferences: MessageListPreferencesManager,
    private val appVersion: String,
    /** Called before the theme is written: the activity will be recreated for it. */
    private val onThemeChange: () -> Unit = {},
) {
    val state: Flow<CivionSettingsState> = combine(
        accountManager.getAccountsFlow(),
        generalSettings.getConfigFlow(),
        messageListPreferences.getConfigFlow(),
    ) { accounts, general, messageList ->
        CivionSettingsState(
            accounts = accounts.map { account ->
                SettingsAccount(
                    uuid = account.uuid,
                    email = account.email,
                    protocol = account.incomingServerSettings.type,
                    checkIntervalMinutes = account.automaticCheckIntervalMinutes,
                )
            },
            previewLines = messageList.previewLines,
            isUnreadBackground = messageList.isUseBackgroundAsUnreadIndicator,
            density = messageList.uiDensity,
            isNotifyNewMail = accounts.any { it.isNotifyNewMail },
            theme = general.display.coreSettings.appTheme,
            appVersion = appVersion,
        )
    }

    fun setPreviewLines(lines: Int) = messageListPreferences.update { it.copy(previewLines = lines) }

    fun setUnreadBackground(enabled: Boolean) =
        messageListPreferences.update { it.copy(isUseBackgroundAsUnreadIndicator = enabled) }

    fun setDensity(density: UiDensity) = messageListPreferences.update { it.copy(uiDensity = density) }

    fun setTheme(theme: AppTheme) {
        if (theme == generalSettings.getConfig().display.coreSettings.appTheme) return

        onThemeChange()
        generalSettings.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    coreSettings = settings.display.coreSettings.copy(appTheme = theme),
                ),
            )
        }
    }

    /** One switch for every account: the product does not ask per account what it asks once. */
    fun setNotifyNewMail(enabled: Boolean) {
        accountManager.getAccounts().forEach { account ->
            if (account.isNotifyNewMail != enabled) {
                account.isNotifyNewMail = enabled
                accountManager.saveAccount(account)
            }
        }
    }
}
