package br.com.leitorcuponsfinancas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.leitorcuponsfinancas.ui.HistoryScreen
import br.com.leitorcuponsfinancas.ui.ManualEntryScreen
import br.com.leitorcuponsfinancas.ui.ProfileScreen
import br.com.leitorcuponsfinancas.ui.NfceScreen
import br.com.leitorcuponsfinancas.ui.ProductScreen
import br.com.leitorcuponsfinancas.ui.ProductViewModel
import br.com.leitorcuponsfinancas.ui.theme.LeitorCuponsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LeitorCuponsTheme {
                Surface(Modifier.fillMaxSize()) {
                    LeitorCuponsApp()
                }
            }
        }
    }
}

private enum class AppScreen {
    HOME,
    PRODUCTS,
    NFCE,
    HISTORY,
    MANUAL,
    PROFILE,
}

@Composable
private fun LeitorCuponsApp(
    productViewModel: ProductViewModel = viewModel(),
) {
    val products by productViewModel.products.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Leitor Cupons Finanças",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Mato Grosso do Sul • MVP",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        HorizontalDivider()

        when (screen) {
            AppScreen.HOME -> HomeScreen(
                productCount = products.size,
                onProducts = { screen = AppScreen.PRODUCTS },
                onNfce = { screen = AppScreen.NFCE },
                onHistory = { screen = AppScreen.HISTORY },
                onManual = { screen = AppScreen.MANUAL },
                onProfile = { screen = AppScreen.PROFILE },
            )

            AppScreen.PRODUCTS -> Column(Modifier.fillMaxSize()) {
                Button(
                    onClick = { screen = AppScreen.HOME },
                    modifier = Modifier.padding(12.dp),
                ) {
                    Text("Voltar")
                }

                ProductScreen(
                    products = products,
                    onSave = productViewModel::save,
                    onDeactivate = productViewModel::deactivate,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            AppScreen.NFCE -> NfceScreen(
                onBack = { screen = AppScreen.HOME },
                modifier = Modifier.fillMaxSize(),
            )

            AppScreen.HISTORY -> HistoryScreen(
                onBack = { screen = AppScreen.HOME },
                modifier = Modifier.fillMaxSize(),
            )

            AppScreen.MANUAL -> ManualEntryScreen(
                onBack = { screen = AppScreen.HOME },
                modifier = Modifier.fillMaxSize(),
            )

            AppScreen.PROFILE -> ProfileScreen(
                onBack = { screen = AppScreen.HOME },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun HomeScreen(
    productCount: Int,
    onProducts: () -> Unit,
    onNfce: () -> Unit,
    onHistory: () -> Unit,
    onManual: () -> Unit,
    onProfile: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Controle seus gastos a partir dos itens realmente comprados.",
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = "Produtos no cadastro mestre: $productCount",
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            text = "O cadastro mestre guarda os produtos usados para classificação e vínculos. Os itens importados das NFC-e ficam em Histórico e gastos.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            onClick = onProducts,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cadastro mestre de produtos")
        }

        Button(
            onClick = onNfce,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ler NFC-e por QR Code")
        }

        Button(
            onClick = onHistory,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Histórico e gastos")
        }

        Button(
            onClick = onManual,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Adicionar compra manual")
        }

        Button(
            onClick = onProfile,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Perfil do usuário")
        }
    }
}
