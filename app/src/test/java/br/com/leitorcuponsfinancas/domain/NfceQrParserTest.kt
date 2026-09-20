package br.com.leitorcuponsfinancas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NfceQrParserTest {

    @Test
    fun parsesVersion3OnlineForMs() {
        val key = validMsNfceKey()
        val url = "https://www.dfe.ms.gov.br/nfce/qrcode?p=$key|3|1"

        val result = NfceQrParser.parse(url)

        assertTrue(result is NfceQrParseResult.Success)
        val data = (result as NfceQrParseResult.Success).data
        assertEquals(key, data.accessKey)
        assertEquals(3, data.qrVersion)
        assertEquals(NfceEnvironment.PRODUCTION, data.environment)
        assertEquals(NfceEmissionMode.ONLINE, data.emissionMode)
    }

    @Test
    fun parsesVersion2OnlineForMs() {
        val key = validMsNfceKey()
        val hash = "A".repeat(40)
        val payload = "$key|2|1|1|$hash"

        val result = NfceQrParser.parse(payload)

        assertTrue(result is NfceQrParseResult.Success)
        val data = (result as NfceQrParseResult.Success).data
        assertEquals(2, data.qrVersion)
        assertEquals(NfceEmissionMode.ONLINE, data.emissionMode)
    }

    @Test
    fun parsesVersion3OfflineAndPreservesPlusInSignature() {
        val key = validMsNfceKey()
        val url = "https://www.dfe.ms.gov.br/nfce/qrcode?p=$key%7C3%7C2%7C09%7C123.45%7C2%7C12345678901%7CabC+deF%2Fghi%3D"

        val result = NfceQrParser.parse(url)

        assertTrue(result is NfceQrParseResult.Success)
        val data = (result as NfceQrParseResult.Success).data
        assertEquals(NfceEnvironment.HOMOLOGATION, data.environment)
        assertEquals(NfceEmissionMode.OFFLINE, data.emissionMode)
        assertEquals("09", data.issueDay)
        assertEquals("123.45", data.totalValue?.toPlainString())
    }

    @Test
    fun rejectsKeyFromAnotherState() {
        val msKey = validMsNfceKey()
        val otherUfWithoutDigit = "35" + msKey.substring(2, 43)
        val otherUfKey = otherUfWithoutDigit + NfceQrParser.calculateCheckDigit(otherUfWithoutDigit)

        val result = NfceQrParser.parse("$otherUfKey|3|1")

        assertTrue(result is NfceQrParseResult.Error)
        assertTrue((result as NfceQrParseResult.Error).message.contains("Mato Grosso do Sul"))
    }

    @Test
    fun rejectsInvalidCheckDigit() {
        val key = validMsNfceKey()
        val wrongDigit = if (key.last() == '9') '0' else key.last() + 1
        val invalidKey = key.dropLast(1) + wrongDigit

        val result = NfceQrParser.parse("$invalidKey|3|1")

        assertTrue(result is NfceQrParseResult.Error)
        assertTrue((result as NfceQrParseResult.Error).message.contains("Dígito verificador"))
    }

    private fun validMsNfceKey(): String {
        val first43 = "5026091234567800019565001000000123112345678"
        return first43 + NfceQrParser.calculateCheckDigit(first43)
    }
}
