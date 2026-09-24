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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeDashboardState(
    val periodLabel: String,
    val analytics: HomeAnalytics = HomeAnalytics(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)

    private val today = LocalDate.now()
    private val start = today.withDayOfMonth(1)
    private val end = today.withDayOfMonth(today.lengthOfMonth())

    val dashboard: StateFlow<HomeDashboardState> =
        database.receiptDao()
            .observeHistory(
                startDate = start.toString(),
                endDate = end.toString(),
            )
            .map { rows ->
                HomeDashboardState(
                    periodLabel = start.format(
                        DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale("pt", "BR")),
                    ).replaceFirstChar { it.uppercase() },
                    analytics = HomeAnalyticsBuilder.build(rows),
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
