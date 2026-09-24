package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PriceComparisonScreen(
    modifier: Modifier = Modifier,
    priceComparisonViewModel: PriceComparisonViewModel = viewModel(),
) {
    val products by priceComparisonViewModel.products.collectAsStateWithLifecycle()
    val selectedProductId by priceComparisonViewModel.selectedProductId.collectAsStateWithLifecycle()
    val comparison by priceComparisonViewModel.comparison.collectAsStateWithLifecycle()
    var productQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHero(
                title = "Comparar preços",
                subtitle = "Veja onde você pagou menos pelo mesmo Produto Mestre nos últimos 12 meses.",
                icon = Icons.Default.CompareArrows,
            )
        }

        item {
            SuggestionTextField(
                value = productQuery,
                onValueChange = { typed ->
                    productQuery = typed
                    val exact = products.firstOrNull {
                        it.normalizedName.equals(typed.trim(), ignoreCase = true)
                    }
                    priceComparisonViewModel.selectProduct(exact?.id)
                },
                suggestions = products.map { it.normalizedName },
                label = { Text("Produto Mestre") },
                modifier = Modifier.fillMaxWidth(),
                onSuggestionSelected = { selected ->
                    productQuery = selected
                    priceComparisonViewModel.selectProduct(
                        products.firstOrNull {
                            it.normalizedName.equals(selected, ignoreCase = true)
                        }?.id,
                    )
                },
            )
        }

        if (selectedProductId == null) {
            item {
                FlowSectionCard(
                    title = "Escolha um produto",
                    subtitle = "Ex.: Arroz, Leite, Macarrão ou outro Produto Mestre já cadastrado.",
                    icon = Icons.Default.CompareArrows,
                ) {
                    Text(
                        "A comparação usa somente o seu próprio histórico de compras.",
                    )
                }
            }
        } else if (comparison == null) {
            item {
                AnimatedInfoCard(
                    visible = true,
                    title = "Ainda não há preços suficientes",
                    message = "Esse Produto Mestre não possui valores unitários válidos no histórico dos últimos 12 meses.",
                )
            }
        } else {
            val data = comparison!!

            item {
                FlowSectionCard(
                    title = data.productName,
                    subtitle = "${data.observations} observação(ões) nos últimos 12 meses",
                    icon = Icons.Default.CompareArrows,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PriceMetric(
                            title = "Menor",
                            value = money(data.lowestUnitPrice),
                            modifier = Modifier.weight(1f),
                        )
                        PriceMetric(
                            title = "Média",
                            value = money(data.averageUnitPrice),
                            modifier = Modifier.weight(1f),
                        )
                        PriceMetric(
                            title = "Maior",
                            value = money(data.highestUnitPrice),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Por estabelecimento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            data.merchants.forEachIndexed { index, merchant ->
                item(key = "merchant_$index") {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "${index + 1}. ${merchant.merchantName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("Preço médio: ${money(merchant.averageUnitPrice)}")
                            Text(
                                "Faixa: ${money(merchant.lowestUnitPrice)} a ${money(merchant.highestUnitPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "${merchant.observations} compra(s) observada(s)${merchant.lastDate?.let { " • última: ${formatIsoDate(it)}" }.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Preços recentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            data.recentPrices.forEachIndexed { index, observation ->
                item(key = "price_$index") {
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    observation.merchantName,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    observation.issuedDate?.let(::formatIsoDate).orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                money(observation.unitPrice),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceMetric(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun money(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

private fun formatIsoDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(value)
