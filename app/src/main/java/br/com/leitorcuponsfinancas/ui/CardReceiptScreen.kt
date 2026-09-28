package br.com.leitorcuponsfinancas.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.leitorcuponsfinancas.domain.CardReceiptDraft
import br.com.leitorcuponsfinancas.domain.CardReceiptPaymentMethod
import br.com.leitorcuponsfinancas.domain.CreditInstallmentMode
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun CardReceiptScreen(
    modifier: Modifier = Modifier,
    viewModel: CardReceiptViewModel = viewModel(),
) {
    val ocrState by viewModel.ocrState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    var merchantName by remember { mutableStateOf("") }
    var merchantCnpj by remember { mutableStateOf("") }
    var issuedAt by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf<CardReceiptPaymentMethod?>(null) }
    var creditMode by remember { mutableStateOf(CreditInstallmentMode.UNKNOWN) }
    var installmentCount by remember { mutableStateOf("") }
    var cardBrand by remember { mutableStateOf("") }
    var cardLast4 by remember { mutableStateOf("") }
    var allowUnidentifiedAmount by remember { mutableStateOf(false) }
    val items = remember { mutableStateListOf<CardReceiptProductDraft>() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.clearOcr()
            viewModel.readDocument(uri)
        }
    }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.clearOcr()
            viewModel.readPhoto(bitmap)
        }
    }

    LaunchedEffect(ocrState.draft) {
        val draft = ocrState.draft ?: return@LaunchedEffect
        merchantName = draft.merchantName
        merchantCnpj = draft.merchantCnpj
        issuedAt = draft.issuedAt
        totalAmount = CurrencyInputFormatter.fromStoredDecimal(draft.totalAmount)
        paymentMethod = draft.paymentMethod
        creditMode = draft.creditMode
        installmentCount = draft.installmentCount?.toString().orEmpty()
        cardBrand = draft.cardBrand
        cardLast4 = draft.cardLast4
        allowUnidentifiedAmount = false
        items.clear()
        items.add(CardReceiptProductDraft())
    }

    val itemTotal = items.fold(BigDecimal.ZERO) { accumulator, item ->
        accumulator.add(
            CurrencyInputFormatter.parse(item.totalAmount) ?: BigDecimal.ZERO,
        )
    }
    val receiptTotal = CurrencyInputFormatter.parse(totalAmount) ?: BigDecimal.ZERO
    val difference = receiptTotal
        .subtract(itemTotal)
        .setScale(2, RoundingMode.HALF_UP)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHero(
            title = "Comprovante de cartão",
            subtitle = "Leia o pagamento e complete os produtos comprados.",
            icon = Icons.Default.CreditCard,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "1. Ler comprovante",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "O app tenta identificar estabelecimento, data, valor, débito/crédito, bandeira e final do cartão. Os produtos serão informados por você.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = { camera.launch(null) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Text(" Foto")
                    }
                    OutlinedButton(
                        onClick = {
                            picker.launch(
                                arrayOf(
                                    "image/*",
                                    "application/pdf",
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Text(" Arquivo")
                    }
                }

                if (ocrState.reading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Lendo comprovante...")
                    }
                }

                ocrState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (ocrState.draft != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "2. Revise os dados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    SuggestionTextField(
                        value = merchantName,
                        onValueChange = { merchantName = it },
                        suggestions = emptyList(),
                        label = { Text("Estabelecimento *") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = merchantCnpj,
                        onValueChange = {
                            merchantCnpj = it.filter(Char::isDigit).take(14)
                        },
                        label = { Text("CNPJ") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = issuedAt,
                        onValueChange = { issuedAt = it },
                        label = { Text("Data/hora *") },
                        placeholder = { Text("DD/MM/AAAA HH:MM") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    CurrencyTextField(
                        value = totalAmount,
                        onValueChange = { totalAmount = it },
                        label = { Text("Valor total *") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = "Forma de pagamento *",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = paymentMethod == CardReceiptPaymentMethod.DEBIT,
                            onClick = {
                                paymentMethod = CardReceiptPaymentMethod.DEBIT
                                creditMode = CreditInstallmentMode.UNKNOWN
                                installmentCount = ""
                            },
                            label = { Text("Débito") },
                        )
                        FilterChip(
                            selected = paymentMethod == CardReceiptPaymentMethod.CREDIT,
                            onClick = {
                                paymentMethod = CardReceiptPaymentMethod.CREDIT
                            },
                            label = { Text("Crédito") },
                        )
                        FilterChip(
                            selected = paymentMethod == CardReceiptPaymentMethod.OTHER,
                            onClick = {
                                paymentMethod = CardReceiptPaymentMethod.OTHER
                                creditMode = CreditInstallmentMode.UNKNOWN
                                installmentCount = ""
                            },
                            label = { Text("Outro") },
                        )
                    }

                    if (paymentMethod == CardReceiptPaymentMethod.CREDIT) {
                        Text(
                            text = "Crédito",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = creditMode == CreditInstallmentMode.CASH,
                                onClick = {
                                    creditMode = CreditInstallmentMode.CASH
                                    installmentCount = "1"
                                },
                                label = { Text("À vista") },
                            )
                            FilterChip(
                                selected = creditMode == CreditInstallmentMode.INSTALLMENT,
                                onClick = {
                                    creditMode = CreditInstallmentMode.INSTALLMENT
                                    if (installmentCount == "1") {
                                        installmentCount = ""
                                    }
                                },
                                label = { Text("Parcelado") },
                            )
                        }

                        if (creditMode == CreditInstallmentMode.INSTALLMENT) {
                            OutlinedTextField(
                                value = installmentCount,
                                onValueChange = {
                                    installmentCount = it.filter(Char::isDigit).take(2)
                                },
                                label = { Text("Quantidade de parcelas *") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = cardBrand,
                            onValueChange = { cardBrand = it.take(30) },
                            label = { Text("Bandeira") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = cardLast4,
                            onValueChange = {
                                cardLast4 = it.filter(Char::isDigit).take(4)
                            },
                            label = { Text("Final") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Text(
                        text = "Por segurança, o app guarda somente os últimos 4 dígitos do cartão.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "3. Produtos da compra *",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "O comprovante da maquininha não informa o que foi comprado. Adicione todos os produtos antes de salvar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    items.forEachIndexed { index, item ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Produto ${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )

                                SuggestionTextField(
                                    value = item.description,
                                    onValueChange = { value ->
                                        items[index] = item.copy(
                                            description = value,
                                            productId = null,
                                        )
                                    },
                                    suggestions = products.map { it.normalizedName },
                                    label = { Text("Produto *") },
                                    placeholder = { Text("Ex.: Pão de queijo") },
                                    modifier = Modifier.fillMaxWidth(),
                                    onSuggestionSelected = { selected ->
                                        val product = products.firstOrNull {
                                            it.normalizedName.equals(
                                                selected,
                                                ignoreCase = true,
                                            )
                                        }
                                        items[index] = item.copy(
                                            description = selected,
                                            productId = product?.id,
                                            unit = product?.unit ?: item.unit,
                                        )
                                    },
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    OutlinedTextField(
                                        value = item.quantity,
                                        onValueChange = { value ->
                                            items[index] = item.copy(
                                                quantity = value
                                                    .filter { ch ->
                                                        ch.isDigit() || ch == ',' || ch == '.'
                                                    }
                                                    .take(8),
                                            )
                                        },
                                        label = { Text("Quantidade *") },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )

                                    CurrencyTextField(
                                        value = item.totalAmount,
                                        onValueChange = { value ->
                                            items[index] = item.copy(
                                                totalAmount = value,
                                            )
                                        },
                                        label = { Text("Valor total *") },
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                if (items.size > 1) {
                                    OutlinedButton(
                                        onClick = {
                                            items.removeAt(index)
                                        },
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                        )
                                        Text(" Remover")
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            items.add(CardReceiptProductDraft())
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(" Adicionar produto")
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (difference.abs() <= BigDecimal("0.01")) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "Conferência",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Comprovante: R$ ${CurrencyInputFormatter.format(receiptTotal)}")
                    Text("Produtos: R$ ${CurrencyInputFormatter.format(itemTotal)}")

                    val differenceText = when {
                        difference.abs() <= BigDecimal("0.01") ->
                            "Diferença: R$ 0,00 ✓"

                        difference > BigDecimal.ZERO ->
                            "Faltam identificar: R$ ${CurrencyInputFormatter.format(difference)}"

                        else ->
                            "Produtos excedem o comprovante em R$ ${CurrencyInputFormatter.format(difference.abs())}"
                    }
                    Text(
                        text = differenceText,
                        color = if (difference.abs() <= BigDecimal("0.01")) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.SemiBold,
                    )

                    if (difference > BigDecimal("0.01")) {
                        Row {
                            Checkbox(
                                checked = allowUnidentifiedAmount,
                                onCheckedChange = { allowUnidentifiedAmount = it },
                            )
                            Text(
                                text = "Existe valor não identificado nesta compra.",
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            }

            saveState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            saveState.message?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.save(
                        draft = CardReceiptDraft(
                            merchantName = merchantName,
                            merchantCnpj = merchantCnpj,
                            issuedAt = issuedAt,
                            totalAmount = totalAmount,
                            paymentMethod = paymentMethod,
                            creditMode = creditMode,
                            installmentCount = installmentCount.toIntOrNull(),
                            cardBrand = cardBrand,
                            cardLast4 = cardLast4,
                        ),
                        items = items.toList(),
                        allowUnidentifiedAmount = allowUnidentifiedAmount,
                    )
                },
                enabled = !saveState.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (saveState.saving) {
                    CircularProgressIndicator()
                } else {
                    Text("Salvar compra")
                }
            }
        }
    }
}
