package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryScope
import com.example.data.model.FamilyCategory

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageCategoriesDialog(
    initialScope: CategoryScope,
    allCategories: List<FamilyCategory>,
    onAddCategory: (scope: CategoryScope, name: String, emoji: String, colorHex: String) -> Unit,
    onUpdateCategory: (category: FamilyCategory, oldName: String) -> Unit,
    onDeleteCategory: (category: FamilyCategory) -> Unit,
    onRestoreDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedScope by remember(initialScope) { mutableStateOf(initialScope) }
    var showForm by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<FamilyCategory?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var emojiInput by remember { mutableStateOf("🏷️") }
    var colorInput by remember { mutableStateOf("#1B4D3E") }

    val scopedCategories = remember(allCategories, selectedScope) {
        allCategories
            .filter { it.scope == selectedScope.name }
            .sortedBy { it.name.lowercase() }
    }

    val emojiChoices = listOf(
        "🏷️", "📌", "🏠", "🛒", "🍳", "🎒", "🩺", "⚽", "🎉", "💼",
        "🚗", "🐾", "🥛", "🥬", "🥩", "🐟", "🧼", "🥖", "🧊", "🍲",
        "🍝", "🥗", "🧁", "🍽️", "🌱", "💡", "✈️", "🎁"
    )
    val colorChoices = listOf(
        "#1B4D3E", "#C25934", "#2847D6", "#0D9488", "#D97706", "#8B5CF6", "#B9382C", "#157A4B"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gestionar categorías",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CategoryScope.entries) { scope ->
                        val picked = selectedScope == scope
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedScope = scope
                                    showForm = false
                                    editingCategory = null
                                }
                                .testTag("category_scope_${scope.name.lowercase()}")
                        ) {
                            Text(
                                text = "${scope.emoji} ${scope.displayName}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (picked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                if (showForm) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (editingCategory == null) "Nueva categoría en ${selectedScope.displayName}" else "Editar categoría",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { if (it.length <= 40) nameInput = it },
                                label = { Text("Nombre de la categoría") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("category_name_input")
                            )
                            Text("Icono", style = MaterialTheme.typography.labelSmall)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                emojiChoices.forEach { em ->
                                    val picked = emojiInput == em
                                    Surface(
                                        shape = CircleShape,
                                        color = if (picked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .clickable { emojiInput = em }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(em, fontSize = 15.sp)
                                        }
                                    }
                                }
                            }
                            Text("Color", style = MaterialTheme.typography.labelSmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                colorChoices.forEach { hex ->
                                    val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                                        .getOrDefault(MaterialTheme.colorScheme.primary)
                                    Surface(
                                        shape = CircleShape,
                                        color = c,
                                        border = if (colorInput == hex) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable { colorInput = hex }
                                    ) {}
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        showForm = false
                                        editingCategory = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancelar")
                                }
                                Button(
                                    onClick = {
                                        val clean = nameInput.trim()
                                        if (clean.isNotBlank()) {
                                            val editing = editingCategory
                                            if (editing == null) {
                                                onAddCategory(selectedScope, clean, emojiInput, colorInput)
                                            } else {
                                                onUpdateCategory(
                                                    editing.copy(
                                                        name = clean,
                                                        emoji = emojiInput,
                                                        colorHex = colorInput
                                                    ),
                                                    editing.name
                                                )
                                            }
                                            showForm = false
                                            editingCategory = null
                                            nameInput = ""
                                        }
                                    },
                                    enabled = nameInput.isNotBlank(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_category_button")
                                ) {
                                    Text(if (editingCategory == null) "Añadir" else "Guardar")
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            editingCategory = null
                            nameInput = ""
                            emojiInput = "🏷️"
                            colorInput = "#1B4D3E"
                            showForm = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_category_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Añadir categoría en ${selectedScope.displayName}")
                    }
                }

                if (scopedCategories.isEmpty()) {
                    Text(
                        text = "No hay categorías en ${selectedScope.displayName}. Añade una o restaura las predeterminadas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            scopedCategories.forEachIndexed { idx, cat ->
                                val dotColor = runCatching {
                                    Color(android.graphics.Color.parseColor(cat.colorHex))
                                }.getOrDefault(MaterialTheme.colorScheme.primary)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = cat.emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingCategory = cat
                                                nameInput = cat.name
                                                emojiInput = cat.emoji
                                                colorInput = cat.colorHex
                                                showForm = true
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Edit,
                                                contentDescription = "Editar categoría",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteCategory(cat) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "Eliminar categoría",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                                if (idx < scopedCategories.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onRestoreDefaults,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restaurar categorías predeterminadas faltantes")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Listo")
            }
        }
    )
}
