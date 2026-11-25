package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryScope
import com.example.data.model.DefaultCategories
import com.example.data.model.FamilyCategory
import com.example.data.model.Recipe
import com.example.data.model.RecipeFormatters
import com.example.data.model.RecipeIngredient
import com.example.ui.RecipeDraft

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddRecipeDialog(
    categories: List<FamilyCategory>,
    initialRecipe: Recipe? = null,
    prefillDraft: RecipeDraft? = null,
    onManageCategories: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (RecipeDraft) -> Unit
) {
    val availableCategories = remember(categories) {
        categories.ifEmpty { DefaultCategories.forScope(CategoryScope.RECIPE) }
    }

    var title by remember(initialRecipe, prefillDraft) {
        mutableStateOf(initialRecipe?.title ?: prefillDraft?.title ?: "")
    }
    var category by remember(initialRecipe, prefillDraft, availableCategories) {
        mutableStateOf(
            initialRecipe?.category?.takeIf { it.isNotBlank() }
                ?: prefillDraft?.category?.takeIf { it.isNotBlank() }
                ?: availableCategories.firstOrNull()?.name
                ?: "Plato principal"
        )
    }
    var prepTimeMinutes by remember(initialRecipe, prefillDraft) {
        mutableStateOf((initialRecipe?.prepTimeMinutes ?: prefillDraft?.prepTimeMinutes ?: 30).toString())
    }
    var servings by remember(initialRecipe, prefillDraft) {
        mutableStateOf((initialRecipe?.servings ?: prefillDraft?.servings ?: 4).toString())
    }
    var selectedEmoji by remember(initialRecipe, prefillDraft) {
        mutableStateOf(initialRecipe?.emoji ?: prefillDraft?.emoji ?: "🍳")
    }

    val rawIngredients = initialRecipe?.ingredients ?: prefillDraft?.ingredients.orEmpty()
    val ingredientRows = remember(initialRecipe, prefillDraft) {
        mutableStateListOf<RecipeIngredient>().apply {
            if (rawIngredients.isNotBlank()) {
                addAll(RecipeFormatters.parseIngredients(rawIngredients))
            } else {
                add(RecipeIngredient())
            }
        }
    }

    val rawInstructions = initialRecipe?.instructions ?: prefillDraft?.instructions.orEmpty()
    val stepRows = remember(initialRecipe, prefillDraft) {
        mutableStateListOf<String>().apply {
            if (rawInstructions.isNotBlank()) {
                addAll(RecipeFormatters.parseSteps(rawInstructions))
            } else {
                add("")
            }
        }
    }

    val emojis = listOf("🍳", "🍕", "🍲", "🥗", "🍝", "🥩", "🐟", "🥪", "🧁", "🥘", "🥣")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialRecipe == null) "Añadir Nueva Receta" else "Editar Receta",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre de la receta (ej: Arroz al horno)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipe_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Icono",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojis.forEach { emoji ->
                        val isSelected = selectedEmoji == emoji
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedEmoji = emoji }
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categoría",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onManageCategories)
                            .testTag("manage_recipe_categories_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Editar categorías",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Editar categorías",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableCategories.forEach { cat ->
                        val isSelected = category.equals(cat.name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { category = cat.name }
                        ) {
                            Text(
                                text = "${cat.emoji} ${cat.name}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = prepTimeMinutes,
                        onValueChange = { prepTimeMinutes = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Minutos") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = servings,
                        onValueChange = { servings = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Personas") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Ingredientes",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ingredientRows.forEachIndexed { idx, ing ->
                        var unitMenuExpanded by remember(idx, ingredientRows.size) { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = ing.name,
                                        onValueChange = { ingredientRows[idx] = ing.copy(name = it) },
                                        label = { Text("Ingrediente ${idx + 1}") },
                                        placeholder = { Text("ej: Arroz, Leche...") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("recipe_ing_name_$idx")
                                    )
                                    if (ingredientRows.size > 1) {
                                        IconButton(
                                            onClick = { ingredientRows.removeAt(idx) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.DeleteOutline,
                                                contentDescription = "Quitar ingrediente",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = ing.quantity,
                                        onValueChange = { raw ->
                                            val cleaned = raw.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                            ingredientRows[idx] = ing.copy(quantity = cleaned)
                                        },
                                        label = { Text("Cant.") },
                                        placeholder = { Text("400") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("recipe_ing_qty_$idx")
                                    )

                                    Box(modifier = Modifier.weight(1.1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { unitMenuExpanded = true }
                                                .testTag("recipe_ing_unit_$idx")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = ing.unit.ifBlank { "ud" },
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = "Seleccionar unidad",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = unitMenuExpanded,
                                            onDismissRequest = { unitMenuExpanded = false }
                                        ) {
                                            RecipeFormatters.units.forEach { u ->
                                                DropdownMenuItem(
                                                    text = { Text(u) },
                                                    onClick = {
                                                        ingredientRows[idx] = ing.copy(unit = u)
                                                        unitMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { ingredientRows.add(RecipeIngredient()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_recipe_ingredient_row_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Añadir ingrediente")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Pasos de preparación",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stepRows.forEachIndexed { idx, stepText ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = stepText,
                                onValueChange = { stepRows[idx] = it },
                                label = { Text("Paso ${idx + 1}") },
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("recipe_step_input_$idx")
                            )
                            if (stepRows.size > 1) {
                                IconButton(
                                    onClick = { stepRows.removeAt(idx) },
                                    modifier = Modifier
                                        .padding(top = 10.dp)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = "Eliminar paso",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { stepRows.add("") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_recipe_step_row_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Añadir paso ${stepRows.size + 1}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            RecipeDraft(
                                title = title,
                                category = category,
                                prepTimeMinutes = prepTimeMinutes.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                                servings = servings.toIntOrNull()?.coerceAtLeast(1) ?: 4,
                                ingredients = RecipeFormatters.formatIngredients(ingredientRows),
                                instructions = RecipeFormatters.formatSteps(stepRows),
                                emoji = selectedEmoji
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_recipe_button")
            ) {
                Text(if (initialRecipe == null) "Guardar Receta" else "Actualizar Receta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
