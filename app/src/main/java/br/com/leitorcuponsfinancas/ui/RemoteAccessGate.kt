package br.com.leitorcuponsfinancas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.leitorcuponsfinancas.data.RemoteAccessDecision
import br.com.leitorcuponsfinancas.data.RemoteAccessManager
import br.com.leitorcuponsfinancas.data.RemoteAccessState

@Composable
fun RemoteAccessGate(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val manager = remember {
        RemoteAccessManager.getInstance(context)
    }
    val state by manager.state.collectAsStateWithLifecycle()

    LaunchedEffect(manager) {
        manager.start()
    }

    if (state.canUseApp) {
        content()
        return
    }

    when (state.decision) {
        RemoteAccessDecision.SIGNED_OUT -> RemoteAccessLoginScreen(
            state = state,
            onSignIn = manager::signIn,
        )

        RemoteAccessDecision.LOADING -> RemoteAccessLoadingScreen(
            message = state.message ?: "Verificando acesso...",
        )

        else -> RemoteAccessStatusScreen(
            state = state,
            onRetry = manager::refresh,
            onSignOut = manager::signOut,
        )
    }
}

@Composable
private fun RemoteAccessLoginScreen(
    state: RemoteAccessState,
    onSignIn: (String, String) -> Unit,
) {
    var email by remember(state.email) {
        mutableStateOf(state.email.orEmpty())
    }
    var password by remember {
        mutableStateOf("")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Leitor Cupons Finanças",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Entre com a conta liberada para este aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("E-mail") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                ),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Senha") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                ),
            )

            if (!state.message.isNullOrBlank()) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = {
                    onSignIn(email, password)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && password.isNotBlank(),
            ) {
                Text("Entrar")
            }
        }
    }
}

@Composable
private fun RemoteAccessLoadingScreen(
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun RemoteAccessStatusScreen(
    state: RemoteAccessState,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    val title = when (state.decision) {
        RemoteAccessDecision.ACCOUNT_PENDING -> "Conta aguardando liberação"
        RemoteAccessDecision.SUSPENDED -> "Acesso suspenso"
        RemoteAccessDecision.BLOCKED -> "Acesso bloqueado"
        RemoteAccessDecision.DEVICE_PENDING -> "Celular aguardando autorização"
        RemoteAccessDecision.DEVICE_REVOKED -> "Celular não autorizado"
        RemoteAccessDecision.ONLINE_REQUIRED -> "Validação online necessária"
        RemoteAccessDecision.CONFIGURATION_ERROR -> "Configuração incompleta"
        else -> "Não foi possível liberar o acesso"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )

            state.email
                ?.takeIf { it.isNotBlank() }
                ?.let { currentEmail ->
                    Text(
                        text = currentEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            Text(
                text = state.message ?: "Tente novamente quando houver conexão.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Tentar novamente")
            }

            TextButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sair da conta")
            }
        }
    }
}
