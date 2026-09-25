package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = viewModel(),
) {
    val profile by profileViewModel.profile.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf(profile.displayName) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profile.displayName) {
        name = profile.displayName
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {

        Text("Perfil local", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Este nome fica registrado nas inclusões e correções feitas neste aparelho. No futuro, o compartilhamento usará a conta autenticada.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = TextInputRules.capitalizeFirstLetter(it)
                savedMessage = null
            },
            label = { Text("Nome do usuário") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            enabled = name.isNotBlank(),
            onClick = {
                profileViewModel.saveName(name)
                savedMessage = "Perfil salvo."
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Salvar perfil") }

        savedMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
