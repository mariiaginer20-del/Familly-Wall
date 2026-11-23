package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.FamilyWallViewModel
import com.example.ui.MainTab
import com.example.ui.components.FamilyHeader
import com.example.ui.dialogs.AddCalendarEventDialog
import com.example.ui.dialogs.AddRecipeDialog
import com.example.ui.dialogs.AddShoppingItemDialog
import com.example.ui.dialogs.AddTaskDialog
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MealsScreen
import com.example.ui.screens.ShoppingScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.FamilyWallTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FamilyWallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FamilyWallTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: FamilyWallViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val members by viewModel.allMembers.collectAsStateWithLifecycle()
    val selectedMember by viewModel.selectedMemberFilter.collectAsStateWithLifecycle()
    val events by viewModel.allEvents.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val shoppingItems by viewModel.rawShoppingItems.collectAsStateWithLifecycle()

    val showAddShopping by viewModel.showAddShoppingDialog.collectAsStateWithLifecycle()
    val showAddEvent by viewModel.showAddEventDialog.collectAsStateWithLifecycle()
    val showAddTask by viewModel.showAddTaskDialog.collectAsStateWithLifecycle()
    val showAddRecipe by viewModel.showAddRecipeDialog.collectAsStateWithLifecycle()
    val selectedDateMillis by viewModel.selectedCalendarDateMillis.collectAsStateWithLifecycle()

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
                members = members,
                selectedMember = selectedMember,
                onSelectMember = { viewModel.selectMemberFilter(it) },
                pendingEventCount = events.size,
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
                                style = MaterialTheme.typography.labelMedium.copy(
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
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (currentTab == MainTab.CALENDAR) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_calendar")
                    )

                    val pendingTasksCount = tasks.count { !it.isCompleted }
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
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (currentTab == MainTab.TASKS) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_tasks")
                    )

                    val pendingShoppingCount = shoppingItems.count { !it.isChecked }
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
                                    contentDescription = "Compra"
                                )
                            }
                        },
                        label = {
                            Text(
                                "Compra",
                                style = MaterialTheme.typography.labelMedium.copy(
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
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (currentTab == MainTab.MEALS) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_meals")
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
            }
        }
    }

    if (showAddShopping) {
        AddShoppingItemDialog(
            members = members,
            onDismiss = { viewModel.setShowAddShoppingDialog(false) },
            onSave = { draft ->
                viewModel.addShoppingItem(draft)
                viewModel.setShowAddShoppingDialog(false)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Añadido '${draft.name}' a la lista de compra")
                }
            }
        )
    }

    if (showAddEvent) {
        AddCalendarEventDialog(
            members = members,
            initialDateMillis = selectedDateMillis,
            onDismiss = { viewModel.setShowAddEventDialog(false) },
            onSave = { draft ->
                viewModel.addCalendarEvent(draft)
                viewModel.setShowAddEventDialog(false)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Evento '${draft.title}' programado y notificado")
                }
            }
        )
    }

    if (showAddTask) {
        AddTaskDialog(
            members = members,
            onDismiss = { viewModel.setShowAddTaskDialog(false) },
            onSave = { draft ->
                viewModel.addTask(draft)
                viewModel.setShowAddTaskDialog(false)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Tarea asignada a ${draft.assignedMemberName}")
                }
            }
        )
    }

    if (showAddRecipe) {
        AddRecipeDialog(
            onDismiss = { viewModel.setShowAddRecipeDialog(false) },
            onSave = { draft ->
                viewModel.addRecipe(draft)
                viewModel.setShowAddRecipeDialog(false)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Receta '${draft.title}' guardada en el recetario")
                }
            }
        )
    }
}
