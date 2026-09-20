package br.com.leitorcuponsfinancas.data

import br.com.leitorcuponsfinancas.domain.NfcePageParseResult
import br.com.leitorcuponsfinancas.domain.NfcePageParser
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup

object NfcePublicClient {

    private const val OFFICIAL_HOST = "www.dfe.ms.gov.br"
    private const val ALT_OFFICIAL_HOST = "dfe.ms.gov.br"

    suspend fun fetch(url: String): NfcePageParseResult = withContext(Dispatchers.IO) {
        val safeUrl = normalizeOfficialUrl(url)
            ?: return@withContext NfcePageParseResult.Error(
                "A consulta automática só é permitida para o domínio oficial dfe.ms.gov.br.",
            )

        try {
            val response = Jsoup.connect(safeUrl)
                .userAgent(
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                        "Chrome/140.0 Mobile Safari/537.36 LeitorCuponsFinancas/0.1",
                )
                .timeout(20_000)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .method(Connection.Method.GET)
                .execute()

            val finalUrl = response.url().toExternalForm()
            if (normalizeOfficialUrl(finalUrl) == null) {
                return@withContext NfcePageParseResult.Error(
                    "A consulta foi redirecionada para um endereço fora do domínio oficial da SEFAZ-MS.",
                )
            }

            if (response.statusCode() !in 200..299) {
                return@withContext NfcePageParseResult.Error(
                    "A SEFAZ-MS retornou HTTP ${response.statusCode()} ao consultar a NFC-e.",
                )
            }

            NfcePageParser.parse(response.body(), finalUrl)
        } catch (error: Exception) {
            NfcePageParseResult.Error(
                "Não foi possível consultar a SEFAZ-MS: ${error.message ?: error::class.java.simpleName}",
            )
        }
    }

    internal fun normalizeOfficialUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return null

        val upgraded = when {
            trimmed.startsWith("http://", ignoreCase = true) ->
                "https://" + trimmed.substringAfter("://")
            else -> trimmed
        }

        val encodedUrl = upgraded
            .replace("|", "%7C")
            .replace(" ", "%20")

        val uri = runCatching { URI(encodedUrl) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null

        val host = uri.host?.lowercase() ?: return null
        if (host != OFFICIAL_HOST && host != ALT_OFFICIAL_HOST) return null

        val path = uri.path.orEmpty()
        if (!path.startsWith("/nfce/qrcode", ignoreCase = true)) return null

        return encodedUrl
    }
}
