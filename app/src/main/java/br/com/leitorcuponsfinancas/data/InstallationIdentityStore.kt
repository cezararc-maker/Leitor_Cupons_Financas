package br.com.leitorcuponsfinancas.data

import android.content.Context
import java.util.UUID

class InstallationIdentityStore private constructor(
    private val context: Context,
) {

    fun getOrCreateInstallationId(): String {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val existing = preferences
            .getString(KEY_INSTALLATION_ID, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (existing != null) return existing

        val created = UUID.randomUUID().toString()
        check(
            preferences.edit()
                .putString(KEY_INSTALLATION_ID, created)
                .commit(),
        ) {
            "Não foi possível criar a identidade local desta instalação."
        }

        return created
    }

    companion object {
        private const val PREFERENCES_NAME = "installation_identity"
        private const val KEY_INSTALLATION_ID = "installation_id"

        @Volatile
        private var instance: InstallationIdentityStore? = null

        fun getInstance(context: Context): InstallationIdentityStore =
            instance ?: synchronized(this) {
                instance ?: InstallationIdentityStore(
                    context.applicationContext,
                ).also { instance = it }
            }
    }
}
