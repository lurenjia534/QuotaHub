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
        assertEquals(ThemeColorSource.System, preferences.themeColorSource)
        assertEquals(ThemePalette.QuotaHub, preferences.themePalette)
        assertFalse(preferences.backgroundRefreshEnabled)
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
    fun setThemeColorSource_updatesStateAndPersistsValue() {
        clearPreferences()
        val repository = UiPreferencesRepository(context)

        repository.setThemeColorSource(ThemeColorSource.AppPalette)

        assertEquals(ThemeColorSource.AppPalette, repository.preferences.value.themeColorSource)
        assertEquals(ThemeColorSource.AppPalette, UiPreferencesRepository(context).preferences.value.themeColorSource)

        repository.setThemeColorSource(ThemeColorSource.System)

        assertEquals(ThemeColorSource.System, repository.preferences.value.themeColorSource)
        assertEquals(ThemeColorSource.System, UiPreferencesRepository(context).preferences.value.themeColorSource)
    }

    @Test
    fun setThemePalette_updatesStateAndPersistsValue() {
        clearPreferences()
        val repository = UiPreferencesRepository(context)

        repository.setThemePalette(ThemePalette.Ember)

        assertEquals(ThemePalette.Ember, repository.preferences.value.themePalette)
        assertEquals(ThemePalette.Ember, UiPreferencesRepository(context).preferences.value.themePalette)

        repository.setThemePalette(ThemePalette.Graphite)

        assertEquals(ThemePalette.Graphite, repository.preferences.value.themePalette)
        assertEquals(ThemePalette.Graphite, UiPreferencesRepository(context).preferences.value.themePalette)
    }

    @Test
    fun setBackgroundRefreshEnabled_updatesStateAndPersistsValue() {
        clearPreferences()
        val repository = UiPreferencesRepository(context)

        repository.setBackgroundRefreshEnabled(true)

        assertTrue(repository.preferences.value.backgroundRefreshEnabled)
        assertTrue(UiPreferencesRepository(context).preferences.value.backgroundRefreshEnabled)

        repository.setBackgroundRefreshEnabled(false)

        assertFalse(repository.preferences.value.backgroundRefreshEnabled)
        assertFalse(UiPreferencesRepository(context).preferences.value.backgroundRefreshEnabled)
    }

    @Test
    fun setBackgroundRefreshEnabled_doesNotMutateForegroundRefreshCadence() {
        clearPreferences()
        val repository = UiPreferencesRepository(context)

        repository.setRefreshCadence(RefreshCadence.Live)
        repository.setBackgroundRefreshEnabled(true)

        val preferences = repository.preferences.value
        assertEquals(RefreshCadence.Live, preferences.refreshCadence)
        assertTrue(preferences.backgroundRefreshEnabled)
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
