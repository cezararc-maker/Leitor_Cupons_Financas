package br.com.leitorcuponsfinancas.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.NfcePublicClient
import br.com.leitorcuponsfinancas.domain.NfcePageParseResult
import br.com.leitorcuponsfinancas.domain.NfceReceipt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NfceLookupState(
    val loading: Boolean = false,
    val receipt: NfceReceipt? = null,
    val error: String? = null,
)

class NfceViewModel : ViewModel() {

    private val _lookupState = MutableStateFlow(NfceLookupState())
    val lookupState: StateFlow<NfceLookupState> = _lookupState.asStateFlow()

    fun lookup(url: String) {
        if (_lookupState.value.loading) return

        _lookupState.value = NfceLookupState(loading = true)

        viewModelScope.launch {
            when (val result = NfcePublicClient.fetch(url)) {
                is NfcePageParseResult.Success -> {
                    _lookupState.value = NfceLookupState(receipt = result.receipt)
                }

                is NfcePageParseResult.Error -> {
                    _lookupState.value = NfceLookupState(error = result.message)
                }
            }
        }
    }

    fun clearLookup() {
        _lookupState.value = NfceLookupState()
    }
}
