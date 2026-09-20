package br.com.leitorcuponsfinancas.domain

import java.math.BigDecimal
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

data class NfceReceiptItem(
    val description: String,
    val code: String?,
    val quantity: BigDecimal?,
    val unit: String?,
    val unitPrice: BigDecimal?,
    val total: BigDecimal?,
)

data class NfceReceipt(
    val sourceUrl: String,
    val merchantName: String?,
    val merchantCnpj: String?,
    val merchantAddress: String?,
    val number: String?,
    val series: String?,
    val issuedAt: String?,
    val totalAmount: BigDecimal?,
    val items: List<NfceReceiptItem>,
)

sealed interface NfcePageParseResult {
    data class Success(val receipt: NfceReceipt) : NfcePageParseResult
    data class Error(val message: String) : NfcePageParseResult
}

object NfcePageParser {

    private val noteInfoRegex = Regex(
        """Número:\s*([0-9]+)\s+Série:\s*([0-9]+)\s+Emissão:\s*([0-9]{2}/[0-9]{2}/[0-9]{4}\s+[0-9]{2}:[0-9]{2}:[0-9]{2})""",
        RegexOption.IGNORE_CASE,
    )

    private val cnpjRegex = Regex("""CNPJ:\s*([0-9./-]+)""", RegexOption.IGNORE_CASE)
    private val codeRegex = Regex("""Código:\s*([^)]*)""", RegexOption.IGNORE_CASE)
    private val payableRegex = Regex("""Valor a pagar R\$:\s*([0-9.,]+)""", RegexOption.IGNORE_CASE)

    fun parse(html: String, sourceUrl: String): NfcePageParseResult {
        if (html.isBlank()) {
            return NfcePageParseResult.Error("A SEFAZ-MS retornou uma página vazia.")
        }

        val originalDocument = Jsoup.parse(html, sourceUrl)
        val embeddedDanfeHtml = extractEmbeddedDanfeHtml(originalDocument)
        val embeddedDocument = embeddedDanfeHtml
            ?.let { Jsoup.parse(it, sourceUrl) }

        val document = when {
            originalDocument.selectFirst("#tabResult") != null -> originalDocument
            embeddedDocument?.selectFirst("#tabResult") != null -> embeddedDocument
            else -> originalDocument
        }

        val bodyText = cleanText(
            listOfNotNull(
                originalDocument.text(),
                embeddedDocument?.text(),
            ).joinToString(" "),
        )

        if (bodyText.contains("Código da Imagem", ignoreCase = true) ||
            bodyText.contains("captcha", ignoreCase = true)
        ) {
            return NfcePageParseResult.Error(
                "A SEFAZ-MS solicitou validação adicional. Abra a consulta no navegador e conclua-a manualmente.",
            )
        }

        if (bodyText.contains("NFC-e ainda não transmitida", ignoreCase = true)) {
            return NfcePageParseResult.Error(
                "A NFC-e ainda não foi transmitida/autorizada para consulta pública.",
            )
        }

        val items = document.select("#tabResult tr").mapNotNull(::parseItem)

        val merchantName = firstText(
            document,
            originalDocument,
            selectors = listOf("#u20", ".txtCenter .txtTopo"),
        )

        val centerTexts = listOf(document, originalDocument)
            .distinct()
            .flatMap { candidate ->
                candidate.select(".txtCenter .text").map { cleanText(it.text()) }
            }
            .distinct()
        val merchantCnpj = centerTexts.firstNotNullOfOrNull { text ->
            cnpjRegex.find(text)?.groupValues?.getOrNull(1)
        } ?: cnpjRegex.find(bodyText)?.groupValues?.getOrNull(1)

        val merchantAddress = centerTexts
            .firstOrNull { text ->
                text.isNotBlank() &&
                    !text.contains("CNPJ:", ignoreCase = true) &&
                    text != merchantName
            }

        val noteInfo = noteInfoRegex.find(bodyText)
        val number = noteInfo?.groupValues?.getOrNull(1)
        val series = noteInfo?.groupValues?.getOrNull(2)
        val issuedAt = noteInfo?.groupValues?.getOrNull(3)

        val totalAmount = findPayableTotal(document.body(), bodyText)
            ?: embeddedDocument?.body()?.let { findPayableTotal(it, bodyText) }
            ?: findPayableTotal(originalDocument.body(), bodyText)

        if (merchantName == null && items.isEmpty()) {
            val title = cleanText(document.title()).takeIf { it.isNotBlank() }
            val hasScripts = document.select("script").isNotEmpty()
            val looksLikeJavascriptShell = hasScripts &&
                document.select("table").isEmpty() &&
                bodyText.length < 1_500

            val diagnostic = when {
                looksLikeJavascriptShell ->
                    "A página recebida parece depender de JavaScript para montar o DANFE."

                title != null ->
                    "A SEFAZ-MS devolveu uma página diferente do DANFE esperado (título: $title)."

                else ->
                    "A página foi recebida, mas o formato não corresponde ao DANFE NFC-e público esperado da SEFAZ-MS."
            }

            return NfcePageParseResult.Error(diagnostic)
        }

        return NfcePageParseResult.Success(
            NfceReceipt(
                sourceUrl = sourceUrl,
                merchantName = merchantName,
                merchantCnpj = merchantCnpj,
                merchantAddress = merchantAddress,
                number = number,
                series = series,
                issuedAt = issuedAt,
                totalAmount = totalAmount,
                items = items,
            ),
        )
    }

    private fun extractEmbeddedDanfeHtml(document: Document): String? {
        document.select("script").forEach { script ->
            val scriptText = script.data().ifBlank { script.html() }
            if (
                !scriptText.contains("tabResult", ignoreCase = true) &&
                !scriptText.contains("DanfeNFCe", ignoreCase = true)
            ) {
                return@forEach
            }

            extractJavascriptStrings(scriptText)
                .asSequence()
                .sortedByDescending { it.length }
                .map(::decodeJavascriptString)
                .map { Parser.unescapeEntities(it, false) }
                .firstOrNull { candidate ->
                    candidate.contains("tabResult", ignoreCase = true) &&
                        candidate.contains("<table", ignoreCase = true)
                }
                ?.let { return it }
        }

        return null
    }

    private fun extractJavascriptStrings(script: String): List<String> {
        val values = mutableListOf<String>()
        var index = 0

        while (index < script.length) {
            val quote = script[index]
            if (quote != '\'' && quote != '"') {
                index++
                continue
            }

            val value = StringBuilder()
            var cursor = index + 1
            var escaped = false

            while (cursor < script.length) {
                val char = script[cursor]

                if (escaped) {
                    value.append('\\')
                    value.append(char)
                    escaped = false
                    cursor++
                    continue
                }

                if (char == '\\') {
                    escaped = true
                    cursor++
                    continue
                }

                if (char == quote) {
                    values += value.toString()
                    index = cursor + 1
                    break
                }

                value.append(char)
                cursor++
            }

            if (cursor >= script.length) {
                index++
            }
        }

        return values
    }

    private fun decodeJavascriptString(value: String): String {
        val result = StringBuilder()
        var index = 0

        while (index < value.length) {
            val char = value[index]
            if (char != '\\' || index + 1 >= value.length) {
                result.append(char)
                index++
                continue
            }

            val next = value[index + 1]
            when (next) {
                'n' -> { result.append('\n'); index += 2 }
                'r' -> { result.append('\r'); index += 2 }
                't' -> { result.append('\t'); index += 2 }
                'b' -> { result.append('\b'); index += 2 }
                'f' -> { result.append('\u000C'); index += 2 }
                '/' -> { result.append('/'); index += 2 }
                '\\' -> { result.append('\\'); index += 2 }
                '\'' -> { result.append('\''); index += 2 }
                '"' -> { result.append('"'); index += 2 }
                'u' -> {
                    val hex = value.substring(index + 2, minOf(index + 6, value.length))
                    val decoded = if (hex.length == 4) hex.toIntOrNull(16)?.toChar() else null
                    if (decoded != null) {
                        result.append(decoded)
                        index += 6
                    } else {
                        result.append(next)
                        index += 2
                    }
                }
                'x' -> {
                    val hex = value.substring(index + 2, minOf(index + 4, value.length))
                    val decoded = if (hex.length == 2) hex.toIntOrNull(16)?.toChar() else null
                    if (decoded != null) {
                        result.append(decoded)
                        index += 4
                    } else {
                        result.append(next)
                        index += 2
                    }
                }
                else -> {
                    result.append(next)
                    index += 2
                }
            }
        }

        return result.toString()
    }

    private fun firstText(
        primary: Document,
        fallback: Document,
        selectors: List<String>,
    ): String? = listOf(primary, fallback)
        .distinct()
        .firstNotNullOfOrNull { document ->
            selectors.firstNotNullOfOrNull { selector ->
                document.selectFirst(selector)
                    ?.text()
                    ?.let(::cleanText)
                    ?.takeIf { it.isNotBlank() }
            }
        }
    private fun parseItem(row: Element): NfceReceiptItem? {
        val description = row.selectFirst(".txtTit")?.text()?.let(::cleanText)
            ?.takeIf { it.isNotBlank() }
            ?: row.selectFirst(".txtTit2")?.text()?.let(::cleanText)
                ?.takeIf { it.isNotBlank() }
            ?: return null

        val codeText = row.selectFirst(".RCod")?.text()?.let(::cleanText)
        val code = codeText?.let { codeRegex.find(it)?.groupValues?.getOrNull(1)?.trim() }
            ?.takeIf { it.isNotBlank() }

        val quantity = parseBrazilianDecimal(
            row.selectFirst(".Rqtd")?.text()?.substringAfter("Qtde.:", "")
        )
        val unit = row.selectFirst(".RUN")?.text()
            ?.substringAfter("UN:", "")
            ?.let(::cleanText)
            ?.takeIf { it.isNotBlank() }
        val unitPrice = parseBrazilianDecimal(
            row.selectFirst(".RvlUnit")?.text()?.substringAfter("Vl. Unit.:", "")
        )
        val total = parseBrazilianDecimal(row.selectFirst(".valor")?.text())

        return NfceReceiptItem(
            description = description,
            code = code,
            quantity = quantity,
            unit = unit,
            unitPrice = unitPrice,
            total = total,
        )
    }

    private fun findPayableTotal(root: Element, bodyText: String): BigDecimal? {
        val line = root.select("#totalNota #linhaTotal")
            .firstOrNull { it.text().contains("Valor a pagar R$", ignoreCase = true) }

        val fromLine = line?.selectFirst(".totalNumb")?.text()?.let(::parseBrazilianDecimal)
        if (fromLine != null) return fromLine

        return payableRegex.find(bodyText)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::parseBrazilianDecimal)
    }

    internal fun parseBrazilianDecimal(value: String?): BigDecimal? {
        val cleaned = value
            ?.replace("\u00A0", " ")
            ?.trim()
            ?.replace(".", "")
            ?.replace(",", ".")
            ?.replace(Regex("""[^0-9.-]"""), "")
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return cleaned.toBigDecimalOrNull()
    }

    private fun cleanText(value: String): String = value
        .replace("\u00A0", " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}
