package br.com.leitorcuponsfinancas.domain

import java.math.BigDecimal

data class OcrReceiptDraft(
    val merchantName: String = "",
    val merchantCnpj: String = "",
    val accessKey: String = "",
    val number: String = "",
    val series: String = "",
    val issuedAt: String = "",
    val totalAmount: String = "",
    val items: List<OcrItemDraft> = emptyList(),
)

data class OcrItemDraft(
    val description: String,
    val code: String = "",
    val quantity: String = "1",
    val unit: String = "UN",
    val unitPrice: String = "",
    val total: String = "",
)

object ReceiptOcrParser {
    private val cnpjRegex = Regex("""CNPJ\s*:?\s*([0-9.\-/ ]{14,22})""", RegexOption.IGNORE_CASE)
    private val dateRegex = Regex("""(\d{2}/\d{2}/\d{4})(?:\s+(\d{2}:\d{2})(?::\d{2})?)?""")
    private val noteRegex = Regex("""NFC-?e\s*(?:n[oº°.]*)?\s*(\d+)\s*(?:S[eé]rie)?\s*(\d+)?""", RegexOption.IGNORE_CASE)
    private val moneyRegex = Regex("""(\d{1,6}[.,]\d{2})""")
    private val itemLineRegex = Regex("""^\s*(?:\S+\s+)?(.{4,}?)\s+(\d+(?:[.,]\d+)?)\s+(UN|KG|G|LT|L|CX|PCT|PC|UND)\s+(\d+[.,]\d{2})\s+(\d+[.,]\d{2})(?:\s+[-0-9.,]+)?\s+(\d+[.,]\d{2})\s*$""", RegexOption.IGNORE_CASE)

    fun parse(text: String): OcrReceiptDraft {
        val lines = text.lines().map { it.replace(Regex("""\s+"""), " ").trim() }.filter { it.isNotBlank() }
        val cnpj = cnpjRegex.find(text)?.groupValues?.get(1)?.filter(Char::isDigit).orEmpty().take(14)
        val accessKey = findAccessKey(text)
        val note = noteRegex.find(text)
        val date = dateRegex.find(text)
        val merchant = lines.firstOrNull { line ->
            line.length >= 5 &&
                !line.contains("CNPJ", true) &&
                !line.contains("Documento Auxiliar", true) &&
                !line.contains("Nota Fiscal", true)
        }.orEmpty()
        val total = lines.asReversed().firstNotNullOfOrNull { line ->
            if (line.contains("VALOR A PAGAR", true) || line.contains("VALOR TOTAL", true)) {
                moneyRegex.findAll(line).lastOrNull()?.value
            } else null
        }.orEmpty()
        val items = lines.mapNotNull(::parseItemLine)

        return OcrReceiptDraft(
            merchantName = merchant,
            merchantCnpj = cnpj,
            accessKey = accessKey,
            number = note?.groupValues?.getOrNull(1).orEmpty(),
            series = note?.groupValues?.getOrNull(2).orEmpty(),
            issuedAt = date?.let { m -> listOf(m.groupValues[1], m.groupValues.getOrNull(2).orEmpty()).filter { it.isNotBlank() }.joinToString(" ") }.orEmpty(),
            totalAmount = normalizeMoney(total),
            items = items,
        )
    }

    private fun parseItemLine(line: String): OcrItemDraft? {
        val match = itemLineRegex.matchEntire(line) ?: return null
        return OcrItemDraft(
            description = match.groupValues[1].trim(),
            quantity = normalizeDecimal(match.groupValues[2]),
            unit = match.groupValues[3].uppercase(),
            unitPrice = normalizeMoney(match.groupValues[4]),
            total = normalizeMoney(match.groupValues[6]),
        )
    }

    private fun findAccessKey(text: String): String {
        val candidates = Regex("""(?:\d[\s.]*){44}""").findAll(text)
        return candidates.map { it.value.filter(Char::isDigit) }.firstOrNull { it.length == 44 }.orEmpty()
    }

    private fun normalizeMoney(value: String): String = normalizeDecimal(value)
    private fun normalizeDecimal(value: String): String {
        val cleaned = value.trim().replace(Regex("""[^0-9,.-]"""), "")
        val normalized = if (cleaned.contains(',')) cleaned.replace(".", "").replace(",", ".") else cleaned
        return normalized.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString().orEmpty()
    }
}
