package br.com.leitorcuponsfinancas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardReceiptParserTest {

    @Test
    fun parsesDebitStoneStyleReceipt() {
        val text = """
            VIA LOJISTA - VENDA
            PAO DE QUEIJO
            CNPJ: 12.345.678/0001-90     28/09/2026 • 19:07
            R. EXEMPLO, 100, CENTRO
            DÉBITO                 R$ 14,25
            VISA - ******1234
            PAYWAVE/VISA
            Aprovado sem senha
            VISA DEBITO
            ONL - Contactless EMV
        """.trimIndent()

        val result = CardReceiptParser.parse(text)

        assertEquals("PAO DE QUEIJO", result.merchantName)
        assertEquals("12345678000190", result.merchantCnpj)
        assertEquals("28/09/2026 19:07", result.issuedAt)
        assertEquals("14.25", result.totalAmount)
        assertEquals(CardReceiptPaymentMethod.DEBIT, result.paymentMethod)
        assertEquals("Visa", result.cardBrand)
        assertEquals("1234", result.cardLast4)
        assertEquals(CreditInstallmentMode.UNKNOWN, result.creditMode)
        assertNull(result.installmentCount)
    }

    @Test
    fun parsesCreditInstallments() {
        val text = """
            VIA CLIENTE
            MERCADO TESTE
            CNPJ 98.765.432/0001-10
            15/09/2026 12:30
            CRÉDITO R$ 120,00
            MASTERCARD ******9876
            3X
        """.trimIndent()

        val result = CardReceiptParser.parse(text)

        assertEquals(CardReceiptPaymentMethod.CREDIT, result.paymentMethod)
        assertEquals(CreditInstallmentMode.INSTALLMENT, result.creditMode)
        assertEquals(3, result.installmentCount)
        assertEquals("Mastercard", result.cardBrand)
        assertEquals("9876", result.cardLast4)
        assertEquals("120.00", result.totalAmount)
    }

    @Test
    fun creditWithoutInstallmentInformationRemainsPending() {
        val text = """
            LOJA TESTE
            CNPJ 11.222.333/0001-44
            10/09/2026 10:20
            CRÉDITO R$ 50,00
            VISA ******4321
        """.trimIndent()

        val result = CardReceiptParser.parse(text)

        assertEquals(CardReceiptPaymentMethod.CREDIT, result.paymentMethod)
        assertEquals(CreditInstallmentMode.UNKNOWN, result.creditMode)
        assertNull(result.installmentCount)
    }
}
