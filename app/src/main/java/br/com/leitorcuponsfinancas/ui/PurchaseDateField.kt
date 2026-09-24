package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

internal object PurchaseDateFormatter {
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

    fun fromTyping(value: String): String {
        val digits = value.filter(Char::isDigit).take(8)

        return buildString {
            digits.forEachIndexed { index, char ->
                append(char)
                if (index == 1 && digits.length > 2) append('/')
                if (index == 3 && digits.length > 4) append('/')
            }
        }
    }

    fun parse(value: String): LocalDate? = try {
        LocalDate.parse(value, formatter)
    } catch (_: DateTimeParseException) {
        null
    }

    fun format(value: LocalDate): String = value.format(formatter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PurchaseDateField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    monthsBack: Long = 6,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Data *") },
            placeholder = { Text("DD/MM/AAAA") },
            trailingIcon = {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = "Abrir calendário",
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { showPicker = true },
        )
    }

    if (showPicker) {
        PurchaseDatePickerDialog(
            currentValue = value,
            monthsBack = monthsBack,
            onDismiss = { showPicker = false },
            onConfirm = {
                onValueChange(it)
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseDatePickerDialog(
    currentValue: String,
    monthsBack: Long,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val minimumDate = remember(today, monthsBack) { today.minusMonths(monthsBack) }
    val initialDate = remember(currentValue, today, minimumDate) {
        PurchaseDateFormatter.parse(currentValue)
            ?.takeIf { !it.isBefore(minimumDate) && !it.isAfter(today) }
            ?: today
    }

    val minimumMillis = remember(minimumDate) {
        minimumDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val maximumMillis = remember(today) {
        today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val initialMillis = remember(initialDate) {
        initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    val selectableDates = remember(minimumMillis, maximumMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis in minimumMillis..maximumMillis

            override fun isSelectableYear(year: Int): Boolean =
                year in minimumDate.year..today.year
        }
    }

    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = minimumDate.year..today.year,
        selectableDates = selectableDates,
    )

    var typingMode by rememberSaveable { mutableStateOf(false) }
    var typedDate by rememberSaveable(currentValue) {
        mutableStateOf(
            currentValue
                .takeIf { PurchaseDateFormatter.parse(it) != null }
                ?: PurchaseDateFormatter.format(initialDate),
        )
    }

    val typedParsed = PurchaseDateFormatter.parse(typedDate)
    val typedValid = typedParsed != null &&
        !typedParsed.isBefore(minimumDate) &&
        !typedParsed.isAfter(today)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = if (typingMode) {
                    typedValid
                } else {
                    state.selectedDateMillis != null
                },
                onClick = {
                    val selected = if (typingMode) {
                        typedParsed
                    } else {
                        state.selectedDateMillis?.let { millis ->
                            Instant
                                .ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                    }

                    selected?.let {
                        onConfirm(PurchaseDateFormatter.format(it))
                    }
                },
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Data da compra",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Disponível: últimos $monthsBack meses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                TextButton(
                    onClick = { typingMode = !typingMode },
                ) {
                    Text(if (typingMode) "Calendário" else "Digitar data")
                }
            }

            if (typingMode) {
                OutlinedTextField(
                    value = typedDate,
                    onValueChange = {
                        typedDate = PurchaseDateFormatter.fromTyping(it)
                    },
                    label = { Text("DD/MM/AAAA") },
                    placeholder = { Text("DD/MM/AAAA") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    singleLine = true,
                    supportingText = {
                        when {
                            typedDate.length < 10 -> Text("Digite dia, mês e ano. As barras são automáticas.")
                            !typedValid -> Text("Escolha uma data entre ${PurchaseDateFormatter.format(minimumDate)} e ${PurchaseDateFormatter.format(today)}.")
                            else -> Text("Data válida.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            } else {
                DatePicker(
                    state = state,
                    showModeToggle = false,
                )
            }
        }
    }
}
