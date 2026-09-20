package br.com.leitorcuponsfinancas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcePageParserTest {

    @Test
    fun parsesPublicMsReceiptHtml() {
        val html = """
            <html>
              <body>
                <div class="txtCenter">
                  <div id="u20" class="txtTopo">MERCADO TESTE LTDA</div>
                  <div class="text">CNPJ: 12.345.678/0001-90</div>
                  <div class="text">RUA EXEMPLO, 123, CENTRO, CAMPO GRANDE, MS</div>
                </div>
                <table id="tabResult">
                  <tr id="Item + 1">
                    <td>
                      <span class="txtTit">ARROZ TIPO 1</span>
                      <span class="RCod">(Código: 123)</span>
                      <span class="Rqtd"><strong>Qtde.:</strong>2</span>
                      <span class="RUN"><strong>UN:</strong> UN</span>
                      <span class="RvlUnit"><strong>Vl. Unit.:</strong> 12,50</span>
                    </td>
                    <td><span class="valor">25,00</span></td>
                  </tr>
                  <tr id="Item + 2">
                    <td>
                      <span class="txtTit">FEIJAO CARIOCA</span>
                      <span class="RCod">(Código: 456)</span>
                      <span class="Rqtd"><strong>Qtde.:</strong>1</span>
                      <span class="RUN"><strong>UN:</strong> UN</span>
                      <span class="RvlUnit"><strong>Vl. Unit.:</strong> 8,75</span>
                    </td>
                    <td><span class="valor">8,75</span></td>
                  </tr>
                </table>
                <div id="totalNota">
                  <div id="linhaTotal"><label>Qtd. total de itens:</label><span class="totalNumb">2</span></div>
                  <div id="linhaTotal"><label>Valor a pagar R$:</label><span class="totalNumb">33,75</span></div>
                </div>
                <div>EMISSÃO NORMAL Número: 251857 Série: 510 Emissão: 19/09/2026 17:45:28 - Via Consumidor</div>
              </body>
            </html>
        """.trimIndent()

        val result = NfcePageParser.parse(html, "https://www.dfe.ms.gov.br/nfce/qrcode/?p=teste")

        assertTrue(result is NfcePageParseResult.Success)
        val receipt = (result as NfcePageParseResult.Success).receipt
        assertEquals("MERCADO TESTE LTDA", receipt.merchantName)
        assertEquals("12.345.678/0001-90", receipt.merchantCnpj)
        assertEquals("251857", receipt.number)
        assertEquals("510", receipt.series)
        assertEquals("19/09/2026 17:45:28", receipt.issuedAt)
        assertEquals("33.75", receipt.totalAmount?.toPlainString())
        assertEquals(2, receipt.items.size)
        assertEquals("ARROZ TIPO 1", receipt.items[0].description)
        assertEquals("123", receipt.items[0].code)
        assertEquals("2", receipt.items[0].quantity?.toPlainString())
        assertEquals("12.50", receipt.items[0].unitPrice?.toPlainString())
        assertEquals("25.00", receipt.items[0].total?.toPlainString())
    }

    @Test
    fun reportsAdditionalValidationInsteadOfParsingIt() {
        val html = "<html><body>Consulta NFC-e Código da Imagem CAPTCHA</body></html>"

        val result = NfcePageParser.parse(html, "https://www.dfe.ms.gov.br/nfce/qrcode/")

        assertTrue(result is NfcePageParseResult.Error)
        assertTrue((result as NfcePageParseResult.Error).message.contains("validação adicional"))
    }
}
