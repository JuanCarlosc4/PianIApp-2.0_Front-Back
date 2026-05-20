package com.piania.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

class SessionManager(private val context: Context) {

    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        // Premium eliminado
        // Chat "último visto" (local). Clave por clase: chat_last_seen_message_id_{classId}
        private fun chatLastSeenKey(classId: Long) =
            longPreferencesKey("chat_last_seen_message_id_$classId")

        // Anuncios: ID máximo visto (local). Sirve para determinar "hay nuevos anuncios"
        private val ANNOUNCEMENTS_LAST_SEEN_ID_KEY = longPreferencesKey("announcements_last_seen_id")
    }

    // --- GUARDADO ---

    /**
     * Guarda únicamente el token de sesión
     */
    suspend fun saveUserSession(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    // --- LECTURA ---

    fun fetchAuthToken(): String? {
        return runBlocking {
            context.dataStore.data.map { it[AUTH_TOKEN_KEY] }.firstOrNull()
        }
    }

    val authToken: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[AUTH_TOKEN_KEY] }

    // Premium eliminado

    // --- CHAT: ÚLTIMO VISTO (LOCAL) ---
    /**
     * Guarda el último messageId que el usuario ha "visto" en un chat/clase.
     * Se guarda en DataStore para persistir entre sesiones.
     */
    suspend fun setChatLastSeenMessageId(classId: Long, messageId: Long) {
        context.dataStore.edit { preferences ->
            preferences[chatLastSeenKey(classId)] = messageId
        }
    }

    /**
     * Devuelve el último messageId visto en un chat/clase (o null si nunca entró).
     */
    fun getChatLastSeenMessageId(classId: Long): Long? {
        return runBlocking {
            context.dataStore.data.map { it[chatLastSeenKey(classId)] }.firstOrNull()
        }
    }

    // --- ANUNCIOS: ÚLTIMO VISTO (LOCAL) ---
    /**
     * Guarda el id más alto de anuncio que el usuario ha marcado como "visto".
     */
    suspend fun setAnnouncementsLastSeenId(id: Long) {
        context.dataStore.edit { preferences ->
            preferences[ANNOUNCEMENTS_LAST_SEEN_ID_KEY] = id
        }
    }

    /**
     * Devuelve el id más alto de anuncio visto (o 0 si nunca vio ninguno).
     */
    fun getAnnouncementsLastSeenId(): Long {
        return runBlocking {
            context.dataStore.data.map { it[ANNOUNCEMENTS_LAST_SEEN_ID_KEY] ?: 0L }.firstOrNull()
                ?: 0L
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear() // Borra TODO para evitar mezclar datos de usuarios
        }
    }
}
