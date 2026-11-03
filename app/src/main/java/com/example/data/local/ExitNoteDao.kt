package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExitNoteDao {

    // --- Template Operations ---

    @Query("SELECT * FROM checklist_templates WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultTemplate(): ChecklistTemplate?

    @Query("SELECT * FROM checklist_templates WHERE isDefault = 1 LIMIT 1")
    fun observeDefaultTemplate(): Flow<ChecklistTemplate?>

    @Query("SELECT * FROM checklist_templates WHERE id = :templateId LIMIT 1")
    suspend fun getTemplateById(templateId: String): ChecklistTemplate?

    @Query("SELECT COUNT(*) FROM checklist_templates")
    suspend fun getTemplateCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTemplate(template: ChecklistTemplate)

    @Update
    suspend fun updateTemplate(template: ChecklistTemplate)

    @Query("DELETE FROM checklist_templates WHERE id = :templateId")
    suspend fun deleteTemplateById(templateId: String)

    // --- Checklist Item Operations ---

    @Query("SELECT * FROM checklist_items WHERE templateId = :templateId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeItemsForTemplate(templateId: String): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items WHERE templateId = :templateId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getItemsForTemplate(templateId: String): List<ChecklistItem>

    @Query("SELECT * FROM checklist_items WHERE templateId = :templateId AND isEnabled = 1 ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getEnabledItemsForTemplate(templateId: String): List<ChecklistItem>

    @Query("SELECT * FROM checklist_items WHERE id = :itemId LIMIT 1")
    suspend fun getChecklistItemById(itemId: String): ChecklistItem?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM checklist_items WHERE templateId = :templateId")
    suspend fun getMaxSortOrderForTemplate(templateId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItem>)

    @Update
    suspend fun updateChecklistItem(item: ChecklistItem)

    @Update
    suspend fun updateChecklistItems(items: List<ChecklistItem>)

    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    suspend fun deleteChecklistItemById(itemId: String)

    @Transaction
    suspend fun insertTemplateWithStarterItems(
        template: ChecklistTemplate,
        items: List<ChecklistItem>
    ) {
        insertTemplate(template)
        insertChecklistItems(items)
    }

    // --- Session & Snapshot Operations ---

    @Transaction
    @Query("SELECT * FROM check_sessions WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSessionWithItems(): Flow<SessionWithItems?>

    @Transaction
    @Query("SELECT * FROM check_sessions WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSessionWithItems(): SessionWithItems?

    @Query("SELECT * FROM check_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): CheckSession?

    @Transaction
    @Query("SELECT * FROM check_sessions WHERE id = :sessionId LIMIT 1")
    fun observeSessionWithItems(sessionId: String): Flow<SessionWithItems?>

    @Transaction
    @Query("SELECT * FROM check_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionWithItems(sessionId: String): SessionWithItems?

    @Transaction
    @Query(
        """
        SELECT * FROM check_sessions 
        WHERE status IN ('COMPLETED', 'COMPLETED_WITH_UNCONFIRMED') 
        ORDER BY completedAt DESC, startedAt DESC 
        LIMIT 1
        """
    )
    fun observeLatestCompletedSession(): Flow<SessionWithItems?>

    @Transaction
    @Query(
        """
        SELECT * FROM check_sessions 
        WHERE status IN ('COMPLETED', 'COMPLETED_WITH_UNCONFIRMED') 
        ORDER BY startedAt DESC
        """
    )
    fun observeCompletedHistory(): Flow<List<SessionWithItems>>

    @Transaction
    @Query(
        """
        SELECT * FROM check_sessions 
        WHERE status IN ('COMPLETED', 'COMPLETED_WITH_UNCONFIRMED') 
        ORDER BY startedAt DESC
        """
    )
    suspend fun getCompletedHistory(): List<SessionWithItems>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: CheckSession)

    @Update
    suspend fun updateSession(session: CheckSession)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessionItems(items: List<SessionItem>)

    @Query("SELECT * FROM session_items WHERE id = :sessionItemId LIMIT 1")
    suspend fun getSessionItemById(sessionItemId: String): SessionItem?

    @Query("SELECT * FROM session_items WHERE sessionId = :sessionId ORDER BY sortOrder ASC")
    suspend fun getSessionItemsForSession(sessionId: String): List<SessionItem>

    @Update
    suspend fun updateSessionItem(item: SessionItem)

    @Transaction
    suspend fun createSessionAtomically(
        session: CheckSession,
        snapshotItems: List<SessionItem>
    ) {
        insertSession(session)
        insertSessionItems(snapshotItems)
    }

    @Transaction
    suspend fun confirmSessionItemAtomically(
        sessionItemId: String,
        checkedAtMillis: Long,
        timezoneId: String
    ): SessionItem? {
        val current = getSessionItemById(sessionItemId) ?: return null
        // Rule: Repeated taps on an already confirmed item must not overwrite its original timestamp
        if (current.status == ItemStatus.CHECKED && current.checkedAt != null) {
            return current
        }
        val updated = current.copy(
            status = ItemStatus.CHECKED,
            checkedAt = checkedAtMillis,
            checkedTimezoneId = timezoneId
        )
        updateSessionItem(updated)
        return updated
    }

    @Transaction
    suspend fun uncheckSessionItemAtomically(
        sessionItemId: String
    ): SessionItem? {
        val current = getSessionItemById(sessionItemId) ?: return null
        val updated = current.copy(
            status = ItemStatus.UNCHECKED,
            checkedAt = null,
            checkedTimezoneId = null
        )
        updateSessionItem(updated)
        return updated
    }

    @Transaction
    suspend fun completeSessionAtomically(
        sessionId: String,
        completedAtMillis: Long
    ): CheckSession? {
        val session = getSessionById(sessionId) ?: return null
        if (session.status != SessionStatus.IN_PROGRESS) {
            return session
        }
        val items = getSessionItemsForSession(sessionId)
        val hasUnconfirmed = items.any { it.status != ItemStatus.CHECKED }
        val finalStatus = if (hasUnconfirmed) {
            SessionStatus.COMPLETED_WITH_UNCONFIRMED
        } else {
            SessionStatus.COMPLETED
        }
        val updatedSession = session.copy(
            completedAt = completedAtMillis,
            status = finalStatus
        )
        updateSession(updatedSession)
        return updatedSession
    }

    @Transaction
    suspend fun abandonSessionAtomically(
        sessionId: String,
        abandonedAtMillis: Long
    ): CheckSession? {
        val session = getSessionById(sessionId) ?: return null
        if (session.status != SessionStatus.IN_PROGRESS) {
            return session
        }
        val updatedSession = session.copy(
            completedAt = abandonedAtMillis,
            status = SessionStatus.ABANDONED
        )
        updateSession(updatedSession)
        return updatedSession
    }
}
