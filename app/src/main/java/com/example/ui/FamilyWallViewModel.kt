package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CalendarEvent
import com.example.data.model.CategoryScope
import com.example.data.model.CustomList
import com.example.data.model.CustomListItem
import com.example.data.model.FamilyCategory
import com.example.data.model.FamilyMember
import com.example.data.model.FamilyTask
import com.example.data.model.FamilyUnit
import com.example.data.model.ItemPriority
import com.example.data.model.MacroIngredient
import com.example.data.model.MacroTargets
import com.example.data.model.MealPlan
import com.example.data.model.MealType
import com.example.data.model.PetFeedingStatus
import com.example.data.model.ProductHistoryEntry
import com.example.data.model.Recipe
import com.example.data.model.ShoppingItem
import com.example.data.model.Supermarket
import com.example.data.model.TaskPriority
import com.example.data.model.UserProfile
import com.example.data.model.resolvedReminders
import com.example.data.preferences.AppDesignStyle
import com.example.data.preferences.AppearanceSettings
import com.example.data.preferences.ThemeDarkMode
import com.example.data.preferences.ThemePreferences
import com.example.data.repository.FirestoreRepository
import com.example.notification.FamilyNotificationManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainTab(val title: String) {
    HOME("Muro"),
    CALENDAR("Agenda"),
    TASKS("Tareas"),
    SHOPPING("Listas"),
    MEALS("Comidas"),
    MACROS("Macros")
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ShoppingItemDraft(
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "ud",
    val supermarket: Supermarket = Supermarket.MERCADONA,
    val priority: ItemPriority = ItemPriority.NORMAL,
    val hasOffer: Boolean = false,
    val offerDetails: String = "",
    val category: String = "Despensa",
    val addedByMemberName: String = "Familia",
    val assignedMemberName: String = "Todos",
    val notes: String = ""
)

data class CalendarEventDraft(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val endDateEpochMillis: Long = 0L,
    val extraDatesEpochMillis: List<Long> = emptyList(),
    val allDay: Boolean = false,
    val startTime: String = "10:00",
    val endTime: String = "11:00",
    val assignedMemberName: String = "Todos",
    val memberColorHex: String = "#1B4D3E",
    val category: String = "General",
    val reminderMinutesBefore: Int = 15,
    val reminderMinutesList: List<Int> = listOf(15)
)

data class TaskDraft(
    val title: String = "",
    val description: String = "",
    val assignedMemberName: String = "Cualquiera",
    val assignedMemberEmoji: String = "⭐",
    val dueDate: String = "Hoy",
    val dueEpochMillis: Long = 0L,
    val dueTime: String = "09:00",
    val reminderMinutesList: List<Int> = emptyList(),
    val priority: TaskPriority = TaskPriority.MEDIA,
    val category: String = "Hogar"
)

data class RecipeDraft(
    val title: String = "",
    val category: String = "Plato principal",
    val prepTimeMinutes: Int = 30,
    val servings: Int = 4,
    val ingredients: String = "",
    val instructions: String = "",
    val emoji: String = "🍳"
)

data class CustomListItemDraft(
    val listId: String = "",
    val title: String = "",
    val quantity: String = "",
    val notes: String = "",
    val assignedMemberName: String = "Todos"
)

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyWallViewModel(
    private val repository: FirestoreRepository,
    private val themePreferences: ThemePreferences,
    val notificationManager: FamilyNotificationManager,
    val currentUserId: String
) : ViewModel() {

    private companion object {
        const val TAG = "FamilyWallVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Error al cargar $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private fun <T> observeFamilySubcollection(
        label: String,
        streamFactory: (String) -> Flow<List<T>>
    ): StateFlow<List<T>> =
        activeFamilyId
            .flatMapLatest { familyId ->
                if (familyId.isNullOrBlank()) flowOf(emptyList())
                else streamFactory(familyId)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    // Appearance (Version 1 vs Version 2 + Light/Dark/System)
    val appearanceSettings: StateFlow<AppearanceSettings> = themePreferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppearanceSettings())

    fun setDesignStyle(style: AppDesignStyle) {
        viewModelScope.launch { themePreferences.setDesignStyle(style) }
    }

    fun setDarkMode(mode: ThemeDarkMode) {
        viewModelScope.launch { themePreferences.setDarkMode(mode) }
    }

    // Snackbar Events
    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private fun emitMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    // User & Family Session
    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiState("perfil")

    private val activeFamilyId: Flow<String?> = userProfileState
        .map { state -> (state as? UiState.Success)?.data?.familyId }
        .distinctUntilChanged()

    val familyUnitState: StateFlow<UiState<FamilyUnit?>> = activeFamilyId
        .flatMapLatest { familyId ->
            if (familyId.isNullOrBlank()) flowOf(null)
            else repository.observeFamilyUnit(familyId)
        }
        .toUiState("unidad familiar")

    private var lastSyncedSignature: String? = null
    private var lastCategorySeededFamilyId: String? = null
    private var lastMacroSeededFamilyId: String? = null
    private var lastScheduledEventSignatures: Map<String, String> = emptyMap()
    private var lastScheduledTaskSignatures: Map<String, String> = emptyMap()

    init {
        viewModelScope.launch {
            familyUnitState.collect { state ->
                val family = (state as? UiState.Success)?.data ?: return@collect
                val sig = "${family.id}:${family.memberUids.sorted().joinToString(",")}"
                if (sig != lastSyncedSignature && family.memberUids.isNotEmpty()) {
                    lastSyncedSignature = sig
                    repository.syncMemberUidsAcrossFamily(family)
                }
                if (family.id != lastCategorySeededFamilyId && family.memberUids.isNotEmpty()) {
                    lastCategorySeededFamilyId = family.id
                    repository.seedDefaultCategoriesIfNeeded(
                        familyId = family.id,
                        memberUids = family.memberUids,
                        onlyIfCompletelyEmpty = true
                    )
                }
                if (family.id != lastMacroSeededFamilyId && family.memberUids.isNotEmpty()) {
                    lastMacroSeededFamilyId = family.id
                    repository.seedDefaultMacroIngredientsIfNeeded(
                        familyId = family.id,
                        memberUids = family.memberUids,
                        onlyIfCompletelyEmpty = true
                    )
                }
            }
        }

        // Schedule local reminders on every assigned member's device when Firestore syncs events
        viewModelScope.launch {
            combine(allEvents, userProfileState, allMembers) { events, profileState, members ->
                Triple(events, (profileState as? UiState.Success)?.data, members)
            }.collect { (events, profile, members) ->
                val myName = members.firstOrNull { it.linkedUid == currentUserId }?.name
                    ?.takeIf { it.isNotBlank() }
                    ?: profile?.displayName?.takeIf { it.isNotBlank() }

                val nextSignatures = mutableMapOf<String, String>()
                events.forEach { ev ->
                    val isAssignedToMe =
                        ev.assignedMemberName.equals("Todos", ignoreCase = true) ||
                        ev.assignedMemberName.equals("Cualquiera", ignoreCase = true) ||
                        (myName != null && ev.assignedMemberName.equals(myName, ignoreCase = true))

                    if (isAssignedToMe && ev.id.isNotBlank()) {
                        val remindersSig = ev.resolvedReminders().sorted().joinToString(",")
                        val sig = "${ev.title}|${ev.dateEpochMillis}|${ev.allDay}|${ev.startTime}|${ev.endTime}|${ev.assignedMemberName}|$remindersSig"
                        nextSignatures[ev.id] = sig
                        if (lastScheduledEventSignatures[ev.id] != sig) {
                            notificationManager.scheduleEventReminders(ev)
                        }
                    }
                }

                lastScheduledEventSignatures.keys
                    .filter { it !in nextSignatures }
                    .forEach { removedId ->
                        notificationManager.cancelEventReminders(removedId)
                    }

                lastScheduledEventSignatures = nextSignatures
            }
        }

        // Schedule local reminders on every assigned member's device when Firestore syncs tasks
        viewModelScope.launch {
            combine(allTasks, userProfileState, allMembers) { tasks, profileState, members ->
                Triple(tasks, (profileState as? UiState.Success)?.data, members)
            }.collect { (tasks, profile, members) ->
                val myName = members.firstOrNull { it.linkedUid == currentUserId }?.name
                    ?.takeIf { it.isNotBlank() }
                    ?: profile?.displayName?.takeIf { it.isNotBlank() }

                val nextTaskSignatures = mutableMapOf<String, String>()
                tasks.forEach { task ->
                    val isAssignedToMe =
                        task.assignedMemberName.equals("Todos", ignoreCase = true) ||
                        task.assignedMemberName.equals("Cualquiera", ignoreCase = true) ||
                        (myName != null && task.assignedMemberName.equals(myName, ignoreCase = true))

                    if (
                        isAssignedToMe &&
                        !task.completed &&
                        task.id.isNotBlank() &&
                        task.dueEpochMillis > 0L &&
                        task.reminderMinutesList.isNotEmpty()
                    ) {
                        val remindersSig = task.reminderMinutesList.distinct().sorted().joinToString(",")
                        val sig = "${task.title}|${task.dueDate}|${task.dueEpochMillis}|${task.dueTime}|${task.assignedMemberName}|$remindersSig"
                        nextTaskSignatures[task.id] = sig
                        if (lastScheduledTaskSignatures[task.id] != sig) {
                            notificationManager.scheduleTaskReminders(task)
                        }
                    }
                }

                lastScheduledTaskSignatures.keys
                    .filter { it !in nextTaskSignatures }
                    .forEach { removedId ->
                        notificationManager.cancelTaskReminders(removedId)
                    }

                lastScheduledTaskSignatures = nextTaskSignatures
            }
        }
    }

    private val _familySetupBusy = MutableStateFlow(false)
    val familySetupBusy: StateFlow<Boolean> = _familySetupBusy.asStateFlow()

    private val _familySetupError = MutableStateFlow<String?>(null)
    val familySetupError: StateFlow<String?> = _familySetupError.asStateFlow()

    fun clearFamilySetupError() {
        _familySetupError.value = null
    }

    fun createFamilyUnit(
        familyName: String,
        memberName: String,
        avatarEmoji: String,
        colorHex: String,
        seedStarterData: Boolean
    ) {
        viewModelScope.launch {
            _familySetupBusy.value = true
            _familySetupError.value = null
            val res = repository.createFamilyUnit(familyName, memberName, avatarEmoji, colorHex, seedStarterData)
            _familySetupBusy.value = false
            res.onFailure { err ->
                _familySetupError.value = err.localizedMessage ?: "No se pudo crear la unidad familiar."
            }
        }
    }

    fun joinFamilyByCode(
        code: String,
        memberName: String,
        avatarEmoji: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            _familySetupBusy.value = true
            _familySetupError.value = null
            val res = repository.joinFamilyByCode(code, memberName, avatarEmoji, colorHex)
            _familySetupBusy.value = false
            res.onFailure { err ->
                _familySetupError.value = err.localizedMessage ?: "No se pudo unir con ese código."
            }
        }
    }

    fun leaveFamily() {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.leaveFamily(fam).onFailure {
                emitMessage("Error al salir de la unidad familiar")
            }
        }
    }

    // Navigation Tab
    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()
    fun selectTab(tab: MainTab) { _currentTab.value = tab }

    // Member Filter
    private val _selectedMemberFilter = MutableStateFlow("Todos")
    val selectedMemberFilter: StateFlow<String> = _selectedMemberFilter.asStateFlow()
    fun selectMemberFilter(memberName: String) { _selectedMemberFilter.value = memberName }

    // Shopping Filters
    private val _selectedSupermarketFilter = MutableStateFlow("Todos")
    val selectedSupermarketFilter: StateFlow<String> = _selectedSupermarketFilter.asStateFlow()

    private val _selectedShoppingPriorityFilter = MutableStateFlow("Todos")
    val selectedShoppingPriorityFilter: StateFlow<String> = _selectedShoppingPriorityFilter.asStateFlow()

    private val _shoppingSearchQuery = MutableStateFlow("")
    val shoppingSearchQuery: StateFlow<String> = _shoppingSearchQuery.asStateFlow()

    fun setSupermarketFilter(filter: String) { _selectedSupermarketFilter.value = filter }
    fun setShoppingPriorityFilter(filter: String) { _selectedShoppingPriorityFilter.value = filter }
    fun setShoppingSearchQuery(query: String) { _shoppingSearchQuery.value = query }

    // Dialog / Edit States
    private val _showAddShoppingDialog = MutableStateFlow(false)
    val showAddShoppingDialog: StateFlow<Boolean> = _showAddShoppingDialog.asStateFlow()
    fun setShowAddShoppingDialog(show: Boolean) {
        _showAddShoppingDialog.value = show
        if (!show) _editingShoppingItem.value = null
    }

    private val _editingShoppingItem = MutableStateFlow<ShoppingItem?>(null)
    val editingShoppingItem: StateFlow<ShoppingItem?> = _editingShoppingItem.asStateFlow()
    fun startEditingShoppingItem(item: ShoppingItem) {
        _editingShoppingItem.value = item
        _showAddShoppingDialog.value = true
    }

    private val _showAddEventDialog = MutableStateFlow(false)
    val showAddEventDialog: StateFlow<Boolean> = _showAddEventDialog.asStateFlow()
    fun setShowAddEventDialog(show: Boolean) {
        _showAddEventDialog.value = show
        if (!show) _editingEvent.value = null
    }

    private val _editingEvent = MutableStateFlow<CalendarEvent?>(null)
    val editingEvent: StateFlow<CalendarEvent?> = _editingEvent.asStateFlow()
    fun startEditingEvent(event: CalendarEvent) {
        _editingEvent.value = event
        _showAddEventDialog.value = true
    }

    private val _showAddTaskDialog = MutableStateFlow(false)
    val showAddTaskDialog: StateFlow<Boolean> = _showAddTaskDialog.asStateFlow()
    fun setShowAddTaskDialog(show: Boolean) {
        _showAddTaskDialog.value = show
        if (!show) _editingTask.value = null
    }

    private val _editingTask = MutableStateFlow<FamilyTask?>(null)
    val editingTask: StateFlow<FamilyTask?> = _editingTask.asStateFlow()
    fun startEditingTask(task: FamilyTask) {
        _editingTask.value = task
        _showAddTaskDialog.value = true
    }

    private val _showAddRecipeDialog = MutableStateFlow(false)
    val showAddRecipeDialog: StateFlow<Boolean> = _showAddRecipeDialog.asStateFlow()
    private val _prefillRecipeDraft = MutableStateFlow<RecipeDraft?>(null)
    val prefillRecipeDraft: StateFlow<RecipeDraft?> = _prefillRecipeDraft.asStateFlow()
    fun setShowAddRecipeDialog(show: Boolean) {
        _showAddRecipeDialog.value = show
        if (!show) _prefillRecipeDraft.value = null
    }
    fun openSaveMacroRecipeDialog(draft: RecipeDraft) {
        _prefillRecipeDraft.value = draft
        _showAddRecipeDialog.value = true
    }

    private val _selectedCalendarDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedCalendarDateMillis: StateFlow<Long> = _selectedCalendarDateMillis.asStateFlow()
    fun selectCalendarDate(millis: Long) { _selectedCalendarDateMillis.value = millis }

    // Category Manager Dialog state
    private val _manageCategoriesScope = MutableStateFlow<CategoryScope?>(null)
    val manageCategoriesScope: StateFlow<CategoryScope?> = _manageCategoriesScope.asStateFlow()
    fun openManageCategories(scope: CategoryScope) { _manageCategoriesScope.value = scope }
    fun closeManageCategories() { _manageCategoriesScope.value = null }

    // Family Data Streams
    val allMembers: StateFlow<List<FamilyMember>> =
        observeFamilySubcollection("miembros") { repository.observeMembers(it) }

    val allCategories: StateFlow<List<FamilyCategory>> =
        observeFamilySubcollection("categorías") { repository.observeCategories(it) }

    val productHistory: StateFlow<List<ProductHistoryEntry>> =
        observeFamilySubcollection("historial de productos") { repository.observeProductHistory(it) }

    val petFeedingStatus: StateFlow<PetFeedingStatus?> =
        observeFamilySubcollection("comida de Oliva") { repository.observePetFeeding(it) }
            .map { entries -> entries.firstOrNull { it.id == "oliva_feeding" } ?: entries.firstOrNull() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val rawShoppingItems: StateFlow<List<ShoppingItem>> =
        observeFamilySubcollection("compra") { repository.observeShoppingItems(it) }

    val filteredShoppingItems: StateFlow<List<ShoppingItem>> = combine(
        rawShoppingItems,
        _selectedSupermarketFilter,
        _selectedShoppingPriorityFilter,
        _shoppingSearchQuery
    ) { items, superMarket, priority, query ->
        items.filter { item ->
            val normalizedItemSuper = Supermarket.fromString(item.supermarket).displayName
            val matchesSupermarket = superMarket == "Todos" ||
                normalizedItemSuper.equals(superMarket, ignoreCase = true)
            val matchesPriority = when (priority) {
                "Todos" -> true
                "Urgente" -> item.priority == "URGENTE"
                "Club" -> item.priority == "CLUB"
                "Ofertas" -> item.hasOffer
                "Comprados" -> item.checked
                "Pendientes" -> !item.checked
                else -> true
            }
            val matchesQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.assignedMemberName.contains(query, ignoreCase = true)
            matchesSupermarket && matchesPriority && matchesQuery
        }.sortedWith(compareBy<ShoppingItem> { it.checked }.thenByDescending { it.createdAt?.seconds ?: 0L })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val customLists: StateFlow<List<CustomList>> =
        observeFamilySubcollection("listas") { repository.observeCustomLists(it) }

    val customListItems: StateFlow<List<CustomListItem>> =
        observeFamilySubcollection("elementos de listas") { repository.observeCustomListItems(it) }

    val allEvents: StateFlow<List<CalendarEvent>> =
        observeFamilySubcollection("agenda") { repository.observeEvents(it) }

    val filteredEvents: StateFlow<List<CalendarEvent>> = combine(
        allEvents,
        _selectedMemberFilter
    ) { events, member ->
        val scoped = if (member == "Todos") events
        else events.filter { it.assignedMemberName == member || it.assignedMemberName == "Todos" }
        scoped.sortedWith(compareBy<CalendarEvent> { it.dateEpochMillis }.thenBy { it.startTime })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val allTasks: StateFlow<List<FamilyTask>> =
        observeFamilySubcollection("tareas") { repository.observeTasks(it) }

    val filteredTasks: StateFlow<List<FamilyTask>> = combine(
        allTasks,
        _selectedMemberFilter
    ) { tasks, member ->
        val scoped = if (member == "Todos") tasks
        else tasks.filter { it.assignedMemberName == member || it.assignedMemberName == "Todos" || it.assignedMemberName == "Cualquiera" }
        scoped.sortedWith(compareBy<FamilyTask> { it.completed }.thenByDescending { it.createdAt?.seconds ?: 0L })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val allMealPlans: StateFlow<List<MealPlan>> =
        observeFamilySubcollection("comidas") { repository.observeMealPlans(it) }

    val allRecipes: StateFlow<List<Recipe>> =
        observeFamilySubcollection("recetas") { repository.observeRecipes(it) }

    val macroTargets: StateFlow<MacroTargets> =
        observeFamilySubcollection("objetivos de macros") { repository.observeMacroTargets(it) }
            .map { list -> list.firstOrNull { it.id == "default" } ?: list.firstOrNull() ?: MacroTargets() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MacroTargets())

    val macroIngredients: StateFlow<List<MacroIngredient>> =
        observeFamilySubcollection("ingredientes de macros") { repository.observeMacroIngredients(it) }
            .map { list -> list.sortedBy { it.name.lowercase() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private fun currentFamily(): FamilyUnit? = (familyUnitState.value as? UiState.Success)?.data
    private fun currentMemberDisplayName(): String {
        val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName
        return profileName?.takeIf { it.isNotBlank() } ?: allMembers.value.firstOrNull()?.name ?: "Familia"
    }

    // --- CATEGORY ACTIONS ---

    fun addCategory(scope: CategoryScope, name: String, emoji: String, colorHex: String) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.addCategory(fam.id, fam.memberUids, scope, name, emoji, colorHex)
                .onSuccess { emitMessage("Categoría '${name.trim()}' añadida") }
                .onFailure { emitMessage("No se pudo añadir la categoría") }
        }
    }

    fun updateCategory(category: FamilyCategory, oldName: String) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.updateCategory(fam.id, category, oldName)
                .onSuccess { emitMessage("Categoría '${category.name.trim()}' actualizada") }
                .onFailure { emitMessage("No se pudo actualizar la categoría") }
        }
    }

    fun deleteCategory(category: FamilyCategory) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.deleteCategory(fam.id, category.id)
                .onSuccess { emitMessage("Categoría '${category.name}' eliminada") }
                .onFailure { emitMessage("No se pudo eliminar la categoría") }
        }
    }

    fun restoreDefaultCategories() {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.seedDefaultCategoriesIfNeeded(fam.id, fam.memberUids, onlyIfCompletelyEmpty = false)
                .onSuccess { emitMessage("Categorías predeterminadas restauradas") }
                .onFailure { emitMessage("No se pudieron restaurar las categorías") }
        }
    }

    // --- MEMBERS ACTIONS ---

    fun addDependentMember(name: String, role: String, emoji: String, colorHex: String) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.addFamilyMember(fam.id, fam.memberUids, name, role, emoji, colorHex)
                .onSuccess { emitMessage("Miembro '$name' añadido al hogar") }
                .onFailure { emitMessage("No se pudo añadir el miembro") }
        }
    }

    // --- SHOPPING ACTIONS ---

    fun saveShoppingItem(draft: ShoppingItemDraft, editing: ShoppingItem?) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            if (editing == null) {
                val item = ShoppingItem(
                    familyId = fam.id,
                    name = draft.name.trim(),
                    quantity = draft.quantity,
                    unit = draft.unit,
                    supermarket = draft.supermarket.displayName,
                    priority = draft.priority.name,
                    hasOffer = draft.hasOffer,
                    offerDetails = if (draft.hasOffer) draft.offerDetails.trim() else "",
                    category = draft.category,
                    checked = false,
                    addedByMemberName = draft.addedByMemberName,
                    assignedMemberName = draft.assignedMemberName,
                    notes = draft.notes.trim()
                )
                repository.addShoppingItem(fam.id, fam.memberUids, item)
                    .onSuccess {
                        repository.recordProductHistory(
                            familyId = fam.id,
                            memberUids = fam.memberUids,
                            name = draft.name,
                            unit = draft.unit,
                            category = draft.category,
                            supermarket = draft.supermarket.displayName
                        )
                        emitMessage("Añadido '${draft.name.trim()}' a la compra")
                    }
                    .onFailure { emitMessage("No se pudo guardar el producto") }
            } else {
                val updated = editing.copy(
                    name = draft.name.trim(),
                    quantity = draft.quantity,
                    unit = draft.unit,
                    supermarket = draft.supermarket.displayName,
                    priority = draft.priority.name,
                    hasOffer = draft.hasOffer,
                    offerDetails = if (draft.hasOffer) draft.offerDetails.trim() else "",
                    category = draft.category,
                    addedByMemberName = draft.addedByMemberName,
                    assignedMemberName = draft.assignedMemberName,
                    notes = draft.notes.trim()
                )
                repository.updateShoppingItem(fam.id, updated)
                    .onSuccess {
                        repository.recordProductHistory(
                            familyId = fam.id,
                            memberUids = fam.memberUids,
                            name = draft.name,
                            unit = draft.unit,
                            category = draft.category,
                            supermarket = draft.supermarket.displayName
                        )
                        emitMessage("Producto '${draft.name.trim()}' actualizado")
                    }
                    .onFailure { emitMessage("No se pudo actualizar el producto") }
            }
        }
    }

    fun toggleShoppingItemChecked(item: ShoppingItem) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.setShoppingItemChecked(fam.id, item.id, !item.checked)
                .onFailure { emitMessage("Error al actualizar el estado") }
        }
    }

    fun updateShoppingItemQuantity(item: ShoppingItem, newQuantity: Double) {
        val fam = currentFamily() ?: return
        if (newQuantity <= 0) {
            deleteShoppingItem(item)
            return
        }
        viewModelScope.launch {
            repository.updateShoppingItemQuantity(fam.id, item.id, newQuantity)
                .onFailure { emitMessage("No se pudo actualizar la cantidad") }
        }
    }

    fun deleteShoppingItem(item: ShoppingItem) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.deleteShoppingItem(fam.id, item.id)
                .onFailure { emitMessage("No se pudo eliminar el producto") }
        }
    }

    fun clearCheckedShoppingItems() {
        val fam = currentFamily() ?: return
        val items = rawShoppingItems.value
        viewModelScope.launch {
            repository.deleteCheckedShoppingItems(fam.id, items)
                .onFailure { emitMessage("No se pudieron limpiar los comprados") }
        }
    }

    // --- GENERAL CUSTOM LISTS ACTIONS ---

    fun createCustomList(title: String, emoji: String, colorHex: String) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.createCustomList(fam.id, fam.memberUids, title, emoji, colorHex)
                .onSuccess { emitMessage("Lista '$title' creada") }
                .onFailure { emitMessage("No se pudo crear la lista") }
        }
    }

    fun deleteCustomList(list: CustomList) {
        val fam = currentFamily() ?: return
        val items = customListItems.value.filter { it.listId == list.id }
        viewModelScope.launch {
            repository.deleteCustomListAndItems(fam.id, list.id, items)
                .onSuccess { emitMessage("Lista '${list.title}' eliminada") }
                .onFailure { emitMessage("No se pudo eliminar la lista") }
        }
    }

    fun saveCustomListItem(draft: CustomListItemDraft, editing: CustomListItem?) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            if (editing == null) {
                val item = CustomListItem(
                    familyId = fam.id,
                    listId = draft.listId,
                    title = draft.title.trim(),
                    quantity = draft.quantity.trim(),
                    notes = draft.notes.trim(),
                    assignedMemberName = draft.assignedMemberName
                )
                repository.addCustomListItem(fam.id, fam.memberUids, item)
                    .onFailure { emitMessage("No se pudo añadir el elemento") }
            } else {
                val updated = editing.copy(
                    title = draft.title.trim(),
                    quantity = draft.quantity.trim(),
                    notes = draft.notes.trim(),
                    assignedMemberName = draft.assignedMemberName
                )
                repository.updateCustomListItem(fam.id, updated)
                    .onSuccess { emitMessage("Elemento actualizado") }
                    .onFailure { emitMessage("No se pudo actualizar el elemento") }
            }
        }
    }

    fun toggleCustomListItemChecked(item: CustomListItem) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.setCustomListItemChecked(fam.id, item.id, !item.checked)
                .onFailure { emitMessage("No se pudo marcar el elemento") }
        }
    }

    fun deleteCustomListItem(item: CustomListItem) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.deleteCustomListItem(fam.id, item.id)
                .onFailure { emitMessage("No se pudo eliminar el elemento") }
        }
    }

    // --- CALENDAR EVENT ACTIONS ---

    fun saveCalendarEvent(draft: CalendarEventDraft, editing: CalendarEvent?) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            val reminders = draft.reminderMinutesList.distinct().ifEmpty { listOf(draft.reminderMinutesBefore) }
            if (editing == null) {
                val entity = CalendarEvent(
                    familyId = fam.id,
                    title = draft.title.trim(),
                    description = draft.description.trim(),
                    location = draft.location.trim(),
                    dateEpochMillis = draft.dateEpochMillis,
                    endDateEpochMillis = draft.endDateEpochMillis,
                    extraDatesEpochMillis = draft.extraDatesEpochMillis,
                    allDay = draft.allDay,
                    startTime = if (draft.allDay) "00:00" else draft.startTime,
                    endTime = if (draft.allDay) "23:59" else draft.endTime,
                    assignedMemberName = draft.assignedMemberName,
                    memberColorHex = draft.memberColorHex,
                    category = draft.category.trim().ifBlank { "General" },
                    reminderMinutesBefore = reminders.first(),
                    reminderMinutesList = reminders,
                    notified = false
                )
                repository.addEvent(fam.id, fam.memberUids, entity)
                    .onSuccess {
                        emitMessage("Evento '${draft.title.trim()}' guardado")
                    }
                    .onFailure { emitMessage("No se pudo guardar el evento") }
            } else {
                val updated = editing.copy(
                    title = draft.title.trim(),
                    description = draft.description.trim(),
                    location = draft.location.trim(),
                    dateEpochMillis = draft.dateEpochMillis,
                    endDateEpochMillis = draft.endDateEpochMillis,
                    extraDatesEpochMillis = draft.extraDatesEpochMillis,
                    allDay = draft.allDay,
                    startTime = if (draft.allDay) "00:00" else draft.startTime,
                    endTime = if (draft.allDay) "23:59" else draft.endTime,
                    assignedMemberName = draft.assignedMemberName,
                    memberColorHex = draft.memberColorHex,
                    category = draft.category.trim().ifBlank { "General" },
                    reminderMinutesBefore = reminders.first(),
                    reminderMinutesList = reminders
                )
                repository.updateEvent(fam.id, updated)
                    .onSuccess {
                        emitMessage("Evento actualizado")
                    }
                    .onFailure { emitMessage("No se pudo actualizar el evento") }
            }
        }
    }

    fun deleteCalendarEvent(event: CalendarEvent) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            notificationManager.cancelEventReminders(event.id)
            repository.deleteEvent(fam.id, event.id)
                .onFailure { emitMessage("No se pudo eliminar el evento") }
        }
    }

    fun triggerEventNotification(event: CalendarEvent) {
        notificationManager.showEventNotification(event)
    }

    // --- TASK ACTIONS ---

    fun saveTask(draft: TaskDraft, editing: FamilyTask?) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            val reminders = if (draft.dueEpochMillis > 0L) draft.reminderMinutesList.distinct() else emptyList()
            if (editing == null) {
                val task = FamilyTask(
                    familyId = fam.id,
                    title = draft.title.trim(),
                    description = draft.description.trim(),
                    assignedMemberName = draft.assignedMemberName,
                    assignedMemberEmoji = draft.assignedMemberEmoji,
                    dueDate = draft.dueDate,
                    dueEpochMillis = draft.dueEpochMillis.coerceAtLeast(0L),
                    dueTime = draft.dueTime.ifBlank { "09:00" },
                    reminderMinutesList = reminders,
                    priority = draft.priority.name,
                    completed = false,
                    category = draft.category
                )
                repository.addTask(fam.id, fam.memberUids, task)
                    .onSuccess { emitMessage("Tarea asignada a ${draft.assignedMemberName}") }
                    .onFailure { emitMessage("No se pudo guardar la tarea") }
            } else {
                val updated = editing.copy(
                    title = draft.title.trim(),
                    description = draft.description.trim(),
                    assignedMemberName = draft.assignedMemberName,
                    assignedMemberEmoji = draft.assignedMemberEmoji,
                    dueDate = draft.dueDate,
                    dueEpochMillis = draft.dueEpochMillis.coerceAtLeast(0L),
                    dueTime = draft.dueTime.ifBlank { "09:00" },
                    reminderMinutesList = reminders,
                    priority = draft.priority.name,
                    category = draft.category
                )
                repository.updateTask(fam.id, updated)
                    .onSuccess { emitMessage("Tarea actualizada") }
                    .onFailure { emitMessage("No se pudo actualizar la tarea") }
            }
        }
    }

    fun toggleTaskCompleted(task: FamilyTask) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.setTaskCompleted(fam.id, task.id, !task.completed)
                .onFailure { emitMessage("No se pudo actualizar la tarea") }
        }
    }

    fun deleteTask(task: FamilyTask) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            notificationManager.cancelTaskReminders(task.id)
            repository.deleteTask(fam.id, task.id)
                .onFailure { emitMessage("No se pudo eliminar la tarea") }
        }
    }

    // --- MEAL PLAN & RECIPE ACTIONS ---

    fun updateMealPlanSlot(dayIndex: Int, mealType: MealType, dishName: String, recipeId: String? = null) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.upsertMealPlan(fam.id, fam.memberUids, dayIndex, mealType.name, dishName, recipeId)
                .onFailure { emitMessage("No se pudo guardar el menú") }
        }
    }

    fun addRecipe(draft: RecipeDraft) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            val recipe = Recipe(
                familyId = fam.id,
                title = draft.title.trim(),
                category = draft.category.trim(),
                prepTimeMinutes = draft.prepTimeMinutes,
                servings = draft.servings,
                ingredients = draft.ingredients.trim(),
                instructions = draft.instructions.trim(),
                emoji = draft.emoji,
                favorite = false
            )
            repository.addRecipe(fam.id, fam.memberUids, recipe)
                .onSuccess { emitMessage("Receta '${draft.title.trim()}' guardada") }
                .onFailure { emitMessage("No se pudo guardar la receta") }
        }
    }

    fun updateRecipe(recipe: Recipe) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.updateRecipe(fam.id, recipe)
                .onFailure { emitMessage("No se pudo actualizar la receta") }
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.deleteRecipe(fam.id, recipe.id)
                .onFailure { emitMessage("No se pudo eliminar la receta") }
        }
    }

    fun toggleRecipeFavorite(recipe: Recipe) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.setRecipeFavorite(fam.id, recipe.id, !recipe.favorite)
                .onFailure { emitMessage("Error al marcar favorita") }
        }
    }

    fun addRecipeIngredientsToShopping(
        recipe: Recipe,
        supermarket: Supermarket = Supermarket.MERCADONA,
        selectedIngredients: List<String>? = null
    ) {
        val fam = currentFamily() ?: return
        val lines = selectedIngredients
            ?: recipe.ingredients.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        viewModelScope.launch {
            repository.addSelectedIngredientsToShoppingList(
                familyId = fam.id,
                memberUids = fam.memberUids,
                recipeTitle = recipe.title,
                ingredients = lines,
                supermarket = supermarket.displayName,
                addedByMemberName = currentMemberDisplayName()
            ).onSuccess {
                notificationManager.showReminderNotification(
                    title = "Ingredientes añadidos",
                    message = "Se han añadido los ingredientes de '${recipe.title}' para comprar en ${supermarket.displayName}."
                )
                emitMessage("${lines.size} ingredientes añadidos a ${supermarket.displayName}")
            }.onFailure {
                emitMessage("No se pudieron añadir los ingredientes")
            }
        }
    }

    fun formatEventDate(epochMillis: Long): String {
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        return sdf.format(Date(epochMillis)).replaceFirstChar { it.uppercase() }
    }

    // --- OLIVA DAILY FEEDING ---

    fun toggleOlivaFedToday(currentlyFedToday: Boolean) {
        val fam = currentFamily() ?: return
        val now = Date()
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
        val timeLabel = SimpleDateFormat("HH:mm", Locale("es", "ES")).format(now)
        val memberName = currentMemberDisplayName()
        viewModelScope.launch {
            repository.toggleOlivaFedToday(
                familyId = fam.id,
                memberUids = fam.memberUids,
                memberName = memberName,
                currentlyFedToday = currentlyFedToday,
                dateKey = dateKey,
                timeLabel = timeLabel
            ).onSuccess { nowFed ->
                emitMessage(
                    if (nowFed) "¡Apuntado! Oliva ya ha comido hoy ($timeLabel)"
                    else "Comida de Oliva marcada como pendiente"
                )
            }.onFailure {
                emitMessage("No se pudo actualizar la comida de Oliva")
            }
        }
    }

    // --- MACROS (TARGETS & INGREDIENTS) ---

    fun saveMacroTargets(targets: MacroTargets) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.saveMacroTargets(fam.id, fam.memberUids, targets)
                .onSuccess { emitMessage("Objetivos de macros guardados") }
                .onFailure { emitMessage("No se pudieron guardar los objetivos") }
        }
    }

    fun addMacroIngredient(
        name: String,
        emoji: String,
        carbsPer100g: Double,
        proteinPer100g: Double,
        fatPer100g: Double
    ) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.addMacroIngredient(
                familyId = fam.id,
                memberUids = fam.memberUids,
                name = name,
                emoji = emoji,
                carbsPer100g = carbsPer100g,
                proteinPer100g = proteinPer100g,
                fatPer100g = fatPer100g
            ).onSuccess {
                emitMessage("Ingrediente '$name' añadido")
            }.onFailure {
                emitMessage("No se pudo añadir el ingrediente")
            }
        }
    }

    fun updateMacroIngredient(ingredient: MacroIngredient) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.updateMacroIngredient(fam.id, ingredient)
                .onSuccess { emitMessage("'${ingredient.name}' actualizado") }
                .onFailure { emitMessage("No se pudo actualizar el ingrediente") }
        }
    }

    fun deleteMacroIngredient(ingredient: MacroIngredient) {
        val fam = currentFamily() ?: return
        viewModelScope.launch {
            repository.deleteMacroIngredient(fam.id, ingredient.id)
                .onSuccess { emitMessage("Ingrediente eliminado") }
                .onFailure { emitMessage("No se pudo eliminar el ingrediente") }
        }
    }
}
