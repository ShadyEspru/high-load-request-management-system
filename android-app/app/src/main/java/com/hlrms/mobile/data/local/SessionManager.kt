package com.hlrms.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(
    name = "hlrms_session"
)

class SessionManager(
    private val context: Context
) {

    companion object {

        private val ACCESS_TOKEN =
            stringPreferencesKey("access_token")

        private val REFRESH_TOKEN =
            stringPreferencesKey("refresh_token")
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String
    ) {

        context.dataStore.edit { preferences ->

            preferences[ACCESS_TOKEN] =
                accessToken

            preferences[REFRESH_TOKEN] =
                refreshToken
        }
    }

    suspend fun updateSession(
        accessToken: String,
        refreshToken: String
    ) {
        context.dataStore.edit { preferences ->

            preferences[ACCESS_TOKEN] =
                accessToken

            preferences[REFRESH_TOKEN] =
                refreshToken
        }
    }

    val accessToken: Flow<String?> =

        context.dataStore.data

            .catch {

                if (it is IOException) {

                    emit(emptyPreferences())

                } else {

                    throw it
                }
            }

            .map {

                it[ACCESS_TOKEN]
            }

    val refreshToken: Flow<String?> =

        context.dataStore.data

            .catch {

                if (it is IOException) {

                    emit(emptyPreferences())

                } else {

                    throw it
                }
            }

            .map {

                it[REFRESH_TOKEN]
            }

    suspend fun clearSession() {

        context.dataStore.edit {

            it.clear()
        }
    }
}