package com.notzeetaa.yakt

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.content.Context

val Context.dataStore by preferencesDataStore(name = "user_preferences")

class DataStoreManager(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val SELECTED_MODE = intPreferencesKey("selected_mode")  // Integer mode
        val APPLY_AT_BOOT = booleanPreferencesKey("apply_at_boot")  // Boolean for apply at boot
        val ONEPLUS = booleanPreferencesKey("oneplus")  // Boolean for oneplus
        val FRAMERATE = booleanPreferencesKey("framerate")  // Boolean for framerate
    }

    // Ensure this is returning an Int
    val selectedModeFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SELECTED_MODE] ?: 0 // Default to Battery Mode (0)
    }

    val applyAtBootFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[APPLY_AT_BOOT] ?: false
    }

    val onePlusFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ONEPLUS] ?: false
    }

    val framerateFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[FRAMERATE] ?: false
    }

    suspend fun saveSelectedMode(mode: Int) {
        dataStore.edit { preferences ->
            preferences[SELECTED_MODE] = mode
        }
    }

    suspend fun saveApplyAtBoot(isChecked: Boolean) {
        dataStore.edit { preferences ->
            preferences[APPLY_AT_BOOT] = isChecked
        }
    }

    suspend fun saveOnePlusBoot(isChecked: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONEPLUS] = isChecked
        }
    }

    suspend fun saveframerateBoot(isChecked: Boolean) {
        dataStore.edit { preferences ->
            preferences[FRAMERATE] = isChecked
        }
    }
}
