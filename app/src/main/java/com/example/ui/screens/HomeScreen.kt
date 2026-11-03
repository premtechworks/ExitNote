package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ChecklistItem
import com.example.data.local.SessionWithItems
import com.example.ui.PrimaryTab
import com.example.ui.components.CompletedSessionSummaryCard
import com.example.ui.components.ExitNoteBrandLogo
import com.example.ui.components.ExitNoteIcons
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.UnfinishedSessionRecoveryCard
import com.example.ui.theme.ExitNoteTheme
import com.example.ui.theme.TimestampTextStyle

@Composable
fun HomeScreen(
    templateName: String,
    checklistItems: List<ChecklistItem>,
    activeSession: SessionWithItems?,
    latestCompletedSession: SessionWithItems?,
    historyCount: Int,
    bannerMessage: String?,
    isErrorBanner: Boolean,
    onDismissBanner: () -> Unit,
    onStartCheck: () -> Unit,
    onResumeCheck: () -> Unit,
    onDiscardCheck: () -> Unit,
    onOpenLatestReceipt: (String) -> Unit,
    onNavigateToTab: (PrimaryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExitNoteTheme.colors
    val enabledItems = checklistItems.filter { it.isEnabled }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. ExitNote Branding Header
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExitNoteBrandLogo(size = 40.dp)
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.primaryText
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryText
                        )
                    }
                }

                IconButton(
                    onClick = { onNavigateToTab(PrimaryTab.SETTINGS) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.primarySurface)
                        .testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = ExitNoteIcons.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        tint = colors.primaryText
                    )
                }
            }
        }

        // Optional feedback banner
        if (bannerMessage != null) {
            item(key = "banner") {
                FeedbackBanner(
                    message = bannerMessage,
                    isError = isErrorBanner,
                    onDismiss = onDismissBanner
                )
            }
        }

        // 2. Unfinished session recovery card (if an IN_PROGRESS session exists)
        if (activeSession != null) {
            item(key = "unfinished_session") {
                UnfinishedSessionRecoveryCard(
                    activeSession = activeSession,
                    onResume = onResumeCheck,
                    onDiscard = onDiscardCheck
                )
            }
        }

        // 3. Prominent Quick Check Card with Start Check CTA
        item(key = "quick_check_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_check_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.accentSubtle
                        ) {
                            Text(
                                text = templateName,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primaryAccent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "${enabledItems.size} active items",
                            style = TimestampTextStyle,
                            color = colors.secondaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.quick_check_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.primaryText
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onStartCheck,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_check_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colors.isDark) colors.primaryAccent else colors.primaryText,
                            contentColor = if (colors.isDark) colors.background else colors.primarySurface
                        )
                    ) {
                        Icon(
                            imageVector = ExitNoteIcons.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.start_check_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 4. Latest Completed Session Receipt Card
        item(key = "latest_receipt_section") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.latest_receipt_header),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryText
                    )
                    if (historyCount > 0) {
                        TextButton(
                            onClick = { onNavigateToTab(PrimaryTab.HISTORY) },
                            modifier = Modifier.testTag("home_view_all_history_button")
                        ) {
                            Text(
                                text = "History ($historyCount)",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.primaryAccent
                            )
                        }
                    }
                }

                if (latestCompletedSession != null) {
                    CompletedSessionSummaryCard(
                        sessionWithItems = latestCompletedSession,
                        onClick = { onOpenLatestReceipt(latestCompletedSession.session.id) },
                        testTagPrefix = "latest_receipt_card"
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_latest_receipt_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                        border = BorderStroke(1.dp, colors.border),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.secondarySurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ExitNoteIcons.Receipt,
                                    contentDescription = null,
                                    tint = colors.secondaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.no_previous_checks_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.no_previous_checks_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.secondaryText
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Current Checklist Preview & Customization Entry Point
        item(key = "checklist_preview_section") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$templateName Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryText
                    )
                    TextButton(
                        onClick = { onNavigateToTab(PrimaryTab.CHECKLIST) },
                        modifier = Modifier.testTag("home_customize_checklist_button")
                    ) {
                        Icon(
                            imageVector = ExitNoteIcons.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colors.primaryAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Customize",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primaryAccent
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            role = Role.Button,
                            onClick = { onNavigateToTab(PrimaryTab.CHECKLIST) }
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                    border = BorderStroke(1.dp, colors.border),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (enabledItems.isEmpty()) {
                            Text(
                                text = "No items are currently enabled. Tap Customize to add or enable departure items.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.warning
                            )
                        } else {
                            enabledItems.take(5).forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.secondarySurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = ExitNoteIcons.forKey(item.icon),
                                            contentDescription = null,
                                            tint = colors.secondaryText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.primaryText,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Ready",
                                        style = TimestampTextStyle,
                                        color = colors.secondaryText
                                    )
                                }
                            }
                            if (enabledItems.size > 5) {
                                Text(
                                    text = "+ ${enabledItems.size - 5} more items in $templateName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.secondaryText,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onNavigateToTab(PrimaryTab.CHECKLIST) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_manage_items_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Icon(
                                    imageVector = ExitNoteIcons.Checklist,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = colors.primaryText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Edit Items",
                                    color = colors.primaryText,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = { onNavigateToTab(PrimaryTab.HISTORY) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_history_entry_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Icon(
                                    imageVector = ExitNoteIcons.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = colors.primaryText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Past Checks",
                                    color = colors.primaryText,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
