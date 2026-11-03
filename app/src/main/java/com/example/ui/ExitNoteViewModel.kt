package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChecklistItem
import com.example.data.local.ChecklistTemplate
import com.example.data.local.SessionWithItems
import com.example.data.local.ThemeMode
import com.example.data.local.ThemePreferences
import com.example.data.repository.ExitNoteRepository
import com.example.data.repository.ItemMutationResult
import com.example.data.repository.StartSessionResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PrimaryTab(val route: String) {
    HOME("home"),
    HISTORY("history"),
    CHECKLIST("checklist"),
    SETTINGS("settings")
}

sealed class ScreenDestination {
    data class MainTab(val tab: PrimaryTab) : ScreenDestination()
    data class ActiveCheck(val sessionId: String) : ScreenDestination()
    data class ExitReceipt(val sessionId: String, val returnTab: PrimaryTab = PrimaryTab.HOME) : ScreenDestination()
}

data class TransientUiState(
    val isInitialized: Boolean = false,
    val currentDestination: ScreenDestination = ScreenDestination.MainTab(PrimaryTab.HOME),
    val immediateSessionSnapshot: SessionWithItems? = null,
    val showUnfinishedSessionPrompt: Boolean = false,
    val showDiscardConfirmationDialog: Boolean = false,
    val sessionIdPendingDiscard: String? = null,
    val showIncompleteFinishDialog: Boolean = false,
    val bannerMessage: String? = null,
    val isErrorBanner: Boolean = false,
    val itemDialogError: String? = null
)

data class ExitNoteUiState(
    val isInitialized: Boolean = false,
    val currentDestination: ScreenDestination = ScreenDestination.MainTab(PrimaryTab.HOME),
    val defaultTemplate: ChecklistTemplate? = null,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val activeSession: SessionWithItems? = null,
    val latestCompletedSession: SessionWithItems? = null,
    val completedHistory: List<SessionWithItems> = emptyList(),
    val inspectedSession: SessionWithItems? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showUnfinishedSessionPrompt: Boolean = false,
    val showDiscardConfirmationDialog: Boolean = false,
    val showIncompleteFinishDialog: Boolean = false,
    val bannerMessage: String? = null,
    val isErrorBanner: Boolean = false,
    val itemDialogError: String? = null
) {
    val enabledChecklistItems: List<ChecklistItem>
        get() = checklistItems.filter { it.isEnabled }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExitNoteViewModel(
    private val repository: ExitNoteRepository,
    private val themePreferences: ThemePreferences? = null
) : ViewModel() {

    private val transientState = MutableStateFlow(TransientUiState())
    private val inspectedSessionId = MutableStateFlow<String?>(null)

    private val templateAndItemsFlow = repository.defaultTemplateFlow.flatMapLatest { template ->
        if (template == null) {
            flowOf(null to emptyList())
        } else {
            repository.observeChecklistItems(template.id).combine(flowOf(template)) { items, tmpl ->
                tmpl to items
            }
        }
    }

    private val inspectedSessionFlow = inspectedSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) {
            flowOf(null)
        } else {
            repository.observeSessionWithItems(sessionId)
        }
    }

    private val themeFlow = themePreferences?.themeModeFlow ?: flowOf(ThemeMode.SYSTEM)

    val uiState: StateFlow<ExitNoteUiState> = combine(
        transientState,
        templateAndItemsFlow,
        repository.activeSessionFlow,
        repository.latestCompletedSessionFlow,
        combine(repository.completedHistoryFlow, inspectedSessionFlow, themeFlow) { history, inspected, theme ->
            Triple(history, inspected, theme)
        }
    ) { transient, (template, items), activeSessionFromFlow, latestCompleted, (history, inspectedFromFlow, themeMode) ->
        val resolvedActive = activeSessionFromFlow
            ?: transient.immediateSessionSnapshot?.takeIf {
                it.session.status == com.example.data.local.SessionStatus.IN_PROGRESS
            }
        val resolvedInspected = inspectedFromFlow ?: transient.immediateSessionSnapshot

        ExitNoteUiState(
            isInitialized = transient.isInitialized,
            currentDestination = transient.currentDestination,
            defaultTemplate = template,
            checklistItems = items,
            activeSession = resolvedActive,
            latestCompletedSession = latestCompleted,
            completedHistory = history,
            inspectedSession = resolvedInspected,
            themeMode = themeMode,
            showUnfinishedSessionPrompt = transient.showUnfinishedSessionPrompt && resolvedActive != null,
            showDiscardConfirmationDialog = transient.showDiscardConfirmationDialog,
            showIncompleteFinishDialog = transient.showIncompleteFinishDialog,
            bannerMessage = transient.bannerMessage,
            isErrorBanner = transient.isErrorBanner,
            itemDialogError = transient.itemDialogError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ExitNoteUiState()
    )

    init {
        viewModelScope.launch {
            try {
                repository.ensureDefaultChecklistInitialized()
                val existingActive = repository.getActiveSessionOnce()
                transientState.update {
                    it.copy(
                        isInitialized = true,
                        immediateSessionSnapshot = existingActive,
                        showUnfinishedSessionPrompt = existingActive != null
                    )
                }
            } catch (e: Exception) {
                transientState.update {
                    it.copy(
                        isInitialized = true,
                        bannerMessage = e.message ?: "Database initialization failed.",
                        isErrorBanner = true
                    )
                }
            }
        }
    }

    fun selectTab(tab: PrimaryTab) {
        transientState.update {
            it.copy(
                currentDestination = ScreenDestination.MainTab(tab),
                bannerMessage = null
            )
        }
    }

    fun navigateHome() {
        transientState.update {
            it.copy(
                currentDestination = ScreenDestination.MainTab(PrimaryTab.HOME),
                showIncompleteFinishDialog = false,
                bannerMessage = null
            )
        }
    }

    fun dismissBanner() {
        transientState.update { it.copy(bannerMessage = null, isErrorBanner = false) }
    }

    fun clearItemDialogError() {
        transientState.update { it.copy(itemDialogError = null) }
    }

    fun dismissUnfinishedSessionPrompt() {
        transientState.update { it.copy(showUnfinishedSessionPrompt = false) }
    }

    fun onStartCheckRequested() {
        viewModelScope.launch {
            val currentActive = repository.getActiveSessionOnce()
            if (currentActive != null) {
                transientState.update {
                    it.copy(
                        immediateSessionSnapshot = currentActive,
                        showUnfinishedSessionPrompt = true
                    )
                }
                return@launch
            }

            when (val result = repository.startNewSession()) {
                is StartSessionResult.Created -> {
                    val createdSnapshot = repository.getSessionWithItemsOnce(result.sessionId)
                    inspectedSessionId.value = result.sessionId
                    transientState.update {
                        it.copy(
                            currentDestination = ScreenDestination.ActiveCheck(result.sessionId),
                            immediateSessionSnapshot = createdSnapshot,
                            showUnfinishedSessionPrompt = false,
                            bannerMessage = null
                        )
                    }
                }
                is StartSessionResult.ExistingUnfinishedSession -> {
                    val existingSnapshot = repository.getSessionWithItemsOnce(result.activeSessionId)
                    transientState.update {
                        it.copy(
                            immediateSessionSnapshot = existingSnapshot,
                            showUnfinishedSessionPrompt = true
                        )
                    }
                }
                is StartSessionResult.NoEnabledItems -> {
                    transientState.update {
                        it.copy(
                            bannerMessage = "Enable or add at least one checklist item before starting a check.",
                            isErrorBanner = true
                        )
                    }
                }
                is StartSessionResult.Error -> {
                    transientState.update {
                        it.copy(
                            bannerMessage = result.message,
                            isErrorBanner = true
                        )
                    }
                }
            }
        }
    }

    fun onResumeUnfinishedSession() {
        val activeSession = uiState.value.activeSession ?: return
        inspectedSessionId.value = activeSession.session.id
        transientState.update {
            it.copy(
                currentDestination = ScreenDestination.ActiveCheck(activeSession.session.id),
                immediateSessionSnapshot = activeSession,
                showUnfinishedSessionPrompt = false,
                bannerMessage = null
            )
        }
    }

    fun onRequestDiscardSession(sessionId: String? = null) {
        val targetId = sessionId ?: uiState.value.activeSession?.session?.id ?: return
        transientState.update {
            it.copy(
                showDiscardConfirmationDialog = true,
                sessionIdPendingDiscard = targetId
            )
        }
    }

    fun onCancelDiscardSession() {
        transientState.update {
            it.copy(
                showDiscardConfirmationDialog = false,
                sessionIdPendingDiscard = null
            )
        }
    }

    fun onConfirmDiscardSession() {
        val targetId = transientState.value.sessionIdPendingDiscard
            ?: uiState.value.activeSession?.session?.id
            ?: return

        viewModelScope.launch {
            val result = repository.abandonSession(targetId)
            result.fold(
                onSuccess = {
                    transientState.update { state ->
                        val nextDestination = if (state.currentDestination is ScreenDestination.ActiveCheck) {
                            ScreenDestination.MainTab(PrimaryTab.HOME)
                        } else {
                            state.currentDestination
                        }
                        state.copy(
                            currentDestination = nextDestination,
                            immediateSessionSnapshot = null,
                            showUnfinishedSessionPrompt = false,
                            showDiscardConfirmationDialog = false,
                            sessionIdPendingDiscard = null,
                            bannerMessage = "Unfinished check was discarded.",
                            isErrorBanner = false
                        )
                    }
                },
                onFailure = { error ->
                    transientState.update {
                        it.copy(
                            showDiscardConfirmationDialog = false,
                            sessionIdPendingDiscard = null,
                            bannerMessage = error.message ?: "Failed to discard session.",
                            isErrorBanner = true
                        )
                    }
                }
            )
        }
    }

    fun onConfirmItem(sessionItemId: String) {
        viewModelScope.launch {
            val result = repository.confirmSessionItem(sessionItemId)
            result.fold(
                onSuccess = { updatedItem ->
                    val refreshed = repository.getSessionWithItemsOnce(updatedItem.sessionId)
                    if (refreshed != null) {
                        transientState.update { it.copy(immediateSessionSnapshot = refreshed) }
                    }
                },
                onFailure = { error ->
                    transientState.update {
                        it.copy(
                            bannerMessage = error.message ?: "Failed to save confirmation.",
                            isErrorBanner = true
                        )
                    }
                }
            )
        }
    }

    fun onUncheckItem(sessionItemId: String) {
        viewModelScope.launch {
            val result = repository.uncheckSessionItem(sessionItemId)
            result.fold(
                onSuccess = { updatedItem ->
                    val refreshed = repository.getSessionWithItemsOnce(updatedItem.sessionId)
                    if (refreshed != null) {
                        transientState.update { it.copy(immediateSessionSnapshot = refreshed) }
                    }
                },
                onFailure = { error ->
                    transientState.update {
                        it.copy(
                            bannerMessage = error.message ?: "Failed to update item state.",
                            isErrorBanner = true
                        )
                    }
                }
            )
        }
    }

    fun onRequestFinishCheck() {
        viewModelScope.launch {
            val activeId = uiState.value.activeSession?.session?.id
                ?: repository.getActiveSessionOnce()?.session?.id
                ?: return@launch
            val freshSession = repository.getSessionWithItemsOnce(activeId) ?: return@launch
            if (freshSession.unconfirmedCount > 0) {
                transientState.update {
                    it.copy(
                        immediateSessionSnapshot = freshSession,
                        showIncompleteFinishDialog = true
                    )
                }
            } else {
                onConfirmFinishCheck()
            }
        }
    }

    fun onDismissIncompleteFinishDialog() {
        transientState.update {
            it.copy(showIncompleteFinishDialog = false)
        }
    }

    fun onConfirmFinishCheck() {
        val sessionId = uiState.value.activeSession?.session?.id ?: return
        viewModelScope.launch {
            val result = repository.finishSession(sessionId)
            result.fold(
                onSuccess = { completedSession ->
                    val completedSnapshot = repository.getSessionWithItemsOnce(completedSession.id)
                    inspectedSessionId.value = completedSession.id
                    transientState.update {
                        it.copy(
                            currentDestination = ScreenDestination.ExitReceipt(
                                sessionId = completedSession.id,
                                returnTab = PrimaryTab.HOME
                            ),
                            immediateSessionSnapshot = completedSnapshot,
                            showIncompleteFinishDialog = false,
                            showUnfinishedSessionPrompt = false,
                            bannerMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    transientState.update {
                        it.copy(
                            showIncompleteFinishDialog = false,
                            bannerMessage = error.message ?: "Failed to complete session.",
                            isErrorBanner = true
                        )
                    }
                }
            )
        }
    }

    fun onOpenReceipt(sessionId: String, fromTab: PrimaryTab = PrimaryTab.HOME) {
        viewModelScope.launch {
            val snapshot = repository.getSessionWithItemsOnce(sessionId)
            inspectedSessionId.value = sessionId
            transientState.update {
                it.copy(
                    currentDestination = ScreenDestination.ExitReceipt(
                        sessionId = sessionId,
                        returnTab = fromTab
                    ),
                    immediateSessionSnapshot = snapshot,
                    bannerMessage = null
                )
            }
        }
    }

    fun onAddChecklistItem(title: String, iconKey: String?, onSuccess: () -> Unit) {
        val templateId = uiState.value.defaultTemplate?.id ?: return
        viewModelScope.launch {
            when (val result = repository.addChecklistItem(templateId, title, iconKey)) {
                is ItemMutationResult.Success -> {
                    transientState.update { it.copy(itemDialogError = null) }
                    onSuccess()
                }
                is ItemMutationResult.ValidationError -> {
                    transientState.update { it.copy(itemDialogError = result.message) }
                }
                is ItemMutationResult.DatabaseError -> {
                    transientState.update { it.copy(itemDialogError = result.message) }
                }
            }
        }
    }

    fun onUpdateChecklistItem(
        itemId: String,
        newTitle: String,
        newIconKey: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            when (val result = repository.updateChecklistItem(itemId, newTitle, newIconKey)) {
                is ItemMutationResult.Success -> {
                    transientState.update { it.copy(itemDialogError = null) }
                    onSuccess()
                }
                is ItemMutationResult.ValidationError -> {
                    transientState.update { it.copy(itemDialogError = result.message) }
                }
                is ItemMutationResult.DatabaseError -> {
                    transientState.update { it.copy(itemDialogError = result.message) }
                }
            }
        }
    }

    fun onToggleChecklistItemEnabled(itemId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val result = repository.setChecklistItemEnabled(itemId, isEnabled)
            if (result is ItemMutationResult.DatabaseError) {
                transientState.update {
                    it.copy(bannerMessage = result.message, isErrorBanner = true)
                }
            }
        }
    }

    fun onDeleteChecklistItem(itemId: String) {
        viewModelScope.launch {
            val result = repository.deleteChecklistItem(itemId)
            if (result is ItemMutationResult.DatabaseError) {
                transientState.update {
                    it.copy(bannerMessage = result.message, isErrorBanner = true)
                }
            }
        }
    }

    fun onReorderChecklistItem(itemId: String, moveUp: Boolean) {
        viewModelScope.launch {
            val result = repository.reorderChecklistItem(itemId, moveUp)
            if (result is ItemMutationResult.DatabaseError) {
                transientState.update {
                    it.copy(bannerMessage = result.message, isErrorBanner = true)
                }
            }
        }
    }

    fun onRestoreStarterItems() {
        viewModelScope.launch {
            when (val result = repository.restoreStarterChecklistItems()) {
                is ItemMutationResult.Success -> {
                    transientState.update {
                        it.copy(
                            bannerMessage = "Default starter items restored for future checks.",
                            isErrorBanner = false
                        )
                    }
                }
                is ItemMutationResult.ValidationError -> {
                    transientState.update {
                        it.copy(bannerMessage = result.message, isErrorBanner = true)
                    }
                }
                is ItemMutationResult.DatabaseError -> {
                    transientState.update {
                        it.copy(bannerMessage = result.message, isErrorBanner = true)
                    }
                }
            }
        }
    }

    fun onSetThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences?.setThemeMode(mode)
        }
    }

    class Factory(
        private val repository: ExitNoteRepository,
        private val themePreferences: ThemePreferences? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExitNoteViewModel(repository, themePreferences) as T
        }
    }
}
