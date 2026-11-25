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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MacroDominance
import com.example.data.model.MacroIngredient
import com.example.data.model.MacroMealSlot
import com.example.data.model.MacroSolver
import com.example.data.model.MacroTriple
import com.example.data.model.MacroUnits
import com.example.data.model.RecipeFormatters
import com.example.data.model.RecipeIngredient
import com.example.data.model.dailyTriple
import com.example.data.model.effectiveCarbsPerRef
import com.example.data.model.effectiveFatPerRef
import com.example.data.model.effectiveProteinPerRef
import com.example.data.model.effectiveRefKcal
import com.example.data.model.formatReferenceLabel
import com.example.data.model.targetForMeal
import com.example.ui.FamilyWallViewModel
import com.example.ui.RecipeDraft
import com.example.ui.dialogs.MacroIngredientEditorDialog
import com.example.ui.dialogs.MacroTargetsDialog
import kotlin.math.roundToInt

private val CarbColor = Color(0xFFD97706)
private val ProteinColor = Color(0xFF157A4B)
private val FatColor = Color(0xFF2847D6)

private fun macroColor(macro: MacroDominance): Color = when (macro) {
    MacroDominance.CARB -> CarbColor
    MacroDominance.PROTEIN -> ProteinColor
    MacroDominance.FAT -> FatColor
}

private fun macroShortLabel(macro: MacroDominance): String = when (macro) {
    MacroDominance.CARB -> "HC"
    MacroDominance.PROTEIN -> "P"
    MacroDominance.FAT -> "G"
}

private fun formatGramsShort(v: Double): String {
    return if (v % 1.0 == 0.0 || kotlin.math.abs(v - v.roundToInt()) < 0.05) {
        "${v.roundToInt()}g"
    } else {
        String.format(java.util.Locale.US, "%.1fg", v)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MacrosScreen(
    viewModel: FamilyWallViewModel,
    modifier: Modifier = Modifier
) {
    val macroTargets by viewModel.macroTargets.collectAsStateWithLifecycle()
    val allIngredients by viewModel.macroIngredients.collectAsStateWithLifecycle()

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Calcular comida, 1 = Alimentos
    var selectedMealSlot by remember { mutableStateOf(MacroMealSlot.COMIDA) }
    val selectedIngredientIds = remember { mutableStateListOf<String>() }

    var calcSearchQuery by remember { mutableStateOf("") }
    var librarySearchQuery by remember { mutableStateOf("") }
    val selectedMacroFilters = remember { mutableStateListOf<MacroDominance>() }

    var showTargetsDialog by remember { mutableStateOf(false) }
    var showIngredientDialog by remember { mutableStateOf(false) }
    var editingIngredient by remember { mutableStateOf<MacroIngredient?>(null) }
    var newIngredientPrefillName by remember { mutableStateOf("") }

    val selectedIngredients = remember(allIngredients, selectedIngredientIds.toList()) {
        selectedIngredientIds.mapNotNull { id -> allIngredients.firstOrNull { it.id == id } }
    }

    val mealTarget = remember(macroTargets, selectedMealSlot) {
        macroTargets.targetForMeal(selectedMealSlot)
    }
    val dailyTarget = remember(macroTargets) { macroTargets.dailyTriple() }

    val calculationResult = remember(selectedIngredients, mealTarget) {
        MacroSolver.calculate(selectedIngredients, mealTarget)
    }

    val calcSuggestions = remember(allIngredients, calcSearchQuery) {
        val q = calcSearchQuery.trim()
        if (q.isBlank()) emptyList()
        else allIngredients.filter { it.name.contains(q, ignoreCase = true) }
    }

    val libraryFilteredIngredients = remember(
        allIngredients,
        librarySearchQuery,
        selectedMacroFilters.toList()
    ) {
        val q = librarySearchQuery.trim()
        allIngredients.filter { ing ->
            val matchesName = q.isBlank() || ing.name.contains(q, ignoreCase = true)
            val matchesMacros = selectedMacroFilters.isEmpty() ||
                selectedMacroFilters.all { macro -> MacroSolver.hasMacro(ing, macro) }
            matchesName && matchesMacros
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NUTRICIÓN Y PORCIONES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Calculadora de Macros",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showTargetsDialog = true }
                            .testTag("macros_edit_targets_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Ajustar objetivos de macros",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Objetivos",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Daily Targets Summary Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { showTargetsDialog = true }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OBJETIVO DIARIO (${dailyTarget.kcal} kcal)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 0.8.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Toca para ajustar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MacroBadgeBox(
                                label = "Hidratos",
                                value = formatGramsShort(dailyTarget.carbsG),
                                color = CarbColor,
                                modifier = Modifier.weight(1f)
                            )
                            MacroBadgeBox(
                                label = "Proteínas",
                                value = formatGramsShort(dailyTarget.proteinG),
                                color = ProteinColor,
                                modifier = Modifier.weight(1f)
                            )
                            MacroBadgeBox(
                                label = "Grasas",
                                value = formatGramsShort(dailyTarget.fatG),
                                color = FatColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Sub-Tab Switcher: Calcular comida vs Alimentos
            item {
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
                        val isCalc = selectedSubTab == 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCalc) MaterialTheme.colorScheme.surface else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedSubTab = 0 }
                                .testTag("macros_tab_calculator")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            ) {
                                Text(
                                    text = "Calcular comida",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isCalc) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isCalc) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isCalc) MaterialTheme.colorScheme.surface else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedSubTab = 1 }
                                .testTag("macros_tab_ingredients")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            ) {
                                Text(
                                    text = "Alimentos (${allIngredients.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (!isCalc) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (!isCalc) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (selectedSubTab == 0) {
                // Step 1: Select Meal Type (Desayuno / Comida / Cena)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "1. Elige la comida",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MacroMealSlot.entries.forEach { slot ->
                                val picked = selectedMealSlot == slot
                                val slotTarget = macroTargets.targetForMeal(slot)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = if (picked) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selectedMealSlot = slot }
                                        .testTag("macros_slot_${slot.name.lowercase()}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = slot.displayName,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (picked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${slotTarget.carbsG.roundToInt()}C · ${slotTarget.proteinG.roundToInt()}P · ${slotTarget.fatG.roundToInt()}G",
                                            fontSize = 10.sp,
                                            color = if (picked) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 2: Type-to-search & add ingredients + removable selected chips
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. Añade ingredientes (${selectedIngredients.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(
                                onClick = {
                                    editingIngredient = null
                                    newIngredientPrefillName = calcSearchQuery.trim()
                                    showIngredientDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nuevo", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        OutlinedTextField(
                            value = calcSearchQuery,
                            onValueChange = { calcSearchQuery = it },
                            placeholder = {
                                Text(
                                    "Escribe un alimento (ej: pollo, arroz, salmón)...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (calcSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { calcSearchQuery = "" }) {
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
                                .testTag("macros_calc_search_input")
                        )

                        // Suggestions list only appears while user types
                        if (calcSearchQuery.isNotBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("macros_calc_suggestions_card")
                            ) {
                                if (calcSuggestions.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "No se encontró \"${calcSearchQuery.trim()}\".",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                editingIngredient = null
                                                newIngredientPrefillName = calcSearchQuery.trim()
                                                showIngredientDialog = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Crear \"${calcSearchQuery.trim()}\"")
                                        }
                                    }
                                } else {
                                    Column {
                                        calcSuggestions.take(8).forEachIndexed { idx, ing ->
                                            val isSelected = ing.id in selectedIngredientIds
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (isSelected) {
                                                            selectedIngredientIds.remove(ing.id)
                                                        } else {
                                                            selectedIngredientIds.add(ing.id)
                                                            calcSearchQuery = ""
                                                        }
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                                    .testTag("macros_pick_ing_${ing.id}"),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = ing.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${ing.formatReferenceLabel()}: ${formatGramsShort(ing.effectiveCarbsPerRef)} HC · ${formatGramsShort(ing.effectiveProteinPerRef)} P · ${formatGramsShort(ing.effectiveFatPerRef)} G",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Add,
                                                            contentDescription = null,
                                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = if (isSelected) "Añadido" else "Añadir",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            if (idx < minOf(calcSuggestions.size, 8) - 1) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (selectedIngredients.isEmpty()) {
                            Text(
                                text = "Empieza a escribir arriba para buscar y añadir ingredientes sin llenar la pantalla.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Selected ingredients chips with individual remove X and Clear All
                        if (selectedIngredients.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "En este plato (${selectedIngredients.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { selectedIngredientIds.clear() },
                                    modifier = Modifier.testTag("macros_clear_all_selected")
                                ) {
                                    Text("Limpiar todo", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedIngredients.forEach { ing ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                        ) {
                                            Text(
                                                text = ing.name,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .clickable { selectedIngredientIds.remove(ing.id) }
                                                    .testTag("macros_remove_chip_${ing.id}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Quitar ${ing.name}",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 3: Auto-Calculated Portions & Save to Recipes
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("macros_result_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "3. Porciones calculadas (${selectedMealSlot.displayName})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (selectedIngredients.isEmpty()) "Añade ingredientes arriba para calcular las cantidades"
                                        else "${calculationResult.achieved.kcal} kcal / ${mealTarget.kcal} kcal objetivo",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selectedIngredients.isNotEmpty()) {
                                    TextButton(onClick = { selectedIngredientIds.clear() }) {
                                        Text("Limpiar todo", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            MacroComparisonBars(
                                achieved = calculationResult.achieved,
                                target = mealTarget
                            )

                            if (calculationResult.portions.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

                                calculationResult.portions.forEachIndexed { idx, portion ->
                                    val domColor = macroColor(portion.dominance)
                                    val sameGroupCount = calculationResult.portions.count { it.dominance == portion.dominance }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(domColor)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = portion.ingredient.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                val shareText = if (sameGroupCount > 1) {
                                                    " · ${portion.groupSharePercent}% de ${portion.dominance.label}"
                                                } else {
                                                    " · ${portion.dominance.label}"
                                                }
                                                Text(
                                                    text = "${formatGramsShort(portion.carbsG)} HC · ${formatGramsShort(portion.proteinG)} P · ${formatGramsShort(portion.fatG)} G$shareText",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = domColor.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = portion.formattedAmount,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = domColor,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { selectedIngredientIds.remove(portion.ingredient.id) },
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("macros_remove_portion_${portion.ingredient.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Quitar ${portion.ingredient.name}",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (idx < calculationResult.portions.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }

                                Button(
                                    onClick = {
                                        val activePortions = calculationResult.portions.filter { it.amount > 0.0 }
                                        val ingRows = activePortions.map { p ->
                                            RecipeIngredient(
                                                name = p.ingredient.name,
                                                quantity = p.formattedQuantity,
                                                unit = p.unit
                                            )
                                        }
                                        val topNames = activePortions.take(3).joinToString(", ") {
                                            it.ingredient.name.substringBefore(" (")
                                        }
                                        val title = "${selectedMealSlot.displayName}: ${topNames.ifBlank { "Plato macros" }}"
                                        val ach = calculationResult.achieved
                                        val summaryStep = "Preparar las cantidades exactas y cocinar al gusto. Macros totales: ${ach.carbsG.roundToInt()}g hidratos, ${ach.proteinG.roundToInt()}g proteínas y ${ach.fatG.roundToInt()}g grasas (~${ach.kcal} kcal)."
                                        val draft = RecipeDraft(
                                            title = title.take(120),
                                            category = selectedMealSlot.defaultRecipeCategory,
                                            prepTimeMinutes = 20,
                                            servings = 1,
                                            ingredients = RecipeFormatters.formatIngredients(ingRows),
                                            instructions = RecipeFormatters.formatSteps(listOf(summaryStep)),
                                            emoji = selectedMealSlot.emoji
                                        )
                                        viewModel.openSaveMacroRecipeDialog(draft)
                                    },
                                    enabled = calculationResult.portions.any { it.amount > 0.0 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("macros_save_to_recipe_button")
                                ) {
                                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Guardar en Recetas", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 2: Ingredient Library CRUD (without restore base button, without icons, with 1/2/3 macro filter)
                item {
                    Button(
                        onClick = {
                            editingIngredient = null
                            newIngredientPrefillName = ""
                            showIngredientDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("macros_add_ingredient_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Añadir alimento", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = librarySearchQuery,
                            onValueChange = { librarySearchQuery = it },
                            placeholder = { Text("Buscar por nombre...", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (librarySearchQuery.isNotBlank()) {
                                    IconButton(onClick = { librarySearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
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
                                .testTag("macros_library_search_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtrar por macros (1, 2 o las 3)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (selectedMacroFilters.isNotEmpty()) {
                                Text(
                                    text = "Quitar filtro",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { selectedMacroFilters.clear() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MacroDominance.entries.forEach { macro ->
                                val active = macro in selectedMacroFilters
                                val color = macroColor(macro)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (active) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (active) 1.5.dp else 1.dp,
                                        color = if (active) color else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (active) selectedMacroFilters.remove(macro)
                                            else selectedMacroFilters.add(macro)
                                        }
                                        .testTag("macros_filter_${macro.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (active) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = color,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = if (macro == MacroDominance.PROTEIN) "Proteínas" else if (macro == MacroDominance.FAT) "Grasas" else "Hidratos",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (active) color else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (libraryFilteredIngredients.isEmpty()) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Ningún alimento coincide con esos filtros",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Prueba con otra combinación de macros o añade un alimento nuevo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedMacroFilters.isNotEmpty() || librarySearchQuery.isNotBlank()) {
                                    TextButton(onClick = {
                                        selectedMacroFilters.clear()
                                        librarySearchQuery = ""
                                    }) {
                                        Text("Mostrar todos")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(libraryFilteredIngredients, key = { it.id }) { ing ->
                        val kcalRef = ing.effectiveRefKcal
                        val meaningfulMacros = MacroDominance.entries.filter { MacroSolver.hasMacro(ing, it) }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    editingIngredient = ing
                                    newIngredientPrefillName = ""
                                    showIngredientDialog = true
                                }
                                .testTag("macros_library_item_${ing.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = ing.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        meaningfulMacros.forEach { macro ->
                                            val c = macroColor(macro)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = c.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = macroShortLabel(macro),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = c,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${ing.formatReferenceLabel()}: ${formatGramsShort(ing.effectiveCarbsPerRef)} HC · ${formatGramsShort(ing.effectiveProteinPerRef)} P · ${formatGramsShort(ing.effectiveFatPerRef)} G · $kcalRef kcal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingIngredient = ing
                                            newIngredientPrefillName = ""
                                            showIngredientDialog = true
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            Icons.Outlined.Edit,
                                            contentDescription = "Editar alimento",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedIngredientIds.remove(ing.id)
                                            viewModel.deleteMacroIngredient(ing)
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Eliminar alimento",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTargetsDialog) {
        MacroTargetsDialog(
            initialTargets = macroTargets,
            onDismiss = { showTargetsDialog = false },
            onSave = { updated ->
                viewModel.saveMacroTargets(updated)
                showTargetsDialog = false
            }
        )
    }

    if (showIngredientDialog) {
        MacroIngredientEditorDialog(
            initialIngredient = editingIngredient,
            initialName = newIngredientPrefillName,
            onDismiss = { showIngredientDialog = false },
            onSave = { name, emoji, refQty, refUnit, carbsPerRef, proteinPerRef, fatPerRef ->
                val editing = editingIngredient
                if (editing == null) {
                    viewModel.addMacroIngredient(
                        name = name,
                        emoji = emoji,
                        referenceQuantity = refQty,
                        referenceUnit = refUnit,
                        carbsPerRef = carbsPerRef,
                        proteinPerRef = proteinPerRef,
                        fatPerRef = fatPerRef
                    )
                } else {
                    val normalized = MacroUnits.toLegacyPer100g(refQty, refUnit, carbsPerRef, proteinPerRef, fatPerRef)
                    viewModel.updateMacroIngredient(
                        editing.copy(
                            name = name,
                            emoji = emoji,
                            referenceQuantity = refQty,
                            referenceUnit = refUnit,
                            carbsPerRef = carbsPerRef,
                            proteinPerRef = proteinPerRef,
                            fatPerRef = fatPerRef,
                            carbsPer100g = normalized.carbsG,
                            proteinPer100g = normalized.proteinG,
                            fatPer100g = normalized.fatG
                        )
                    )
                }
                newIngredientPrefillName = ""
                showIngredientDialog = false
            }
        )
    }
}

@Composable
private fun MacroBadgeBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MacroComparisonBars(
    achieved: MacroTriple,
    target: MacroTriple
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroProgressRow(
            label = "Hidratos",
            achieved = achieved.carbsG,
            target = target.carbsG,
            barColor = CarbColor
        )
        MacroProgressRow(
            label = "Proteínas",
            achieved = achieved.proteinG,
            target = target.proteinG,
            barColor = ProteinColor
        )
        MacroProgressRow(
            label = "Grasas",
            achieved = achieved.fatG,
            target = target.fatG,
            barColor = FatColor
        )
    }
}

@Composable
private fun MacroProgressRow(
    label: String,
    achieved: Double,
    target: Double,
    barColor: Color
) {
    val progress = if (target > 0.0) (achieved / target).toFloat().coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${formatGramsShort(achieved)} / ${formatGramsShort(target)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = barColor
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            color = barColor,
            trackColor = barColor.copy(alpha = 0.16f),
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}
