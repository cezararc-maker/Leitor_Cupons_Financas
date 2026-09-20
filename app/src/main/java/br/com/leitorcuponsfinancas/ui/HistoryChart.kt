package br.com.leitorcuponsfinancas.ui

import br.com.leitorcuponsfinancas.data.HistoryItemRow
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class HistoryChartBar(
    val key: String,
    val label: String,
    val start: LocalDate,
    val end: LocalDate,
    val total: BigDecimal,
    val selected: Boolean,
)

data class HistoryChartWindow(
    val start: LocalDate,
    val end: LocalDate,
    val buckets: List<Pair<LocalDate, LocalDate>>,
)

object HistoryChartBuilder {

    fun buildWindow(
        type: HistoryPeriodType,
        anchor: LocalDate,
        currentYear: Int = LocalDate.now().year,
    ): HistoryChartWindow {
        val buckets = when (type) {
            HistoryPeriodType.WEEKLY -> {
                val start = anchor.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY),
                )
                (0L..6L).map { offset ->
                    val day = start.plusDays(offset)
                    day to day
                }
            }

            HistoryPeriodType.MONTHLY -> {
                (1..12).map { month ->
                    val start = LocalDate.of(anchor.year, month, 1)
                    start to start.withDayOfMonth(start.lengthOfMonth())
                }
            }

            HistoryPeriodType.QUARTERLY -> {
                (0..3).map { quarter ->
                    val start = LocalDate.of(anchor.year, quarter * 3 + 1, 1)
                    start to start.plusMonths(3).minusDays(1)
                }
            }

            HistoryPeriodType.SEMIANNUAL -> {
                listOf(1, 7).map { month ->
                    val start = LocalDate.of(anchor.year, month, 1)
                    start to start.plusMonths(6).minusDays(1)
                }
            }

            HistoryPeriodType.ANNUAL -> {
                val endYear = maxOf(currentYear, anchor.year)
                ((endYear - 4)..endYear).map { year ->
                    LocalDate.of(year, 1, 1) to LocalDate.of(year, 12, 31)
                }
            }
        }

        return HistoryChartWindow(
            start = buckets.first().first,
            end = buckets.last().second,
            buckets = buckets,
        )
    }

    fun buildBars(
        type: HistoryPeriodType,
        anchor: LocalDate,
        selectedDay: LocalDate?,
        window: HistoryChartWindow,
        rows: List<HistoryItemRow>,
    ): List<HistoryChartBar> {
        val values = rows.mapNotNull { row ->
            val date = row.issuedDate?.let { value ->
                runCatching { LocalDate.parse(value) }.getOrNull()
            } ?: return@mapNotNull null

            val amount = row.displayTotalAmount
                ?.toBigDecimalOrNull()
                ?: BigDecimal.ZERO

            date to amount
        }

        return window.buckets.mapIndexed { index, bucket ->
            val start = bucket.first
            val end = bucket.second
            val total = values
                .filter { entry ->
                    val date = entry.first
                    !date.isBefore(start) && !date.isAfter(end)
                }
                .fold(BigDecimal.ZERO) { acc, entry -> acc + entry.second }

            HistoryChartBar(
                key = type.name + "-" + start.toString(),
                label = label(type, start, index),
                start = start,
                end = end,
                total = total,
                selected = isSelected(
                    type = type,
                    anchor = anchor,
                    selectedDay = selectedDay,
                    start = start,
                    end = end,
                ),
            )
        }
    }

    private fun isSelected(
        type: HistoryPeriodType,
        anchor: LocalDate,
        selectedDay: LocalDate?,
        start: LocalDate,
        end: LocalDate,
    ): Boolean = when (type) {
        HistoryPeriodType.WEEKLY -> {
            if (selectedDay != null) {
                selectedDay == start
            } else {
                !anchor.isBefore(start) && !anchor.isAfter(end)
            }
        }

        HistoryPeriodType.MONTHLY ->
            anchor.year == start.year && anchor.monthValue == start.monthValue

        HistoryPeriodType.QUARTERLY,
        HistoryPeriodType.SEMIANNUAL ->
            !anchor.isBefore(start) && !anchor.isAfter(end)

        HistoryPeriodType.ANNUAL -> anchor.year == start.year
    }

    private fun label(
        type: HistoryPeriodType,
        start: LocalDate,
        index: Int,
    ): String = when (type) {
        HistoryPeriodType.WEEKLY -> listOf(
            "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom",
        )[index]

        HistoryPeriodType.MONTHLY -> listOf(
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez",
        )[start.monthValue - 1]

        HistoryPeriodType.QUARTERLY -> "T" + (index + 1)
        HistoryPeriodType.SEMIANNUAL -> (index + 1).toString() + "ºS"
        HistoryPeriodType.ANNUAL -> start.year.toString()
    }
}
