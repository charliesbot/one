package com.charliesbot.shared.core.services

import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.notifications.NotificationScheduler
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class FastingEventManagerTest {

    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var callbacks: FastingEventCallbacks
    private lateinit var eventManager: FastingEventManager

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0

        notificationScheduler = mockk(relaxed = true)
        callbacks = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    single { notificationScheduler }
                },
            )
        }

        eventManager = FastingEventManager()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `false to true schedules notifications and calls onFastingStarted`() = runTest {
        val previous = FastingDataItem(isFasting = false)
        val current = FastingDataItem(
            isFasting = true,
            startTimeInMillis = 1000L,
            fastingGoalId = "16:8",
        )

        eventManager.processStateChange(previous, current, callbacks)

        coVerify { notificationScheduler.scheduleNotifications(1000L, "16:8") }
        coVerify { callbacks.onFastingStarted(current) }
        coVerify(exactly = 0) { notificationScheduler.cancelAllNotifications() }
    }

    @Test
    fun `true to false cancels notifications and calls onFastingCompleted`() = runTest {
        val previous = FastingDataItem(isFasting = true)
        val current = FastingDataItem(isFasting = false)

        eventManager.processStateChange(previous, current, callbacks)

        coVerify { notificationScheduler.cancelAllNotifications() }
        coVerify { callbacks.onFastingCompleted(current) }
        coVerify(exactly = 0) { notificationScheduler.scheduleNotifications(any(), any()) }
    }

    @Test
    fun `true to true reschedules notifications and calls onFastingUpdated`() = runTest {
        val previous = FastingDataItem(isFasting = true)
        val current = FastingDataItem(
            isFasting = true,
            startTimeInMillis = 2000L,
            fastingGoalId = "18:6",
        )

        eventManager.processStateChange(previous, current, callbacks)

        coVerify { notificationScheduler.cancelAllNotifications() }
        coVerify { notificationScheduler.scheduleNotifications(2000L, "18:6") }
        coVerify { callbacks.onFastingUpdated(current) }
    }

    @Test
    fun `false to false only calls onFastingUpdated with no notification side effects`() = runTest {
        val previous = FastingDataItem(isFasting = false)
        val current = FastingDataItem(isFasting = false)

        eventManager.processStateChange(previous, current, callbacks)

        coVerify { callbacks.onFastingUpdated(current) }
        coVerify(exactly = 0) { notificationScheduler.scheduleNotifications(any(), any()) }
        coVerify(exactly = 0) { notificationScheduler.cancelAllNotifications() }
    }

    @Test
    fun `null previousItem treated as not fasting triggers start path`() = runTest {
        val current = FastingDataItem(
            isFasting = true,
            startTimeInMillis = 3000L,
            fastingGoalId = "circadian",
        )

        eventManager.processStateChange(null, current, callbacks)

        coVerify { notificationScheduler.scheduleNotifications(3000L, "circadian") }
        coVerify { callbacks.onFastingStarted(current) }
    }

    @Test
    fun `null previousItem treated as not fasting triggers inactive update`() = runTest {
        val current = FastingDataItem(isFasting = false)

        eventManager.processStateChange(null, current, callbacks)

        coVerify { callbacks.onFastingUpdated(current) }
        coVerify(exactly = 0) { notificationScheduler.scheduleNotifications(any(), any()) }
        coVerify(exactly = 0) { notificationScheduler.cancelAllNotifications() }
    }

    @Test
    fun `uses currentItem values for notification scheduling not previousItem`() = runTest {
        val previous = FastingDataItem(
            isFasting = false,
            startTimeInMillis = 999L,
            fastingGoalId = "old-goal",
        )
        val current = FastingDataItem(
            isFasting = true,
            startTimeInMillis = 5000L,
            fastingGoalId = "20:4",
        )

        eventManager.processStateChange(previous, current, callbacks)

        coVerify { notificationScheduler.scheduleNotifications(5000L, "20:4") }
        coVerify(exactly = 0) { notificationScheduler.scheduleNotifications(999L, any()) }
        coVerify(exactly = 0) { notificationScheduler.scheduleNotifications(any(), "old-goal") }
    }
}
