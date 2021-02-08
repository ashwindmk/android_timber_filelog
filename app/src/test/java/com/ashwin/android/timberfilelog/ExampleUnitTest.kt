package com.ashwin.android.timberfilelog

import android.content.Context
import io.mockk.*
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
    val context = mockk<Context>()
    val view: MainContract.View = mockk()
    val analyticsManager: AnalyticsManager = mockk()

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTrackEvent() {
        val event = "event_1"

        every { view.getContext() } returns context

        mockkStatic(AnalyticsManager::class)
        mockkObject(AnalyticsManager)
        every { AnalyticsManager.getInstance(any()) } returns analyticsManager
        every { analyticsManager.logEvent(any()) } returns Unit

        val presenter = MainPresenter(view)

        presenter.trackEvent(event)

        verify(exactly = 1) { analyticsManager.logEvent(event) }
    }
}