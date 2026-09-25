package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun SuggestionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onSuggestionSelected: (String) -> Unit = onValueChange,
) {
    var focused by remember { mutableStateOf(false) }

    val filtered = remember(value, suggestions) {
        val query = value.trim()
        suggestions
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .filter { candidate ->
                query.isBlank() || candidate.contains(query, ignoreCase = true)
            }
            .take(8)
            .toList()
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { typed ->
                onValueChange(
                    if (keyboardType == KeyboardType.Text) {
                        TextInputRules.capitalizeFirstLetter(typed)
                    } else {
                        typed
                    },
                )
            },
            label = label,
            placeholder = placeholder,
            supportingText = supportingText,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = if (keyboardType == KeyboardType.Text) {
                    KeyboardCapitalization.Sentences
                } else {
                    KeyboardCapitalization.None
                },
                keyboardType = keyboardType,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
        )

        DropdownMenu(
            expanded = focused && filtered.isNotEmpty(),
            onDismissRequest = { focused = false },
            modifier = Modifier.fillMaxWidth(),
        ) {
            filtered.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onSuggestionSelected(suggestion)
                        focused = false
                    },
                )
            }
        }
    }
}
