package nl.civion.mobile.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import nl.civion.mobile.ui.drawer.text3
import nl.civion.mobile.ui.screen.AppBarAction
import androidx.compose.material.icons.Icons as MaterialIcons

/*
 * Search (mockup screen 07), measured from the mockup: the field takes the bar, 44dp on a 22dp
 * radius with a 20dp glass and 15sp text; under it a row of 13sp filter chips, 8dp corners,
 * that combine.
 */
private const val BAR_VERTICAL_DP = 8
private const val BAR_EDGE_DP = 6
private const val FIELD_HEIGHT_DP = 44
private const val FIELD_EDGE_DP = 14
private const val FIELD_ICON_DP = 20
private const val FIELD_GAP_DP = 10
private const val FIELD_TEXT_SP = 15
private const val FIELD_LINE_SP = 20
private const val CHIP_ROW_TOP_DP = 4
private const val CHIP_ROW_EDGE_DP = 12
private const val CHIP_ROW_BOTTOM_DP = 10
private const val CHIP_GAP_DP = 8
private const val CHIP_CORNER_DP = 8
private const val CHIP_HORIZONTAL_DP = 11
private const val CHIP_VERTICAL_DP = 5
private const val CHIP_TEXT_SP = 13
private const val CHIP_LINE_SP = 18
private const val LIGHT_SURFACE_LUMINANCE = 0.5f

/** The field's own surface: #F1F1F1 on the light window, #323232 on the dark. */
private val FieldLight = Color(color = 0xFFF1F1F1)
private val FieldDark = Color(color = 0xFF323232)

/**
 * Search (mockup screen 07): the words in a field that takes the bar, and under it the filters -
 * all folders or this one, with attachment, unread, starred - which combine. Searching hands the
 * result to upstream's message list, which shows it with the Inbox row unchanged.
 */
@Composable
internal fun CivionSearchScreen(
    initial: CivionSearchQuery,
    onBack: () -> Unit,
    onSearch: (CivionSearchQuery) -> Unit,
) {
    var query by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    Surface(
        color = BoltTheme.colors.surface,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            SearchBar(
                text = query.text,
                onTextChange = { query = query.copy(text = it) },
                onBack = onBack,
                onSubmit = { onSearch(query) },
                focus = focus,
            )
            FilterRow(query = query, onChange = { query = it })
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BoltTheme.colors.surfaceContainer),
            )
        }
    }
}

@Composable
private fun SearchBar(
    text: String,
    onTextChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    focus: FocusRequester,
) {
    val fieldColour = if (BoltTheme.colors.surface.luminance() < LIGHT_SURFACE_LUMINANCE) FieldDark else FieldLight
    val textStyle = BoltTheme.typography.bodyLarge.copy(
        color = BoltTheme.colors.onSurface,
        fontSize = FIELD_TEXT_SP.sp,
        lineHeight = FIELD_LINE_SP.sp,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BAR_EDGE_DP.dp, vertical = BAR_VERTICAL_DP.dp),
    ) {
        AppBarAction(icon = Icons.Outlined.ArrowBack, label = "Back", onClick = onBack)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .height(FIELD_HEIGHT_DP.dp)
                .clip(CircleShape)
                .background(fieldColour)
                .padding(horizontal = FIELD_EDGE_DP.dp),
        ) {
            Icon(
                imageVector = MaterialIcons.Outlined.Search,
                tint = text3(),
                modifier = Modifier.size(FIELD_ICON_DP.dp),
            )
            Spacer(modifier = Modifier.width(FIELD_GAP_DP.dp))
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(BoltTheme.colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            BasicText(text = "Search mail", style = textStyle.copy(color = text3()))
                        }
                        field()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
            )
        }
    }
}

@Composable
private fun FilterRow(query: CivionSearchQuery, onChange: (CivionSearchQuery) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CHIP_GAP_DP.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = CHIP_ROW_EDGE_DP.dp,
                end = CHIP_ROW_EDGE_DP.dp,
                top = CHIP_ROW_TOP_DP.dp,
                bottom = CHIP_ROW_BOTTOM_DP.dp,
            ),
    ) {
        FilterChip(label = "All folders", on = query.allFolders) {
            onChange(query.copy(allFolders = !query.allFolders))
        }
        FilterChip(label = "With attachment", on = query.withAttachment) {
            onChange(query.copy(withAttachment = !query.withAttachment))
        }
        FilterChip(label = "Unread", on = query.unread) {
            onChange(query.copy(unread = !query.unread))
        }
        FilterChip(label = "Starred", on = query.starred) {
            onChange(query.copy(starred = !query.starred))
        }
    }
}

/** One filter: outlined when off, on the secondary container when on. */
@Composable
private fun FilterChip(label: String, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(CHIP_CORNER_DP.dp)

    BasicText(
        text = label,
        maxLines = 1,
        style = BoltTheme.typography.bodyLarge.copy(
            color = if (on) BoltTheme.colors.onSecondaryContainer else BoltTheme.colors.onSurfaceVariant,
            fontSize = CHIP_TEXT_SP.sp,
            lineHeight = CHIP_LINE_SP.sp,
        ),
        modifier = Modifier
            .clip(shape)
            .background(if (on) BoltTheme.colors.secondaryContainer else Color.Transparent)
            .border(1.dp, if (on) Color.Transparent else BoltTheme.colors.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = CHIP_HORIZONTAL_DP.dp, vertical = CHIP_VERTICAL_DP.dp),
    )
}
