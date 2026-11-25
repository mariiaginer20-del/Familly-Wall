package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.CategoryScope
import com.example.data.preferences.AppDesignStyle
import com.example.data.preferences.AppearanceSettings
import com.example.data.preferences.ThemeDarkMode
import com.example.data.preferences.ThemePreferences
import com.example.data.repository.FirestoreRepository
import com.example.notification.FamilyNotificationManager
import com.example.ui.FamilyWallViewModel
import com.example.ui.MainTab
import com.example.ui.UiState
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutWithCleanup
import com.example.ui.components.FamilyHeader
import com.example.ui.dialogs.AddCalendarEventDialog
import com.example.ui.dialogs.AddRecipeDialog
import com.example.ui.dialogs.AddShoppingItemDialog
import com.example.ui.dialogs.AddTaskDialog
import com.example.ui.dialogs.FamilySettingsDialog
import com.example.ui.dialogs.ManageCategoriesDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.FamilySetupScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MacrosScreen
import com.example.ui.screens.MealsScreen
import com.example.ui.screens.ShoppingScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.FamilyWallTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        themePreferences = ThemePreferences(applicationContext)

        setContent {
            val appearance by themePreferences.settingsFlow.collectAsStateWithLifecycle(
                initialValue = AppearanceSettings()
            )

            FamilyWallTheme(
                designStyle = appearance.designStyle,
                darkMode = appearance.darkMode
            ) {
                AppNavigation(
                    themePreferences = themePreferences,
                    appearance = appearance
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    themePreferences: ThemePreferences,
    appearance: AppearanceSettings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val authUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val uid = authUser?.uid
    if (uid == null) {
        AuthScreen(onAuthSuccess = {})
        return
    }

    key(uid) {
        val dbName = context.getString(R.string.firestore_database_id)
        val viewModel: FamilyWallViewModel = viewModel(
            key = uid,
            factory = viewModelFactory {
                initializer {
                    val repo = FirestoreRepository(
                        db = Firebase.firestore(dbName),
                        auth = Firebase.auth
                    )
                    FamilyWallViewModel(
                        repository = repo,
                        themePreferences = themePreferences,
                        notificationManager = FamilyNotificationManager(context.applicationContext),
                        currentUserId = uid
                    )
                }
            }
        )

        val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
        val familyUnitState by viewModel.familyUnitState.collectAsStateWithLifecycle()

        val onSignOut: () -> Unit = {
            signOutWithCleanup(
                credentialManager = credentialManager,
                scope = scope,
                onSignOutComplete = {}
            )
        }

        when (val profileState = userProfileState) {
            is UiState.Loading -> {
                FullScreenStatus(message = "Cargando tu perfil familiar...")
            }
            is UiState.Error -> {
                FullScreenError(
                    message = profileState.message,
                    onSignOut = onSignOut
                )
            }
            is UiState.Success -> {
                val profile = profileState.data
                if (profile == null || profile.familyId.isNullOrBlank()) {
                    FamilySetupScreen(
                        viewModel = viewModel,
                        defaultDisplayName = authUser?.displayName ?: "Yo",
                        userEmail = authUser?.email,
                        onSignOut = onSignOut
                    )
                } else {
                    when (val famState = familyUnitState) {
                        is UiState.Loading -> FullScreenStatus(message = "Sincronizando unidad familiar...")
                        is UiState.Error -> FullScreenError(message = famState.message, onSignOut = onSignOut)
                        is UiState.Success -> {
                            val family = famState.data
                            if (family == null) {
                                FamilySetupScreen(
                                    viewModel = viewModel,
                                    defaultDisplayName = authUser?.displayName ?: "Yo",
                                    userEmail = authUser?.email,
                                    onSignOut = onSignOut
                                )
                            } else {
                                MainApp(
                                    viewModel = viewModel,
                                    appearance = appearance,
                                    onSignOut = onSignOut
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenStatus(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FullScreenError(
    message: String,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "No se pudo conectar con tu hogar",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onSignOut) {
                Text("Volver a iniciar sesión")
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: FamilyWallViewModel,
    appearance: AppearanceSettings,
    onSignOut: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val familyState by viewModel.familyUnitState.collectAsStateWithLifecycle()
    val family = (familyState as? UiState.Success)?.data
    val members by viewModel.allMembers.collectAsStateWithLifecycle()
    val selectedMember by viewModel.selectedMemberFilter.collectAsStateWithLifecycle()
    val events by viewModel.allEvents.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val shoppingItems by viewModel.rawShoppingItems.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val productHistory by viewModel.productHistory.collectAsStateWithLifecycle()
    val manageCategoriesScope by viewModel.manageCategoriesScope.collectAsStateWithLifecycle()

    val showAddShopping by viewModel.showAddShoppingDialog.collectAsStateWithLifecycle()
    val editingShoppingItem by viewModel.editingShoppingItem.collectAsStateWithLifecycle()
    val showAddEvent by viewModel.showAddEventDialog.collectAsStateWithLifecycle()
    val editingEvent by viewModel.editingEvent.collectAsStateWithLifecycle()
    val showAddTask by viewModel.showAddTaskDialog.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()
    val showAddRecipe by viewModel.showAddRecipeDialog.collectAsStateWithLifecycle()
    val prefillRecipeDraft by viewModel.prefillRecipeDraft.collectAsStateWithLifecycle()
    val selectedDateMillis by viewModel.selectedCalendarDateMillis.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.notificationManager.showReminderNotification(
                    title = "Notificaciones activadas",
                    message = "Recibirás avisos para los próximos eventos y compras familiares."
                )
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FamilyHeader(
                familyName = family?.name ?: "Family Wall",
                inviteCode = family?.inviteCode,
                members = members,
                selectedMember = selectedMember,
                onSelectMember = { viewModel.selectMemberFilter(it) },
                pendingEventCount = events.size,
                darkMode = appearance.darkMode,
                onToggleStyleQuick = {
                    val nextStyle = if (appearance.designStyle == AppDesignStyle.EDITORIAL) {
                        AppDesignStyle.NORDIC_BENTO
                    } else {
                        AppDesignStyle.EDITORIAL
                    }
                    viewModel.setDesignStyle(nextStyle)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Interfaz cambiada a ${nextStyle.title}")
                    }
                },
                onToggleDarkModeQuick = {
                    val nextMode = when (appearance.darkMode) {
                        ThemeDarkMode.DARK -> ThemeDarkMode.LIGHT
                        else -> ThemeDarkMode.DARK
                    }
                    viewModel.setDarkMode(nextMode)
                },
                onOpenSettings = { showSettingsDialog = true },
                onNotificationClick = {
                    if (events.isNotEmpty()) {
                        viewModel.triggerEventNotification(events.first())
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Aviso de '${events.first().title}' enviado")
                        }
                    } else {
                        viewModel.notificationManager.showReminderNotification(
                            title = "Family Wall",
                            message = "No hay eventos urgentes pendientes para hoy."
                        )
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Notificación de prueba enviada")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.HOME,
                        onClick = { viewModel.selectTab(MainTab.HOME) },
                        colors = navItemColors,
                        icon = {
                            Icon(
                                if (currentTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Muro"
                            )
                        },
                        label = {
                            Text(
                                "Muro",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (currentTab == MainTab.HOME) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.CALENDAR,
                        onClick = { viewModel.selectTab(MainTab.CALENDAR) },
                        colors = navItemColors,
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (events.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ) { Text("${events.size}") }
                                    }
                                }
                            ) {
                                Icon(
                                    if (currentTab == MainTab.CALENDAR) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                                    contentDescription = "Agenda"
                                )
                            }
                        },
                        label = {
                            Text(
                                "Agenda",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (currentTab == MainTab.CALENDAR) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_calendar")
                    )

                    val pendingTasksCount = tasks.count { !it.completed }
                    NavigationBarItem(
                        selected = currentTab == MainTab.TASKS,
                        onClick = { viewModel.selectTab(MainTab.TASKS) },
                        colors = navItemColors,
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingTasksCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ) { Text("$pendingTasksCount") }
                                    }
                                }
                            ) {
                                Icon(
                                    if (currentTab == MainTab.TASKS) Icons.Filled.Assignment else Icons.Outlined.Assignment,
                                    contentDescription = "Tareas"
                                )
                            }
                        },
                        label = {
                            Text(
                                "Tareas",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (currentTab == MainTab.TASKS) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_tasks")
                    )

                    val pendingShoppingCount = shoppingItems.count { !it.checked }
                    NavigationBarItem(
                        selected = currentTab == MainTab.SHOPPING,
                        onClick = { viewModel.selectTab(MainTab.SHOPPING) },
                        colors = navItemColors,
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingShoppingCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ) { Text("$pendingShoppingCount") }
                                    }
                                }
                            ) {
                                Icon(
                                    if (currentTab == MainTab.SHOPPING) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                    contentDescription = "Listas"
                                )
                            }
                        },
                        label = {
                            Text(
                                "Listas",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (currentTab == MainTab.SHOPPING) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_shopping")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.MEALS,
                        onClick = { viewModel.selectTab(MainTab.MEALS) },
                        colors = navItemColors,
                        icon = {
                            Icon(
                                if (currentTab == MainTab.MEALS) Icons.Filled.Restaurant else Icons.Outlined.Restaurant,
                                contentDescription = "Comidas"
                            )
                        },
                        label = {
                            Text(
                                "Comidas",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (currentTab == MainTab.MEALS) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_meals")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.MACROS,
                        onClick = { viewModel.selectTab(MainTab.MACROS) },
                        colors = navItemColors,
                        icon = {
                            Icon(
                                if (currentTab == MainTab.MACROS) Icons.Filled.Calculate else Icons.Outlined.Calculate,
                                contentDescription = "Macros"
                            )
                        },
                        label = {
                            Text(
                                "Macros",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (currentTab == MainTab.MACROS) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_macros")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { viewModel.selectTab(it) }
                )
                MainTab.CALENDAR -> CalendarScreen(
                    viewModel = viewModel
                )
                MainTab.TASKS -> TasksScreen(
                    viewModel = viewModel
                )
                MainTab.SHOPPING -> ShoppingScreen(
                    viewModel = viewModel
                )
                MainTab.MEALS -> MealsScreen(
                    viewModel = viewModel
                )
                MainTab.MACROS -> MacrosScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    if (showSettingsDialog) {
        FamilySettingsDialog(
            family = family,
            members = members,
            appearance = appearance,
            currentUserId = viewModel.currentUserId,
            onSelectDesignStyle = { viewModel.setDesignStyle(it) },
            onSelectDarkMode = { viewModel.setDarkMode(it) },
            onAddDependentMember = { name, role, emoji, color ->
                viewModel.addDependentMember(name, role, emoji, color)
            },
            onLeaveFamily = {
                showSettingsDialog = false
                viewModel.leaveFamily()
            },
            onSignOut = {
                showSettingsDialog = false
                onSignOut()
            },
            onCodeCopied = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Código copiado al portapapeles")
                }
            },
            onOpenManageCategories = {
                showSettingsDialog = false
                viewModel.openManageCategories(CategoryScope.SHOPPING)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showAddShopping) {
        AddShoppingItemDialog(
            members = members,
            categories = allCategories.filter { it.scope == CategoryScope.SHOPPING.name },
            productHistory = productHistory,
            existingShoppingItems = shoppingItems,
            initialItem = editingShoppingItem,
            onManageCategories = { viewModel.openManageCategories(CategoryScope.SHOPPING) },
            onDismiss = { viewModel.setShowAddShoppingDialog(false) },
            onSave = { draft ->
                viewModel.saveShoppingItem(draft, editingShoppingItem)
                viewModel.setShowAddShoppingDialog(false)
            }
        )
    }

    if (showAddEvent) {
        AddCalendarEventDialog(
            members = members,
            categories = allCategories.filter { it.scope == CategoryScope.EVENT.name },
            initialDateMillis = selectedDateMillis,
            initialEvent = editingEvent,
            onManageCategories = { viewModel.openManageCategories(CategoryScope.EVENT) },
            onDismiss = { viewModel.setShowAddEventDialog(false) },
            onSave = { draft ->
                viewModel.saveCalendarEvent(draft, editingEvent)
                viewModel.setShowAddEventDialog(false)
            }
        )
    }

    if (showAddTask) {
        AddTaskDialog(
            members = members,
            categories = allCategories.filter { it.scope == CategoryScope.TASK.name },
            initialTask = editingTask,
            onManageCategories = { viewModel.openManageCategories(CategoryScope.TASK) },
            onDismiss = { viewModel.setShowAddTaskDialog(false) },
            onSave = { draft ->
                viewModel.saveTask(draft, editingTask)
                viewModel.setShowAddTaskDialog(false)
            }
        )
    }

    if (showAddRecipe) {
        AddRecipeDialog(
            categories = allCategories.filter { it.scope == CategoryScope.RECIPE.name },
            prefillDraft = prefillRecipeDraft,
            onManageCategories = { viewModel.openManageCategories(CategoryScope.RECIPE) },
            onDismiss = { viewModel.setShowAddRecipeDialog(false) },
            onSave = { draft ->
                viewModel.addRecipe(draft)
                viewModel.setShowAddRecipeDialog(false)
            }
        )
    }

    if (manageCategoriesScope != null) {
        ManageCategoriesDialog(
            initialScope = manageCategoriesScope!!,
            allCategories = allCategories,
            onAddCategory = { scopeCat, name, emoji, colorHex ->
                viewModel.addCategory(scopeCat, name, emoji, colorHex)
            },
            onUpdateCategory = { category, oldName ->
                viewModel.updateCategory(category, oldName)
            },
            onDeleteCategory = { category ->
                viewModel.deleteCategory(category)
            },
            onRestoreDefaults = {
                viewModel.restoreDefaultCategories()
            },
            onDismiss = { viewModel.closeManageCategories() }
        )
    }
}
