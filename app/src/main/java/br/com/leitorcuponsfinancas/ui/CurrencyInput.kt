package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import java.math.BigDecimal
import java.math.RoundingMode

internal object CurrencyInputFormatter {

    fun fromTyping(value: String): String {
        val digits = value
            .filter(Char::isDigit)
            .trimStart('0')

        if (digits.isEmpty()) return ""

        val padded = digits.padStart(3, '0')
        val integerPart = padded.dropLast(2)
        val centsPart = padded.takeLast(2)

        return "${groupThousands(integerPart)},$centsPart"
    }

    fun fromStoredDecimal(value: String?): String {
        val decimal = parse(value) ?: return ""
        return format(decimal)
    }

    fun displayValue(value: String): String =
        value.ifBlank { "0,00" }

    fun parse(value: String?): BigDecimal? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        val cleaned = trimmed.replace(Regex("""[^0-9,.-]"""), "")
        if (cleaned.isBlank()) return null

        val normalized = when {
            cleaned.contains(",") -> cleaned
                .replace(".", "")
                .replace(",", ".")

            else -> cleaned
        }

        return normalized.toBigDecimalOrNull()
    }

    fun format(value: BigDecimal): String {
        val scaled = value.setScale(2, RoundingMode.HALF_UP)
        val plain = scaled.abs().toPlainString()
        val parts = plain.split(".")
        val integerPart = parts.firstOrNull().orEmpty().ifBlank { "0" }
        val centsPart = parts.getOrNull(1).orEmpty().padEnd(2, '0').take(2)
        val sign = if (scaled.signum() < 0) "-" else ""

        return "$sign${groupThousands(integerPart)},$centsPart"
    }

    private fun groupThousands(value: String): String {
        val clean = value.trimStart('0').ifBlank { "0" }
        return clean
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()
    }
}

@Composable
internal fun CurrencyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
) {
    val displayedValue = CurrencyInputFormatter.displayValue(value)

    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = displayedValue,
                selection = TextRange(displayedValue.length),
            ),
        )
    }

    LaunchedEffect(displayedValue) {
        if (fieldValue.text != displayedValue) {
            fieldValue = TextFieldValue(
                text = displayedValue,
                selection = TextRange(displayedValue.length),
            )
        } else if (fieldValue.selection.end != displayedValue.length) {
            fieldValue = fieldValue.copy(
                selection = TextRange(displayedValue.length),
            )
        }
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { typed ->
            if (typed.text == fieldValue.text) {
                fieldValue = typed.copy(
                    selection = TextRange(typed.text.length),
                )
            } else {
                val formatted = CurrencyInputFormatter.fromTyping(typed.text)
                val nextDisplay = CurrencyInputFormatter.displayValue(formatted)

                fieldValue = TextFieldValue(
                    text = nextDisplay,
                    selection = TextRange(nextDisplay.length),
                )

                if (formatted != value) {
                    onValueChange(formatted)
                }
            }
        },
        label = label,
        prefix = { Text("R$") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
        enabled = enabled,
        singleLine = singleLine,
        modifier = modifier,
    )
}
