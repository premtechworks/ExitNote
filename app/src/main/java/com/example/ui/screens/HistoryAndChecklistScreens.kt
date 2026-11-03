package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ChecklistItem
import com.example.data.local.SessionWithItems
import com.example.data.local.ThemeMode
import com.example.domain.TimeFormatter
import com.example.ui.components.ExitNoteIcons
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.SessionStatusBadge
import com.example.ui.theme.ExitNoteTheme
import com.example.ui.theme.ReceiptMetaTextStyle
import com.example.ui.theme.TimestampTextStyle

@Composable
fun HistoryScreen(
    completedSessions: List<SessionWithItems>,
    onOpenReceipt: (String) -> Unit,
    onStartFirstCheck: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateHome)

    val colors = ExitNoteTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("history_screen")
    ) {
        // Screen Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.history_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap any past session to inspect its immutable Exit Receipt.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText
            )
        }

        if (completedSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_empty_state_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                    border = BorderStroke(1.dp, colors.border),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(colors.secondarySurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ExitNoteIcons.History,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.history_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.history_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.secondaryText
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onStartFirstCheck,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("history_empty_start_check_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryAccent
                            )
                        ) {
                            Icon(
                                imageVector = ExitNoteIcons.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.start_check_button))
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("history_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = completedSessions,
                    key = { it.session.id }
                ) { sessionWithItems ->
                    val session = sessionWithItems.session
                    val dateLabel = TimeFormatter.formatDate(
                        session.completedAt ?: session.startedAt,
                        session.timezoneId
                    )
                    val startTime = TimeFormatter.formatTime(session.startedAt, session.timezoneId)
                    val endTime = session.completedAt?.let {
                        TimeFormatter.formatTime(it, session.timezoneId)
                    } ?: "—"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_session_card_${session.id}")
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                role = Role.Button,
                                onClick = { onOpenReceipt(session.id) }
                            ),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                        border = BorderStroke(1.dp, colors.border),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SessionStatusBadge(status = session.status)
                                Text(
                                    text = dateLabel,
                                    style = ReceiptMetaTextStyle,
                                    color = colors.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.templateNameSnapshot,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.primaryText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Started $startTime · Completed $endTime",
                                        style = TimestampTextStyle,
                                        color = colors.secondaryText
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.secondarySurface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = ExitNoteIcons.Receipt,
                                            contentDescription = null,
                                            tint = colors.primaryText,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Receipt",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.primaryText
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${sessionWithItems.confirmedCount} confirmed",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.success
                                )
                                Text(
                                    text = "·",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.secondaryText
                                )
                                Text(
                                    text = "${sessionWithItems.unconfirmedCount} unconfirmed",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (sessionWithItems.unconfirmedCount > 0) {
                                        colors.warning
                                    } else {
                                        colors.secondaryText
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChecklistManagementScreen(
    templateName: String,
    items: List<ChecklistItem>,
    dialogError: String?,
    bannerMessage: String?,
    isErrorBanner: Boolean,
    onDismissBanner: () -> Unit,
    onClearDialogError: () -> Unit,
    onAddItem: (String, String?, () -> Unit) -> Unit,
    onEditItem: (String, String, String?, () -> Unit) -> Unit,
    onToggleItemEnabled: (String, Boolean) -> Unit,
    onDeleteItem: (String) -> Unit,
    onReorderItem: (String, Boolean) -> Unit,
    onRestoreStarterItems: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateHome)

    val colors = ExitNoteTheme.colors
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ChecklistItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("checklist_management_screen")
    ) {
        // Header + Add button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.checklist_screen_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.checklist_screen_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    onClearDialogError()
                    showAddDialog = true
                },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_checklist_item_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = ExitNoteIcons.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.add_item_button))
            }
        }

        if (bannerMessage != null) {
            FeedbackBanner(
                message = bannerMessage,
                isError = isErrorBanner,
                onDismiss = onDismissBanner,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("checklist_editor_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (items.isEmpty()) {
                item(key = "empty_checklist") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your $templateName checklist is empty.",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onRestoreStarterItems,
                                modifier = Modifier.testTag("restore_starter_items_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.reset_checklist_action))
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.id }
                ) { index, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checklist_editor_item_${item.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isEnabled) colors.primarySurface else colors.secondarySurface
                        ),
                        border = BorderStroke(1.dp, colors.border),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.secondarySurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = ExitNoteIcons.forKey(item.icon),
                                        contentDescription = null,
                                        tint = if (item.isEnabled) colors.primaryText else colors.disabled,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (item.isEnabled) colors.primaryText else colors.secondaryText
                                    )
                                    Text(
                                        text = if (item.isEnabled) "Included in checks" else "Excluded from future checks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.secondaryText
                                    )
                                }

                                Switch(
                                    checked = item.isEnabled,
                                    onCheckedChange = { enabled ->
                                        onToggleItemEnabled(item.id, enabled)
                                    },
                                    modifier = Modifier.testTag("toggle_item_${item.id}"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.primarySurface,
                                        checkedTrackColor = colors.primaryAccent
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onReorderItem(item.id, true) },
                                    enabled = index > 0,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("move_up_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = ExitNoteIcons.MoveUp,
                                        contentDescription = stringResource(R.string.move_up_action),
                                        tint = if (index > 0) colors.secondaryText else colors.disabled
                                    )
                                }

                                IconButton(
                                    onClick = { onReorderItem(item.id, false) },
                                    enabled = index < items.lastIndex,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("move_down_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = ExitNoteIcons.MoveDown,
                                        contentDescription = stringResource(R.string.move_down_action),
                                        tint = if (index < items.lastIndex) colors.secondaryText else colors.disabled
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        onClearDialogError()
                                        editingItem = item
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("edit_item_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = ExitNoteIcons.Edit,
                                        contentDescription = "Edit ${item.title}",
                                        tint = colors.primaryText
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteItem(item.id) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("delete_item_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = ExitNoteIcons.Delete,
                                        contentDescription = "Delete ${item.title}",
                                        tint = colors.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ChecklistItemEditorDialog(
            dialogTitle = stringResource(R.string.add_item_dialog_title),
            initialTitle = "",
            initialIconKey = "door_lock",
            errorMessage = dialogError,
            onDismiss = {
                onClearDialogError()
                showAddDialog = false
            },
            onSave = { title, iconKey ->
                onAddItem(title, iconKey) {
                    showAddDialog = false
                }
            }
        )
    }

    editingItem?.let { targetItem ->
        ChecklistItemEditorDialog(
            dialogTitle = stringResource(R.string.edit_item_dialog_title),
            initialTitle = targetItem.title,
            initialIconKey = targetItem.icon ?: "check",
            errorMessage = dialogError,
            onDismiss = {
                onClearDialogError()
                editingItem = null
            },
            onSave = { newTitle, newIconKey ->
                onEditItem(targetItem.id, newTitle, newIconKey) {
                    editingItem = null
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChecklistItemEditorDialog(
    dialogTitle: String,
    initialTitle: String,
    initialIconKey: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    val colors = ExitNoteTheme.colors
    var titleInput by rememberSaveable(initialTitle) { mutableStateOf(initialTitle) }
    var selectedIconKey by rememberSaveable(initialIconKey) { mutableStateOf(initialIconKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.primarySurface,
        titleContentColor = colors.primaryText,
        title = {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(stringResource(R.string.item_title_label)) },
                    placeholder = { Text(stringResource(R.string.item_title_placeholder)) },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checklist_item_title_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        modifier = Modifier.testTag("checklist_item_dialog_error")
                    )
                }

                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.secondaryText
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExitNoteIcons.availableIconKeys.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedIconKey == key,
                            onClick = { selectedIconKey = key },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = ExitNoteIcons.forKey(key),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentSubtle,
                                selectedLabelColor = colors.primaryAccent,
                                selectedLeadingIconColor = colors.primaryAccent
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(titleInput, selectedIconKey) },
                modifier = Modifier.testTag("save_checklist_item_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent
                )
            ) {
                Text(stringResource(R.string.save_action))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_checklist_item_button")
            ) {
                Text(
                    text = stringResource(R.string.cancel_action),
                    color = colors.secondaryText
                )
            }
        }
    )
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    bannerMessage: String?,
    isErrorBanner: Boolean,
    onDismissBanner: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onRestoreStarterItems: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateHome)

    val colors = ExitNoteTheme.colors

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "settings_header") {
            Column {
                Text(
                    text = stringResource(R.string.settings_screen_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.secondaryText
                )
            }
        }

        if (bannerMessage != null) {
            item(key = "settings_banner") {
                FeedbackBanner(
                    message = bannerMessage,
                    isError = isErrorBanner,
                    onDismiss = onDismissBanner
                )
            }
        }

        // Theme Mode Selection
        item(key = "theme_section") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.theme_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                            ThemeMode.LIGHT to stringResource(R.string.theme_light),
                            ThemeMode.DARK to stringResource(R.string.theme_dark)
                        )
                        options.forEach { (mode, label) ->
                            val selected = themeMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        role = Role.RadioButton,
                                        onClick = { onSelectThemeMode(mode) }
                                    )
                                    .testTag("theme_option_${mode.name.lowercase()}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) colors.accentSubtle else colors.secondarySurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) colors.primaryAccent else colors.border
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) colors.primaryAccent else colors.primaryText,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Restore Default Checklist Starter Items
        item(key = "restore_starter_section") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Checklist Defaults",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryText
                    )
                    Text(
                        text = "Re-add any deleted default items to your Leaving Home template without affecting custom items or historical receipts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText
                    )
                    OutlinedButton(
                        onClick = onRestoreStarterItems,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_restore_defaults_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(
                            imageVector = ExitNoteIcons.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = colors.primaryText
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.reset_checklist_action),
                            color = colors.primaryText
                        )
                    }
                }
            }
        }

        // Offline & Local Storage Disclosure (Required by PRD)
        item(key = "privacy_disclosure_section") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_disclosure_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = ExitNoteIcons.Shield,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.privacy_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryText
                        )
                    }

                    Text(
                        text = stringResource(R.string.privacy_storage_disclosure),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText
                    )

                    Text(
                        text = stringResource(R.string.product_truth_disclosure),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText
                    )
                }
            }
        }
    }
}
