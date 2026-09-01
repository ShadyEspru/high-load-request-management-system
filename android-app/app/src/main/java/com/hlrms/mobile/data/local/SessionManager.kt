package com.hlrms.mobile.data.local

import android.content.Context
import androidx.datastore.core.DataStore
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
    context: Context
) {

    private val dataStore: DataStore<Preferences> =
        context.applicationContext.dataStore

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

        dataStore.edit { preferences ->

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
        dataStore.edit { preferences ->

            preferences[ACCESS_TOKEN] =
                accessToken

            preferences[REFRESH_TOKEN] =
                refreshToken
        }
    }

    val accessToken: Flow<String?> =

        dataStore.data

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

        dataStore.data

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

        dataStore.edit {

            it.clear()
        }
    }
}