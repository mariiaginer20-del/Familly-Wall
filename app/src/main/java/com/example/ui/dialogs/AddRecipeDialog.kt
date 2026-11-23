package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.Recipe
import com.example.ui.RecipeDraft

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddRecipeDialog(
    initialRecipe: Recipe? = null,
    onDismiss: () -> Unit,
    onSave: (RecipeDraft) -> Unit
) {
    var title by remember(initialRecipe) { mutableStateOf(initialRecipe?.title ?: "") }
    var category by remember(initialRecipe) { mutableStateOf(initialRecipe?.category ?: "Plato principal") }
    var prepTimeMinutes by remember(initialRecipe) { mutableStateOf((initialRecipe?.prepTimeMinutes ?: 30).toString()) }
    var servings by remember(initialRecipe) { mutableStateOf((initialRecipe?.servings ?: 4).toString()) }
    var ingredients by remember(initialRecipe) { mutableStateOf(initialRecipe?.ingredients ?: "") }
    var instructions by remember(initialRecipe) { mutableStateOf(initialRecipe?.instructions ?: "") }
    var selectedEmoji by remember(initialRecipe) { mutableStateOf(initialRecipe?.emoji ?: "🍳") }

    val emojis = listOf("🍳", "🍕", "🍲", "🥗", "🍝", "🥩", "🐟", "🥪", "🧁", "🥘", "🥣")
    val categories = listOf("Plato principal", "Primeros Platos", "Guisos", "Pastas", "Cenas Rápidas", "Postres", "Desayunos")

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

                // Emoji picker
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

                // Category
                Text(
                    text = "Categoría",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ingredients,
                    onValueChange = { ingredients = it },
                    label = { Text("Ingredientes (uno por línea)") },
                    placeholder = { Text("ej:\n400g Arroz\n1 Cebolla picada\nTomate frito\nAceite de oliva") },
                    singleLine = false,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipe_ingredients_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Pasos de preparación") },
                    placeholder = { Text("1. Sofreír los ingredientes...\n2. Añadir agua y cocer...") },
                    singleLine = false,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipe_instructions_input")
                )
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
                                ingredients = ingredients,
                                instructions = instructions,
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
