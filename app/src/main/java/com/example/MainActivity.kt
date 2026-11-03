package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ExitNoteDatabase
import com.example.data.local.ThemeMode
import com.example.data.local.ThemePreferences
import com.example.data.repository.ExitNoteRepository
import com.example.ui.ExitNoteUiState
import com.example.ui.ExitNoteViewModel
import com.example.ui.PrimaryTab
import com.example.ui.ScreenDestination
import com.example.ui.components.DiscardSessionConfirmationDialog
import com.example.ui.components.ExitNoteIcons
import com.example.ui.components.IncompleteSessionConfirmationDialog
import com.example.ui.components.UnfinishedSessionPromptDialog
import com.example.ui.screens.ActiveCheckScreen
import com.example.ui.screens.ChecklistManagementScreen
import com.example.ui.screens.ExitReceiptScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ExitNoteAppTheme
import com.example.ui.theme.ExitNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appContext = LocalContext.current.applicationContext
            val database = remember { ExitNoteDatabase.getInstance(appContext) }
            val repository = remember { ExitNoteRepository(database.exitNoteDao()) }
            val themePrefs = remember { ThemePreferences(appContext) }
            val viewModel: ExitNoteViewModel = viewModel(
                factory = ExitNoteViewModel.Factory(repository, themePrefs)
            )
            ExitNoteRoot(viewModel = viewModel)
        }
    }
}

@Composable
fun ExitNoteRoot(viewModel: ExitNoteViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    ExitNoteAppTheme(darkTheme = useDarkTheme) {
        ExitNoteContent(
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@Composable
fun ExitNoteContent(
    uiState: ExitNoteUiState,
    viewModel: ExitNoteViewModel
) {
    val colors = ExitNoteTheme.colors
    val destination = uiState.currentDestination

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (destination is ScreenDestination.MainTab) {
                ExitNoteBottomBar(
                    selectedTab = destination.tab,
                    onSelectTab = viewModel::selectTab
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
            ) {
                when (destination) {
                    is ScreenDestination.MainTab -> {
                        when (destination.tab) {
                            PrimaryTab.HOME -> {
                                HomeScreen(
                                    templateName = uiState.defaultTemplate?.name
                                        ?: ExitNoteRepository.DEFAULT_TEMPLATE_NAME,
                                    checklistItems = uiState.checklistItems,
                                    activeSession = uiState.activeSession,
                                    latestCompletedSession = uiState.latestCompletedSession,
                                    historyCount = uiState.completedHistory.size,
                                    bannerMessage = uiState.bannerMessage,
                                    isErrorBanner = uiState.isErrorBanner,
                                    onDismissBanner = viewModel::dismissBanner,
                                    onStartCheck = viewModel::onStartCheckRequested,
                                    onResumeCheck = viewModel::onResumeUnfinishedSession,
                                    onDiscardCheck = {
                                        viewModel.onRequestDiscardSession(uiState.activeSession?.session?.id)
                                    },
                                    onOpenLatestReceipt = { sessionId ->
                                        viewModel.onOpenReceipt(sessionId, PrimaryTab.HOME)
                                    },
                                    onNavigateToTab = viewModel::selectTab
                                )
                            }
                            PrimaryTab.HISTORY -> {
                                HistoryScreen(
                                    completedSessions = uiState.completedHistory,
                                    onOpenReceipt = { sessionId ->
                                        viewModel.onOpenReceipt(sessionId, PrimaryTab.HISTORY)
                                    },
                                    onStartFirstCheck = viewModel::onStartCheckRequested,
                                    onNavigateHome = viewModel::navigateHome
                                )
                            }
                            PrimaryTab.CHECKLIST -> {
                                ChecklistManagementScreen(
                                    templateName = uiState.defaultTemplate?.name
                                        ?: ExitNoteRepository.DEFAULT_TEMPLATE_NAME,
                                    items = uiState.checklistItems,
                                    dialogError = uiState.itemDialogError,
                                    bannerMessage = uiState.bannerMessage,
                                    isErrorBanner = uiState.isErrorBanner,
                                    onDismissBanner = viewModel::dismissBanner,
                                    onClearDialogError = viewModel::clearItemDialogError,
                                    onAddItem = viewModel::onAddChecklistItem,
                                    onEditItem = viewModel::onUpdateChecklistItem,
                                    onToggleItemEnabled = viewModel::onToggleChecklistItemEnabled,
                                    onDeleteItem = viewModel::onDeleteChecklistItem,
                                    onReorderItem = viewModel::onReorderChecklistItem,
                                    onRestoreStarterItems = viewModel::onRestoreStarterItems,
                                    onNavigateHome = viewModel::navigateHome
                                )
                            }
                            PrimaryTab.SETTINGS -> {
                                SettingsScreen(
                                    themeMode = uiState.themeMode,
                                    bannerMessage = uiState.bannerMessage,
                                    isErrorBanner = uiState.isErrorBanner,
                                    onDismissBanner = viewModel::dismissBanner,
                                    onSelectThemeMode = viewModel::onSetThemeMode,
                                    onRestoreStarterItems = viewModel::onRestoreStarterItems,
                                    onNavigateHome = viewModel::navigateHome
                                )
                            }
                        }
                    }

                    is ScreenDestination.ActiveCheck -> {
                        val sessionData = if (uiState.activeSession?.session?.id == destination.sessionId) {
                            uiState.activeSession
                        } else {
                            uiState.inspectedSession ?: uiState.activeSession
                        }
                        ActiveCheckScreen(
                            sessionWithItems = sessionData,
                            bannerMessage = uiState.bannerMessage,
                            isErrorBanner = uiState.isErrorBanner,
                            onDismissBanner = viewModel::dismissBanner,
                            onConfirmItem = viewModel::onConfirmItem,
                            onUncheckItem = viewModel::onUncheckItem,
                            onFinishCheck = viewModel::onRequestFinishCheck,
                            onDiscardSession = {
                                viewModel.onRequestDiscardSession(destination.sessionId)
                            },
                            onNavigateBack = viewModel::navigateHome
                        )
                    }

                    is ScreenDestination.ExitReceipt -> {
                        val receiptSession = when {
                            uiState.inspectedSession?.session?.id == destination.sessionId -> uiState.inspectedSession
                            uiState.latestCompletedSession?.session?.id == destination.sessionId -> uiState.latestCompletedSession
                            else -> uiState.completedHistory.firstOrNull { it.session.id == destination.sessionId }
                        }
                        ExitReceiptScreen(
                            sessionWithItems = receiptSession,
                            onNavigateBack = {
                                viewModel.selectTab(destination.returnTab)
                            }
                        )
                    }
                }
            }
        }
    }

    // Unfinished session prompt (when attempting Start Check while an active session exists)
    if (uiState.showUnfinishedSessionPrompt && uiState.activeSession != null && destination is ScreenDestination.MainTab) {
        UnfinishedSessionPromptDialog(
            activeSession = uiState.activeSession,
            onResume = viewModel::onResumeUnfinishedSession,
            onDiscard = {
                viewModel.dismissUnfinishedSessionPrompt()
                viewModel.onRequestDiscardSession(uiState.activeSession.session.id)
            },
            onDismiss = viewModel::dismissUnfinishedSessionPrompt
        )
    }

    // Incomplete items confirmation dialog before finishing a session
    if (uiState.showIncompleteFinishDialog && uiState.activeSession != null) {
        IncompleteSessionConfirmationDialog(
            unconfirmedCount = uiState.activeSession.unconfirmedCount,
            totalCount = uiState.activeSession.totalCount,
            onConfirmFinish = viewModel::onConfirmFinishCheck,
            onReturnToCheck = viewModel::onDismissIncompleteFinishDialog
        )
    }

    // Discard unfinished session confirmation dialog
    if (uiState.showDiscardConfirmationDialog) {
        DiscardSessionConfirmationDialog(
            onConfirmDiscard = viewModel::onConfirmDiscardSession,
            onCancel = viewModel::onCancelDiscardSession
        )
    }
}

@Composable
private fun ExitNoteBottomBar(
    selectedTab: PrimaryTab,
    onSelectTab: (PrimaryTab) -> Unit
) {
    val colors = ExitNoteTheme.colors

    NavigationBar(
        containerColor = colors.primarySurface,
        tonalElevation = 0.dp
    ) {
        val navItems = listOf(
            Triple(PrimaryTab.HOME, stringResource(R.string.nav_home), ExitNoteIcons.Home),
            Triple(PrimaryTab.HISTORY, stringResource(R.string.nav_history), ExitNoteIcons.History),
            Triple(PrimaryTab.CHECKLIST, stringResource(R.string.nav_checklist), ExitNoteIcons.Checklist),
            Triple(PrimaryTab.SETTINGS, stringResource(R.string.nav_settings), ExitNoteIcons.Settings)
        )

        navItems.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.testTag("bottom_nav_${tab.route}"),
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(text = label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primaryAccent,
                    selectedTextColor = colors.primaryAccent,
                    indicatorColor = colors.accentSubtle,
                    unselectedIconColor = colors.secondaryText,
                    unselectedTextColor = colors.secondaryText
                )
            )
        }
    }
}
