package br.com.leitorcuponsfinancas.domain

enum class CardReceiptPaymentMethod {
    DEBIT,
    CREDIT,
    OTHER,
}

enum class CreditInstallmentMode {
    UNKNOWN,
    CASH,
    INSTALLMENT,
}

data class CardReceiptDraft(
    val merchantName: String = "",
    val merchantCnpj: String = "",
    val issuedAt: String = "",
    val totalAmount: String = "",
    val paymentMethod: CardReceiptPaymentMethod? = null,
    val creditMode: CreditInstallmentMode = CreditInstallmentMode.UNKNOWN,
    val installmentCount: Int? = null,
    val cardBrand: String = "",
    val cardLast4: String = "",
)

object CardReceiptParser {

    private val cnpjRegex = Regex(
        """CNPJ\s*:?\s*([0-9.\-/ ]{14,24})""",
        RegexOption.IGNORE_CASE,
    )
    private val dateRegex = Regex(
        """(\d{2}/\d{2}/\d{4})(?:\s*[^0-9\n]{0,5}\s*(\d{2}:\d{2})(?::\d{2})?)?""",
    )
    private val moneyRegex = Regex(
        """(?:R\$\s*)?(\d{1,7}[.,]\d{2})""",
        RegexOption.IGNORE_CASE,
    )
    private val maskedCardRegex = Regex(
        """(?:\*|X|x){2,}\s*(\d{4})\b""",
    )
    private val installmentXRegex = Regex(
        """\b(\d{1,2})\s*[xX]\b""",
    )
    private val installmentWordsRegex = Regex(
        """\b(\d{1,2})\s*(?:PARCELA|PARCELAS|PARC)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val installmentPrefixRegex = Regex(
        """\b(?:PARC|PARCELA|PARCELAS)\s*[:.\-]?\s*(\d{1,2})\b""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(text: String): CardReceiptDraft {
        val lines = text.lines()
            .map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter { it.isNotBlank() }

        val cnpjMatch = cnpjRegex.find(text)
        val cnpj = cnpjMatch
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
            .filter(Char::isDigit)
            .take(14)

        val cnpjLineIndex = lines.indexOfFirst { it.contains("CNPJ", ignoreCase = true) }
        val merchant = findMerchant(lines, cnpjLineIndex)

        val date = dateRegex.find(text)
        val issuedAt = date?.let { match ->
            listOf(
                match.groupValues.getOrNull(1).orEmpty(),
                match.groupValues.getOrNull(2).orEmpty(),
            ).filter { it.isNotBlank() }.joinToString(" ")
        }.orEmpty()

        val paymentMethod = when {
            text.contains("DÉBITO", ignoreCase = true) ||
                text.contains("DEBITO", ignoreCase = true) -> CardReceiptPaymentMethod.DEBIT

            text.contains("CRÉDITO", ignoreCase = true) ||
                text.contains("CREDITO", ignoreCase = true) -> CardReceiptPaymentMethod.CREDIT

            else -> null
        }

        val amount = findPaymentAmount(lines, paymentMethod)

        val installmentCount = findInstallmentCount(text)
        val creditMode = when {
            paymentMethod != CardReceiptPaymentMethod.CREDIT -> CreditInstallmentMode.UNKNOWN
            installmentCount != null && installmentCount > 1 -> CreditInstallmentMode.INSTALLMENT
            containsCashCreditMarker(text) -> CreditInstallmentMode.CASH
            installmentCount == 1 -> CreditInstallmentMode.CASH
            else -> CreditInstallmentMode.UNKNOWN
        }

        val brand = findBrand(text)
        val cardLast4 = maskedCardRegex.find(text)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()

        return CardReceiptDraft(
            merchantName = merchant,
            merchantCnpj = cnpj,
            issuedAt = issuedAt,
            totalAmount = normalizeMoney(amount),
            paymentMethod = paymentMethod,
            creditMode = creditMode,
            installmentCount = installmentCount,
            cardBrand = brand,
            cardLast4 = cardLast4,
        )
    }

    private fun findMerchant(
        lines: List<String>,
        cnpjLineIndex: Int,
    ): String {
        if (cnpjLineIndex > 0) {
            for (index in cnpjLineIndex - 1 downTo 0) {
                val candidate = lines[index]
                if (isMerchantCandidate(candidate)) {
                    return candidate
                }
            }
        }

        return lines.firstOrNull(::isMerchantCandidate).orEmpty()
    }

    private fun isMerchantCandidate(line: String): Boolean {
        val normalized = line.uppercase()
        if (line.length < 3) return false
        if (normalized.contains("VIA LOJISTA")) return false
        if (normalized == "VENDA") return false
        if (normalized.contains("CNPJ")) return false
        if (normalized.startsWith("R.")) return false
        if (normalized.startsWith("RUA ")) return false
        if (normalized.contains("DÉBITO") || normalized.contains("DEBITO")) return false
        if (normalized.contains("CRÉDITO") || normalized.contains("CREDITO")) return false
        if (normalized.contains("VISA") || normalized.contains("MASTERCARD")) return false
        if (normalized.contains("STONE")) return false
        if (dateRegex.containsMatchIn(line)) return false
        return line.any(Char::isLetter)
    }

    private fun findPaymentAmount(
        lines: List<String>,
        method: CardReceiptPaymentMethod?,
    ): String {
        val preferred = lines.firstNotNullOfOrNull { line ->
            val matchesMethod = when (method) {
                CardReceiptPaymentMethod.DEBIT ->
                    line.contains("DÉBITO", true) || line.contains("DEBITO", true)

                CardReceiptPaymentMethod.CREDIT ->
                    line.contains("CRÉDITO", true) || line.contains("CREDITO", true)

                else -> false
            }

            if (matchesMethod) {
                moneyRegex.findAll(line).lastOrNull()?.groupValues?.getOrNull(1)
            } else {
                null
            }
        }
        if (!preferred.isNullOrBlank()) return preferred

        val labeled = lines.firstNotNullOfOrNull { line ->
            val upper = line.uppercase()
            if (
                upper.contains("TOTAL") ||
                upper.contains("VALOR")
            ) {
                moneyRegex.findAll(line).lastOrNull()?.groupValues?.getOrNull(1)
            } else {
                null
            }
        }
        if (!labeled.isNullOrBlank()) return labeled

        return lines
            .flatMap { line ->
                moneyRegex.findAll(line)
                    .mapNotNull { it.groupValues.getOrNull(1) }
                    .toList()
            }
            .maxByOrNull { normalizeMoney(it).toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
            .orEmpty()
    }

    private fun findInstallmentCount(text: String): Int? {
        val xCount = installmentXRegex.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        if (xCount != null) return xCount

        val wordsCount = installmentWordsRegex.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        if (wordsCount != null) return wordsCount

        return installmentPrefixRegex.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }

    private fun containsCashCreditMarker(text: String): Boolean =
        text.contains("A VISTA", ignoreCase = true) ||
            text.contains("À VISTA", ignoreCase = true) ||
            text.contains("CREDITO AVISTA", ignoreCase = true) ||
            text.contains("CRÉDITO À VISTA", ignoreCase = true)

    private fun findBrand(text: String): String {
        val candidates = listOf(
            "MASTERCARD" to "Mastercard",
            "HIPERCARD" to "Hipercard",
            "AMERICAN EXPRESS" to "American Express",
            "AMEX" to "American Express",
            "ELO" to "Elo",
            "VISA" to "Visa",
        )
        val upper = text.uppercase()
        return candidates.firstOrNull { (needle, _) -> upper.contains(needle) }
            ?.second
            .orEmpty()
    }

    private fun normalizeMoney(value: String): String {
        val cleaned = value.trim().replace(Regex("""[^0-9,.-]"""), "")
        val normalized = if (cleaned.contains(',')) {
            cleaned.replace(".", "").replace(",", ".")
        } else {
            cleaned
        }

        return normalized.toBigDecimalOrNull()
            ?.setScale(2, java.math.RoundingMode.HALF_UP)
            ?.toPlainString()
            .orEmpty()
    }
}
