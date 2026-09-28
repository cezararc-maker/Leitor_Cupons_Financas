package br.com.leitorcuponsfinancas.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import br.com.leitorcuponsfinancas.data.CardReceiptItemInput
import br.com.leitorcuponsfinancas.data.ProductEntity
import br.com.leitorcuponsfinancas.data.ReceiptOcrReadResult
import br.com.leitorcuponsfinancas.data.ReceiptOcrReader
import br.com.leitorcuponsfinancas.data.ReceiptRepository
import br.com.leitorcuponsfinancas.data.UserProfileStore
import br.com.leitorcuponsfinancas.domain.CardReceiptDraft
import br.com.leitorcuponsfinancas.domain.CardReceiptParser
import br.com.leitorcuponsfinancas.domain.CardReceiptPaymentMethod
import br.com.leitorcuponsfinancas.domain.CreditInstallmentMode
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CardReceiptOcrState(
    val reading: Boolean = false,
    val draft: CardReceiptDraft? = null,
    val error: String? = null,
)

data class CardReceiptSaveState(
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

data class CardReceiptProductDraft(
    val description: String = "",
    val quantity: String = "1",
    val unit: String = "UN",
    val totalAmount: String = "",
    val productId: Long? = null,
)

class CardReceiptViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val userProfileStore = UserProfileStore.getInstance(application)
    private val repository = ReceiptRepository(
        receiptDao = database.receiptDao(),
        productDao = database.productDao(),
        linkDao = database.merchantProductLinkDao(),
        merchantDao = database.merchantDao(),
    )

    val products: StateFlow<List<ProductEntity>> = database.productDao()
        .observeActive()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _ocrState = MutableStateFlow(CardReceiptOcrState())
    val ocrState: StateFlow<CardReceiptOcrState> = _ocrState.asStateFlow()

    private val _saveState = MutableStateFlow(CardReceiptSaveState())
    val saveState: StateFlow<CardReceiptSaveState> = _saveState.asStateFlow()

    fun readDocument(uri: Uri) {
        if (_ocrState.value.reading) return
        _ocrState.value = CardReceiptOcrState(reading = true)
        _saveState.value = CardReceiptSaveState()

        viewModelScope.launch {
            when (val result = ReceiptOcrReader.read(getApplication(), uri)) {
                is ReceiptOcrReadResult.Success -> {
                    _ocrState.value = CardReceiptOcrState(
                        draft = CardReceiptParser.parse(result.text),
                    )
                }

                is ReceiptOcrReadResult.Error -> {
                    _ocrState.value = CardReceiptOcrState(
                        error = result.message,
                    )
                }
            }
        }
    }

    fun readPhoto(bitmap: Bitmap) {
        if (_ocrState.value.reading) return
        _ocrState.value = CardReceiptOcrState(reading = true)
        _saveState.value = CardReceiptSaveState()

        viewModelScope.launch {
            when (val result = ReceiptOcrReader.read(bitmap)) {
                is ReceiptOcrReadResult.Success -> {
                    _ocrState.value = CardReceiptOcrState(
                        draft = CardReceiptParser.parse(result.text),
                    )
                }

                is ReceiptOcrReadResult.Error -> {
                    _ocrState.value = CardReceiptOcrState(
                        error = result.message,
                    )
                }
            }
        }
    }

    fun save(
        draft: CardReceiptDraft,
        items: List<CardReceiptProductDraft>,
        allowUnidentifiedAmount: Boolean,
    ) {
        if (_saveState.value.saving) return

        val total = CurrencyInputFormatter.parse(draft.totalAmount)
        if (total == null || total <= BigDecimal.ZERO) {
            _saveState.value = CardReceiptSaveState(
                error = "Informe o valor total do comprovante.",
            )
            return
        }

        if (draft.merchantName.isBlank()) {
            _saveState.value = CardReceiptSaveState(
                error = "Informe o estabelecimento.",
            )
            return
        }

        if (draft.issuedAt.isBlank()) {
            _saveState.value = CardReceiptSaveState(
                error = "Informe a data da compra.",
            )
            return
        }

        val paymentMethod = draft.paymentMethod
        if (paymentMethod == null) {
            _saveState.value = CardReceiptSaveState(
                error = "Confirme se o pagamento foi débito, crédito ou outro.",
            )
            return
        }

        val installmentCount = when (paymentMethod) {
            CardReceiptPaymentMethod.DEBIT,
            CardReceiptPaymentMethod.OTHER -> null

            CardReceiptPaymentMethod.CREDIT -> when (draft.creditMode) {
                CreditInstallmentMode.CASH -> 1
                CreditInstallmentMode.INSTALLMENT ->
                    draft.installmentCount
                        ?.takeIf { it >= 2 }
                        ?: run {
                            _saveState.value = CardReceiptSaveState(
                                error = "Informe a quantidade de parcelas do crédito.",
                            )
                            return
                        }

                CreditInstallmentMode.UNKNOWN -> {
                    _saveState.value = CardReceiptSaveState(
                        error = "Confirme se o crédito foi à vista ou parcelado.",
                    )
                    return
                }
            }
        }

        if (items.isEmpty()) {
            _saveState.value = CardReceiptSaveState(
                error = "Adicione pelo menos um produto comprado.",
            )
            return
        }

        val parsedItems = mutableListOf<CardReceiptItemInput>()
        var itemSum = BigDecimal.ZERO

        items.forEachIndexed { index, item ->
            if (item.description.isBlank()) {
                _saveState.value = CardReceiptSaveState(
                    error = "Informe o nome do produto ${index + 1}.",
                )
                return
            }

            val quantity = parseDecimal(item.quantity)
                ?.takeIf { it > BigDecimal.ZERO }
            if (quantity == null) {
                _saveState.value = CardReceiptSaveState(
                    error = "Informe uma quantidade válida no produto ${index + 1}.",
                )
                return
            }

            val itemTotal = CurrencyInputFormatter.parse(item.totalAmount)
                ?.takeIf { it >= BigDecimal.ZERO }
            if (itemTotal == null) {
                _saveState.value = CardReceiptSaveState(
                    error = "Informe o valor do produto ${index + 1}.",
                )
                return
            }

            itemSum = itemSum.add(itemTotal)
            parsedItems += CardReceiptItemInput(
                description = item.description,
                quantity = quantity.stripTrailingZeros().toPlainString(),
                unit = item.unit,
                totalAmount = itemTotal.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                productId = item.productId,
            )
        }

        val difference = total.subtract(itemSum)
        val tolerance = BigDecimal("0.01")

        if (difference < tolerance.negate()) {
            _saveState.value = CardReceiptSaveState(
                error = "A soma dos produtos é maior que o valor do comprovante.",
            )
            return
        }

        if (difference > tolerance && !allowUnidentifiedAmount) {
            _saveState.value = CardReceiptSaveState(
                error = "Ainda faltam R$ ${CurrencyInputFormatter.format(difference)} em produtos. Corrija os itens ou marque valor não identificado.",
            )
            return
        }

        _saveState.value = CardReceiptSaveState(saving = true)

        viewModelScope.launch {
            try {
                val result = repository.saveCardReceiptPurchase(
                    merchantName = draft.merchantName,
                    merchantCnpj = draft.merchantCnpj,
                    issuedAt = draft.issuedAt,
                    totalAmount = total.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    paymentMethod = paymentMethod.name,
                    installmentCount = installmentCount,
                    cardBrand = draft.cardBrand,
                    cardLast4 = draft.cardLast4,
                    items = parsedItems,
                    actor = userProfileStore.profile.value,
                )

                _saveState.value = CardReceiptSaveState(
                    message = "Comprovante salvo no histórico. Produtos: ${result.totalItems}.",
                )
            } catch (error: Exception) {
                _saveState.value = CardReceiptSaveState(
                    error = "Não foi possível salvar: ${error.message ?: error::class.java.simpleName}",
                )
            }
        }
    }

    fun clearMessage() {
        _saveState.value = CardReceiptSaveState()
    }

    fun clearOcr() {
        _ocrState.value = CardReceiptOcrState()
        _saveState.value = CardReceiptSaveState()
    }

    private fun parseDecimal(value: String): BigDecimal? {
        val cleaned = value.trim().replace(Regex("""[^0-9,.-]"""), "")
        if (cleaned.isBlank()) return null
        val normalized = if (cleaned.contains(',')) {
            cleaned.replace(".", "").replace(",", ".")
        } else {
            cleaned
        }
        return normalized.toBigDecimalOrNull()
    }
}
