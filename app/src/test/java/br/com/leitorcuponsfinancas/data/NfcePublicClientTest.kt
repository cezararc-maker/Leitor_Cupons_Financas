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
    fun upgradesOfficialHttpUrlToHttps() {
        val raw = "http://www.dfe.ms.gov.br/nfce/qrcode/?p=abc|2|1"

        val normalized = NfcePublicClient.normalizeOfficialUrl(raw)

        assertEquals("https://www.dfe.ms.gov.br/nfce/qrcode/?p=abc%7C2%7C1", normalized)
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
