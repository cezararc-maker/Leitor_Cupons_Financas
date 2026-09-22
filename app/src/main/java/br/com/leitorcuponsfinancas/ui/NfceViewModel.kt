package br.com.leitorcuponsfinancas.ui

import android.app.Application
import android.net.Uri
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.NfcePublicClient
import br.com.leitorcuponsfinancas.data.QrImageReadResult
import br.com.leitorcuponsfinancas.data.QrImageReader
import br.com.leitorcuponsfinancas.data.ReceiptOcrReader
import br.com.leitorcuponsfinancas.data.ReceiptOcrReadResult
import br.com.leitorcuponsfinancas.domain.OcrReceiptDraft
import br.com.leitorcuponsfinancas.domain.ReceiptOcrParser
import br.com.leitorcuponsfinancas.domain.NfceReceiptItem
import br.com.leitorcuponsfinancas.data.ReceiptRepository
import br.com.leitorcuponsfinancas.data.UserProfileStore
import br.com.leitorcuponsfinancas.domain.NfcePageParseResult
import br.com.leitorcuponsfinancas.domain.NfceReceipt
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

data class NfceImageState(
    val reading: Boolean = false,
    val qrText: String? = null,
    val error: String? = null,
)

data class ReceiptOcrState(
    val reading: Boolean = false,
    val draft: OcrReceiptDraft? = null,
    val error: String? = null,
)

data class NfceDuplicateState(
    val checking: Boolean = false,
    val alreadyImported: Boolean = false,
    val firstImportedAt: Long? = null,
    val formattedFirstImportedAt: String? = null,
)

class NfceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val userProfileStore = UserProfileStore.getInstance(application)
    private val receiptRepository = ReceiptRepository(
        receiptDao = database.receiptDao(),
        productDao = database.productDao(),
        linkDao = database.merchantProductLinkDao(),
    )

    private val _lookupState = MutableStateFlow(NfceLookupState())
    val lookupState: StateFlow<NfceLookupState> = _lookupState.asStateFlow()

    private val _saveState = MutableStateFlow(NfceSaveState())
    val saveState: StateFlow<NfceSaveState> = _saveState.asStateFlow()

    private val _imageState = MutableStateFlow(NfceImageState())
    val imageState: StateFlow<NfceImageState> = _imageState.asStateFlow()

    private val _ocrState = MutableStateFlow(ReceiptOcrState())
    val ocrState: StateFlow<ReceiptOcrState> = _ocrState.asStateFlow()

    private val _duplicateState = MutableStateFlow(NfceDuplicateState())
    val duplicateState: StateFlow<NfceDuplicateState> = _duplicateState.asStateFlow()

    fun readReceiptDocument(uri: Uri) {
        if (_ocrState.value.reading) return
        _ocrState.value = ReceiptOcrState(reading = true)
        viewModelScope.launch {
            when (val result = ReceiptOcrReader.read(getApplication(), uri)) {
                is ReceiptOcrReadResult.Success -> _ocrState.value =
                    ReceiptOcrState(draft = ReceiptOcrParser.parse(result.text))
                is ReceiptOcrReadResult.Error -> _ocrState.value =
                    ReceiptOcrState(error = result.message)
            }
        }
    }

    fun readReceiptPhoto(bitmap: Bitmap) {
        if (_ocrState.value.reading) return
        _ocrState.value = ReceiptOcrState(reading = true)
        viewModelScope.launch {
            when (val result = ReceiptOcrReader.read(bitmap)) {
                is ReceiptOcrReadResult.Success -> _ocrState.value =
                    ReceiptOcrState(draft = ReceiptOcrParser.parse(result.text))
                is ReceiptOcrReadResult.Error -> _ocrState.value =
                    ReceiptOcrState(error = result.message)
            }
        }
    }

    fun clearOcrState() {
        _ocrState.value = ReceiptOcrState()
        _saveState.value = NfceSaveState()
    }

    fun saveOcrDraft(draft: OcrReceiptDraft) {
        if (_saveState.value.saving) return
        if (draft.merchantName.isBlank()) {
            _saveState.value = NfceSaveState(error = "Informe o estabelecimento antes de salvar.")
            return
        }
        if (draft.issuedAt.isBlank()) {
            _saveState.value = NfceSaveState(error = "Informe a data da compra antes de salvar.")
            return
        }
        if (draft.items.isEmpty() || draft.items.any { it.description.isBlank() }) {
            _saveState.value = NfceSaveState(error = "Revise os itens identificados antes de salvar.")
            return
        }

        _saveState.value = NfceSaveState(saving = true)
        viewModelScope.launch {
            try {
                val key = draft.accessKey.filter(Char::isDigit).takeIf { it.length == 44 }
                    ?: "OCR:" + java.util.UUID.randomUUID().toString()
                val receipt = NfceReceipt(
                    sourceUrl = "ocr://document",
                    merchantName = draft.merchantName.trim(),
                    merchantCnpj = draft.merchantCnpj.filter(Char::isDigit).ifBlank { null },
                    merchantAddress = null,
                    number = draft.number.trim().ifBlank { null },
                    series = draft.series.trim().ifBlank { null },
                    issuedAt = draft.issuedAt.trim(),
                    totalAmount = draft.totalAmount.toBigDecimalOrNull(),
                    items = draft.items.map { item ->
                        NfceReceiptItem(
                            description = item.description.trim(),
                            code = item.code.trim().ifBlank { null },
                            quantity = item.quantity.toBigDecimalOrNull(),
                            unit = item.unit.trim().ifBlank { null },
                            unitPrice = item.unitPrice.toBigDecimalOrNull(),
                            total = item.total.toBigDecimalOrNull(),
                        )
                    },
                )
                val result = receiptRepository.save(
                    accessKey = key,
                    receipt = receipt,
                    actor = userProfileStore.profile.value,
                )
                _saveState.value = if (result.inserted) {
                    NfceSaveState(message = "Comprovante revisado e salvo no histórico. Itens: ${result.totalItems}.")
                } else {
                    NfceSaveState(message = "Esta NFC-e já estava no histórico. Nenhum lançamento duplicado foi criado.")
                }
            } catch (error: Exception) {
                _saveState.value = NfceSaveState(error = "Não foi possível salvar: ${error.message ?: error::class.java.simpleName}")
            }
        }
    }

    fun checkDuplicate(accessKey: String) {
        _duplicateState.value = NfceDuplicateState(checking = true)

        viewModelScope.launch {
            val existing = receiptRepository.findImportedReceipt(accessKey)

            _duplicateState.value = if (existing != null) {
                NfceDuplicateState(
                    alreadyImported = true,
                    firstImportedAt = existing.createdAt,
                    formattedFirstImportedAt = formatImportedAt(existing.createdAt),
                )
            } else {
                NfceDuplicateState()
            }
        }
    }

    fun clearDuplicateState() {
        _duplicateState.value = NfceDuplicateState()
    }

    fun readQrImage(uri: Uri) {
        if (_imageState.value.reading) return

        _imageState.value = NfceImageState(reading = true)

        viewModelScope.launch {
            when (
                val result = QrImageReader.read(
                    context = getApplication(),
                    uri = uri,
                )
            ) {
                is QrImageReadResult.Success -> {
                    _imageState.value = NfceImageState(
                        qrText = result.text,
                    )
                }

                is QrImageReadResult.Error -> {
                    _imageState.value = NfceImageState(
                        error = result.message,
                    )
                }
            }
        }
    }

    fun clearImageState() {
        _imageState.value = NfceImageState()
    }

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
                    actor = userProfileStore.profile.value,
                )

                _saveState.value = if (result.inserted) {
                    NfceSaveState(
                        message = buildString {
                            append("NFC-e salva no histórico.")
                            append(" Itens: ${result.totalItems}.")
                            append(" Vinculados automaticamente: ${result.matchedItems}.")
                        },
                    )
                } else {
                    val formatted = formatImportedAt(result.firstImportedAt)
                    _duplicateState.value = NfceDuplicateState(
                        alreadyImported = true,
                        firstImportedAt = result.firstImportedAt,
                        formattedFirstImportedAt = formatted,
                    )
                    NfceSaveState(
                        message = "Esta NFC-e já havia sido importada em $formatted. Nenhum novo lançamento foi criado.",
                    )
                }
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

    private fun formatImportedAt(timestamp: Long): String =
        DateTimeFormatter
            .ofPattern("dd/MM/yyyy 'às' HH:mm:ss")
            .format(
                Instant
                    .ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault()),
            )
}
