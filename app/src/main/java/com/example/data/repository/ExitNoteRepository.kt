package com.example.data.repository

import com.example.data.local.CheckSession
import com.example.data.local.ChecklistItem
import com.example.data.local.ChecklistTemplate
import com.example.data.local.ExitNoteDao
import com.example.data.local.ItemStatus
import com.example.data.local.SessionItem
import com.example.data.local.SessionStatus
import com.example.data.local.SessionWithItems
import com.example.domain.ClockProvider
import com.example.domain.SystemClockProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

sealed class StartSessionResult {
    data class Created(val sessionId: String) : StartSessionResult()
    data class ExistingUnfinishedSession(val activeSessionId: String) : StartSessionResult()
    data object NoEnabledItems : StartSessionResult()
    data class Error(val message: String) : StartSessionResult()
}

sealed class ItemMutationResult {
    data object Success : ItemMutationResult()
    data class ValidationError(val message: String) : ItemMutationResult()
    data class DatabaseError(val message: String) : ItemMutationResult()
}

class ExitNoteRepository(
    private val dao: ExitNoteDao,
    private val clock: ClockProvider = SystemClockProvider()
) {
    private val initMutex = Mutex()
    private val sessionMutex = Mutex()

    companion object {
        const val DEFAULT_TEMPLATE_NAME = "Leaving Home"

        val STARTER_ITEMS: List<Pair<String, String>> = listOf(
            "Front door locked" to "door_lock",
            "Back door locked" to "back_door",
            "Stove switched off" to "stove",
            "Oven switched off" to "oven",
            "Windows closed" to "windows",
            "Lights and fans switched off" to "lights",
            "Keys collected" to "keys",
            "Wallet collected" to "wallet",
            "Phone collected" to "phone"
        )

        fun inferIconKeyFromTitle(title: String, explicitIcon: String? = null): String {
            if (!explicitIcon.isNullOrBlank()) return explicitIcon
            val lower = title.lowercase()
            return when {
                lower.contains("back door") || lower.contains("patio") || lower.contains("balcony") -> "back_door"
                lower.contains("door") || lower.contains("lock") || lower.contains("gate") -> "door_lock"
                lower.contains("stove") || lower.contains("gas") || lower.contains("burner") -> "stove"
                lower.contains("oven") || lower.contains("toaster") || lower.contains("coffee") || lower.contains("appliance") -> "oven"
                lower.contains("window") || lower.contains("blind") -> "windows"
                lower.contains("light") || lower.contains("fan") || lower.contains("ac") || lower.contains("heater") -> "lights"
                lower.contains("key") -> "keys"
                lower.contains("wallet") || lower.contains("card") || lower.contains("id") || lower.contains("badge") -> "wallet"
                lower.contains("phone") || lower.contains("charger") || lower.contains("laptop") -> "phone"
                else -> "check"
            }
        }
    }

    val defaultTemplateFlow: Flow<ChecklistTemplate?> = dao.observeDefaultTemplate()
    val activeSessionFlow: Flow<SessionWithItems?> = dao.observeActiveSessionWithItems()
    val latestCompletedSessionFlow: Flow<SessionWithItems?> = dao.observeLatestCompletedSession()
    val completedHistoryFlow: Flow<List<SessionWithItems>> = dao.observeCompletedHistory()

    fun observeChecklistItems(templateId: String): Flow<List<ChecklistItem>> {
        return dao.observeItemsForTemplate(templateId)
    }

    fun observeSessionWithItems(sessionId: String): Flow<SessionWithItems?> {
        return dao.observeSessionWithItems(sessionId)
    }

    /**
     * Ensures the default "Leaving Home" checklist is created once on first launch.
     * Safe to invoke multiple times; never creates duplicate default templates.
     */
    suspend fun ensureDefaultChecklistInitialized(): ChecklistTemplate {
        return initMutex.withLock {
            val existing = dao.getDefaultTemplate()
            if (existing != null) {
                return@withLock existing
            }
            val now = clock.nowMillis()
            val templateId = UUID.randomUUID().toString()
            val defaultTemplate = ChecklistTemplate(
                id = templateId,
                name = DEFAULT_TEMPLATE_NAME,
                isDefault = true,
                createdAt = now,
                updatedAt = now
            )
            val starterEntities = STARTER_ITEMS.mapIndexed { index, (title, iconKey) ->
                ChecklistItem(
                    id = UUID.randomUUID().toString(),
                    templateId = templateId,
                    title = title,
                    icon = iconKey,
                    sortOrder = index,
                    isEnabled = true,
                    createdAt = now + index
                )
            }
            dao.insertTemplateWithStarterItems(defaultTemplate, starterEntities)
            defaultTemplate
        }
    }

    /**
     * Restores missing starter items to the default template without deleting custom user items
     * or altering historical session records.
     */
    suspend fun restoreStarterChecklistItems(): ItemMutationResult {
        return try {
            val template = ensureDefaultChecklistInitialized()
            val currentItems = dao.getItemsForTemplate(template.id)
            val existingTitlesLower = currentItems.map { it.title.trim().lowercase() }.toSet()
            var nextSortOrder = (currentItems.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val now = clock.nowMillis()

            val missingStarterItems = STARTER_ITEMS.mapNotNull { (title, iconKey) ->
                if (title.lowercase() in existingTitlesLower) {
                    null
                } else {
                    ChecklistItem(
                        id = UUID.randomUUID().toString(),
                        templateId = template.id,
                        title = title,
                        icon = iconKey,
                        sortOrder = nextSortOrder++,
                        isEnabled = true,
                        createdAt = now
                    )
                }
            }

            if (missingStarterItems.isNotEmpty()) {
                dao.insertChecklistItems(missingStarterItems)
                dao.updateTemplate(template.copy(updatedAt = now))
            }
            ItemMutationResult.Success
        } catch (e: Exception) {
            ItemMutationResult.DatabaseError(e.message ?: "Failed to restore starter items.")
        }
    }

    suspend fun addChecklistItem(
        templateId: String,
        rawTitle: String,
        iconKey: String? = null
    ): ItemMutationResult {
        val cleanTitle = rawTitle.trim()
        if (cleanTitle.isEmpty()) {
            return ItemMutationResult.ValidationError("Please enter a non-empty item title.")
        }
        return try {
            val existingItems = dao.getItemsForTemplate(templateId)
            val isDuplicate = existingItems.any { it.title.equals(cleanTitle, ignoreCase = true) }
            if (isDuplicate) {
                return ItemMutationResult.ValidationError("An item with this title already exists.")
            }
            val now = clock.nowMillis()
            val nextSortOrder = (existingItems.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val resolvedIcon = inferIconKeyFromTitle(cleanTitle, iconKey)
            val newItem = ChecklistItem(
                id = UUID.randomUUID().toString(),
                templateId = templateId,
                title = cleanTitle,
                icon = resolvedIcon,
                sortOrder = nextSortOrder,
                isEnabled = true,
                createdAt = now
            )
            dao.insertChecklistItem(newItem)
            dao.getTemplateById(templateId)?.let { template ->
                dao.updateTemplate(template.copy(updatedAt = now))
            }
            ItemMutationResult.Success
        } catch (e: Exception) {
            ItemMutationResult.DatabaseError(e.message ?: "Could not save checklist item.")
        }
    }

    suspend fun updateChecklistItem(
        itemId: String,
        newRawTitle: String,
        newIconKey: String? = null
    ): ItemMutationResult {
        val cleanTitle = newRawTitle.trim()
        if (cleanTitle.isEmpty()) {
            return ItemMutationResult.ValidationError("Please enter a non-empty item title.")
        }
        return try {
            val currentItem = dao.getChecklistItemById(itemId)
                ?: return ItemMutationResult.ValidationError("Checklist item no longer exists.")
            val siblings = dao.getItemsForTemplate(currentItem.templateId)
            val duplicateSibling = siblings.any {
                it.id != itemId && it.title.equals(cleanTitle, ignoreCase = true)
            }
            if (duplicateSibling) {
                return ItemMutationResult.ValidationError("Another item already uses this title.")
            }
            val now = clock.nowMillis()
            val updated = currentItem.copy(
                title = cleanTitle,
                icon = newIconKey ?: currentItem.icon ?: inferIconKeyFromTitle(cleanTitle)
            )
            dao.updateChecklistItem(updated)
            dao.getTemplateById(currentItem.templateId)?.let { template ->
                dao.updateTemplate(template.copy(updatedAt = now))
            }
            ItemMutationResult.Success
        } catch (e: Exception) {
            ItemMutationResult.DatabaseError(e.message ?: "Could not update checklist item.")
        }
    }

    suspend fun setChecklistItemEnabled(itemId: String, isEnabled: Boolean): ItemMutationResult {
        return try {
            val currentItem = dao.getChecklistItemById(itemId)
                ?: return ItemMutationResult.ValidationError("Checklist item not found.")
            val now = clock.nowMillis()
            dao.updateChecklistItem(currentItem.copy(isEnabled = isEnabled))
            dao.getTemplateById(currentItem.templateId)?.let { template ->
                dao.updateTemplate(template.copy(updatedAt = now))
            }
            ItemMutationResult.Success
        } catch (e: Exception) {
            ItemMutationResult.DatabaseError(e.message ?: "Could not update item state.")
        }
    }

    suspend fun deleteChecklistItem(itemId: String): ItemMutationResult {
        return try {
            val currentItem = dao.getChecklistItemById(itemId) ?: return ItemMutationResult.Success
            dao.deleteChecklistItemById(itemId)
            // Normalize remaining sort orders
            val remaining = dao.getItemsForTemplate(currentItem.templateId)
                .mapIndexed { index, item -> item.copy(sortOrder = index) }
            if (remaining.isNotEmpty()) {
                dao.updateChecklistItems(remaining)
            }
            dao.getTemplateById(currentItem.templateId)?.let { template ->
                dao.updateTemplate(template.copy(updatedAt = clock.nowMillis()))
            }
            ItemMutationResult.Success
        } catch (e: Exception) {
            ItemMutationResult.DatabaseError(e.message ?: "Could not delete checklist item.")
        }
    }

    suspend fun reorderChecklistItem(itemId: String, moveUp: Boolean): ItemMutationResult {
        return try {
            val currentItem = dao.getChecklistItemById(itemId)
                ?: return ItemMutationResult.ValidationError("Item not found.")
            val items = dao.getItemsForTemplate(currentItem.templateId).toMutableList()
            val currentIndex = items.indexOfFirst { it.id == itemId }
            if (currentIndex == -1) return ItemMutationResult.Success

            val targetIndex = if (moveUp) currentIndex - 1 else currentIndex + 1
            if (targetIndex !in items.indices) return ItemMutationResult.Success

            val swapped = items[targetIndex]
            items[targetIndex] = currentItem
            items[currentIndex] = swapped

            val normalized = items.mapIndexed { index, item ->
                item.copy(sortOrder = index)
            }
            dao.updateChecklistItems(normalized)
            dao.getTemplateById(currentItem.templateId)?.let { template ->
                dao.updateTemplate(template.copy(updatedAt = clock.nowMillis()))
            }
            ItemMutationResult.Success
        } catch (e: Exception) {
            ItemMutationResult.DatabaseError(e.message ?: "Could not reorder checklist items.")
        }
    }

    /**
     * Starts a new departure check session if no unfinished session exists.
     * Snapshots the template name and all enabled checklist items atomically.
     */
    suspend fun startNewSession(templateId: String? = null): StartSessionResult {
        return sessionMutex.withLock {
            try {
                val activeSession = dao.getActiveSessionWithItems()
                if (activeSession != null) {
                    return@withLock StartSessionResult.ExistingUnfinishedSession(activeSession.session.id)
                }

                val template = if (templateId != null) {
                    dao.getTemplateById(templateId) ?: ensureDefaultChecklistInitialized()
                } else {
                    ensureDefaultChecklistInitialized()
                }

                val enabledItems = dao.getEnabledItemsForTemplate(template.id)
                if (enabledItems.isEmpty()) {
                    return@withLock StartSessionResult.NoEnabledItems
                }

                val now = clock.nowMillis()
                val zoneId = clock.currentZoneId()
                val sessionId = UUID.randomUUID().toString()

                val newSession = CheckSession(
                    id = sessionId,
                    templateId = template.id,
                    templateNameSnapshot = template.name,
                    startedAt = now,
                    completedAt = null,
                    timezoneId = zoneId,
                    status = SessionStatus.IN_PROGRESS
                )

                val sessionItems = enabledItems.mapIndexed { index, templateItem ->
                    SessionItem(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        sourceItemId = templateItem.id,
                        titleSnapshot = templateItem.title,
                        sortOrder = index,
                        status = ItemStatus.UNCHECKED,
                        checkedAt = null,
                        checkedTimezoneId = null
                    )
                }

                dao.createSessionAtomically(newSession, sessionItems)
                StartSessionResult.Created(sessionId)
            } catch (e: Exception) {
                StartSessionResult.Error(e.message ?: "Unable to start check session.")
            }
        }
    }

    /**
     * Confirms a session item and persists its timestamp immediately.
     * Repeatedly confirming an already checked item preserves its original timestamp.
     */
    suspend fun confirmSessionItem(sessionItemId: String): Result<SessionItem> {
        return try {
            val now = clock.nowMillis()
            val zoneId = clock.currentZoneId()
            val updated = dao.confirmSessionItemAtomically(
                sessionItemId = sessionItemId,
                checkedAtMillis = now,
                timezoneId = zoneId
            ) ?: return Result.failure(IllegalArgumentException("Session item not found."))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unchecks a session item and clears its confirmation timestamp.
     */
    suspend fun uncheckSessionItem(sessionItemId: String): Result<SessionItem> {
        return try {
            val updated = dao.uncheckSessionItemAtomically(sessionItemId)
                ?: return Result.failure(IllegalArgumentException("Session item not found."))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Completes an active session and computes COMPLETED vs COMPLETED_WITH_UNCONFIRMED.
     * Never automatically marks unchecked items as completed.
     */
    suspend fun finishSession(sessionId: String): Result<CheckSession> {
        return try {
            val now = clock.nowMillis()
            val completedSession = dao.completeSessionAtomically(
                sessionId = sessionId,
                completedAtMillis = now
            ) ?: return Result.failure(IllegalArgumentException("Session not found."))
            Result.success(completedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marks an unfinished session as ABANDONED when the user explicitly discards it.
     */
    suspend fun abandonSession(sessionId: String): Result<CheckSession> {
        return try {
            val now = clock.nowMillis()
            val abandonedSession = dao.abandonSessionAtomically(
                sessionId = sessionId,
                abandonedAtMillis = now
            ) ?: return Result.failure(IllegalArgumentException("Session not found."))
            Result.success(abandonedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSessionWithItemsOnce(sessionId: String): SessionWithItems? {
        return dao.getSessionWithItems(sessionId)
    }

    suspend fun getActiveSessionOnce(): SessionWithItems? {
        return dao.getActiveSessionWithItems()
    }
}
