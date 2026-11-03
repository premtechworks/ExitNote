package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ExitNoteDao
import com.example.data.local.ExitNoteDatabase
import com.example.data.local.ItemStatus
import com.example.data.local.SessionStatus
import com.example.data.repository.ExitNoteRepository
import com.example.data.repository.ItemMutationResult
import com.example.data.repository.StartSessionResult
import com.example.domain.ClockProvider
import com.example.ui.ExitNoteViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeClock(
    var currentMillis: Long = 1_793_700_000_000L,
    var zoneId: String = "America/New_York"
) : ClockProvider {
    override fun nowMillis(): Long = currentMillis
    override fun currentZoneId(): String = zoneId

    fun advanceSeconds(seconds: Long) {
        currentMillis += seconds * 1_000L
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: ExitNoteDatabase
    private lateinit var dao: ExitNoteDao
    private lateinit var fakeClock: FakeClock
    private lateinit var repository: ExitNoteRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ExitNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.exitNoteDao()
        fakeClock = FakeClock()
        repository = ExitNoteRepository(dao, fakeClock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `1 and 2 - first launch creates Leaving Home default checklist once and reopening does not duplicate`() = runBlocking {
        val appName = context.getString(R.string.app_name)
        assertEquals("ExitNote", appName)

        val firstTemplate = repository.ensureDefaultChecklistInitialized()
        val secondTemplate = repository.ensureDefaultChecklistInitialized()

        assertEquals(firstTemplate.id, secondTemplate.id)
        assertEquals("Leaving Home", firstTemplate.name)
        assertEquals(1, dao.getTemplateCount())

        val items = dao.getItemsForTemplate(firstTemplate.id)
        assertEquals(9, items.size)
        assertEquals("Front door locked", items.first().title)
        assertEquals("Phone collected", items.last().title)
    }

    @Test
    fun `3 to 7 - session snapshot creation and timestamp rules on confirm, repeat tap, uncheck, and recheck`() = runBlocking {
        val template = repository.ensureDefaultChecklistInitialized()
        val allItems = dao.getItemsForTemplate(template.id)

        // Disable 2 items so only 7 are enabled
        repository.setChecklistItemEnabled(allItems[7].id, false)
        repository.setChecklistItemEnabled(allItems[8].id, false)

        val startResult = repository.startNewSession(template.id)
        assertTrue(startResult is StartSessionResult.Created)
        val sessionId = (startResult as StartSessionResult.Created).sessionId

        val sessionWithItems = repository.getSessionWithItemsOnce(sessionId)!!
        assertEquals(7, sessionWithItems.totalCount)
        assertEquals(0, sessionWithItems.confirmedCount)

        val firstItem = sessionWithItems.sortedItems.first()
        assertEquals(ItemStatus.UNCHECKED, firstItem.status)
        assertNull(firstItem.checkedAt)

        // 4. Confirm item saves timestamp
        fakeClock.currentMillis = 1_793_700_010_000L
        val confirmedOnce = repository.confirmSessionItem(firstItem.id).getOrThrow()
        assertEquals(ItemStatus.CHECKED, confirmedOnce.status)
        assertEquals(1_793_700_010_000L, confirmedOnce.checkedAt)
        assertEquals("America/New_York", confirmedOnce.checkedTimezoneId)

        // 5. Repeated confirmation does NOT overwrite original timestamp
        fakeClock.advanceSeconds(45)
        val confirmedTwice = repository.confirmSessionItem(firstItem.id).getOrThrow()
        assertEquals(ItemStatus.CHECKED, confirmedTwice.status)
        assertEquals(1_793_700_010_000L, confirmedTwice.checkedAt)

        // 6. Unchecking clears the timestamp
        fakeClock.advanceSeconds(15)
        val unchecked = repository.uncheckSessionItem(firstItem.id).getOrThrow()
        assertEquals(ItemStatus.UNCHECKED, unchecked.status)
        assertNull(unchecked.checkedAt)
        assertNull(unchecked.checkedTimezoneId)

        // 7. Rechecking creates a new timestamp
        fakeClock.currentMillis = 1_793_700_099_000L
        val rechecked = repository.confirmSessionItem(firstItem.id).getOrThrow()
        assertEquals(ItemStatus.CHECKED, rechecked.status)
        assertEquals(1_793_700_099_000L, rechecked.checkedAt)
    }

    @Test
    fun `8 to 10 - finishing complete vs incomplete session sets accurate status and preserves unchecked items`() = runBlocking {
        repository.ensureDefaultChecklistInitialized()

        // Complete all items in session 1
        val start1 = repository.startNewSession() as StartSessionResult.Created
        val session1Items = repository.getSessionWithItemsOnce(start1.sessionId)!!.sortedItems
        session1Items.forEach { item ->
            fakeClock.advanceSeconds(2)
            repository.confirmSessionItem(item.id)
        }
        fakeClock.advanceSeconds(5)
        val finished1 = repository.finishSession(start1.sessionId).getOrThrow()
        assertEquals(SessionStatus.COMPLETED, finished1.status)
        assertNotNull(finished1.completedAt)

        // Complete only 2 items in session 2
        fakeClock.advanceSeconds(60)
        val start2 = repository.startNewSession() as StartSessionResult.Created
        val session2Items = repository.getSessionWithItemsOnce(start2.sessionId)!!.sortedItems
        repository.confirmSessionItem(session2Items[0].id)
        repository.confirmSessionItem(session2Items[1].id)

        fakeClock.advanceSeconds(10)
        val finished2 = repository.finishSession(start2.sessionId).getOrThrow()
        assertEquals(SessionStatus.COMPLETED_WITH_UNCONFIRMED, finished2.status)

        // Verify unchecked items remain explicitly UNCHECKED
        val savedSession2 = repository.getSessionWithItemsOnce(start2.sessionId)!!
        assertEquals(2, savedSession2.confirmedCount)
        assertEquals(7, savedSession2.unconfirmedCount)
        savedSession2.sortedItems.drop(2).forEach { unconfirmedItem ->
            assertEquals(ItemStatus.UNCHECKED, unconfirmedItem.status)
            assertNull(unconfirmedItem.checkedAt)
        }
    }

    @Test
    fun `11 to 15 - unfinished session recovery, duplicate prevention, and historical snapshot immutability after template edits or deletion`() = runBlocking {
        val template = repository.ensureDefaultChecklistInitialized()
        val startResult = repository.startNewSession(template.id) as StartSessionResult.Created

        // Attempting to start another session while one is IN_PROGRESS returns ExistingUnfinishedSession
        val duplicateAttempt = repository.startNewSession(template.id)
        assertTrue(duplicateAttempt is StartSessionResult.ExistingUnfinishedSession)
        assertEquals(
            startResult.sessionId,
            (duplicateAttempt as StartSessionResult.ExistingUnfinishedSession).activeSessionId
        )

        // Simulate app restart with a fresh Repository instance reading from the same Room DB
        val restartedRepo = ExitNoteRepository(dao, fakeClock)
        val recoveredActive = restartedRepo.getActiveSessionOnce()
        assertNotNull(recoveredActive)
        assertEquals(startResult.sessionId, recoveredActive!!.session.id)

        // Confirm first item in Tokyo timezone and finish session
        fakeClock.zoneId = "Asia/Tokyo"
        fakeClock.currentMillis = 1_793_750_000_000L
        restartedRepo.confirmSessionItem(recoveredActive.sortedItems[0].id)
        restartedRepo.finishSession(startResult.sessionId)

        // Now modify the template: rename first item, delete second item, add new item, then delete template
        val templateItems = dao.getItemsForTemplate(template.id)
        val renameResult = restartedRepo.updateChecklistItem(templateItems[0].id, "Renamed Smart Lock", "door_lock")
        assertTrue(renameResult is ItemMutationResult.Success)
        restartedRepo.deleteChecklistItem(templateItems[1].id)
        restartedRepo.addChecklistItem(template.id, "Brand New Item", "check")

        // Delete the template itself to verify SET_NULL foreign key policy preserves historical session
        dao.deleteTemplateById(template.id)

        // Verify the historical receipt is 100% unchanged
        val historicalReceipt = restartedRepo.getSessionWithItemsOnce(startResult.sessionId)!!
        assertEquals("Leaving Home", historicalReceipt.session.templateNameSnapshot)
        assertNull(historicalReceipt.session.templateId)
        assertEquals(9, historicalReceipt.totalCount)
        assertEquals("Front door locked", historicalReceipt.sortedItems[0].titleSnapshot)
        assertEquals("Back door locked", historicalReceipt.sortedItems[1].titleSnapshot)
        assertEquals("Asia/Tokyo", historicalReceipt.sortedItems[0].checkedTimezoneId)
    }

    @Test
    fun `Compose UI CUJ - Home screen renders ExitNote, starts check, confirms item, and generates Exit Receipt`() {
        val viewModel = ExitNoteViewModel(repository, null)

        composeTestRule.setContent {
            ExitNoteRoot(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Verify Home Screen branding & Start Check button
        composeTestRule.onNodeWithText("ExitNote").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_check_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_check_button").performClick()

        // Wait for Room session creation transaction to complete and transition to Active Check
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            viewModel.uiState.value.activeSession != null
        }
        composeTestRule.waitForIdle()

        // Verify Active Check screen is displayed
        composeTestRule.onNodeWithTag("active_check_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Front door locked").assertIsDisplayed()

        // Confirm "Front door locked"
        composeTestRule.onNodeWithText("Front door locked").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            viewModel.uiState.value.activeSession?.confirmedCount == 1
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("1 of 9 items confirmed").assertIsDisplayed()

        // Tap Finish Check -> Incomplete items dialog appears
        composeTestRule.onNodeWithTag("finish_check_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            viewModel.uiState.value.showIncompleteFinishDialog
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Unconfirmed items remain").assertIsDisplayed()

        // Confirm Finish Anyway -> navigates to Exit Receipt
        composeTestRule.onNodeWithTag("confirm_incomplete_finish_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            viewModel.uiState.value.currentDestination is com.example.ui.ScreenDestination.ExitReceipt
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exit_receipt_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("receipt_summary_header").assertIsDisplayed()
    }
}
