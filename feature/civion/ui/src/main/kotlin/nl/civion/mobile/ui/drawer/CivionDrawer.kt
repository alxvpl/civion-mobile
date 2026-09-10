package nl.civion.mobile.ui.drawer

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawer
import net.thunderbird.feature.navigation.drawer.api.R
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android Mail's navigation drawer, in place of the upstream one.
 *
 * It implements the same [NavigationDrawer] interface and takes the same callbacks, so the host
 * activity constructs one instead of the other and nothing else changes. The DrawerLayout and
 * the ComposeView it fills are upstream's, found by the ids the drawer API already publishes -
 * there was no reason to own a layout as well as a drawer.
 *
 * The state is assembled here, from the engine's accounts and folders, so the Compose layer is
 * handed a finished picture and never queries anything itself.
 */
@Suppress("LongParameterList")
class CivionDrawer(
    override val parent: AppCompatActivity,
    private val openAccount: (accountUuid: String) -> Unit,
    private val openFolder: (accountUuid: String, folderId: Long) -> Unit,
    private val openUnifiedFolder: () -> Unit,
    private val openSearch: (LocalMessageSearch) -> Unit,
    private val openSettings: () -> Unit,
    private val openAddAccount: () -> Unit,
    createDrawerListener: () -> DrawerLayout.DrawerListener,
) : NavigationDrawer, KoinComponent {

    private val themeProvider: FeatureThemeProvider by inject()
    private val accountManager: LegacyAccountDtoManager by inject()
    private val displayFolderRepository: DisplayFolderRepository by inject()

    private val drawer: DrawerLayout = parent.findViewById(R.id.navigation_drawer_layout)
    private val drawerContent: ComposeView = parent.findViewById(R.id.navigation_drawer_content)

    private val state = MutableStateFlow(CivionDrawerState())

    init {
        drawer.addDrawerListener(createDrawerListener())

        ViewCompat.setOnApplyWindowInsetsListener(drawer) { _, insets ->
            drawerContent.dispatchApplyWindowInsets(insets.toWindowInsets())
            insets
        }

        observeAccounts()

        drawerContent.setContent {
            themeProvider.WithTheme {
                val drawerState = state.collectAsStateWithLifecycle()

                CivionDrawerContent(
                    state = drawerState.value,
                    onAllInboxesClick = {
                        close()
                        openUnifiedFolder()
                    },
                    onAccountClick = {
                        close()
                        openAccount(it)
                    },
                    onAccountMove = ::moveAccount,
                    onAddAccountClick = {
                        close()
                        openAddAccount()
                    },
                    onSmartDestinationClick = { destination ->
                        state.update { it.copy(selectedShortcut = destination) }
                        close()
                        openSearch(CivionDrawerSearches.forDestination(destination))
                    },
                    onFolderClick = { accountUuid, folderId ->
                        close()
                        openFolder(accountUuid, folderId)
                    },
                    onSettingsClick = {
                        close()
                        openSettings()
                    },
                )
            }
        }
    }

    /**
     * Keeps the drawer in step with the accounts and their folders.
     *
     * Folder flows are re-subscribed whenever the set of accounts changes, so an account added or
     * removed while the application is running is reflected without the drawer being rebuilt.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeAccounts() {
        accountManager.getAccountsFlow()
            .flatMapLatest { accounts ->
                if (accounts.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        accounts.map { account ->
                            displayFolderRepository.getDisplayFoldersFlow(account.uuid)
                        },
                    ) { folderLists ->
                        accounts.mapIndexed { index, account ->
                            val folders = folderLists[index].map { displayFolder ->
                                DrawerFolder(
                                    id = displayFolder.folder.id,
                                    name = displayFolder.folder.name,
                                    type = displayFolder.folder.type,
                                    unreadCount = displayFolder.unreadMessageCount,
                                )
                            }

                            DrawerAccount(
                                uuid = account.uuid,
                                email = account.email,
                                // The account's own number is its Inbox. Counting every folder
                                // would add Spam and Drafts to a badge the user reads as "mail
                                // waiting for me".
                                unreadCount = folders.firstOrNull { it.type == FolderType.INBOX }?.unreadCount ?: 0,
                                folders = folders,
                            )
                        }
                    }
                }
            }
            .onEach { accounts ->
                state.update {
                    it.copy(
                        accounts = accounts,
                        unifiedUnreadCount = accounts.sumOf { account -> account.unreadCount },
                    )
                }
            }
            .launchIn(parent.lifecycleScope)
    }

    private fun moveAccount(accountUuid: String, toPosition: Int) {
        val account = accountManager.getAccount(accountUuid) ?: return

        accountManager.moveAccount(account, toPosition)
    }

    override val isOpen: Boolean
        get() = drawer.isDrawerOpen(GravityCompat.START)

    override fun selectAccount(accountUuid: String) {
        state.update {
            it.copy(selectedAccountUuid = accountUuid, isUnifiedSelected = false, selectedShortcut = null)
        }
    }

    override fun selectFolder(accountUuid: String, folderId: Long) {
        state.update {
            it.copy(
                selectedAccountUuid = accountUuid,
                selectedFolderId = folderId,
                isUnifiedSelected = false,
                selectedShortcut = null,
            )
        }
    }

    override fun selectUnifiedInbox() {
        state.update {
            it.copy(isUnifiedSelected = true, selectedFolderId = null, selectedShortcut = null)
        }
    }

    override fun deselect() {
        state.update { it.copy(selectedFolderId = null) }
    }

    override fun open() = drawer.openDrawer(GravityCompat.START)

    override fun close() = drawer.closeDrawer(GravityCompat.START)

    override fun lock() = drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

    override fun unlock() = drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
}
