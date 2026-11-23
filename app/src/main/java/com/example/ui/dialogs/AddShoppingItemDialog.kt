package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import com.example.data.local.FamilyMemberEntity
import com.example.data.model.ItemPriority
import com.example.data.model.Supermarket
import com.example.ui.ShoppingItemDraft
import com.example.ui.theme.ColorAlcampo
import com.example.ui.theme.ColorCarrefour
import com.example.ui.theme.ColorLidl
import com.example.ui.theme.ColorMercadona
import com.example.ui.theme.PriorityClub
import com.example.ui.theme.PriorityNormal
import com.example.ui.theme.PriorityUrgente

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddShoppingItemDialog(
    members: List<FamilyMemberEntity>,
    onDismiss: () -> Unit,
    onSave: (ShoppingItemDraft) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableDoubleStateOf(1.0) }
    var unit by remember { mutableStateOf("ud") }
    var selectedSupermarket by remember { mutableStateOf(Supermarket.MERCADONA) }
    var selectedPriority by remember { mutableStateOf(ItemPriority.NORMAL) }
    var hasOffer by remember { mutableStateOf(false) }
    var offerDetails by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Despensa") }
    var addedBy by remember { mutableStateOf(members.firstOrNull()?.name ?: "Mamá") }
    var notes by remember { mutableStateOf("") }

    val units = listOf("ud", "kg", "g", "L", "pack", "bote")
    val categories = listOf("Despensa", "Lácteos", "Frutas y Verduras", "Carnicería", "Pescadería", "Limpieza", "Panadería", "Bebidas", "Congelados")

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
                    text = "Añadir a la Compra",
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
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Producto (ej: Leche, Manzanas, Detergente)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shopping_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quantity & Unit
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

                    // Unit selector pills
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        units.take(4).forEach { u ->
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

                // Supermarket selector
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
                            Supermarket.ALCAMPO -> ColorAlcampo
                            Supermarket.OTRO -> Color(0xFF64748B)
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

                // Priority: Normal, Urgente, Club
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
                            ItemPriority.URGENTE -> PriorityUargenteWrapper()
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

                // Offer Switch & Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🏷️ ¿Está en oferta?",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Añade notas de descuento o promoción",
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
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = offerDetails,
                        onValueChange = { offerDetails = it },
                        label = { Text("Detalle de oferta (ej: 2x1, 50% en 2ª ud, Cupón app)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shopping_offer_details_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Member who added
                Text(
                    text = "Añadido por:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
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
                                notes = notes
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_shopping_item_button")
            ) {
                Text("Añadir a la lista")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun PriorityUargenteWrapper(): Color = PriorityUrgente
