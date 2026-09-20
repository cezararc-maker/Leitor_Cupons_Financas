package br.com.leitorcuponsfinancas.ui

import br.com.leitorcuponsfinancas.data.HistoryItemRow
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryChartBuilderTest {

    @Test
    fun monthlyChartUsesIssuedDateAsCompetence() {
        val anchor = LocalDate.of(2026, 9, 20)
        val window = HistoryChartBuilder.buildWindow(
            type = HistoryPeriodType.MONTHLY,
            anchor = anchor,
            currentYear = 2026,
        )

        val rows = listOf(
            row(id = 1, issuedDate = "2026-08-31", total = "25.00"),
            row(id = 2, issuedDate = "2026-09-01", total = "40.00"),
            row(id = 3, issuedDate = "2026-09-15", total = "10.00"),
        )

        val bars = HistoryChartBuilder.buildBars(
            type = HistoryPeriodType.MONTHLY,
            anchor = anchor,
            selectedDay = null,
            window = window,
            rows = rows,
        )

        assertEquals(BigDecimal("25.00"), bars[7].total)
        assertEquals(BigDecimal("50.00"), bars[8].total)
        assertTrue(bars[8].selected)
    }

    @Test
    fun chartUsesCorrectedTotalWhenItemWasEdited() {
        val anchor = LocalDate.of(2026, 9, 20)
        val window = HistoryChartBuilder.buildWindow(
            type = HistoryPeriodType.MONTHLY,
            anchor = anchor,
            currentYear = 2026,
        )

        val edited = row(
            id = 1,
            issuedDate = "2026-09-10",
            total = "20.00",
            correctedTotal = "18.50",
        )

        val bars = HistoryChartBuilder.buildBars(
            type = HistoryPeriodType.MONTHLY,
            anchor = anchor,
            selectedDay = null,
            window = window,
            rows = listOf(edited),
        )

        assertEquals(BigDecimal("18.50"), bars[8].total)
    }

    @Test
    fun weeklyChartBuildsMondayThroughSunday() {
        val anchor = LocalDate.of(2026, 9, 20)
        val window = HistoryChartBuilder.buildWindow(
            type = HistoryPeriodType.WEEKLY,
            anchor = anchor,
            currentYear = 2026,
        )

        assertEquals(LocalDate.of(2026, 9, 14), window.start)
        assertEquals(LocalDate.of(2026, 9, 20), window.end)
        assertEquals(7, window.buckets.size)

        val bars = HistoryChartBuilder.buildBars(
            type = HistoryPeriodType.WEEKLY,
            anchor = anchor,
            selectedDay = LocalDate.of(2026, 9, 18),
            window = window,
            rows = emptyList(),
        )

        assertEquals(listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"), bars.map { it.label })
        assertTrue(bars[4].selected)
    }

    private fun row(
        id: Long,
        issuedDate: String,
        total: String,
        correctedTotal: String? = null,
    ) = HistoryItemRow(
        itemId = id,
        receiptId = id,
        issuedDate = issuedDate,
        issuedAt = issuedDate,
        merchantName = "Teste",
        merchantCnpj = null,
        receiptNumber = null,
        receiptSeries = null,
        sourceType = "NFCE",
        createdByName = null,
        fiscalDescription = "ITEM $id",
        itemCode = null,
        quantity = "1",
        unit = "UN",
        unitPrice = total,
        totalAmount = total,
        productId = null,
        productName = null,
        sector = null,
        category = null,
        subcategory = null,
        correctedDescription = null,
        correctedQuantity = null,
        correctedUnit = null,
        correctedUnitPrice = null,
        correctedTotalAmount = correctedTotal,
        correctedByName = null,
        correctedAt = correctedTotal?.let { 1L },
    )
}
