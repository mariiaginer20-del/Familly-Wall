package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CalendarEventEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.FamilyTaskEntity
import com.example.data.local.MealPlanEntity
import com.example.data.local.RecipeEntity
import com.example.data.local.ShoppingItemEntity
import com.example.data.model.EventCategory
import com.example.data.model.ItemPriority
import com.example.data.model.MealType
import com.example.data.model.Supermarket
import com.example.data.model.TaskPriority
import com.example.data.repository.FamilyRepository
import com.example.notification.FamilyNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class MainTab(val title: String, val iconName: String) {
    HOME("Muro", "Home"),
    CALENDAR("Agenda", "CalendarMonth"),
    TASKS("Tareas", "Checklist"),
    SHOPPING("Compra", "ShoppingCart"),
    MEALS("Comidas", "Restaurant")
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
    val addedByMemberName: String = "Mamá",
    val notes: String = ""
)

data class CalendarEventDraft(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val startTime: String = "10:00",
    val endTime: String = "11:00",
    val assignedMemberName: String = "Todos",
    val memberColorHex: String = "#3B82F6",
    val category: EventCategory = EventCategory.GENERAL,
    val reminderMinutesBefore: Int = 15
)

data class TaskDraft(
    val title: String = "",
    val description: String = "",
    val assignedMemberName: String = "Todos",
    val assignedMemberEmoji: String = "⭐",
    val dueDate: String = "Hoy",
    val priority: TaskPriority = TaskPriority.MEDIA,
    val category: String = "Hogar"
)

data class RecipeDraft(
    val title: String = "",
    val category: String = "General",
    val prepTimeMinutes: Int = 30,
    val servings: Int = 4,
    val ingredients: String = "",
    val instructions: String = "",
    val emoji: String = "🍳"
)

class FamilyWallViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FamilyRepository
    val notificationManager: FamilyNotificationManager

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = FamilyRepository(db.familyDao())
        notificationManager = FamilyNotificationManager(application)
    }

    // Navigation Tab
    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    // Active viewing member ("Todos", "Mamá", etc.)
    private val _selectedMemberFilter = MutableStateFlow("Todos")
    val selectedMemberFilter: StateFlow<String> = _selectedMemberFilter.asStateFlow()

    fun selectMemberFilter(memberName: String) {
        _selectedMemberFilter.value = memberName
    }

    // Shopping List Filters
    private val _selectedSupermarketFilter = MutableStateFlow("Todos")
    val selectedSupermarketFilter: StateFlow<String> = _selectedSupermarketFilter.asStateFlow()

    private val _selectedShoppingPriorityFilter = MutableStateFlow("Todos") // Todos, Urgente, Club, Ofertas
    val selectedShoppingPriorityFilter: StateFlow<String> = _selectedShoppingPriorityFilter.asStateFlow()

    private val _shoppingSearchQuery = MutableStateFlow("")
    val shoppingSearchQuery: StateFlow<String> = _shoppingSearchQuery.asStateFlow()

    fun setSupermarketFilter(filter: String) { _selectedSupermarketFilter.value = filter }
    fun setShoppingPriorityFilter(filter: String) { _selectedShoppingPriorityFilter.value = filter }
    fun setShoppingSearchQuery(query: String) { _shoppingSearchQuery.value = query }

    // Dialog & Sheet States
    private val _showAddShoppingDialog = MutableStateFlow(false)
    val showAddShoppingDialog: StateFlow<Boolean> = _showAddShoppingDialog.asStateFlow()
    fun setShowAddShoppingDialog(show: Boolean) { _showAddShoppingDialog.value = show }

    private val _showAddEventDialog = MutableStateFlow(false)
    val showAddEventDialog: StateFlow<Boolean> = _showAddEventDialog.asStateFlow()
    fun setShowAddEventDialog(show: Boolean) { _showAddEventDialog.value = show }

    private val _showAddTaskDialog = MutableStateFlow(false)
    val showAddTaskDialog: StateFlow<Boolean> = _showAddTaskDialog.asStateFlow()
    fun setShowAddTaskDialog(show: Boolean) { _showAddTaskDialog.value = show }

    private val _showAddRecipeDialog = MutableStateFlow(false)
    val showAddRecipeDialog: StateFlow<Boolean> = _showAddRecipeDialog.asStateFlow()
    fun setShowAddRecipeDialog(show: Boolean) { _showAddRecipeDialog.value = show }

    private val _selectedCalendarDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedCalendarDateMillis: StateFlow<Long> = _selectedCalendarDateMillis.asStateFlow()
    fun selectCalendarDate(millis: Long) { _selectedCalendarDateMillis.value = millis }

    // Repositories StateFlows
    val allMembers: StateFlow<List<FamilyMemberEntity>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawShoppingItems: StateFlow<List<ShoppingItemEntity>> = repository.allShoppingItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredShoppingItems: StateFlow<List<ShoppingItemEntity>> = combine(
        rawShoppingItems,
        _selectedSupermarketFilter,
        _selectedShoppingPriorityFilter,
        _shoppingSearchQuery
    ) { items, superMarket, priority, query ->
        items.filter { item ->
            val matchesSupermarket = superMarket == "Todos" || item.supermarket.equals(superMarket, ignoreCase = true)
            val matchesPriority = when (priority) {
                "Todos" -> true
                "Urgente" -> item.priority == "URGENTE"
                "Club" -> item.priority == "CLUB"
                "Ofertas" -> item.hasOffer
                "Comprados" -> item.isChecked
                "Pendientes" -> !item.isChecked
                else -> true
            }
            val matchesQuery = query.isBlank() || item.name.contains(query, ignoreCase = true) || item.category.contains(query, ignoreCase = true)
            matchesSupermarket && matchesPriority && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEvents: StateFlow<List<CalendarEventEntity>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredEvents: StateFlow<List<CalendarEventEntity>> = combine(
        allEvents,
        _selectedMemberFilter
    ) { events, member ->
        if (member == "Todos") {
            events
        } else {
            events.filter { it.assignedMemberName == member || it.assignedMemberName == "Todos" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<FamilyTaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTasks: StateFlow<List<FamilyTaskEntity>> = combine(
        allTasks,
        _selectedMemberFilter
    ) { tasks, member ->
        if (member == "Todos") {
            tasks
        } else {
            tasks.filter { it.assignedMemberName == member || it.assignedMemberName == "Todos" || it.assignedMemberName == "Cualquiera" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMealPlans: StateFlow<List<MealPlanEntity>> = repository.allMealPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecipes: StateFlow<List<RecipeEntity>> = repository.allRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shopping Actions
    fun addShoppingItem(draft: ShoppingItemDraft) {
        viewModelScope.launch {
            val entity = ShoppingItemEntity(
                name = draft.name.trim(),
                quantity = draft.quantity,
                unit = draft.unit,
                supermarket = draft.supermarket.displayName,
                priority = draft.priority.name,
                hasOffer = draft.hasOffer,
                offerDetails = if (draft.hasOffer) draft.offerDetails.trim() else "",
                category = draft.category,
                isChecked = false,
                addedByMemberName = draft.addedByMemberName,
                notes = draft.notes.trim()
            )
            repository.insertShoppingItem(entity)
        }
    }

    fun toggleShoppingItemChecked(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.setShoppingItemChecked(item.id, !item.isChecked)
        }
    }

    fun updateShoppingItemQuantity(item: ShoppingItemEntity, newQuantity: Double) {
        if (newQuantity <= 0) {
            deleteShoppingItem(item)
            return
        }
        viewModelScope.launch {
            repository.updateShoppingItem(item.copy(quantity = newQuantity))
        }
    }

    fun deleteShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.deleteShoppingItem(item)
        }
    }

    fun clearCheckedShoppingItems() {
        viewModelScope.launch {
            repository.deleteCheckedShoppingItems()
        }
    }

    // Calendar Event Actions
    fun addCalendarEvent(draft: CalendarEventDraft) {
        viewModelScope.launch {
            val entity = CalendarEventEntity(
                title = draft.title.trim(),
                description = draft.description.trim(),
                location = draft.location.trim(),
                dateEpochMillis = draft.dateEpochMillis,
                startTime = draft.startTime,
                endTime = draft.endTime,
                assignedMemberName = draft.assignedMemberName,
                memberColorHex = draft.memberColorHex,
                category = draft.category.name,
                reminderMinutesBefore = draft.reminderMinutesBefore,
                isNotified = false
            )
            val newId = repository.insertEvent(entity)

            // Trigger notification test/confirmation for the user
            notificationManager.showEventNotification(entity.copy(id = newId))
        }
    }

    fun deleteCalendarEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    fun triggerEventNotification(event: CalendarEventEntity) {
        notificationManager.showEventNotification(event)
    }

    // Task Actions
    fun addTask(draft: TaskDraft) {
        viewModelScope.launch {
            val entity = FamilyTaskEntity(
                title = draft.title.trim(),
                description = draft.description.trim(),
                assignedMemberName = draft.assignedMemberName,
                assignedMemberEmoji = draft.assignedMemberEmoji,
                dueDate = draft.dueDate,
                priority = draft.priority.name,
                isCompleted = false,
                category = draft.category
            )
            repository.insertTask(entity)
        }
    }

    fun toggleTaskCompleted(task: FamilyTaskEntity) {
        viewModelScope.launch {
            repository.setTaskCompleted(task.id, !task.isCompleted)
        }
    }

    fun deleteTask(task: FamilyTaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Meal Plan Actions
    fun updateMealPlanSlot(dayIndex: Int, mealType: MealType, dishName: String, recipeId: Long? = null) {
        viewModelScope.launch {
            val trimmed = dishName.trim()
            val existing = allMealPlans.value.firstOrNull { it.dayOfWeekIndex == dayIndex && it.mealType == mealType.name }
            if (trimmed.isBlank()) {
                if (existing != null) {
                    repository.deleteMealPlan(existing)
                }
            } else if (existing != null) {
                repository.updateMealPlan(existing.copy(dishName = trimmed, recipeId = recipeId))
            } else {
                repository.insertMealPlan(
                    MealPlanEntity(
                        dayOfWeekIndex = dayIndex,
                        mealType = mealType.name,
                        dishName = trimmed,
                        recipeId = recipeId
                    )
                )
            }
        }
    }

    // Recipe Actions
    fun addRecipe(draft: RecipeDraft) {
        viewModelScope.launch {
            val entity = RecipeEntity(
                title = draft.title.trim(),
                category = draft.category.trim(),
                prepTimeMinutes = draft.prepTimeMinutes,
                servings = draft.servings,
                ingredients = draft.ingredients.trim(),
                instructions = draft.instructions.trim(),
                emoji = draft.emoji
            )
            repository.insertRecipe(entity)
        }
    }

    fun updateRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            repository.updateRecipe(recipe)
        }
    }

    fun deleteRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            repository.deleteRecipe(recipe)
        }
    }

    fun toggleRecipeFavorite(recipe: RecipeEntity) {
        viewModelScope.launch {
            repository.updateRecipe(recipe.copy(isFavorite = !recipe.isFavorite))
        }
    }

    fun addRecipeIngredientsToShopping(
        recipe: RecipeEntity,
        supermarket: Supermarket = Supermarket.MERCADONA,
        selectedIngredients: List<String>? = null
    ) {
        viewModelScope.launch {
            if (selectedIngredients != null) {
                repository.addSelectedIngredientsToShoppingList(recipe.title, selectedIngredients, supermarket.displayName)
            } else {
                repository.addRecipeIngredientsToShoppingList(recipe, supermarket.displayName)
            }
            notificationManager.showReminderNotification(
                title = "🛒 Ingredientes añadidos",
                message = "Se han añadido los ingredientes de '${recipe.title}' para comprar en ${supermarket.displayName}."
            )
        }
    }

    // Quick helper for formatted date strings
    fun formatEventDate(epochMillis: Long): String {
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        return sdf.format(Date(epochMillis)).replaceFirstChar { it.uppercase() }
    }
}
