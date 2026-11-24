package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryScope
import com.example.data.model.MealPlan
import com.example.data.model.MealType
import com.example.data.model.Recipe
import com.example.data.model.Supermarket
import com.example.ui.FamilyWallViewModel
import com.example.ui.dialogs.AddRecipeDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class PlannerDayInfo(
    val index: Int,
    val dayName: String,
    val shortName: String,
    val dateLabel: String,
    val isToday: Boolean,
    val weekIndex: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealsScreen(
    viewModel: FamilyWallViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Planning de Comidas, 1 = Recetario

    val mealPlans by viewModel.allMealPlans.collectAsStateWithLifecycle()
    val recipes by viewModel.allRecipes.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    val plannerDays = remember {
        val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        val shortNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val todayCal = Calendar.getInstance()
        val dow = todayCal.get(Calendar.DAY_OF_WEEK)
        val todayOffset = if (dow == Calendar.SUNDAY) 6 else dow - 2
        val mondayCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -todayOffset)
        }
        val sdf = SimpleDateFormat("d MMM", Locale("es", "ES"))
        (0 until 14).map { idx ->
            val cal = (mondayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, idx) }
            PlannerDayInfo(
                index = idx,
                dayName = dayNames[idx % 7],
                shortName = shortNames[idx % 7],
                dateLabel = sdf.format(cal.time),
                isToday = idx == todayOffset,
                weekIndex = idx / 7
            )
        }
    }
    val todayIndex = remember(plannerDays) {
        plannerDays.firstOrNull { it.isToday }?.index ?: 0
    }

    var showFullWeekView by remember { mutableStateOf(true) }
    var weekFilter by remember { mutableIntStateOf(0) } // 0 = 2 semanas, 1 = Esta semana, 2 = Próx. semana
    var selectedDayIndex by remember { mutableIntStateOf(todayIndex) }

    var editingSlot by remember { mutableStateOf<Pair<Int, MealType>?>(null) }
    var editingDishName by remember { mutableStateOf("") }
    var editingLinkedRecipeId by remember { mutableStateOf<String?>(null) }

    var recipeSearchQuery by remember { mutableStateOf("") }
    var selectedRecipeFilter by remember { mutableStateOf("Todas") }
    var selectedRecipeForDetails by remember { mutableStateOf<Recipe?>(null) }
    var recipeToEdit by remember { mutableStateOf<Recipe?>(null) }
    var recipeToTransfer by remember { mutableStateOf<Pair<Recipe, List<String>?>?>(null) }
    var recipeToAssignToPlan by remember { mutableStateOf<Recipe?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val customRecipeCategories = remember(allCategories) {
        allCategories.filter { it.scope == CategoryScope.RECIPE.name }
    }

    val recipeCategories = remember(recipes, customRecipeCategories) {
        val merged = (customRecipeCategories.map { it.name } + recipes.map { it.category })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()
        listOf("Todas", "Favoritas") + merged
    }

    val filteredRecipes = remember(recipes, recipeSearchQuery, selectedRecipeFilter) {
        recipes.filter { r ->
            val matchesFilter = when (selectedRecipeFilter) {
                "Todas" -> true
                "Favoritas" -> r.favorite
                else -> r.category.equals(selectedRecipeFilter, ignoreCase = true)
            }
            val matchesQuery = recipeSearchQuery.isBlank() ||
                r.title.contains(recipeSearchQuery, ignoreCase = true) ||
                r.ingredients.contains(recipeSearchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedSubTab == 0) "COMIDAS Y CENAS · 2 SEMANAS" else "COCINA Y DESPENSA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (selectedSubTab == 0) "Planning quincenal" else "Recetario familiar",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                if (selectedSubTab == 1) {
                    IconButton(
                        onClick = { viewModel.openManageCategories(CategoryScope.RECIPE) },
                        modifier = Modifier.testTag("recipes_manage_categories_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Category,
                            contentDescription = "Gestionar categorías de recetas",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (feedbackMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = feedbackMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { feedbackMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Cerrar aviso",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Editorial Segmented Switcher: Planning vs Recetario
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isPlan = selectedSubTab == 0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPlan) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedSubTab = 0 }
                            .testTag("tab_weekly_planner")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        ) {
                            Text(
                                text = "Planning semanal",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isPlan) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isPlan) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isPlan) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedSubTab = 1 }
                            .testTag("tab_recipes")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        ) {
                            Text(
                                text = "Recetario (${recipes.size})",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (!isPlan) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (!isPlan) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedSubTab == 0) {
                // PLANNING DE COMIDAS (2 SEMANAS, COMIDA Y CENA)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (showFullWeekView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = if (showFullWeekView) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showFullWeekView = true }
                            .testTag("planner_mode_week")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.ViewWeek,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (showFullWeekView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Vista completa",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (showFullWeekView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (!showFullWeekView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = if (!showFullWeekView) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showFullWeekView = false }
                            .testTag("planner_mode_day")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.ViewAgenda,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (!showFullWeekView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Por día",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (!showFullWeekView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (showFullWeekView) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val scopeOptions = listOf(
                            0 to "2 semanas (14 días)",
                            1 to "Esta semana",
                            2 to "Próxima semana"
                        )
                        items(scopeOptions) { (id, label) ->
                            val picked = weekFilter == id
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (picked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { weekFilter = id }
                                    .testTag("planner_week_filter_$id")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (picked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val visibleDays = remember(plannerDays, weekFilter) {
                        when (weekFilter) {
                            1 -> plannerDays.filter { it.weekIndex == 0 }
                            2 -> plannerDays.filter { it.weekIndex == 1 }
                            else -> plannerDays
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val week1Days = visibleDays.filter { it.weekIndex == 0 }
                        val week2Days = visibleDays.filter { it.weekIndex == 1 }

                        if (week1Days.isNotEmpty()) {
                            item {
                                Text(
                                    text = "ESTA SEMANA",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.1.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(week1Days, key = { it.index }) { dayInfo ->
                                val dayMeals = mealPlans.filter { it.dayOfWeekIndex == dayInfo.index }
                                WeeklyDayCard(
                                    dayName = "${dayInfo.dayName} · ${dayInfo.dateLabel}",
                                    isToday = dayInfo.isToday,
                                    dayMeals = dayMeals,
                                    onSlotClick = { mType, currentDish, linkedId ->
                                        editingSlot = dayInfo.index to mType
                                        editingDishName = currentDish
                                        editingLinkedRecipeId = linkedId
                                    }
                                )
                            }
                        }

                        if (week2Days.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "PRÓXIMA SEMANA",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.1.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            items(week2Days, key = { it.index }) { dayInfo ->
                                val dayMeals = mealPlans.filter { it.dayOfWeekIndex == dayInfo.index }
                                WeeklyDayCard(
                                    dayName = "${dayInfo.dayName} · ${dayInfo.dateLabel}",
                                    isToday = dayInfo.isToday,
                                    dayMeals = dayMeals,
                                    onSlotClick = { mType, currentDish, linkedId ->
                                        editingSlot = dayInfo.index to mType
                                        editingDishName = currentDish
                                        editingLinkedRecipeId = linkedId
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(plannerDays, key = { it.index }) { dayInfo ->
                            val isSelected = selectedDayIndex == dayInfo.index
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { selectedDayIndex = dayInfo.index }
                                    .testTag("planner_day_${dayInfo.index}")
                            ) {
                                Text(
                                    text = if (dayInfo.isToday) "${dayInfo.shortName} ${dayInfo.dateLabel} · Hoy" else "${dayInfo.shortName} ${dayInfo.dateLabel}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val selectedDayInfo = plannerDays.getOrNull(selectedDayIndex) ?: plannerDays.first()
                    val dayMeals = mealPlans.filter { it.dayOfWeekIndex == selectedDayInfo.index }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Menú del ${selectedDayInfo.dayName}, ${selectedDayInfo.dateLabel}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(MealType.plannerTypes) { mType ->
                            val existingPlan = dayMeals.firstOrNull { it.mealType == mType.name }
                            val dish = existingPlan?.dishName ?: "Sin planificar"
                            val linkedRecipe = existingPlan?.recipeId?.let { rid ->
                                recipes.firstOrNull { it.id == rid }
                            } ?: recipes.firstOrNull { it.title.equals(dish, ignoreCase = true) }

                            MealSlotCard(
                                mealType = mType,
                                dishName = dish,
                                hasLinkedRecipe = linkedRecipe != null,
                                onEdit = {
                                    editingSlot = selectedDayInfo.index to mType
                                    editingDishName = if (dish != "Sin planificar") dish else ""
                                    editingLinkedRecipeId = existingPlan?.recipeId
                                },
                                onOpenRecipe = {
                                    if (linkedRecipe != null) {
                                        selectedRecipeForDetails = linkedRecipe
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            } else {
                // RECETARIO
                OutlinedTextField(
                    value = recipeSearchQuery,
                    onValueChange = { recipeSearchQuery = it },
                    placeholder = { Text("Buscar receta o ingrediente...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (recipeSearchQuery.isNotBlank()) {
                            IconButton(onClick = { recipeSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipe_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recipeCategories) { cat ->
                        val isSelected = selectedRecipeFilter == cat
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { selectedRecipeFilter = cat }
                                .testTag("recipe_filter_$cat")
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredRecipes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (recipes.isEmpty()) "Aún no hay recetas guardadas" else "No hay recetas con ese filtro",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Guarda tus recetas familiares con ingredientes, pasos y añádelas a la compra.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredRecipes, key = { it.id }) { recipe ->
                            RecipeCard(
                                recipe = recipe,
                                onOpen = { selectedRecipeForDetails = recipe },
                                onToggleFavorite = { viewModel.toggleRecipeFavorite(recipe) },
                                onAddToShopping = { recipeToTransfer = recipe to null }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(84.dp))
                        }
                    }
                }
            }
        }

        if (selectedSubTab == 1) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.setShowAddRecipeDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva receta", style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_recipe_fab")
            )
        }
    }

    // Dialog: Edit / Assign Meal Slot
    if (editingSlot != null) {
        val (dayIdx, mType) = editingSlot!!
        val dayInfo = plannerDays.getOrNull(dayIdx)
        val dayTitle = if (dayInfo != null) "${dayInfo.dayName} (${dayInfo.dateLabel})" else "Día ${dayIdx + 1}"
        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = {
                Text("${mType.displayName} · $dayTitle", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editingDishName,
                        onValueChange = {
                            editingDishName = it
                            editingLinkedRecipeId = null
                        },
                        label = { Text("Plato o menú") },
                        placeholder = { Text("ej: Lentejas, Salmón al horno...") },
                        singleLine = true,
                        trailingIcon = {
                            if (editingDishName.isNotBlank()) {
                                IconButton(onClick = {
                                    editingDishName = ""
                                    editingLinkedRecipeId = null
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Vaciar")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_meal_input")
                    )

                    if (recipes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Elegir del recetario:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recipes.forEach { recipe ->
                                val isPicked = editingLinkedRecipeId == recipe.id || editingDishName.equals(recipe.title, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isPicked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            editingDishName = recipe.title
                                            editingLinkedRecipeId = recipe.id
                                        }
                                ) {
                                    Text(
                                        text = "${recipe.emoji} ${recipe.title}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isPicked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateMealPlanSlot(dayIdx, mType, editingDishName, editingLinkedRecipeId)
                        feedbackMessage = if (editingDishName.isBlank()) {
                            "${mType.displayName} ($dayTitle) vaciado"
                        } else {
                            "${mType.displayName} del $dayTitle: $editingDishName"
                        }
                        editingSlot = null
                    },
                    modifier = Modifier.testTag("save_meal_button")
                ) {
                    Text(if (editingDishName.isBlank()) "Vaciar" else "Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSlot = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Recipe Details
    if (selectedRecipeForDetails != null) {
        val r = selectedRecipeForDetails!!
        val ingredientLines = remember(r.id, r.ingredients) {
            r.ingredients.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        }
        val selectedIngredients = remember(r.id, r.ingredients) {
            mutableStateListOf<Boolean>().apply {
                addAll(List(ingredientLines.size) { true })
            }
        }

        AlertDialog(
            onDismissRequest = { selectedRecipeForDetails = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(text = r.emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = r.title,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Row {
                        IconButton(
                            onClick = {
                                val current = r
                                selectedRecipeForDetails = null
                                recipeToEdit = current
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar receta", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                viewModel.deleteRecipe(r)
                                selectedRecipeForDetails = null
                                feedbackMessage = "Receta '${r.title}' eliminada"
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Eliminar receta",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "${r.prepTimeMinutes} min · ${r.servings} personas · ${r.category}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Ingredientes (marca los que faltan)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (ingredientLines.isEmpty()) {
                        Text(
                            text = "Sin ingredientes especificados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ingredientLines.forEachIndexed { idx, ing ->
                            val isChecked = selectedIngredients.getOrElse(idx) { true }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (idx < selectedIngredients.size) {
                                            selectedIngredients[idx] = !isChecked
                                        }
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (idx < selectedIngredients.size) {
                                            selectedIngredients[idx] = checked
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ing,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Pasos de preparación",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = r.instructions.ifBlank { "Sin pasos de preparación." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            val current = r
                            selectedRecipeForDetails = null
                            recipeToAssignToPlan = current
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Asignar al planning semanal")
                    }
                }
            },
            confirmButton = {
                val chosenList = ingredientLines.filterIndexed { idx, _ -> selectedIngredients.getOrElse(idx) { true } }
                Button(
                    onClick = {
                        val current = r
                        selectedRecipeForDetails = null
                        recipeToTransfer = current to chosenList
                    },
                    enabled = chosenList.isNotEmpty(),
                    modifier = Modifier.testTag("recipe_detail_add_to_shopping_btn")
                ) {
                    Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Añadir (${chosenList.size}) a compra")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRecipeForDetails = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (recipeToEdit != null) {
        val editing = recipeToEdit!!
        AddRecipeDialog(
            categories = customRecipeCategories,
            initialRecipe = editing,
            onManageCategories = { viewModel.openManageCategories(CategoryScope.RECIPE) },
            onDismiss = { recipeToEdit = null },
            onSave = { draft ->
                viewModel.updateRecipe(
                    editing.copy(
                        title = draft.title.trim(),
                        category = draft.category.trim(),
                        prepTimeMinutes = draft.prepTimeMinutes,
                        servings = draft.servings,
                        ingredients = draft.ingredients.trim(),
                        instructions = draft.instructions.trim(),
                        emoji = draft.emoji
                    )
                )
                feedbackMessage = "Receta '${draft.title.trim()}' actualizada"
                recipeToEdit = null
            }
        )
    }

    if (recipeToAssignToPlan != null) {
        val r = recipeToAssignToPlan!!
        var selectedWeek by remember { mutableIntStateOf(todayIndex / 7) }
        var targetDayInWeek by remember { mutableIntStateOf(todayIndex % 7) }
        var targetMealType by remember { mutableStateOf(MealType.COMIDA) }
        val targetDayIdx = selectedWeek * 7 + targetDayInWeek
        val weekDays = plannerDays.filter { it.weekIndex == selectedWeek }

        AlertDialog(
            onDismissRequest = { recipeToAssignToPlan = null },
            title = { Text("Asignar '${r.title}'", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Semana", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Esta semana", 1 to "Próxima semana").forEach { (wIdx, label) ->
                            val isSelected = selectedWeek == wIdx
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedWeek = wIdx }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Día", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weekDays.forEach { dayInfo ->
                            val dayInWeek = dayInfo.index % 7
                            val isSelected = targetDayInWeek == dayInWeek
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { targetDayInWeek = dayInWeek }
                            ) {
                                Text(
                                    text = "${dayInfo.shortName} ${dayInfo.dateLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Turno", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MealType.plannerTypes.forEach { mType ->
                            val isSelected = targetMealType == mType
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { targetMealType = mType }
                            ) {
                                Text(
                                    text = mType.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val chosenDay = plannerDays.getOrNull(targetDayIdx)
                val dayDesc = if (chosenDay != null) "${chosenDay.dayName} ${chosenDay.dateLabel}" else "día"
                Button(
                    onClick = {
                        viewModel.updateMealPlanSlot(targetDayIdx, targetMealType, r.title, r.id)
                        feedbackMessage = "'${r.title}' añadido al $dayDesc (${targetMealType.displayName})"
                        recipeToAssignToPlan = null
                    }
                ) {
                    Text("Asignar al menú")
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToAssignToPlan = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (recipeToTransfer != null) {
        val (r, chosenIngredients) = recipeToTransfer!!
        var chosenSupermarket by remember { mutableStateOf(Supermarket.MERCADONA) }

        AlertDialog(
            onDismissRequest = { recipeToTransfer = null },
            title = { Text("Pasar ingredientes a la compra", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    val countLabel = chosenIngredients?.size?.toString() ?: "todos los"
                    Text(
                        text = "¿En qué supermercado quieres añadir $countLabel ingredientes de '${r.title}'?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Supermarket.entries.forEach { smkt ->
                        val isSelected = chosenSupermarket == smkt
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { chosenSupermarket = smkt }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = smkt.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addRecipeIngredientsToShopping(r, chosenSupermarket, chosenIngredients)
                        feedbackMessage = "Ingredientes de '${r.title}' añadidos a ${chosenSupermarket.displayName}"
                        recipeToTransfer = null
                    },
                    modifier = Modifier.testTag("confirm_transfer_ingredients_btn")
                ) {
                    Text("Añadir a la lista")
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToTransfer = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun WeeklyDayCard(
    dayName: String,
    isToday: Boolean,
    dayMeals: List<MealPlan>,
    onSlotClick: (MealType, String, String?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isToday) 1.5.dp else 1.dp,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (isToday) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "HOY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                val plannerTypeNames = MealType.plannerTypes.map { it.name }
                val plannedCount = dayMeals.count { it.dishName.isNotBlank() && it.mealType in plannerTypeNames }
                Text(
                    text = "$plannedCount / ${MealType.plannerTypes.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

            MealType.plannerTypes.forEachIndexed { idx, mType ->
                val slot = dayMeals.firstOrNull { it.mealType == mType.name }
                val dish = slot?.dishName?.takeIf { it.isNotBlank() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSlotClick(mType, dish ?: "", slot?.recipeId) }
                        .padding(vertical = 9.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mType.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(88.dp)
                    )

                    Text(
                        text = dish ?: "Añadir ${mType.displayName.lowercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (dish != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Editar ${mType.displayName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                }
                if (idx < MealType.plannerTypes.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                }
            }
        }
    }
}

@Composable
fun MealSlotCard(
    mealType: MealType,
    dishName: String,
    hasLinkedRecipe: Boolean = false,
    onEdit: () -> Unit,
    onOpenRecipe: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onEdit),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mealType.displayName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dishName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (dishName != "Sin planificar") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasLinkedRecipe) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onOpenRecipe)
                    ) {
                        Text(
                            text = "Ver receta",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Editar plato",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun RecipeCard(
    recipe: Recipe,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToShopping: () -> Unit
) {
    val ingredientCount = remember(recipe.ingredients) {
        recipe.ingredients.split("\n").map { it.trim() }.count { it.isNotBlank() }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpen)
            .testTag("recipe_card_${recipe.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${recipe.emoji} ${recipe.title}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (recipe.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorita",
                        tint = if (recipe.favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${recipe.prepTimeMinutes} min · ${recipe.servings} pers. · $ingredientCount ing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onAddToShopping,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("recipe_quick_add_shopping_${recipe.id}"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "A la compra",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
