package nl.civion.mobile.ui.drawer

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import nl.civion.mobile.ui.accounts.CivionAccountsScreen
import nl.civion.mobile.ui.folders.CivionFoldersScreen
import nl.civion.mobile.ui.screen.CivionScreenOverlay
import nl.civion.mobile.ui.search.CivionSearchNavigator
import nl.civion.mobile.ui.search.CivionSearchQuery
import nl.civion.mobile.ui.search.CivionSearchScreen
import nl.civion.mobile.ui.settings.CivionSettingsActions
import nl.civion.mobile.ui.settings.CivionSettingsModel
import nl.civion.mobile.ui.settings.CivionSettingsNavigator
import nl.civion.mobile.ui.settings.CivionSettingsScreen
import nl.civion.mobile.ui.settings.CivionSettingsState

/**
 * The screens the drawer leads to - Accounts (mockup 03) and Settings (mockup 09) - shown over
 * the activity that owns the drawer, on the same account callbacks the host hands it.
 */
@Suppress("LongParameterList")
internal class CivionDrawerScreens(
    private val parent: AppCompatActivity,
    private val themeProvider: FeatureThemeProvider,
    private val drawerState: StateFlow<CivionDrawerState>,
    private val settingsNavigator: CivionSettingsNavigator,
    private val searchNavigator: CivionSearchNavigator,
    settingsModel: (onThemeChange: () -> Unit) -> CivionSettingsModel,
    private val openAccount: (accountUuid: String) -> Unit,
    private val openUnifiedFolder: () -> Unit,
    private val openAddAccount: () -> Unit,
    private val moveAccount: (accountUuid: String, toPosition: Int) -> Unit,
    private val openFolder: (accountUuid: String, folderId: Long) -> Unit,
    private val openManageFolders: () -> Unit,
) {
    private val overlay = CivionScreenOverlay(parent)

    // A theme change recreates the activity, and this object with it. The flag outlives both so
    // that the new drawer puts Settings back where the user was.
    private val settingsModel = settingsModel { reopenSettings = true }

    init {
        if (reopenSettings) {
            reopenSettings = false
            showSettings()
        }
    }

    /** The account list; choosing closes it. */
    fun showAccounts() {
        overlay.show {
            themeProvider.WithTheme {
                val state = drawerState.collectAsStateWithLifecycle()

                CivionAccountsScreen(
                    state = state.value,
                    onBack = overlay::hide,
                    onAllInboxesClick = {
                        overlay.hide()
                        openUnifiedFolder()
                    },
                    onAccountClick = {
                        overlay.hide()
                        openAccount(it)
                    },
                    onAccountMove = moveAccount,
                    onAddAccountClick = {
                        overlay.hide()
                        openAddAccount()
                    },
                )
            }
        }
    }

    /** The account's folders on a screen (mockup 08); opening one closes it. */
    fun showFolders() {
        overlay.show {
            themeProvider.WithTheme {
                val state = drawerState.collectAsStateWithLifecycle()
                val account = state.value.selectedAccount

                CivionFoldersScreen(
                    state = state.value,
                    onBack = overlay::hide,
                    onFolderClick = { folderId ->
                        overlay.hide()
                        account?.let { openFolder(it.uuid, folderId) }
                    },
                    onManageFoldersClick = openManageFolders,
                )
            }
        }
    }

    /** Search (mockup 07); searching hands the result to upstream's list and closes it. */
    fun showSearch() {
        overlay.show {
            themeProvider.WithTheme {
                CivionSearchScreen(
                    initial = CivionSearchQuery(),
                    onBack = overlay::hide,
                    onSearch = { query ->
                        val state = drawerState.value
                        val accountUuid = state.selectedAccountUuid.takeUnless { state.isUnifiedSelected }

                        overlay.hide()
                        searchNavigator.showResults(parent, query.toLocalSearch(accountUuid, state.selectedFolderId))
                    },
                )
            }
        }
    }

    fun showSettings() {
        val actions = CivionSettingsActions(
            onBack = overlay::hide,
            onAccountClick = { settingsNavigator.openAccountSettings(parent, it) },
            onAddAccountClick = {
                overlay.hide()
                openAddAccount()
            },
            onNotificationActionsClick = { settingsNavigator.openNotificationActions(parent) },
            onOpenUrl = { settingsNavigator.openUrl(parent, it) },
        )

        overlay.show {
            themeProvider.WithTheme {
                val state = settingsModel.state.collectAsStateWithLifecycle(CivionSettingsState())

                CivionSettingsScreen(state = state.value, model = settingsModel, actions = actions)
            }
        }
    }

    private companion object {
        var reopenSettings = false
    }
}
