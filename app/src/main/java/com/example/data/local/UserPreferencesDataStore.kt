package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hirafi_prefs")

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val FULL_NAME = stringPreferencesKey("full_name")
        val PHONE = stringPreferencesKey("phone")
        val CITY = stringPreferencesKey("city")
        val ROLE = stringPreferencesKey("role")
        val SPECIALTY_ID = stringPreferencesKey("craftsman_specialty_id")
        val SPECIALTY_LOCKED = booleanPreferencesKey("craftsman_specialty_locked")
        val IS_ONLINE = booleanPreferencesKey("craftsman_is_online")
        val SUPABASE_EMAIL = stringPreferencesKey("supabase_email")
        val SUPABASE_ACCESS_TOKEN = stringPreferencesKey("supabase_access_token")
    }

    val userSessionFlow: Flow<UserSession> = context.dataStore.data.map { prefs ->
        val roleDb = prefs[Keys.ROLE] ?: UserRole.CLIENT.dbValue
        UserSession(
            userId = prefs[Keys.USER_ID] ?: "local_user",
            fullName = prefs[Keys.FULL_NAME] ?: "يونس العلمي",
            phone = prefs[Keys.PHONE] ?: "+212 612-345678",
            city = prefs[Keys.CITY] ?: "الدار البيضاء",
            role = if (roleDb == UserRole.CRAFTSMAN.dbValue) UserRole.CRAFTSMAN else UserRole.CLIENT,
            craftsmanSpecialtyId = prefs[Keys.SPECIALTY_ID] ?: "plumbing",
            isCraftsmanSpecialtyLocked = prefs[Keys.SPECIALTY_LOCKED] ?: false,
            isCraftsmanOnline = prefs[Keys.IS_ONLINE] ?: true,
            supabaseEmail = prefs[Keys.SUPABASE_EMAIL],
            supabaseAccessToken = prefs[Keys.SUPABASE_ACCESS_TOKEN]
        )
    }

    suspend fun setRole(role: UserRole) {
        context.dataStore.edit { it[Keys.ROLE] = role.dbValue }
    }

    suspend fun setCraftsmanOnline(isOnline: Boolean) {
        context.dataStore.edit { it[Keys.IS_ONLINE] = isOnline }
    }

    suspend fun setCity(city: String) {
        context.dataStore.edit { it[Keys.CITY] = city }
    }

    suspend fun setSpecialty(specialtyId: String): Boolean {
        var updated = false
        context.dataStore.edit { prefs ->
            val alreadyLocked = prefs[Keys.SPECIALTY_LOCKED] ?: false
            if (!alreadyLocked) {
                prefs[Keys.SPECIALTY_ID] = specialtyId
                prefs[Keys.SPECIALTY_LOCKED] = true
                updated = true
            }
        }
        return updated
    }

    suspend fun updateProfile(fullName: String, phone: String, city: String) {
        context.dataStore.edit {
            it[Keys.FULL_NAME] = fullName
            it[Keys.PHONE] = phone
            it[Keys.CITY] = city
        }
    }

    suspend fun saveSupabaseAuth(userId: String, email: String, accessToken: String) {
        context.dataStore.edit {
            it[Keys.USER_ID] = userId
            it[Keys.SUPABASE_EMAIL] = email
            it[Keys.SUPABASE_ACCESS_TOKEN] = accessToken
        }
    }

    suspend fun clearSupabaseAuth() {
        context.dataStore.edit {
            it.remove(Keys.SUPABASE_EMAIL)
            it.remove(Keys.SUPABASE_ACCESS_TOKEN)
            it[Keys.USER_ID] = "local_user"
        }
    }
}
