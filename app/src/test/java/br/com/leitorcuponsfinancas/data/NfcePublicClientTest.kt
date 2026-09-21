package br.com.leitorcuponsfinancas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NfcePublicClientTest {

    @Test
    fun normalizesOfficialUrlWithRawQrSeparators() {
        val raw = "https://www.dfe.ms.gov.br/nfce/qrcode/?p=50123456789012345678901234567890123456789012|2|1|1|ABCDEF"

        val normalized = NfcePublicClient.normalizeOfficialUrl(raw)

        assertEquals(
            "https://www.dfe.ms.gov.br/nfce/qrcode/?p=50123456789012345678901234567890123456789012%7C2%7C1%7C1%7CABCDEF",
            normalized,
        )
    }

    @Test
    fun canonicalizesQrEndpointWithoutSlashBeforeQuery() {
        val raw = "https://www.dfe.ms.gov.br/nfce/qrcode?p=abc|3|1"

        val normalized = NfcePublicClient.normalizeOfficialUrl(raw)

        assertEquals(
            "https://www.dfe.ms.gov.br/nfce/qrcode/?p=abc%7C3%7C1",
            normalized,
        )
    }

    @Test
    fun upgradesOfficialHttpUrlToHttps() {
        val raw = "http://www.dfe.ms.gov.br/nfce/qrcode/?p=abc|2|1"

        val normalized = NfcePublicClient.normalizeOfficialUrl(raw)

        assertEquals("https://www.dfe.ms.gov.br/nfce/qrcode/?p=abc%7C2%7C1", normalized)
    }

    @Test
    fun resolvesRelativeRedirectInsideOfficialHost() {
        val current = "https://www.dfe.ms.gov.br/nfce/qrcode/?p=abc%7C2%7C1"

        val resolved = NfcePublicClient.resolveRedirect(
            currentUrl = current,
            location = "/nfce/qrcode/?p=def|2|1",
        )

        assertEquals(
            "https://www.dfe.ms.gov.br/nfce/qrcode/?p=def%7C2%7C1",
            resolved,
        )
    }

    @Test
    fun upgradesHttpRedirectBackToHttps() {
        val redirected = NfcePublicClient.normalizeOfficialUrl(
            "http://www.dfe.ms.gov.br/nfce/qrcode/?p=abc|2|1",
        )

        assertEquals(
            "https://www.dfe.ms.gov.br/nfce/qrcode/?p=abc%7C2%7C1",
            redirected,
        )
    }

    @Test
    fun followsOfficialMetaRefresh() {
        val html = """
            <html>
              <head>
                <meta http-equiv="refresh" content="0;url=/nfce/qrcode/?p=abc|3|1">
              </head>
            </html>
        """.trimIndent()

        val redirected = NfcePublicClient.findHtmlRedirect(
            html = html,
            currentUrl = "https://www.dfe.ms.gov.br/nfce/qrcode?p=abc%7C3%7C1",
        )

        assertEquals(
            "https://www.dfe.ms.gov.br/nfce/qrcode/?p=abc%7C3%7C1",
            redirected,
        )
    }

    @Test
    fun followsOfficialJavascriptRedirect() {
        val html = """
            <html>
              <body>
                <script>window.location.href = "/nfce/qrcode/?p=abc|3|1";</script>
              </body>
            </html>
        """.trimIndent()

        val redirected = NfcePublicClient.findHtmlRedirect(
            html = html,
            currentUrl = "https://www.dfe.ms.gov.br/nfce/qrcode?p=abc%7C3%7C1",
        )

        assertEquals(
            "https://www.dfe.ms.gov.br/nfce/qrcode/?p=abc%7C3%7C1",
            redirected,
        )
    }

    @Test
    fun rejectsLookalikeAndForeignHosts() {
        assertNull(
            NfcePublicClient.normalizeOfficialUrl(
                "https://www.dfe.ms.gov.br.exemplo.com/nfce/qrcode/?p=abc|2|1",
            ),
        )
        assertNull(NfcePublicClient.normalizeOfficialUrl("https://example.com/nfce/qrcode/?p=abc|2|1"))
    }
}
