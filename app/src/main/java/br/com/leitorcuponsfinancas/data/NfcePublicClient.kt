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
    private const val MAX_REDIRECTS = 5

    suspend fun fetch(url: String): NfcePageParseResult = withContext(Dispatchers.IO) {
        var currentUrl = normalizeOfficialUrl(url)
            ?: return@withContext NfcePageParseResult.Error(
                "A consulta automática só é permitida para o domínio oficial dfe.ms.gov.br.",
            )

        val cookies = mutableMapOf<String, String>()

        try {
            repeat(MAX_REDIRECTS + 1) { redirectCount ->
                val response = Jsoup.connect(currentUrl)
                    .userAgent(
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                            "Chrome/140.0 Mobile Safari/537.36 LeitorCuponsFinancas/0.1",
                    )
                    .timeout(20_000)
                    .followRedirects(false)
                    .ignoreHttpErrors(true)
                    .cookies(cookies)
                    .method(Connection.Method.GET)
                    .execute()

                cookies.putAll(response.cookies())

                if (response.statusCode() in 300..399) {
                    if (redirectCount >= MAX_REDIRECTS) {
                        return@withContext NfcePageParseResult.Error(
                            "A SEFAZ-MS excedeu o limite de redirecionamentos da consulta.",
                        )
                    }

                    val location = response.header("Location")
                        ?: return@withContext NfcePageParseResult.Error(
                            "A SEFAZ-MS retornou um redirecionamento sem endereço de destino.",
                        )

                    val resolved = resolveRedirect(
                        currentUrl = currentUrl,
                        location = location,
                    ) ?: return@withContext NfcePageParseResult.Error(
                        "A consulta foi redirecionada para um endereço inválido.",
                    )

                    currentUrl = normalizeOfficialUrl(resolved)
                        ?: return@withContext NfcePageParseResult.Error(
                            "A consulta foi redirecionada para um endereço fora do domínio oficial da SEFAZ-MS.",
                        )

                    return@repeat
                }

                val finalUrl = normalizeOfficialUrl(response.url().toExternalForm())
                    ?: return@withContext NfcePageParseResult.Error(
                        "A consulta terminou em um endereço fora do domínio oficial da SEFAZ-MS.",
                    )

                if (response.statusCode() !in 200..299) {
                    return@withContext NfcePageParseResult.Error(
                        "A SEFAZ-MS retornou HTTP ${response.statusCode()} ao consultar a NFC-e.",
                    )
                }

                return@withContext NfcePageParser.parse(
                    html = response.body(),
                    sourceUrl = finalUrl,
                )
            }

            NfcePageParseResult.Error(
                "Não foi possível concluir os redirecionamentos da consulta da SEFAZ-MS.",
            )
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

    internal fun resolveRedirect(
        currentUrl: String,
        location: String,
    ): String? {
        val current = runCatching { URI(currentUrl) }.getOrNull() ?: return null
        val safeLocation = location
            .trim()
            .replace("|", "%7C")
            .replace(" ", "%20")

        return runCatching {
            current.resolve(safeLocation).toString()
        }.getOrNull()
    }
}
