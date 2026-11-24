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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.example.data.model.DefaultCategories
import com.example.data.model.FamilyCategory
import com.example.data.model.FamilyMember
import com.example.data.model.ItemPriority
import com.example.data.model.ProductHistoryEntry
import com.example.data.model.ShoppingItem
import com.example.data.model.ShoppingOfferPresets
import com.example.data.model.Supermarket
import com.example.ui.ShoppingItemDraft
import com.example.ui.theme.ColorCarrefour
import com.example.ui.theme.ColorLidl
import com.example.ui.theme.ColorMercadona
import com.example.ui.theme.ColorOtherSupermarket
import com.example.ui.theme.OfferBadgeBg
import com.example.ui.theme.OfferBadgeColor
import com.example.ui.theme.PriorityClub
import com.example.ui.theme.PriorityNormal
import com.example.ui.theme.PriorityUrgente

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddShoppingItemDialog(
    members: List<FamilyMember>,
    categories: List<FamilyCategory>,
    productHistory: List<ProductHistoryEntry>,
    existingShoppingItems: List<ShoppingItem>,
    initialItem: ShoppingItem? = null,
    onManageCategories: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (ShoppingItemDraft) -> Unit
) {
    val availableCategories = remember(categories) {
        categories.ifEmpty { DefaultCategories.forScope(CategoryScope.SHOPPING) }
    }

    var name by remember(initialItem) { mutableStateOf(initialItem?.name ?: "") }
    var quantity by remember(initialItem) { mutableDoubleStateOf(initialItem?.quantity ?: 1.0) }
    var unit by remember(initialItem) { mutableStateOf(initialItem?.unit ?: "ud") }
    var selectedSupermarket by remember(initialItem) {
        mutableStateOf(
            initialItem?.let { Supermarket.fromString(it.supermarket) } ?: Supermarket.MERCADONA
        )
    }
    var selectedPriority by remember(initialItem) {
        mutableStateOf(
            initialItem?.let { ItemPriority.fromString(it.priority) } ?: ItemPriority.NORMAL
        )
    }
    var hasOffer by remember(initialItem) { mutableStateOf(initialItem?.hasOffer ?: false) }
    var offerDetails by remember(initialItem) { mutableStateOf(initialItem?.offerDetails ?: "") }
    var category by remember(initialItem, availableCategories) {
        mutableStateOf(
            initialItem?.category?.takeIf { it.isNotBlank() }
                ?: availableCategories.firstOrNull()?.name
                ?: "Despensa"
        )
    }
    var addedBy by remember(initialItem) {
        mutableStateOf(initialItem?.addedByMemberName ?: members.firstOrNull()?.name ?: "Familia")
    }
    var assignedMemberName by remember(initialItem) {
        mutableStateOf(initialItem?.assignedMemberName?.takeIf { it.isNotBlank() } ?: "Todos")
    }
    var notes by remember(initialItem) { mutableStateOf(initialItem?.notes ?: "") }

    val units = listOf("ud", "kg", "g", "L", "pack", "bote")

    // Merge stored product history with any existing items for instant autocomplete
    val combinedSuggestions = remember(name, productHistory, existingShoppingItems) {
        val q = name.trim().lowercase()
        val historyEntries = productHistory.associateBy { it.nameLower }
        val fallbackEntries = existingShoppingItems
            .filter { it.name.isNotBlank() }
            .map {
                ProductHistoryEntry(
                    id = "item_${it.id}",
                    name = it.name,
                    nameLower = it.name.trim().lowercase(),
                    lastUnit = it.unit,
                    lastCategory = it.category,
                    lastSupermarket = Supermarket.fromString(it.supermarket).displayName,
                    useCount = 1
                )
            }
            .filter { it.nameLower !in historyEntries }

        val allEntries = productHistory + fallbackEntries.distinctBy { it.nameLower }
        if (q.isBlank()) {
            allEntries.sortedByDescending { it.useCount }.take(5)
        } else {
            allEntries
                .filter { it.nameLower.contains(q) && it.nameLower != q }
                .sortedWith(
                    compareByDescending<ProductHistoryEntry> { it.nameLower.startsWith(q) }
                        .thenByDescending { it.useCount }
                )
                .take(5)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialItem == null) "Añadir a la Compra" else "Editar Producto",
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Producto (ej: Leche, Manzanas, Detergente)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shopping_name_input")
                )

                if (combinedSuggestions.isNotEmpty() && initialItem == null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (name.isBlank()) "Habituales:" else "Sugerencias:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        combinedSuggestions.forEach { entry ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        name = entry.name
                                        if (entry.lastUnit.isNotBlank()) unit = entry.lastUnit
                                        if (entry.lastCategory.isNotBlank()) category = entry.lastCategory
                                        if (entry.lastSupermarket.isNotBlank()) {
                                            selectedSupermarket = Supermarket.fromString(entry.lastSupermarket)
                                        }
                                    }
                                    .testTag("product_suggestion_${entry.nameLower}")
                            ) {
                                Text(
                                    text = "${entry.name} · ${entry.lastUnit}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Cantidad y unidad",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = {
                                if (quantity > 1.0) quantity -= 1.0
                                else if (quantity > 0.5) quantity = 0.5
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("shopping_qty_minus")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(18.dp))
                        }

                        val qtyText = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
                        Text(
                            text = qtyText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .padding(horizontal = 14.dp)
                                .testTag("shopping_qty_text")
                        )

                        FilledTonalIconButton(
                            onClick = { quantity += 1.0 },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("shopping_qty_plus")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(18.dp))
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        units.forEach { u ->
                            val isSelected = unit == u
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { unit = u }
                            ) {
                                Text(
                                    text = u,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Supermercado",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Supermarket.entries.forEach { superMkt ->
                        val isSelected = selectedSupermarket == superMkt
                        val superColor = when (superMkt) {
                            Supermarket.MERCADONA -> ColorMercadona
                            Supermarket.CARREFOUR -> ColorCarrefour
                            Supermarket.LIDL -> ColorLidl
                            Supermarket.OTRO -> ColorOtherSupermarket
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) superColor else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedSupermarket = superMkt }
                                .testTag("supermarket_${superMkt.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = superMkt.emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = superMkt.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Tipo / Prioridad",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ItemPriority.entries.forEach { priority ->
                        val isSelected = selectedPriority == priority
                        val pColor = when (priority) {
                            ItemPriority.NORMAL -> PriorityNormal
                            ItemPriority.URGENTE -> PriorityUrgente
                            ItemPriority.CLUB -> PriorityClub
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) pColor else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedPriority = priority }
                                .testTag("priority_${priority.name.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = priority.badgeText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                            .testTag("manage_shopping_categories_btn")
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¿Está en oferta?",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Elige promoción rápida o personalízala",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hasOffer,
                        onCheckedChange = { hasOffer = it },
                        modifier = Modifier.testTag("shopping_offer_switch")
                    )
                }

                if (hasOffer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShoppingOfferPresets.presets.forEach { preset ->
                            val isSelected = offerDetails.equals(preset, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) OfferBadgeColor else OfferBadgeBg,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        offerDetails = if (isSelected) "" else preset
                                    }
                                    .testTag("offer_preset_$preset")
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else OfferBadgeColor,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = offerDetails,
                        onValueChange = { offerDetails = it },
                        label = { Text("Detalle de oferta (editable)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shopping_offer_details_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Asignar producto a:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val assignOpts = listOf("Todos" to "👨‍👩‍👧‍👦") + members.map { it.name to it.avatarEmoji }
                    assignOpts.forEach { (mName, mEmoji) ->
                        val isSelected = assignedMemberName == mName
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { assignedMemberName = mName }
                                .testTag("shopping_assign_$mName")
                        ) {
                            Text(
                                text = "$mEmoji $mName",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (members.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Añadido por:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        members.forEach { m ->
                            val isSelected = addedBy == m.name
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { addedBy = m.name }
                            ) {
                                Text(
                                    text = "${m.avatarEmoji} ${m.name}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (marca preferida, tamaño, etc.)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            ShoppingItemDraft(
                                name = name,
                                quantity = quantity,
                                unit = unit,
                                supermarket = selectedSupermarket,
                                priority = selectedPriority,
                                hasOffer = hasOffer,
                                offerDetails = offerDetails,
                                category = category,
                                addedByMemberName = addedBy,
                                assignedMemberName = assignedMemberName,
                                notes = notes
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_shopping_item_button")
            ) {
                Text(if (initialItem == null) "Añadir a la lista" else "Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
