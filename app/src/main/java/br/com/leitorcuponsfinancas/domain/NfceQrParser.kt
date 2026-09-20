package br.com.leitorcuponsfinancas.domain

import java.math.BigDecimal

enum class NfceEnvironment(val code: String, val label: String) {
    PRODUCTION("1", "Produção"),
    HOMOLOGATION("2", "Homologação");

    companion object {
        fun fromCode(code: String): NfceEnvironment? = entries.firstOrNull { it.code == code }
    }
}

enum class NfceEmissionMode(val label: String) {
    ONLINE("Online"),
    OFFLINE("Contingência offline"),
}

data class NfceQrData(
    val originalText: String,
    val consultationUrl: String?,
    val payload: String,
    val accessKey: String,
    val qrVersion: Int,
    val environment: NfceEnvironment,
    val emissionMode: NfceEmissionMode,
    val issueDay: String? = null,
    val totalValue: BigDecimal? = null,
    val recipientType: String? = null,
    val recipientId: String? = null,
)

sealed interface NfceQrParseResult {
    data class Success(val data: NfceQrData) : NfceQrParseResult
    data class Error(val message: String) : NfceQrParseResult
}

object NfceQrParser {

    private const val MS_UF_CODE = "50"
    private const val NFCE_MODEL = "65"

    fun parse(rawText: String): NfceQrParseResult {
        val original = rawText.trim()
        if (original.isBlank()) {
            return NfceQrParseResult.Error("Informe o link ou conteúdo do QR Code da NFC-e.")
        }

        val payload = extractPayload(original)
            ?: return NfceQrParseResult.Error(
                "Não foi possível localizar o parâmetro 'p' do QR Code. Cole a URL completa ou o conteúdo separado por '|'.",
            )

        val parts = payload.split('|')
        if (parts.size < 3) {
            return NfceQrParseResult.Error("Conteúdo do QR Code incompleto.")
        }

        val accessKey = parts[0].trim()
        validateAccessKey(accessKey)?.let { return NfceQrParseResult.Error(it) }

        val version = parts[1].trim().toIntOrNull()
            ?: return NfceQrParseResult.Error("Versão do QR Code inválida.")

        if (version !in setOf(2, 3)) {
            return NfceQrParseResult.Error("Versão de QR Code não suportada: $version. O MVP aceita versões 2 e 3.")
        }

        val environment = NfceEnvironment.fromCode(parts[2].trim())
            ?: return NfceQrParseResult.Error("Ambiente inválido. Esperado 1 (Produção) ou 2 (Homologação).")

        return when (version) {
            2 -> parseVersion2(original, payload, parts, accessKey, environment)
            3 -> parseVersion3(original, payload, parts, accessKey, environment)
            else -> NfceQrParseResult.Error("Versão de QR Code não suportada.")
        }
    }

    private fun parseVersion2(
        original: String,
        payload: String,
        parts: List<String>,
        accessKey: String,
        environment: NfceEnvironment,
    ): NfceQrParseResult {
        val mode = when (parts.size) {
            5 -> NfceEmissionMode.ONLINE
            8 -> NfceEmissionMode.OFFLINE
            else -> {
                return NfceQrParseResult.Error(
                    "QR Code versão 2 com quantidade inesperada de parâmetros (${parts.size}).",
                )
            }
        }

        val issueDay = if (mode == NfceEmissionMode.OFFLINE) {
            validateIssueDay(parts[3]) ?: return NfceQrParseResult.Error("Dia de emissão inválido no QR Code offline.")
            parts[3]
        } else {
            null
        }

        val total = if (mode == NfceEmissionMode.OFFLINE) {
            parts[4].toBigDecimalOrNull()
                ?: return NfceQrParseResult.Error("Valor total inválido no QR Code offline.")
        } else {
            null
        }

        return success(
            original = original,
            payload = payload,
            accessKey = accessKey,
            version = 2,
            environment = environment,
            mode = mode,
            issueDay = issueDay,
            total = total,
        )
    }

    private fun parseVersion3(
        original: String,
        payload: String,
        parts: List<String>,
        accessKey: String,
        environment: NfceEnvironment,
    ): NfceQrParseResult {
        val mode = when (parts.size) {
            3 -> NfceEmissionMode.ONLINE
            8 -> NfceEmissionMode.OFFLINE
            else -> {
                return NfceQrParseResult.Error(
                    "QR Code versão 3 com quantidade inesperada de parâmetros (${parts.size}).",
                )
            }
        }

        if (mode == NfceEmissionMode.ONLINE) {
            return success(
                original = original,
                payload = payload,
                accessKey = accessKey,
                version = 3,
                environment = environment,
                mode = mode,
            )
        }

        validateIssueDay(parts[3]) ?: return NfceQrParseResult.Error("Dia de emissão inválido no QR Code offline.")

        val total = parts[4].toBigDecimalOrNull()
            ?: return NfceQrParseResult.Error("Valor total inválido no QR Code offline.")

        val recipientType = parts[5].ifBlank { null }
        if (recipientType != null && recipientType !in setOf("1", "2", "3")) {
            return NfceQrParseResult.Error("Tipo de identificação do destinatário inválido.")
        }

        val recipientId = parts[6].ifBlank { null }

        return success(
            original = original,
            payload = payload,
            accessKey = accessKey,
            version = 3,
            environment = environment,
            mode = mode,
            issueDay = parts[3],
            total = total,
            recipientType = recipientType,
            recipientId = recipientId,
        )
    }

    private fun success(
        original: String,
        payload: String,
        accessKey: String,
        version: Int,
        environment: NfceEnvironment,
        mode: NfceEmissionMode,
        issueDay: String? = null,
        total: BigDecimal? = null,
        recipientType: String? = null,
        recipientId: String? = null,
    ): NfceQrParseResult.Success {
        return NfceQrParseResult.Success(
            NfceQrData(
                originalText = original,
                consultationUrl = original.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) },
                payload = payload,
                accessKey = accessKey,
                qrVersion = version,
                environment = environment,
                emissionMode = mode,
                issueDay = issueDay,
                totalValue = total,
                recipientType = recipientType,
                recipientId = recipientId,
            ),
        )
    }

    private fun extractPayload(text: String): String? {
        if ('|' in text && !text.contains("?")) {
            return text.removePrefix("p=").trim().takeIf { it.isNotBlank() }
        }

        val queryStart = text.indexOf('?')
        if (queryStart < 0) {
            return text.removePrefix("p=").trim().takeIf { '|' in it }
        }

        val query = text.substring(queryStart + 1)
        val pValue = query
            .split('&')
            .firstOrNull { parameter -> parameter.substringBefore('=', "") == "p" }
            ?.substringAfter('=', "")

        return pValue
            ?.let(::percentDecodePreservingPlus)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun percentDecodePreservingPlus(value: String): String {
        val output = StringBuilder(value.length)
        var index = 0

        while (index < value.length) {
            if (value[index] == '%' && index + 2 < value.length) {
                val hex = value.substring(index + 1, index + 3)
                val decoded = hex.toIntOrNull(16)
                if (decoded != null) {
                    output.append(decoded.toChar())
                    index += 3
                    continue
                }
            }

            output.append(value[index])
            index++
        }

        return output.toString()
    }

    private fun validateAccessKey(key: String): String? {
        if (key.length != 44 || key.any { !it.isDigit() }) {
            return "A chave de acesso da NFC-e deve conter exatamente 44 dígitos."
        }

        if (!key.startsWith(MS_UF_CODE)) {
            return "A NFC-e informada não é de Mato Grosso do Sul (código UF 50)."
        }

        if (key.substring(20, 22) != NFCE_MODEL) {
            return "A chave informada não é de NFC-e modelo 65."
        }

        val expectedDigit = calculateCheckDigit(key.substring(0, 43))
        val actualDigit = key.last().digitToInt()

        if (actualDigit != expectedDigit) {
            return "Dígito verificador da chave de acesso inválido."
        }

        return null
    }

    internal fun calculateCheckDigit(first43Digits: String): Int {
        require(first43Digits.length == 43 && first43Digits.all(Char::isDigit))

        var weight = 2
        var sum = 0

        for (char in first43Digits.reversed()) {
            sum += char.digitToInt() * weight
            weight++
            if (weight > 9) weight = 2
        }

        val remainder = sum % 11
        return if (remainder == 0 || remainder == 1) 0 else 11 - remainder
    }

    private fun validateIssueDay(value: String): String? {
        val day = value.toIntOrNull() ?: return null
        return value.takeIf { it.length == 2 && day in 1..31 }
    }
}
