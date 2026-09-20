package br.com.leitorcuponsfinancas.ui

import java.math.BigDecimal
import java.math.RoundingMode

enum class ManualUnitType(
    val code: String,
    val label: String,
    val quantityLabel: String,
    val priceLabel: String,
) {
    UNIT("UN", "Unidade", "Quantidade", "Valor por unidade"),
    KILOGRAM("KG", "Quilo", "Peso (kg)", "Valor por kg"),
    GRAM("G", "Grama", "Peso (g)", "Valor por g"),
    LITER("L", "Litro", "Volume (L)", "Valor por litro"),
    MILLILITER("ML", "Mililitro", "Volume (ml)", "Valor por ml"),
    PACKAGE("PCT", "Pacote", "Quantidade de pacotes", "Valor por pacote"),
    OTHER("OUTRO", "Outro", "Quantidade", "Valor por unidade"),
}

internal object ManualEntryCalculator {

    fun parseDecimal(value: String): BigDecimal? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null

        val cleaned = trimmed.replace(Regex("""[^0-9,.-]"""), "")
        val normalized = if (cleaned.contains(",")) {
            cleaned.replace(".", "").replace(",", ".")
        } else {
            cleaned
        }

        return normalized.toBigDecimalOrNull()
    }

    fun calculateTotal(
        quantity: String,
        unitPrice: String,
    ): BigDecimal? {
        val quantityValue = parseDecimal(quantity) ?: return null
        val priceValue = parseDecimal(unitPrice) ?: return null
        if (quantityValue <= BigDecimal.ZERO || priceValue < BigDecimal.ZERO) return null

        return quantityValue
            .multiply(priceValue)
            .setScale(2, RoundingMode.HALF_UP)
    }

    fun formatMoney(value: BigDecimal): String = value
        .setScale(2, RoundingMode.HALF_UP)
        .toPlainString()
        .replace(".", ",")
}
