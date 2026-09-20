package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.NfcePublicClient
import br.com.leitorcuponsfinancas.data.ReceiptRepository
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

data class NfceSaveState(
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class NfceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val receiptRepository = ReceiptRepository(
        receiptDao = database.receiptDao(),
        productDao = database.productDao(),
        linkDao = database.merchantProductLinkDao(),
    )

    private val _lookupState = MutableStateFlow(NfceLookupState())
    val lookupState: StateFlow<NfceLookupState> = _lookupState.asStateFlow()

    private val _saveState = MutableStateFlow(NfceSaveState())
    val saveState: StateFlow<NfceSaveState> = _saveState.asStateFlow()

    fun lookup(url: String) {
        if (_lookupState.value.loading) return

        _lookupState.value = NfceLookupState(loading = true)
        _saveState.value = NfceSaveState()

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

    fun saveReceipt(accessKey: String) {
        if (_saveState.value.saving) return

        val receipt = _lookupState.value.receipt
        if (receipt == null) {
            _saveState.value = NfceSaveState(
                error = "Consulte a NFC-e antes de salvá-la.",
            )
            return
        }

        _saveState.value = NfceSaveState(saving = true)

        viewModelScope.launch {
            try {
                val result = receiptRepository.save(
                    accessKey = accessKey,
                    receipt = receipt,
                )

                val prefix = if (result.inserted) {
                    "NFC-e salva no histórico."
                } else {
                    "Esta NFC-e já estava salva no histórico."
                }

                _saveState.value = NfceSaveState(
                    message = buildString {
                        append(prefix)
                        append(" Itens: ${result.totalItems}.")
                        append(" Vinculados automaticamente: ${result.matchedItems}.")
                    },
                )
            } catch (error: Exception) {
                _saveState.value = NfceSaveState(
                    error = "Não foi possível salvar a NFC-e: ${error.message ?: error::class.java.simpleName}",
                )
            }
        }
    }

    fun clearLookup() {
        _lookupState.value = NfceLookupState()
        _saveState.value = NfceSaveState()
    }
}
