package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SessionWithItems
import com.example.domain.TimeFormatter
import com.example.ui.components.ActiveSessionItemRow
import com.example.ui.components.ExitNoteIcons
import com.example.ui.components.FeedbackBanner
import com.example.ui.theme.ExitNoteTheme
import com.example.ui.theme.ReceiptMetaTextStyle

@Composable
fun ActiveCheckScreen(
    sessionWithItems: SessionWithItems?,
    bannerMessage: String?,
    isErrorBanner: Boolean,
    onDismissBanner: () -> Unit,
    onConfirmItem: (String) -> Unit,
    onUncheckItem: (String) -> Unit,
    onFinishCheck: () -> Unit,
    onDiscardSession: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val colors = ExitNoteTheme.colors
    val haptics = LocalHapticFeedback.current

    if (sessionWithItems == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Loading active check...",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.secondaryText
            )
        }
        return
    }

    val session = sessionWithItems.session
    val sortedItems = sessionWithItems.sortedItems
    val confirmedCount = sessionWithItems.confirmedCount
    val totalCount = sessionWithItems.totalCount
    val progressFraction = if (totalCount > 0) confirmedCount.toFloat() / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 240),
        label = "sessionProgress"
    )
    val startedTimeFormatted = TimeFormatter.formatTime(session.startedAt, session.timezoneId)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("active_check_screen")
    ) {
        // Top App Bar
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
                        .testTag("active_check_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = colors.primaryText
                    )
                }
                Column {
                    Text(
                        text = session.templateNameSnapshot,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primaryText
                    )
                    Text(
                        text = "Started at $startedTimeFormatted",
                        style = ReceiptMetaTextStyle,
                        color = colors.secondaryText
                    )
                }
            }

            TextButton(
                onClick = onDiscardSession,
                modifier = Modifier.testTag("active_check_discard_button")
            ) {
                Text(
                    text = stringResource(R.string.discard_check_button),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.error
                )
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

        // Progress Indicator Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("active_check_progress_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
            border = BorderStroke(1.dp, colors.border),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$confirmedCount of $totalCount items confirmed",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryText,
                        modifier = Modifier.testTag("progress_count_text")
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (confirmedCount == totalCount && totalCount > 0) {
                            colors.successSubtle
                        } else {
                            colors.accentSubtle
                        }
                    ) {
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            style = ReceiptMetaTextStyle,
                            color = if (confirmedCount == totalCount && totalCount > 0) {
                                colors.success
                            } else {
                                colors.primaryAccent
                            },
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (confirmedCount == totalCount && totalCount > 0) {
                        colors.success
                    } else {
                        colors.primaryAccent
                    },
                    trackColor = colors.secondarySurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (confirmedCount == totalCount && totalCount > 0) {
                        "All items confirmed. Tap Finish Check to save your Exit Receipt."
                    } else {
                        "Tap each item after you personally check it."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText
                )
            }
        }

        // Checklist Items List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("active_session_items_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = sortedItems,
                key = { it.id }
            ) { sessionItem ->
                ActiveSessionItemRow(
                    item = sessionItem,
                    onConfirm = {
                        if (!sessionItem.isChecked) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onConfirmItem(sessionItem.id)
                    },
                    onUncheck = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUncheckItem(sessionItem.id)
                    }
                )
            }
        }

        // Bottom Finish Check Action Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.primarySurface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(color = colors.border, modifier = Modifier.padding(bottom = 12.dp))
                Button(
                    onClick = onFinishCheck,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("finish_check_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (confirmedCount == totalCount && totalCount > 0) {
                            colors.success
                        } else {
                            colors.primaryText
                        },
                        contentColor = colors.primarySurface
                    )
                ) {
                    Icon(
                        imageVector = ExitNoteIcons.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.finish_check_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
