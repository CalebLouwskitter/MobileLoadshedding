package com.prog7314.IdleLoadsheddingMobile.auth

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val AUTH_DATASTORE_NAME = "auth_preferences"

val Context.authPreferencesDataStore by preferencesDataStore(name = AUTH_DATASTORE_NAME)

class AuthPreferences(private val context: Context) {

    private object Keys {
        val rememberMe: Preferences.Key<Boolean> = booleanPreferencesKey("remember_me")
        val rememberedEmail: Preferences.Key<String> = stringPreferencesKey("remembered_email")
    }

    val rememberMe: Flow<Boolean> = context.authPreferencesDataStore.data.map { preferences ->
        preferences[Keys.rememberMe] ?: false
    }

    val rememberedEmail: Flow<String> = context.authPreferencesDataStore.data.map { preferences ->
        preferences[Keys.rememberedEmail] ?: ""
    }

    suspend fun setRememberMe(value: Boolean) {
        context.authPreferencesDataStore.edit { preferences ->
            preferences[Keys.rememberMe] = value
            if (!value) {
                preferences.remove(Keys.rememberedEmail)
            }
        }
    }

    suspend fun setRememberedEmail(email: String) {
        context.authPreferencesDataStore.edit { preferences ->
            if (email.isNotBlank()) {
                preferences[Keys.rememberedEmail] = email
            } else {
                preferences.remove(Keys.rememberedEmail)
            }
        }
    }

    suspend fun clearRememberedEmail() {
        context.authPreferencesDataStore.edit { preferences ->
            preferences.remove(Keys.rememberedEmail)
        }
    }
}
