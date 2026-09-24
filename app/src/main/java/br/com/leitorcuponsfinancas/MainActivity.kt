package br.com.leitorcuponsfinancas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.leitorcuponsfinancas.data.AppPreferences
import br.com.leitorcuponsfinancas.data.AppPreferencesStore
import br.com.leitorcuponsfinancas.ui.BackArrowButton
import br.com.leitorcuponsfinancas.ui.BackupScreen
import br.com.leitorcuponsfinancas.ui.ContextualTipDialog
import br.com.leitorcuponsfinancas.ui.GuidedTutorialOverlay
import br.com.leitorcuponsfinancas.ui.GuidedTutorialStep
import br.com.leitorcuponsfinancas.ui.LocalTutorialTargetRegistry
import br.com.leitorcuponsfinancas.ui.TutorialTargetRegistry
import br.com.leitorcuponsfinancas.ui.tutorialTarget
import br.com.leitorcuponsfinancas.ui.HistoryScreen
import br.com.leitorcuponsfinancas.ui.HomeDashboardState
import br.com.leitorcuponsfinancas.ui.HomeViewModel
import br.com.leitorcuponsfinancas.ui.ManualEntryScreen
import br.com.leitorcuponsfinancas.ui.NfceScreen
import br.com.leitorcuponsfinancas.ui.ProductScreen
import br.com.leitorcuponsfinancas.ui.ProductViewModel
import br.com.leitorcuponsfinancas.ui.ProfileScreen
import br.com.leitorcuponsfinancas.ui.ReceiptOcrScreen
import br.com.leitorcuponsfinancas.ui.SettingsScreen
import br.com.leitorcuponsfinancas.ui.ScreenHero
import br.com.leitorcuponsfinancas.ui.theme.LeitorCuponsTheme
import br.com.leitorcuponsfinancas.ui.theme.LocalAppVisuals
import java.text.NumberFormat
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferencesStore = remember {
                AppPreferencesStore.getInstance(applicationContext)
            }
            val preferences by preferencesStore.state.collectAsStateWithLifecycle()

            LeitorCuponsTheme(
                themeMode = preferences.themeMode,
                colorPalette = preferences.colorPalette,
                gradientEnabled = preferences.gradientEnabled,
                fontScale = preferences.fontScale,
            ) {
                Surface(Modifier.fillMaxSize()) {
                    LeitorCuponsApp(
                        preferences = preferences,
                        preferencesStore = preferencesStore,
                    )
                }
            }
        }
    }
}

private enum class AppScreen {
    NFCE,
    OCR,
    MANUAL,
    BACKUP,
    SETTINGS,
}

private enum class MainTab(val page: Int) {
    HOME(0),
    HISTORY(1),
    PRODUCTS(2),
    PROFILE(3),
}

private data class AppTip(
    val key: String,
    val title: String,
    val text: String,
)

@Composable
private fun LeitorCuponsApp(
    preferences: AppPreferences,
    preferencesStore: AppPreferencesStore,
    productViewModel: ProductViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
) {
    val products by productViewModel.products.collectAsStateWithLifecycle()
    val learnedLinks by productViewModel.learnedLinks.collectAsStateWithLifecycle()
    val dashboard by homeViewModel.dashboard.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(
        initialPage = MainTab.HOME.page,
        pageCount = { MainTab.entries.size },
    )
    val scope = rememberCoroutineScope()

    var taskScreen by rememberSaveable { mutableStateOf<AppScreen?>(null) }
    var addMenuOpen by rememberSaveable { mutableStateOf(false) }
    var pendingTip by remember { mutableStateOf<AppTip?>(null) }

    val tutorialRegistry = remember { TutorialTargetRegistry() }
    var tutorialStep by rememberSaveable { mutableIntStateOf(0) }
    val tutorialSteps = remember {
        listOf(
            GuidedTutorialStep(
                targetKey = "home.hero",
                title = "Bem-vindo ao Leitor Cupons Finanças",
                text = "Este é o seu painel principal. Durante o tutorial, a própria tela continua visível e vamos destacar exatamente o que está sendo explicado.",
            ),
            GuidedTutorialStep(
                targetKey = "home.metrics",
                title = "Seu resumo financeiro",
                text = "Estes cartões mostram gasto do mês, compras, Produtos Mestres e atalhos de leitura. Eles também são clicáveis.",
            ),
            GuidedTutorialStep(
                targetKey = "bottom.add",
                title = "Adicionar uma compra",
                text = "O botão + abre as formas de entrada: QR/chave NFC-e, foto ou PDF e lançamento manual.",
            ),
            GuidedTutorialStep(
                targetKey = "products.hero",
                title = "Produtos Mestres",
                text = "Produto Mestre é o produto raiz, sem marca. O tutorial muda para esta tela para você enxergar a função enquanto ela é explicada.",
            ),
            GuidedTutorialStep(
                targetKey = "header.settings",
                title = "Configurações",
                text = "A engrenagem reúne a personalização do aplicativo, inclusive tema, paleta e fonte.",
            ),
            GuidedTutorialStep(
                targetKey = "settings.appearance",
                title = "Personalize o aplicativo",
                text = "Escolha claro, escuro ou sistema, sua paleta preferida, degradê e ajuste a fonte arrastando o controle.",
            ),
        )
    }

    fun showTip(tip: AppTip?) {
        if (
            tip != null &&
            preferences.showContextualTips &&
            tip.key !in preferences.seenTips
        ) {
            pendingTip = tip
        }
    }

    fun openTask(target: AppScreen, tip: AppTip? = null) {
        addMenuOpen = false
        taskScreen = target
        showTip(tip)
    }

    fun openTab(tab: MainTab, tip: AppTip? = null) {
        addMenuOpen = false
        taskScreen = null
        showTip(tip)
        scope.launch {
            pagerState.animateScrollToPage(tab.page)
        }
    }

    LaunchedEffect(preferences.onboardingCompleted) {
        if (!preferences.onboardingCompleted) {
            tutorialStep = 0
        }
    }

    LaunchedEffect(preferences.onboardingCompleted, tutorialStep) {
        if (!preferences.onboardingCompleted) {
            addMenuOpen = false

            when (tutorialStep) {
                0, 1, 2 -> {
                    taskScreen = null
                    pagerState.animateScrollToPage(MainTab.HOME.page)
                }

                3 -> {
                    taskScreen = null
                    pagerState.animateScrollToPage(MainTab.PRODUCTS.page)
                }

                4 -> {
                    taskScreen = null
                    pagerState.animateScrollToPage(MainTab.HOME.page)
                }

                5 -> {
                    taskScreen = AppScreen.SETTINGS
                }
            }
        }
    }

    BackHandler(
        enabled = addMenuOpen || taskScreen != null || pagerState.currentPage != MainTab.HOME.page,
    ) {
        when {
            addMenuOpen -> addMenuOpen = false
            taskScreen != null -> taskScreen = null
            else -> scope.launch {
                pagerState.animateScrollToPage(MainTab.HOME.page)
            }
        }
    }

    BackHandler(enabled = !preferences.onboardingCompleted) {
        if (tutorialStep > 0) {
            tutorialStep -= 1
        } else {
            preferencesStore.completeOnboarding()
        }
    }

    CompositionLocalProvider(
        LocalTutorialTargetRegistry provides tutorialRegistry,
    ) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
        bottomBar = {
            AppBottomBar(
                currentPage = pagerState.currentPage,
                addMenuOpen = addMenuOpen,
                onHome = { openTab(MainTab.HOME) },
                onHistory = {
                    openTab(
                        MainTab.HISTORY,
                        AppTip(
                            key = "history",
                            title = "Histórico e gastos",
                            text = "Aqui você consulta compras por período, pesquisa itens e corrige informações quando necessário.",
                        ),
                    )
                },
                onAdd = { addMenuOpen = !addMenuOpen },
                onProducts = {
                    openTab(
                        MainTab.PRODUCTS,
                        AppTip(
                            key = "products",
                            title = "Produtos mestres",
                            text = "Produto Mestre representa o produto raiz, sem marca. Vincule diferentes descrições fiscais ao mesmo item e reutilize setores, categorias e subcategorias.",
                        ),
                    )
                },
                onProfile = { openTab(MainTab.PROFILE) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(Modifier.fillMaxSize()) {
                AppHeader(
                    onSettings = { openTask(AppScreen.SETTINGS) },
                )

                AnimatedContent(
                    targetState = taskScreen,
                    transitionSpec = {
                        if (targetState == null) {
                            (
                                slideInHorizontally(
                                    animationSpec = tween(240),
                                    initialOffsetX = { fullWidth -> -fullWidth / 4 },
                                ) +
                                    fadeIn(animationSpec = tween(200))
                                ).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(220),
                                    targetOffsetX = { fullWidth -> fullWidth / 3 },
                                ) +
                                    fadeOut(animationSpec = tween(160)),
                            )
                        } else {
                            (
                                slideInHorizontally(
                                    animationSpec = tween(260),
                                    initialOffsetX = { fullWidth -> fullWidth / 3 },
                                ) +
                                    fadeIn(animationSpec = tween(220)) +
                                    scaleIn(
                                        initialScale = 0.97f,
                                        animationSpec = tween(220),
                                    )
                                ).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(210),
                                    targetOffsetX = { fullWidth -> -fullWidth / 7 },
                                ) +
                                    fadeOut(animationSpec = tween(160)),
                            )
                        }
                    },
                    label = "taskTransition",
                ) { targetTask ->
                    if (targetTask == null) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            key = { it },
                        ) { page ->
                            when (page) {
                                MainTab.HOME.page -> HomeScreen(
                                    productCount = products.size,
                                    dashboard = dashboard,
                                    onHistory = { openTab(MainTab.HISTORY) },
                                    onProducts = { openTab(MainTab.PRODUCTS) },
                                    onRead = { addMenuOpen = true },
                                    onBackup = { openTask(AppScreen.BACKUP) },
                                )

                                MainTab.HISTORY.page -> HistoryScreen(
                                    onBack = { openTab(MainTab.HOME) },
                                    modifier = Modifier.fillMaxSize(),
                                )

                                MainTab.PRODUCTS.page -> ProductScreen(
                                    products = products,
                                    learnedLinks = learnedLinks,
                                    onSave = productViewModel::save,
                                    onDeactivate = productViewModel::deactivate,
                                    modifier = Modifier.fillMaxSize(),
                                )

                                MainTab.PROFILE.page -> ProfileScreen(
                                    onBack = { openTab(MainTab.HOME) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    } else {
                        SecondaryScreenScaffold(
                            onBack = { taskScreen = null },
                        ) {
                            when (targetTask) {
                                AppScreen.NFCE -> NfceScreen(
                                    onBack = { taskScreen = null },
                                    modifier = Modifier.fillMaxSize(),
                                )

                                AppScreen.OCR -> ReceiptOcrScreen(
                                    modifier = Modifier.fillMaxSize(),
                                )

                                AppScreen.MANUAL -> ManualEntryScreen(
                                    onBack = { taskScreen = null },
                                    modifier = Modifier.fillMaxSize(),
                                )

                                AppScreen.BACKUP -> BackupScreen(
                                    onBack = { taskScreen = null },
                                    modifier = Modifier.fillMaxSize(),
                                )

                                AppScreen.SETTINGS -> SettingsScreen(
                                    preferences = preferences,
                                    onFontScaleChange = preferencesStore::setFontScale,
                                    onShowTipsChange = preferencesStore::setShowContextualTips,
                                    onRestartTutorial = preferencesStore::restartOnboarding,
                                    onResetTips = preferencesStore::resetTips,
                                    onThemeModeChange = preferencesStore::setThemeMode,
                                    onColorPaletteChange = preferencesStore::setColorPalette,
                                    onGradientEnabledChange = preferencesStore::setGradientEnabled,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = addMenuOpen,
                enter = fadeIn(animationSpec = tween(140)),
                exit = fadeOut(animationSpec = tween(120)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.18f))
                        .clickable { addMenuOpen = false },
                )
            }

            QuickAddMenu(
                open = addMenuOpen,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                onNfce = {
                    openTask(
                        AppScreen.NFCE,
                        AppTip(
                            key = "nfce",
                            title = "QR Code ou chave NFC-e",
                            text = "Use esta opção quando você tiver o QR Code ou a chave de acesso da NFC-e.",
                        ),
                    )
                },
                onOcr = {
                    openTask(
                        AppScreen.OCR,
                        AppTip(
                            key = "ocr",
                            title = "Foto, imagem ou PDF",
                            text = "O aplicativo identifica os dados do cupom e sempre apresenta uma etapa de revisão antes de salvar.",
                        ),
                    )
                },
                onManual = {
                    openTask(
                        AppScreen.MANUAL,
                        AppTip(
                            key = "manual",
                            title = "Lançamento manual",
                            text = "Use as sugestões de produtos e estabelecimentos já conhecidos para preencher mais rápido e evitar duplicidades.",
                        ),
                    )
                },
            )
        }
    }

            if (!preferences.onboardingCompleted) {
                GuidedTutorialOverlay(
                    steps = tutorialSteps,
                    stepIndex = tutorialStep,
                    registry = tutorialRegistry,
                    onBack = {
                        if (tutorialStep > 0) tutorialStep -= 1
                    },
                    onNext = {
                        if (tutorialStep == tutorialSteps.lastIndex) {
                            preferencesStore.completeOnboarding()
                            taskScreen = null
                            scope.launch {
                                pagerState.animateScrollToPage(MainTab.HOME.page)
                            }
                        } else {
                            tutorialStep += 1
                        }
                    },
                    onSkip = {
                        preferencesStore.completeOnboarding()
                        taskScreen = null
                        scope.launch {
                            pagerState.animateScrollToPage(MainTab.HOME.page)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    pendingTip?.let { tip ->
        ContextualTipDialog(
            title = tip.title,
            text = tip.text,
            onDismiss = {
                preferencesStore.markTipSeen(tip.key)
                pendingTip = null
            },
            onSkipAll = {
                preferencesStore.setShowContextualTips(false)
                pendingTip = null
            },
        )
    }
}

@Composable
private fun AppHeader(
    onSettings: () -> Unit,
) {
    val visuals = LocalAppVisuals.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(visuals.heroBrush)
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Leitor Cupons Finanças",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = visuals.onGradient,
            )
            Text(
                text = "Organize compras. Entenda seus gastos.",
                style = MaterialTheme.typography.bodySmall,
                color = visuals.onGradient.copy(alpha = 0.88f),
            )
        }

        IconButton(
            onClick = onSettings,
            modifier = Modifier.tutorialTarget("header.settings"),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configurações",
                tint = visuals.onGradient,
            )
        }
    }
}

@Composable
private fun AppBottomBar(
    currentPage: Int,
    addMenuOpen: Boolean,
    onHome: () -> Unit,
    onHistory: () -> Unit,
    onAdd: () -> Unit,
    onProducts: () -> Unit,
    onProfile: () -> Unit,
) {
    val addRotation by animateFloatAsState(
        targetValue = if (addMenuOpen) 45f else 0f,
        animationSpec = tween(180),
        label = "addRotation",
    )

    NavigationBar {
        NavigationBarItem(
            selected = currentPage == MainTab.HOME.page,
            onClick = onHome,
            icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
            label = { Text("Início") },
        )
        NavigationBarItem(
            selected = currentPage == MainTab.HISTORY.page,
            onClick = onHistory,
            icon = { Icon(Icons.Default.History, contentDescription = "Histórico") },
            label = { Text("Histórico") },
        )
        NavigationBarItem(
            selected = addMenuOpen,
            onClick = onAdd,
            modifier = Modifier.tutorialTarget("bottom.add"),
            icon = {
                val visuals = LocalAppVisuals.current
                Surface(
                    onClick = onAdd,
                    shape = CircleShape,
                    color = Color.Transparent,
                    shadowElevation = 8.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(visuals.accentBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Adicionar",
                            tint = visuals.onGradient,
                            modifier = Modifier.graphicsLayer(rotationZ = addRotation),
                        )
                    }
                }
            },
            label = { Text("Adicionar") },
        )
        NavigationBarItem(
            selected = currentPage == MainTab.PRODUCTS.page,
            onClick = onProducts,
            icon = { Icon(Icons.Default.Inventory2, contentDescription = "Produtos") },
            label = { Text("Produtos") },
        )
        NavigationBarItem(
            selected = currentPage == MainTab.PROFILE.page,
            onClick = onProfile,
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Perfil") },
            label = { Text("Perfil") },
        )
    }
}

@Composable
private fun QuickAddMenu(
    open: Boolean,
    modifier: Modifier = Modifier,
    onNfce: () -> Unit,
    onOcr: () -> Unit,
    onManual: () -> Unit,
) {
    val actions = listOf(
        QuickAction(
            label = "QR Code / chave NFC-e",
            icon = Icons.Default.QrCodeScanner,
            accent = MaterialTheme.colorScheme.primary,
            delayMillis = 90,
            onClick = onNfce,
        ),
        QuickAction(
            label = "Foto, imagem ou PDF",
            icon = Icons.Default.CameraAlt,
            accent = MaterialTheme.colorScheme.secondary,
            delayMillis = 45,
            onClick = onOcr,
        ),
        QuickAction(
            label = "Lançamento manual",
            icon = Icons.Default.Keyboard,
            accent = MaterialTheme.colorScheme.tertiary,
            delayMillis = 0,
            onClick = onManual,
        ),
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        actions.forEach { action ->
            AnimatedVisibility(
                visible = open,
                enter =
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 150,
                            delayMillis = action.delayMillis,
                        ),
                    ) +
                        scaleIn(
                            initialScale = 0.72f,
                            transformOrigin = TransformOrigin(0.5f, 1f),
                            animationSpec = tween(
                                durationMillis = 190,
                                delayMillis = action.delayMillis,
                            ),
                        ) +
                        slideInVertically(
                            animationSpec = tween(
                                durationMillis = 190,
                                delayMillis = action.delayMillis,
                            ),
                            initialOffsetY = { height -> height / 3 },
                        ),
                exit =
                    fadeOut(animationSpec = tween(110)) +
                        scaleOut(
                            targetScale = 0.84f,
                            transformOrigin = TransformOrigin(0.5f, 1f),
                            animationSpec = tween(130),
                        ) +
                        slideOutVertically(
                            animationSpec = tween(130),
                            targetOffsetY = { height -> height / 4 },
                        ),
            ) {
                QuickActionBubble(action)
            }
        }
    }
}

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val delayMillis: Int,
    val onClick: () -> Unit,
)

@Composable
private fun QuickActionBubble(
    action: QuickAction,
) {
    Surface(
        onClick = action.onClick,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.widthIn(min = 250.dp, max = 330.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = action.accent.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = action.accent,
                    )
                }
            }
            Text(
                text = action.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SecondaryScreenScaffold(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
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
    dashboard: HomeDashboardState,
    onHistory: () -> Unit,
    onProducts: () -> Unit,
    onRead: () -> Unit,
    onBackup: () -> Unit,
) {
    val analytics = dashboard.analytics

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHero(
                title = "Visão geral",
                subtitle = dashboard.periodLabel,
                icon = Icons.Default.TrendingUp,
                modifier = Modifier.tutorialTarget("home.hero"),
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tutorialTarget("home.metrics"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardMetric(
                    title = "Gasto no mês",
                    value = formatCurrency(analytics.totalSpent),
                    icon = Icons.Default.TrendingUp,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = onHistory,
                    modifier = Modifier.weight(1f),
                )
                DashboardMetric(
                    title = "Compras",
                    value = analytics.purchaseCount.toString(),
                    icon = Icons.Default.ShoppingCart,
                    accent = MaterialTheme.colorScheme.secondary,
                    onClick = onHistory,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardMetric(
                    title = "Produtos mestres",
                    value = productCount.toString(),
                    icon = Icons.Default.Inventory2,
                    accent = MaterialTheme.colorScheme.tertiary,
                    onClick = onProducts,
                    modifier = Modifier.weight(1f),
                )
                DashboardMetric(
                    title = "Leitura",
                    value = "QR + OCR",
                    icon = Icons.Default.ReceiptLong,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = onRead,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            DashboardRankingCard(
                title = "Estabelecimentos que mais custaram",
                subtitle = "Ranking por gasto no mês",
                icon = Icons.Default.Storefront,
                accent = MaterialTheme.colorScheme.tertiary,
                rows = analytics.merchantsBySpend.take(3).map {
                    it.name to "${formatCurrency(it.totalSpent)} • ${it.purchaseCount} compra(s)"
                },
            )
        }

        item {
            DashboardRankingCard(
                title = "Onde você compra com mais frequência",
                subtitle = "Quantidade de compras no mês",
                icon = Icons.Default.History,
                accent = MaterialTheme.colorScheme.secondary,
                rows = analytics.merchantsByFrequency.take(3).map {
                    it.name to "${it.purchaseCount} compra(s) • ${formatCurrency(it.totalSpent)}"
                },
            )
        }

        item {
            DashboardRankingCard(
                title = "Produtos mais comprados",
                subtitle = "Quantidade acumulada no mês",
                icon = Icons.Default.ShoppingCart,
                accent = MaterialTheme.colorScheme.primary,
                rows = analytics.productsMostPurchased.take(3).map {
                    it.name to "${formatQuantity(it.quantity)} • ${formatCurrency(it.totalSpent)}"
                },
            )
        }

        item {
            DashboardRankingCard(
                title = "Produtos com maior preço unitário",
                subtitle = "Maior valor unitário registrado no mês",
                icon = Icons.Default.TrendingUp,
                accent = MaterialTheme.colorScheme.tertiary,
                rows = analytics.productsMostExpensive.take(3).map {
                    it.name to formatCurrency(it.highestUnitPrice)
                },
            )
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
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = accent)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DashboardRankingCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    rows: List<Pair<String, String>>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(icon, contentDescription = null, tint = accent)
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (rows.isEmpty()) {
                Text(
                    text = "Ainda não há dados suficientes neste mês.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                rows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "${index + 1}.",
                            color = accent,
                            fontWeight = FontWeight.Bold,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = row.first,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = row.second,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
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
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatCurrency(value: Double): String =
    NumberFormat
        .getCurrencyInstance(Locale("pt", "BR"))
        .format(value)

private fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) {
        "${value.toLong()} unidade(s)"
    } else {
        String.format(Locale("pt", "BR"), "%.2f unidade(s)", value)
    }
