package com.example

import com.example.data.local.CheckSession
import com.example.data.local.ItemStatus
import com.example.data.local.SessionItem
import com.example.data.local.SessionStatus
import com.example.data.local.SessionWithItems
import com.example.data.repository.ExitNoteRepository
import com.example.domain.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ExampleUnitTest {

    @Test
    fun `SessionWithItems computes confirmed and unconfirmed counts and sort order accurately`() {
        val session = CheckSession(
            id = "session-1",
            templateId = "template-1",
            templateNameSnapshot = "Leaving Home",
            startedAt = 1_700_000_000_000L,
            completedAt = null,
            timezoneId = "UTC",
            status = SessionStatus.IN_PROGRESS
        )
        val items = listOf(
            SessionItem(
                id = "item-2",
                sessionId = "session-1",
                sourceItemId = "src-2",
                titleSnapshot = "Stove switched off",
                sortOrder = 1,
                status = ItemStatus.UNCHECKED,
                checkedAt = null,
                checkedTimezoneId = null
            ),
            SessionItem(
                id = "item-1",
                sessionId = "session-1",
                sourceItemId = "src-1",
                titleSnapshot = "Front door locked",
                sortOrder = 0,
                status = ItemStatus.CHECKED,
                checkedAt = 1_700_000_005_000L,
                checkedTimezoneId = "UTC"
            )
        )

        val sessionWithItems = SessionWithItems(session = session, items = items)
        assertEquals(2, sessionWithItems.totalCount)
        assertEquals(1, sessionWithItems.confirmedCount)
        assertEquals(1, sessionWithItems.unconfirmedCount)
        assertEquals("Front door locked", sessionWithItems.sortedItems[0].titleSnapshot)
        assertEquals("Stove switched off", sessionWithItems.sortedItems[1].titleSnapshot)
        assertTrue(sessionWithItems.sortedItems[0].isChecked)
        assertFalse(sessionWithItems.sortedItems[1].isChecked)
    }

    @Test
    fun `TimeFormatter preserves historical timezone independently of device changes`() {
        // 2026-11-03 14:30:00 UTC
        val epochMillis = 1_793_716_200_000L
        val utcFormatted = TimeFormatter.formatTime(epochMillis, "UTC", Locale.US)
        val tokyoFormatted = TimeFormatter.formatTime(epochMillis, "Asia/Tokyo", Locale.US)

        assertEquals("2:30 PM", utcFormatted)
        assertEquals("11:30 PM", tokyoFormatted)
    }

    @Test
    fun `inferIconKeyFromTitle maps common departure phrases to expected icons`() {
        assertEquals("door_lock", ExitNoteRepository.inferIconKeyFromTitle("Front door locked"))
        assertEquals("back_door", ExitNoteRepository.inferIconKeyFromTitle("Back door locked"))
        assertEquals("stove", ExitNoteRepository.inferIconKeyFromTitle("Gas stove switched off"))
        assertEquals("oven", ExitNoteRepository.inferIconKeyFromTitle("Oven switched off"))
        assertEquals("windows", ExitNoteRepository.inferIconKeyFromTitle("Bedroom windows closed"))
        assertEquals("lights", ExitNoteRepository.inferIconKeyFromTitle("Lights and fans switched off"))
        assertEquals("keys", ExitNoteRepository.inferIconKeyFromTitle("House keys collected"))
        assertEquals("wallet", ExitNoteRepository.inferIconKeyFromTitle("Wallet collected"))
        assertEquals("phone", ExitNoteRepository.inferIconKeyFromTitle("Phone collected"))
    }
}
