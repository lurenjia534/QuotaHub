package com.lurenjia534.quotahub.data.preferences

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiPreferencesRepositoryTest {
    @After
    fun tearDown() {
        clearPreferences()
    }

    @Test
    fun preferencesDefaultToUserFacingRecommendedValues() {
        clearPreferences()

        val preferences = UiPreferencesRepository(context).preferences.value

        assertTrue(preferences.highEmphasisMetrics)
        assertTrue(preferences.hapticConfirmation)
        assertFalse(preferences.landscapeMonitorMode)
        assertEquals(RefreshCadence.Balanced, preferences.refreshCadence)
        assertNull(preferences.dismissedUpdateTag)
    }

    @Test
    fun setLandscapeMonitorMode_updatesStateAndPersistsValue() {
        clearPreferences()
        val repository = UiPreferencesRepository(context)

        repository.setLandscapeMonitorMode(true)

        assertTrue(repository.preferences.value.landscapeMonitorMode)
        assertTrue(UiPreferencesRepository(context).preferences.value.landscapeMonitorMode)

        repository.setLandscapeMonitorMode(false)

        assertFalse(repository.preferences.value.landscapeMonitorMode)
        assertFalse(UiPreferencesRepository(context).preferences.value.landscapeMonitorMode)
    }

    @Test
    fun setLandscapeMonitorMode_doesNotMutateOtherUiPreferences() {
        clearPreferences()
        val repository = UiPreferencesRepository(context)

        repository.setHighEmphasisMetrics(false)
        repository.setHapticConfirmation(false)
        repository.setLandscapeMonitorMode(true)

        val preferences = repository.preferences.value
        assertFalse(preferences.highEmphasisMetrics)
        assertFalse(preferences.hapticConfirmation)
        assertTrue(preferences.landscapeMonitorMode)
    }

    @Test
    fun setDismissedUpdateTag_updatesStateAndPersistsValue() {
        clearPreferences()
        val repository = UiPreferencesRepository(context)

        repository.setDismissedUpdateTag("v1.2")

        assertEquals("v1.2", repository.preferences.value.dismissedUpdateTag)
        assertEquals("v1.2", UiPreferencesRepository(context).preferences.value.dismissedUpdateTag)
    }

    @Test
    fun setRefreshCadence_updatesStateAndPersistsValue() {
        clearPreferences()
        val repository = UiPreferencesRepository(context)

        repository.setRefreshCadence(RefreshCadence.Live)

        assertEquals(RefreshCadence.Live, repository.preferences.value.refreshCadence)
        assertEquals(RefreshCadence.Live, UiPreferencesRepository(context).preferences.value.refreshCadence)

        repository.setRefreshCadence(RefreshCadence.Manual)

        assertEquals(RefreshCadence.Manual, repository.preferences.value.refreshCadence)
        assertEquals(RefreshCadence.Manual, UiPreferencesRepository(context).preferences.value.refreshCadence)
    }

    private fun clearPreferences() {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private companion object {
        const val PREFERENCES_NAME = "quotahub_ui_preferences"

        val context: Context
            get() = InstrumentationRegistry.getInstrumentation().targetContext
    }
}
