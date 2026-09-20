package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import br.com.leitorcuponsfinancas.data.LocalUserProfile
import br.com.leitorcuponsfinancas.data.UserProfileStore
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val store = UserProfileStore.getInstance(application)
    val profile: StateFlow<LocalUserProfile> = store.profile

    fun saveName(name: String) {
        store.updateDisplayName(name)
    }
}
