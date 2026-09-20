package br.com.leitorcuponsfinancas.data

import android.content.Context
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocalUserProfile(
    val id: String,
    val displayName: String,
)

class UserProfileStore private constructor(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        "user_profile",
        Context.MODE_PRIVATE,
    )

    private val profileId: String = preferences.getString(KEY_ID, null)
        ?: UUID.randomUUID().toString().also { generated ->
            preferences.edit().putString(KEY_ID, generated).apply()
        }

    private val _profile = MutableStateFlow(
        LocalUserProfile(
            id = profileId,
            displayName = preferences.getString(KEY_NAME, null)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_NAME,
        ),
    )
    val profile: StateFlow<LocalUserProfile> = _profile.asStateFlow()

    fun updateDisplayName(value: String) {
        val name = value.trim().ifBlank { DEFAULT_NAME }
        preferences.edit().putString(KEY_NAME, name).apply()
        _profile.value = _profile.value.copy(displayName = name)
    }

    companion object {
        private const val KEY_ID = "profile_id"
        private const val KEY_NAME = "display_name"
        private const val DEFAULT_NAME = "Usuário local"

        @Volatile
        private var instance: UserProfileStore? = null

        fun getInstance(context: Context): UserProfileStore =
            instance ?: synchronized(this) {
                instance ?: UserProfileStore(context).also { instance = it }
            }
    }
}
