package br.com.leitorcuponsfinancas.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppPreferences(
    val fontScale: Float = 1.0f,
    val onboardingCompleted: Boolean = false,
    val showContextualTips: Boolean = true,
    val seenTips: Set<String> = emptySet(),
)

class AppPreferencesStore private constructor(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppPreferences> = _state.asStateFlow()

    fun setFontScale(value: Float) {
        val safeValue = value.coerceIn(0.9f, 1.3f)
        preferences.edit().putFloat(KEY_FONT_SCALE, safeValue).apply()
        _state.value = _state.value.copy(fontScale = safeValue)
    }

    fun completeOnboarding() {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
        _state.value = _state.value.copy(onboardingCompleted = true)
    }

    fun restartOnboarding() {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, false).apply()
        _state.value = _state.value.copy(onboardingCompleted = false)
    }

    fun setShowContextualTips(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_CONTEXTUAL_TIPS, enabled).apply()
        _state.value = _state.value.copy(showContextualTips = enabled)
    }

    fun markTipSeen(key: String) {
        val updated = _state.value.seenTips + key
        preferences.edit().putStringSet(KEY_SEEN_TIPS, updated).apply()
        _state.value = _state.value.copy(seenTips = updated)
    }

    fun resetTips() {
        preferences.edit().remove(KEY_SEEN_TIPS).apply()
        _state.value = _state.value.copy(seenTips = emptySet())
    }

    private fun load(): AppPreferences = AppPreferences(
        fontScale = preferences.getFloat(KEY_FONT_SCALE, 1.0f),
        onboardingCompleted = preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false),
        showContextualTips = preferences.getBoolean(KEY_SHOW_CONTEXTUAL_TIPS, true),
        seenTips = preferences.getStringSet(KEY_SEEN_TIPS, emptySet()).orEmpty().toSet(),
    )

    companion object {
        private const val PREFERENCES_NAME = "leitor_cupons_preferences"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_SHOW_CONTEXTUAL_TIPS = "show_contextual_tips"
        private const val KEY_SEEN_TIPS = "seen_tips"

        @Volatile
        private var instance: AppPreferencesStore? = null

        fun getInstance(context: Context): AppPreferencesStore =
            instance ?: synchronized(this) {
                instance ?: AppPreferencesStore(context).also { instance = it }
            }
    }
}
