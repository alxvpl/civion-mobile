package nl.civion.mobile.ui.drawer

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import app.k9mail.legacy.ui.folder.FolderIconProvider
import app.k9mail.legacy.ui.folder.FolderNameFormatter
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
import nl.civion.mobile.ui.accounts.CivionAccountsScreen
import nl.civion.mobile.ui.screen.CivionScreenOverlay
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
    private val openManageFolders: () -> Unit,
    private val syncAccount: (accountUuid: String) -> Unit,
    private val openSettings: () -> Unit,
    private val openAddAccount: () -> Unit,
    createDrawerListener: () -> DrawerLayout.DrawerListener,
) : NavigationDrawer, KoinComponent {

    private val themeProvider: FeatureThemeProvider by inject()
    private val accountManager: LegacyAccountDtoManager by inject()
    private val displayFolderRepository: DisplayFolderRepository by inject()
    private val folderNameFormatter: FolderNameFormatter by inject()

    private val folderTree = CivionFolderTree(
        nameFormatter = folderNameFormatter,
        iconProvider = FolderIconProvider(),
    )

    private val drawer: DrawerLayout = parent.findViewById(R.id.navigation_drawer_layout)
    private val drawerContent: ComposeView = parent.findViewById(R.id.navigation_drawer_content)

    private val state = MutableStateFlow(CivionDrawerState())
    private val screens = CivionScreenOverlay(parent)

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
                    onAccountsClick = {
                        close()
                        showAccounts()
                    },
                    onAllInboxesClick = {
                        close()
                        openUnifiedFolder()
                    },
                    onAddAccountClick = {
                        close()
                        openAddAccount()
                    },
                    onFoldersToggle = {
                        state.update { it.copy(isFoldersOpen = !it.isFoldersOpen) }
                    },
                    onFolderClick = { accountUuid, folderId ->
                        close()
                        openFolder(accountUuid, folderId)
                    },
                    onManageFoldersClick = {
                        close()
                        openManageFolders()
                    },
                    onSyncAccountClick = {
                        close()
                        state.value.selectedAccountUuid?.let(syncAccount)
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
                            val displayFolders = folderLists[index]

                            DrawerAccount(
                                uuid = account.uuid,
                                email = account.email,
                                // The account's own number is its Inbox. Counting every folder
                                // would add Spam and Drafts to a badge the user reads as "mail
                                // waiting for me".
                                unreadCount = displayFolders
                                    .firstOrNull { it.folder.type == FolderType.INBOX }
                                    ?.unreadMessageCount ?: 0,
                                inboxFolderId = displayFolders
                                    .firstOrNull { it.folder.type == FolderType.INBOX }
                                    ?.folder?.id,
                                folders = folderTree.build(displayFolders),
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

    /** The account list (mockup screen 03), over this activity; choosing closes it. */
    private fun showAccounts() {
        screens.show {
            themeProvider.WithTheme {
                val drawerState = state.collectAsStateWithLifecycle()

                CivionAccountsScreen(
                    state = drawerState.value,
                    onBack = screens::hide,
                    onAllInboxesClick = {
                        screens.hide()
                        openUnifiedFolder()
                    },
                    onAccountClick = {
                        screens.hide()
                        openAccount(it)
                    },
                    onAccountMove = ::moveAccount,
                    onAddAccountClick = {
                        screens.hide()
                        openAddAccount()
                    },
                )
            }
        }
    }

    override val isOpen: Boolean
        get() = drawer.isDrawerOpen(GravityCompat.START)

    override fun selectAccount(accountUuid: String) {
        state.update {
            it.copy(selectedAccountUuid = accountUuid, isUnifiedSelected = false)
        }
    }

    override fun selectFolder(accountUuid: String, folderId: Long) {
        state.update {
            it.copy(
                selectedAccountUuid = accountUuid,
                selectedFolderId = folderId,
                isUnifiedSelected = false,
            )
        }
    }

    override fun selectUnifiedInbox() {
        state.update {
            it.copy(isUnifiedSelected = true, selectedFolderId = null)
        }
    }

    override fun deselect() {
        state.update { it.copy(selectedFolderId = null) }
    }

    /** Every opening starts at the same place: the account, its Inbox, and nothing unfolded. */
    override fun open() {
        state.update { it.copy(isFoldersOpen = false) }
        drawer.openDrawer(GravityCompat.START)
    }

    override fun close() = drawer.closeDrawer(GravityCompat.START)

    override fun lock() = drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

    override fun unlock() = drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
}
