package br.com.leitorcuponsfinancas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.leitorcuponsfinancas.data.AppDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeMonthComparison(
    val previousTotal: Double = 0.0,
    val spendingDifference: Double = 0.0,
    val spendingPercent: Double? = null,
    val previousPurchaseCount: Int = 0,
    val purchaseDifference: Int = 0,
)

data class HomeDashboardState(
    val periodLabel: String,
    val analytics: HomeAnalytics = HomeAnalytics(),
    val comparison: HomeMonthComparison = HomeMonthComparison(),
    val reviewCount: Int = 0,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)

    private val today = LocalDate.now()
    private val start = today.withDayOfMonth(1)
    private val end = today.withDayOfMonth(today.lengthOfMonth())
    private val previousStart = start.minusMonths(1)
    private val previousEnd = start.minusDays(1)

    private val currentRows = database.receiptDao()
        .observeHistory(
            startDate = start.toString(),
            endDate = end.toString(),
        )

    private val previousRows = database.receiptDao()
        .observeHistory(
            startDate = previousStart.toString(),
            endDate = previousEnd.toString(),
        )

    private val reviewCount = database.receiptDao()
        .observeItemsNeedingReview()
        .map { it.size }

    val dashboard: StateFlow<HomeDashboardState> =
        combine(
            currentRows,
            previousRows,
            reviewCount,
        ) { current, previous, pendingReview ->
            val currentAnalytics = HomeAnalyticsBuilder.build(current)
            val previousAnalytics = HomeAnalyticsBuilder.build(previous)
            val difference = currentAnalytics.totalSpent - previousAnalytics.totalSpent
            val percent = previousAnalytics.totalSpent
                .takeIf { it > 0.0 }
                ?.let { difference / it * 100.0 }

            HomeDashboardState(
                periodLabel = start.format(
                    DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale("pt", "BR")),
                ).replaceFirstChar { it.uppercase() },
                analytics = currentAnalytics,
                comparison = HomeMonthComparison(
                    previousTotal = previousAnalytics.totalSpent,
                    spendingDifference = difference,
                    spendingPercent = percent,
                    previousPurchaseCount = previousAnalytics.purchaseCount,
                    purchaseDifference =
                        currentAnalytics.purchaseCount - previousAnalytics.purchaseCount,
                ),
                reviewCount = pendingReview,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeDashboardState(
                    periodLabel = start.format(
                        DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale("pt", "BR")),
                    ).replaceFirstChar { it.uppercase() },
                ),
            )
}
