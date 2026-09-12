package nl.civion.mobile.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import nl.civion.mobile.ui.screen.CivionScreen

private const val THUNDERBIRD_URL = "https://www.thunderbird.net/mobile"
private const val PREVIEW_LINES_MAX = 5
private val PREVIEW_LINE_CHOICES = (0..PREVIEW_LINES_MAX).toList()

/** What the Settings screen can be asked to do beyond writing a setting. */
internal class CivionSettingsActions(
    val onBack: () -> Unit,
    val onAccountClick: (accountUuid: String) -> Unit,
    val onAddAccountClick: () -> Unit,
    val onNotificationActionsClick: () -> Unit,
    val onOpenUrl: (url: String) -> Unit,
)

/**
 * Settings (mockup screen 09): the product's own settings in five groups, each row naming its
 * current value. The values the product decided - preview lines, the background as unread
 * indicator - are ordinary settings here that the user may change.
 */
@Composable
internal fun CivionSettingsScreen(
    state: CivionSettingsState,
    model: CivionSettingsModel,
    actions: CivionSettingsActions,
) {
    var choice by remember { mutableStateOf<Choice?>(null) }

    CivionScreen(title = "Settings", onBack = actions.onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            AccountsGroup(state = state, actions = actions)
            ReadingGroup(state = state, model = model, onChoose = { choice = it })
            NotificationsGroup(state = state, model = model, actions = actions)
            AppearanceGroup(state = state, onChoose = { choice = it })
            AboutGroup(state = state, actions = actions)
        }
    }

    choice?.let { open ->
        ChoiceFor(choice = open, state = state, model = model, onDismiss = { choice = null })
    }
}

private enum class Choice { PreviewLines, Density, Theme }

@Composable
private fun AccountsGroup(state: CivionSettingsState, actions: CivionSettingsActions) {
    SettingsGroup(title = "ACCOUNTS")
    state.accounts.forEach { account ->
        SettingsRow(
            title = account.email,
            subtitle = accountSummary(account),
            onClick = { actions.onAccountClick(account.uuid) },
        )
    }
    SettingsRow(title = "Add account", onClick = actions.onAddAccountClick)
}

@Composable
private fun ReadingGroup(state: CivionSettingsState, model: CivionSettingsModel, onChoose: (Choice) -> Unit) {
    SettingsGroup(title = "READING")
    SettingsRow(
        title = "Preview lines",
        subtitle = previewLinesLabel(state.previewLines),
        onClick = { onChoose(Choice.PreviewLines) },
    )
    SettingsRow(
        title = "Background as unread indicator",
        subtitle = "Unread rows carry the list background",
        onClick = { model.setUnreadBackground(!state.isUnreadBackground) },
    ) {
        CivionSwitch(
            checked = state.isUnreadBackground,
            label = "Background as unread indicator",
            onCheckedChange = model::setUnreadBackground,
        )
    }
    SettingsRow(
        title = "Message list density",
        subtitle = densityLabel(state.density),
        onClick = { onChoose(Choice.Density) },
    )
}

@Composable
private fun NotificationsGroup(state: CivionSettingsState, model: CivionSettingsModel, actions: CivionSettingsActions) {
    SettingsGroup(title = "NOTIFICATIONS")
    SettingsRow(
        title = "New mail notifications",
        subtitle = "All accounts",
        onClick = { model.setNotifyNewMail(!state.isNotifyNewMail) },
    ) {
        CivionSwitch(
            checked = state.isNotifyNewMail,
            label = "New mail notifications",
            onCheckedChange = model::setNotifyNewMail,
        )
    }
    SettingsRow(
        title = "Notification actions",
        subtitle = "Archive, Delete, Mark read",
        onClick = actions.onNotificationActionsClick,
    )
}

@Composable
private fun AppearanceGroup(state: CivionSettingsState, onChoose: (Choice) -> Unit) {
    SettingsGroup(title = "APPEARANCE")
    SettingsRow(
        title = "Theme",
        subtitle = themeLabel(state.theme),
        onClick = { onChoose(Choice.Theme) },
    )
    SettingsRow(title = "Accent", subtitle = "Deep Violet") {
        ColourSwatch(colour = BoltTheme.colors.primary)
    }
}

@Composable
private fun AboutGroup(state: CivionSettingsState, actions: CivionSettingsActions) {
    SettingsGroup(title = "ABOUT")
    SettingsRow(title = "CIVION Mail", subtitle = state.appVersion)
    SettingsRow(
        title = "Built on Thunderbird for Android",
        subtitle = "thunderbird.net/mobile",
        onClick = { actions.onOpenUrl(THUNDERBIRD_URL) },
    )
}

@Composable
private fun ChoiceFor(
    choice: Choice,
    state: CivionSettingsState,
    model: CivionSettingsModel,
    onDismiss: () -> Unit,
) {
    when (choice) {
        Choice.PreviewLines -> ChoiceDialog(
            title = "Preview lines",
            options = Choices(PREVIEW_LINE_CHOICES.map { it to previewLinesLabel(it) }),
            selected = state.previewLines,
            onSelect = model::setPreviewLines,
            onDismiss = onDismiss,
        )

        Choice.Density -> ChoiceDialog(
            title = "Message list density",
            options = Choices(UiDensity.entries.map { it to densityLabel(it) }),
            selected = state.density,
            onSelect = model::setDensity,
            onDismiss = onDismiss,
        )

        Choice.Theme -> ChoiceDialog(
            title = "Theme",
            options = Choices(AppTheme.entries.map { it to themeLabel(it) }),
            selected = state.theme,
            onSelect = model::setTheme,
            onDismiss = onDismiss,
        )
    }
}

private fun accountSummary(account: SettingsAccount): String {
    val protocol = account.protocol.uppercase()
    val interval = when {
        account.checkIntervalMinutes <= 0 -> "manual"
        else -> "every ${account.checkIntervalMinutes} min"
    }
    return "$protocol · $interval"
}

private fun previewLinesLabel(lines: Int): String = when (lines) {
    0 -> "None"
    1 -> "1 line"
    else -> "$lines lines"
}

private fun densityLabel(density: UiDensity): String = when (density) {
    UiDensity.Compact -> "Compact"
    UiDensity.Default -> "Standard"
    UiDensity.Relaxed -> "Relaxed"
}

private fun themeLabel(theme: AppTheme): String = when (theme) {
    AppTheme.LIGHT -> "Light"
    AppTheme.DARK -> "Dark"
    AppTheme.FOLLOW_SYSTEM -> "Follow system"
}
