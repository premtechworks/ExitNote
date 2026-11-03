package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SessionItem
import com.example.data.local.SessionStatus
import com.example.data.local.SessionWithItems
import com.example.data.repository.ExitNoteRepository
import com.example.domain.TimeFormatter
import com.example.ui.components.ExitNoteBrandLogo
import com.example.ui.components.ExitNoteIcons
import com.example.ui.components.SessionStatusBadge
import com.example.ui.theme.ExitNoteTheme
import com.example.ui.theme.ReceiptMetaTextStyle
import com.example.ui.theme.TimestampTextStyle

@Composable
fun ExitReceiptScreen(
    sessionWithItems: SessionWithItems?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val colors = ExitNoteTheme.colors

    if (sessionWithItems == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Receipt not found.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.secondaryText
            )
        }
        return
    }

    val session = sessionWithItems.session
    val sortedItems = sessionWithItems.sortedItems
    val confirmedItems = sortedItems.filter { it.isChecked }
    val unconfirmedItems = sortedItems.filter { !it.isChecked }

    val dateFormatted = TimeFormatter.formatDate(
        session.completedAt ?: session.startedAt,
        session.timezoneId
    )
    val startTimeFormatted = TimeFormatter.formatTimeWithSeconds(
        session.startedAt,
        session.timezoneId
    )
    val completedTimeFormatted = session.completedAt?.let {
        TimeFormatter.formatTimeWithSeconds(it, session.timezoneId)
    } ?: "—"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("exit_receipt_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("receipt_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.primaryText
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.receipt_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primaryText
                    )
                    Text(
                        text = session.templateNameSnapshot,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText
                    )
                }
            }

            ExitNoteBrandLogo(size = 34.dp)
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Receipt Header Summary Card
            item(key = "receipt_header_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_summary_header"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                    border = BorderStroke(1.dp, colors.border),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        SessionStatusBadge(
                            status = session.status,
                            modifier = Modifier.testTag("receipt_status_badge")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = when (session.status) {
                                SessionStatus.COMPLETED -> stringResource(R.string.receipt_status_completed)
                                SessionStatus.COMPLETED_WITH_UNCONFIRMED -> stringResource(R.string.receipt_status_unconfirmed)
                                else -> stringResource(R.string.receipt_status_abandoned)
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.primaryText,
                            modifier = Modifier.testTag("receipt_status_headline")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = dateFormatted,
                            style = ReceiptMetaTextStyle,
                            color = colors.secondaryText,
                            modifier = Modifier.testTag("receipt_date_text")
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = colors.border)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Start Time, Completion Time, Timezone metadata grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "STARTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.secondaryText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = startTimeFormatted,
                                    style = ReceiptMetaTextStyle,
                                    color = colors.primaryText,
                                    modifier = Modifier.testTag("receipt_start_time")
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "COMPLETED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.secondaryText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = completedTimeFormatted,
                                    style = ReceiptMetaTextStyle,
                                    color = colors.primaryText,
                                    modifier = Modifier.testTag("receipt_completed_time")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Confirmed vs Unconfirmed count pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("receipt_confirmed_count_box"),
                                shape = RoundedCornerShape(12.dp),
                                color = colors.successSubtle
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "${confirmedItems.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.success,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Confirmed items",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.primaryText
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("receipt_unconfirmed_count_box"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (unconfirmedItems.isEmpty()) {
                                    colors.secondarySurface
                                } else {
                                    colors.warningSubtle
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "${unconfirmedItems.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (unconfirmedItems.isEmpty()) {
                                            colors.secondaryText
                                        } else {
                                            colors.warning
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Unconfirmed items",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.primaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Mandatory Product Truth Disclaimer Card (immediately visible at top of receipt)
            item(key = "receipt_disclaimer") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_disclaimer_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.secondarySurface,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = ExitNoteIcons.Info,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.receipt_disclaimer),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondaryText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Timezone recorded: ${session.timezoneId}",
                                style = TimestampTextStyle,
                                color = colors.secondaryText
                            )
                        }
                    }
                }
            }

            // 3. Confirmed Items Section
            if (confirmedItems.isNotEmpty()) {
                item(key = "confirmed_header") {
                    Text(
                        text = "${stringResource(R.string.receipt_confirmed_section)} (${confirmedItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryText
                    )
                }

                items(
                    items = confirmedItems,
                    key = { "confirmed_${it.id}" }
                ) { item ->
                    ReceiptConfirmedItemCard(item = item)
                }
            }

            // 4. Unconfirmed Items Section (explicitly visually distinct, never shown as completed)
            if (unconfirmedItems.isNotEmpty()) {
                item(key = "unconfirmed_header") {
                    Text(
                        text = "${stringResource(R.string.receipt_unconfirmed_section)} (${unconfirmedItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.warning
                    )
                }

                items(
                    items = unconfirmedItems,
                    key = { "unconfirmed_${it.id}" }
                ) { item ->
                    ReceiptUnconfirmedItemCard(item = item)
                }
            }
        }

        // Bottom Done button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.primarySurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("receipt_done_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryText,
                        contentColor = colors.primarySurface
                    )
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptConfirmedItemCard(item: SessionItem) {
    val colors = ExitNoteTheme.colors
    val iconKey = ExitNoteRepository.inferIconKeyFromTitle(item.titleSnapshot)
    val confirmedTime = item.checkedAt?.let {
        TimeFormatter.formatTimeWithSeconds(it, item.checkedTimezoneId)
    } ?: "—"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("receipt_item_confirmed_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colors.success),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ExitNoteIcons.CheckMark,
                    contentDescription = "Confirmed",
                    tint = colors.primarySurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = ExitNoteIcons.forKey(iconKey),
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.titleSnapshot,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primaryText
                )
                Text(
                    text = "You confirmed this.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.successSubtle
            ) {
                Text(
                    text = confirmedTime,
                    style = TimestampTextStyle,
                    color = colors.success,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ReceiptUnconfirmedItemCard(item: SessionItem) {
    val colors = ExitNoteTheme.colors
    val iconKey = ExitNoteRepository.inferIconKeyFromTitle(item.titleSnapshot)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("receipt_item_unconfirmed_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
        border = BorderStroke(1.dp, colors.warning.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colors.warningSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ExitNoteIcons.Pending,
                    contentDescription = "Unconfirmed",
                    tint = colors.warning,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = ExitNoteIcons.forKey(iconKey),
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.titleSnapshot,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primaryText
                )
                Text(
                    text = "This item remains unconfirmed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.warning
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.warningSubtle
            ) {
                Text(
                    text = "Unconfirmed",
                    style = TimestampTextStyle,
                    color = colors.warning,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
