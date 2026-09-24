package br.com.leitorcuponsfinancas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.leitorcuponsfinancas.ui.BackArrowButton
import br.com.leitorcuponsfinancas.ui.BackupScreen
import br.com.leitorcuponsfinancas.ui.HistoryScreen
import br.com.leitorcuponsfinancas.ui.ManualEntryScreen
import br.com.leitorcuponsfinancas.ui.NfceScreen
import br.com.leitorcuponsfinancas.ui.ProductScreen
import br.com.leitorcuponsfinancas.ui.ProductViewModel
import br.com.leitorcuponsfinancas.ui.ProfileScreen
import br.com.leitorcuponsfinancas.ui.ReceiptOcrScreen
import br.com.leitorcuponsfinancas.ui.theme.LeitorCuponsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LeitorCuponsTheme {
                Surface(Modifier.fillMaxSize()) { LeitorCuponsApp() }
            }
        }
    }
}

private enum class AppScreen { HOME, PRODUCTS, NFCE, OCR, HISTORY, MANUAL, PROFILE, BACKUP }

@Composable
private fun LeitorCuponsApp(productViewModel: ProductViewModel = viewModel()) {
    val products by productViewModel.products.collectAsStateWithLifecycle()
    val learnedLinks by productViewModel.learnedLinks.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var addMenuOpen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = addMenuOpen || screen != AppScreen.HOME) {
        if (addMenuOpen) addMenuOpen = false else screen = AppScreen.HOME
    }

    Scaffold(
        bottomBar = {
            AppBottomBar(
                current = screen,
                addMenuOpen = addMenuOpen,
                onHome = { addMenuOpen = false; screen = AppScreen.HOME },
                onHistory = { addMenuOpen = false; screen = AppScreen.HISTORY },
                onAdd = { addMenuOpen = !addMenuOpen },
                onProducts = { addMenuOpen = false; screen = AppScreen.PRODUCTS },
                onProfile = { addMenuOpen = false; screen = AppScreen.PROFILE },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(Modifier.fillMaxSize()) {
                AppHeader()
                HorizontalDivider()
                when (screen) {
                    AppScreen.HOME -> HomeScreen(
                        productCount = products.size,
                        onHistory = { screen = AppScreen.HISTORY },
                        onProducts = { screen = AppScreen.PRODUCTS },
                        onBackup = { screen = AppScreen.BACKUP },
                    )
                    else -> SecondaryScreenScaffold(onBack = { screen = AppScreen.HOME }) {
                        when (screen) {
                            AppScreen.PRODUCTS -> ProductScreen(
                                products = products,
                                learnedLinks = learnedLinks,
                                onSave = productViewModel::save,
                                onDeactivate = productViewModel::deactivate,
                                modifier = Modifier.fillMaxSize(),
                            )
                            AppScreen.NFCE -> NfceScreen(
                                onBack = { screen = AppScreen.HOME },
                                modifier = Modifier.fillMaxSize(),
                            )
                            AppScreen.OCR -> ReceiptOcrScreen(modifier = Modifier.fillMaxSize())
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
                            AppScreen.BACKUP -> BackupScreen(
                                onBack = { screen = AppScreen.HOME },
                                modifier = Modifier.fillMaxSize(),
                            )
                            AppScreen.HOME -> Unit
                        }
                    }
                }
            }

            if (addMenuOpen) {
                QuickAddMenu(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    onNfce = { addMenuOpen = false; screen = AppScreen.NFCE },
                    onOcr = { addMenuOpen = false; screen = AppScreen.OCR },
                    onManual = { addMenuOpen = false; screen = AppScreen.MANUAL },
                )
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text = "Leitor Cupons Finanças",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Organize compras. Entenda seus gastos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppBottomBar(
    current: AppScreen,
    addMenuOpen: Boolean,
    onHome: () -> Unit,
    onHistory: () -> Unit,
    onAdd: () -> Unit,
    onProducts: () -> Unit,
    onProfile: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = current == AppScreen.HOME,
            onClick = onHome,
            icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
            label = { Text("Início") },
        )
        NavigationBarItem(
            selected = current == AppScreen.HISTORY,
            onClick = onHistory,
            icon = { Icon(Icons.Default.History, contentDescription = "Histórico") },
            label = { Text("Histórico") },
        )
        NavigationBarItem(
            selected = addMenuOpen,
            onClick = onAdd,
            icon = {
                FloatingActionButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar")
                }
            },
            label = { Text("Adicionar") },
        )
        NavigationBarItem(
            selected = current == AppScreen.PRODUCTS,
            onClick = onProducts,
            icon = { Icon(Icons.Default.Inventory2, contentDescription = "Produtos") },
            label = { Text("Produtos") },
        )
        NavigationBarItem(
            selected = current == AppScreen.PROFILE,
            onClick = onProfile,
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Perfil") },
            label = { Text("Perfil") },
        )
    }
}

@Composable
private fun QuickAddMenu(
    modifier: Modifier = Modifier,
    onNfce: () -> Unit,
    onOcr: () -> Unit,
    onManual: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExtendedFloatingActionButton(
            onClick = onNfce,
            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
            text = { Text("QR Code / chave NFC-e") },
        )
        ExtendedFloatingActionButton(
            onClick = onOcr,
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
            text = { Text("Foto, imagem ou PDF") },
        )
        ExtendedFloatingActionButton(
            onClick = onManual,
            icon = { Icon(Icons.Default.Keyboard, contentDescription = null) },
            text = { Text("Lançamento manual") },
        )
    }
}

@Composable
private fun SecondaryScreenScaffold(onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BackArrowButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
        )
        content()
    }
}

@Composable
private fun HomeScreen(
    productCount: Int,
    onHistory: () -> Unit,
    onProducts: () -> Unit,
    onBackup: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = "Visão geral",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Seu controle financeiro começa pelos itens realmente comprados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardMetric(
                    title = "Produtos",
                    value = productCount.toString(),
                    icon = Icons.Default.Inventory2,
                    modifier = Modifier.weight(1f),
                )
                DashboardMetric(
                    title = "Leitura",
                    value = "QR + OCR",
                    icon = Icons.Default.ReceiptLong,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Text(
                text = "Acessos rápidos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            DashboardActionCard(
                icon = Icons.Default.History,
                title = "Histórico e gastos",
                subtitle = "Consulte compras e itens já registrados.",
                onClick = onHistory,
            )
        }
        item {
            DashboardActionCard(
                icon = Icons.Default.Inventory2,
                title = "Produtos mestres",
                subtitle = "Organize categorias, vínculos e aprendizado.",
                onClick = onProducts,
            )
        }
        item {
            DashboardActionCard(
                icon = Icons.Default.Backup,
                title = "Backup e segurança",
                subtitle = "Proteja os dados e configurações do aplicativo.",
                onClick = onBackup,
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Adicionar uma compra",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Use o botão + abaixo para escolher QR/chave NFC-e, foto/PDF ou lançamento manual.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMetric(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DashboardActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconButton(onClick = onClick) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
