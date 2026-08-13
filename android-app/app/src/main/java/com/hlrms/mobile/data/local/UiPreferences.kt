package com.hlrms.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.uiDataStore by preferencesDataStore(
    name = "hlrms_ui_preferences"
)

class UiPreferences(
    private val context: Context
) {

    companion object {

        private val DARK_MODE =
            booleanPreferencesKey(
                "dark_mode"
            )
    }

    val darkMode: Flow<Boolean?> =
        context
            .uiDataStore
            .data
            .catch { exception ->

                if (exception is IOException) {

                    emit(
                        emptyPreferences()
                    )

                } else {

                    throw exception
                }
            }
            .map { preferences ->

                preferences[DARK_MODE]
            }

    suspend fun setDarkMode(
        enabled: Boolean
    ) {

        context
            .uiDataStore
            .edit { preferences ->

                preferences[DARK_MODE] =
                    enabled
            }
    }
}