package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

object SessionStatus {
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val COMPLETED_WITH_UNCONFIRMED = "COMPLETED_WITH_UNCONFIRMED"
    const val ABANDONED = "ABANDONED"
}

object ItemStatus {
    const val UNCHECKED = "UNCHECKED"
    const val CHECKED = "CHECKED"
}

@Entity(tableName = "checklist_templates")
data class ChecklistTemplate(
    @PrimaryKey
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["templateId", "sortOrder"])
    ]
)
data class ChecklistItem(
    @PrimaryKey
    val id: String,
    val templateId: String,
    val title: String,
    val icon: String?,
    val sortOrder: Int,
    val isEnabled: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "check_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["status"]),
        Index(value = ["startedAt"]),
        Index(value = ["templateId"])
    ]
)
data class CheckSession(
    @PrimaryKey
    val id: String,
    val templateId: String?,
    val templateNameSnapshot: String,
    val startedAt: Long,
    val completedAt: Long?,
    val timezoneId: String,
    val status: String
)

@Entity(
    tableName = "session_items",
    foreignKeys = [
        ForeignKey(
            entity = CheckSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId", "sortOrder"])
    ]
)
data class SessionItem(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val sourceItemId: String?,
    val titleSnapshot: String,
    val sortOrder: Int,
    val status: String,
    val checkedAt: Long?,
    val checkedTimezoneId: String?
) {
    val isChecked: Boolean
        get() = status == ItemStatus.CHECKED
}

data class SessionWithItems(
    @Embedded val session: CheckSession,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val items: List<SessionItem>
) {
    val sortedItems: List<SessionItem>
        get() = items.sortedBy { it.sortOrder }

    val confirmedCount: Int
        get() = items.count { it.status == ItemStatus.CHECKED }

    val unconfirmedCount: Int
        get() = items.count { it.status != ItemStatus.CHECKED }

    val totalCount: Int
        get() = items.size
}
