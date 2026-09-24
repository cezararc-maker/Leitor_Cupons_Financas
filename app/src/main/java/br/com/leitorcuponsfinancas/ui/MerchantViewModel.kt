package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.MerchantEntity
import br.com.leitorcuponsfinancas.data.MerchantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MerchantEditState(
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class MerchantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MerchantRepository(
        AppDatabase.getInstance(application).merchantDao(),
    )

    val merchants: StateFlow<List<MerchantEntity>> =
        repository.merchants.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _editState = MutableStateFlow(MerchantEditState())
    val editState: StateFlow<MerchantEditState> = _editState.asStateFlow()

    fun clearMessage() {
        _editState.value = MerchantEditState()
    }

    fun rename(
        merchant: MerchantEntity,
        name: String,
    ) {
        if (_editState.value.saving) return
        _editState.value = MerchantEditState(saving = true)

        viewModelScope.launch {
            val error = repository.rename(merchant, name)
            _editState.value = if (error == null) {
                MerchantEditState(message = "Estabelecimento atualizado.")
            } else {
                MerchantEditState(error = error)
            }
        }
    }
}
