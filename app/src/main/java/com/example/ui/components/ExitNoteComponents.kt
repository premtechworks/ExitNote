package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SessionItem
import com.example.data.local.SessionStatus
import com.example.data.local.SessionWithItems
import com.example.data.repository.ExitNoteRepository
import com.example.domain.TimeFormatter
import com.example.ui.theme.ExitNoteTheme
import com.example.ui.theme.ReceiptMetaTextStyle
import com.example.ui.theme.TimestampTextStyle

@Composable
fun SessionStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val colors = ExitNoteTheme.colors
    val (label, bgColor, textColor) = when (status) {
        SessionStatus.COMPLETED -> Triple(
            stringResource(R.string.receipt_status_completed),
            colors.successSubtle,
            colors.success
        )
        SessionStatus.COMPLETED_WITH_UNCONFIRMED -> Triple(
            stringResource(R.string.receipt_status_unconfirmed),
            colors.warningSubtle,
            colors.warning
        )
        SessionStatus.ABANDONED -> Triple(
            stringResource(R.string.receipt_status_abandoned),
            colors.errorSubtle,
            colors.error
        )
        else -> Triple(
            "In progress",
            colors.accentSubtle,
            colors.primaryAccent
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Reusable interactive checklist item row for an active check session.
 * - Tapping an unchecked row immediately confirms the item.
 * - Tapping an already confirmed row does NOT overwrite its timestamp.
 * - An explicit "Uncheck" control clears the confirmation timestamp so rechecking records a new time.
 */
@Composable
fun ActiveSessionItemRow(
    item: SessionItem,
    onConfirm: () -> Unit,
    onUncheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExitNoteTheme.colors
    val isChecked = item.isChecked
    val containerColor by animateColorAsState(
        targetValue = if (isChecked) colors.primarySurface else colors.primarySurface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "itemBgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isChecked) colors.success.copy(alpha = 0.45f) else colors.border,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "itemBorderColor"
    )
    val iconKey = ExitNoteRepository.inferIconKeyFromTitle(item.titleSnapshot)
    val formattedTimestamp = item.checkedAt?.let {
        TimeFormatter.formatTimeWithSeconds(it, item.checkedTimezoneId)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("session_item_row_${item.id}")
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                role = Role.Button,
                onClick = {
                    // Tapping the main card confirms an unchecked item;
                    // repeated taps on an already confirmed item never overwrite its timestamp.
                    onConfirm()
                }
            )
            .semantics {
                contentDescription = if (isChecked) {
                    "${item.titleSnapshot}, Confirmed at ${formattedTimestamp ?: ""}"
                } else {
                    "${item.titleSnapshot}, Unconfirmed. Tap to confirm."
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Check circle indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isChecked) colors.success else colors.secondarySurface)
                    .border(
                        width = if (isChecked) 0.dp else 1.5.dp,
                        color = if (isChecked) colors.success else colors.disabled,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) {
                    Icon(
                        imageVector = ExitNoteIcons.CheckMark,
                        contentDescription = null,
                        tint = colors.primarySurface,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Category icon badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isChecked) colors.successSubtle else colors.secondarySurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ExitNoteIcons.forKey(iconKey),
                    contentDescription = null,
                    tint = if (isChecked) colors.success else colors.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Item title + confirmation status/timestamp
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.titleSnapshot,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (isChecked && formattedTimestamp != null) {
                    Text(
                        text = "You confirmed this at $formattedTimestamp",
                        style = TimestampTextStyle,
                        color = colors.success,
                        modifier = Modifier.testTag("session_item_timestamp_${item.id}")
                    )
                } else {
                    Text(
                        text = stringResource(R.string.item_unconfirmed_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText
                    )
                }
            }

            // Explicit Uncheck action when confirmed, or subtle dash when unconfirmed
            if (isChecked) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onUncheck,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("uncheck_button_${item.id}")
                ) {
                    Text(
                        text = stringResource(R.string.uncheck_action),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.secondaryText
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "—",
                    style = TimestampTextStyle,
                    color = colors.disabled
                )
            }
        }
    }
}

/**
 * Reusable compact summary card for a completed session (used on Home for Latest Receipt and in History).
 */
@Composable
fun CompletedSessionSummaryCard(
    sessionWithItems: SessionWithItems,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "receipt_summary_card"
) {
    val colors = ExitNoteTheme.colors
    val session = sessionWithItems.session
    val completedTime = TimeFormatter.formatTime(
        session.completedAt ?: session.startedAt,
        session.timezoneId
    )
    val startTime = TimeFormatter.formatTime(session.startedAt, session.timezoneId)
    val dateLabel = TimeFormatter.formatDate(
        session.completedAt ?: session.startedAt,
        session.timezoneId
    )
    val confirmedCount = sessionWithItems.confirmedCount
    val totalCount = sessionWithItems.totalCount
    val unconfirmedCount = sessionWithItems.unconfirmedCount

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTagPrefix)
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick),
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

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Last check — $completedTime",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (unconfirmedCount == 0) {
                            "$confirmedCount of $totalCount items confirmed"
                        } else {
                            "$confirmedCount confirmed · $unconfirmedCount unconfirmed"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (unconfirmedCount == 0) colors.success else colors.warning,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.secondarySurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = ExitNoteIcons.Receipt,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.view_receipt_action),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preview of up to 3 confirmed items with their timestamps
            val previewItems = sessionWithItems.sortedItems.take(3)
            if (previewItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.secondarySurface.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    previewItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isChecked) ExitNoteIcons.Confirm else ExitNoteIcons.Pending,
                                    contentDescription = null,
                                    tint = if (item.isChecked) colors.success else colors.secondaryText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = item.titleSnapshot,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = item.checkedAt?.let {
                                    TimeFormatter.formatTime(it, item.checkedTimezoneId)
                                } ?: "Unconfirmed",
                                style = TimestampTextStyle,
                                color = if (item.isChecked) colors.secondaryText else colors.warning
                            )
                        }
                    }
                    if (sessionWithItems.totalCount > 3) {
                        Text(
                            text = "+ ${sessionWithItems.totalCount - 3} more items in full receipt (Started $startTime)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryText
                        )
                    }
                }
            }
        }
    }
}

/**
 * Prominent recovery card when an unfinished IN_PROGRESS session exists.
 */
@Composable
fun UnfinishedSessionRecoveryCard(
    activeSession: SessionWithItems,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExitNoteTheme.colors
    val startedTime = TimeFormatter.formatTime(
        activeSession.session.startedAt,
        activeSession.session.timezoneId
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("unfinished_session_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.accentSubtle),
        border = BorderStroke(1.dp, colors.primaryAccent.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = ExitNoteIcons.Clock,
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.unfinished_session_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryText
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Started at $startedTime · ${activeSession.confirmedCount} of ${activeSession.totalCount} items confirmed so far.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("resume_session_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.resume_check_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("discard_session_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.primarySurface,
                        contentColor = colors.primaryText
                    )
                ) {
                    Text(
                        text = stringResource(R.string.discard_check_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun IncompleteSessionConfirmationDialog(
    unconfirmedCount: Int,
    totalCount: Int,
    onConfirmFinish: () -> Unit,
    onReturnToCheck: () -> Unit
) {
    val colors = ExitNoteTheme.colors
    AlertDialog(
        onDismissRequest = onReturnToCheck,
        containerColor = colors.primarySurface,
        titleContentColor = colors.primaryText,
        textContentColor = colors.secondaryText,
        icon = {
            Icon(
                imageVector = ExitNoteIcons.Warning,
                contentDescription = null,
                tint = colors.warning
            )
        },
        title = {
            Text(
                text = stringResource(R.string.incomplete_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "$unconfirmedCount of $totalCount items have not been confirmed in this check. If you finish now, these items will be explicitly recorded as unconfirmed on your Exit Receipt.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmFinish,
                modifier = Modifier.testTag("confirm_incomplete_finish_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryText,
                    contentColor = colors.primarySurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.incomplete_dialog_Confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onReturnToCheck,
                modifier = Modifier.testTag("return_to_check_button")
            ) {
                Text(
                    text = stringResource(R.string.incomplete_dialog_return),
                    color = colors.primaryAccent
                )
            }
        }
    )
}

@Composable
fun DiscardSessionConfirmationDialog(
    onConfirmDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = ExitNoteTheme.colors
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = colors.primarySurface,
        titleContentColor = colors.primaryText,
        textContentColor = colors.secondaryText,
        title = {
            Text(
                text = stringResource(R.string.discard_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.discard_dialog_body),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDiscard,
                modifier = Modifier.testTag("confirm_discard_dialog_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.error,
                    contentColor = colors.primarySurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.discard_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("cancel_discard_dialog_button")
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
fun UnfinishedSessionPromptDialog(
    activeSession: SessionWithItems,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ExitNoteTheme.colors
    val startedTime = TimeFormatter.formatTime(
        activeSession.session.startedAt,
        activeSession.session.timezoneId
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.primarySurface,
        titleContentColor = colors.primaryText,
        textContentColor = colors.secondaryText,
        title = {
            Text(
                text = stringResource(R.string.unfinished_session_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "You started a departure check at $startedTime (${activeSession.confirmedCount} of ${activeSession.totalCount} items confirmed). Would you like to resume where you left off or discard it?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onResume,
                modifier = Modifier.testTag("dialog_resume_session_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.resume_check_button))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDiscard,
                modifier = Modifier.testTag("dialog_discard_session_button")
            ) {
                Text(
                    text = stringResource(R.string.discard_check_button),
                    color = colors.error
                )
            }
        }
    )
}

@Composable
fun FeedbackBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExitNoteTheme.colors
    val bgColor = if (isError) colors.errorSubtle else colors.accentSubtle
    val contentColor = if (isError) colors.error else colors.primaryText

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("feedback_banner"),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = ExitNoteIcons.Close,
                    contentDescription = "Dismiss notification",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
