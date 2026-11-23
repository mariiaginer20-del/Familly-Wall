package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ShoppingItemEntity
import com.example.data.model.Supermarket
import com.example.ui.FamilyWallViewModel
import com.example.ui.theme.ColorAlcampo
import com.example.ui.theme.ColorCarrefour
import com.example.ui.theme.ColorLidl
import com.example.ui.theme.ColorMercadona
import com.example.ui.theme.OfferBadgeColor
import com.example.ui.theme.PriorityClub
import com.example.ui.theme.PriorityNormal
import com.example.ui.theme.PriorityUrgente

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingScreen(
    viewModel: FamilyWallViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.filteredShoppingItems.collectAsStateWithLifecycle()
    val allItems by viewModel.rawShoppingItems.collectAsStateWithLifecycle()
    val selectedSupermarket by viewModel.selectedSupermarketFilter.collectAsStateWithLifecycle()
    val selectedPriority by viewModel.selectedShoppingPriorityFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.shoppingSearchQuery.collectAsStateWithLifecycle()

    val pendingCount = allItems.count { !it.isChecked }
    val completedCount = allItems.count { it.isChecked }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Title & Action Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🛒 Lista de la Compra",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "$pendingCount pendientes · $completedCount comprados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (completedCount > 0) {
                    OutlinedButton(
                        onClick = { viewModel.clearCheckedShoppingItems() },
                        modifier = Modifier.height(34.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Limpiar ($completedCount)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setShoppingSearchQuery(it) },
                placeholder = { Text("Buscar alimento o producto...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setShoppingSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shopping_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Supermarket Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    SupermarketFilterChip(
                        name = "Todos",
                        emoji = "🏪",
                        isSelected = selectedSupermarket == "Todos",
                        onClick = { viewModel.setSupermarketFilter("Todos") },
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(Supermarket.entries) { smkt ->
                    val color = when (smkt) {
                        Supermarket.MERCADONA -> ColorMercadona
                        Supermarket.CARREFOUR -> ColorCarrefour
                        Supermarket.LIDL -> ColorLidl
                        Supermarket.ALCAMPO -> ColorAlcampo
                        Supermarket.OTRO -> Color(0xFF64748B)
                    }
                    SupermarketFilterChip(
                        name = smkt.displayName,
                        emoji = smkt.emoji,
                        isSelected = selectedSupermarket.equals(smkt.displayName, ignoreCase = true),
                        onClick = { viewModel.setSupermarketFilter(smkt.displayName) },
                        color = color
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val priorityFilters = remember(primaryColor, secondaryColor) {
                listOf(
                    "Todos" to primaryColor,
                    "Urgente" to PriorityUrgente,
                    "Club" to PriorityClub,
                    "Ofertas" to OfferBadgeColor,
                    "Pendientes" to secondaryColor,
                    "Comprados" to Color(0xFF10B981)
                )
            }

            // Priority & Offers Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(priorityFilters) { (filterName, color) ->
                    val isSelected = selectedPriority == filterName
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.setShoppingPriorityFilter(filterName) }
                            .testTag("priority_filter_$filterName")
                    ) {
                        Text(
                            text = when (filterName) {
                                "Urgente" -> "⚡ Urgente"
                                "Club" -> "⭐ Club"
                                "Ofertas" -> "🏷️ Ofertas"
                                else -> filterName
                            },
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Items List
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛒", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Lista vacía con estos filtros",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Añade alimentos eligiendo supermercado, cantidades y si es Urgente o Club.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ShoppingItemCard(
                            item = item,
                            onToggle = { viewModel.toggleShoppingItemChecked(item) },
                            onQtyChange = { delta ->
                                viewModel.updateShoppingItemQuantity(item, item.quantity + delta)
                            },
                            onDelete = { viewModel.deleteShoppingItem(item) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Extended FAB
        ExtendedFloatingActionButton(
            onClick = { viewModel.setShowAddShoppingDialog(true) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Añadir Alimento") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_shopping_fab")
        )
    }
}

@Composable
fun ShoppingItemCard(
    item: ShoppingItemEntity,
    onToggle: () -> Unit,
    onQtyChange: (Double) -> Unit,
    onDelete: () -> Unit
) {
    val superColor = when (item.supermarket.lowercase()) {
        "mercadona" -> ColorMercadona
        "carrefour" -> ColorCarrefour
        "lidl" -> ColorLidl
        "alcampo" -> ColorAlcampo
        else -> Color(0xFF64748B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shopping_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isChecked) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("shopping_checkbox_${item.id}")
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Product details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Supermarket Badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = superColor
                        ) {
                            Text(
                                text = item.supermarket,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Priority Badge: Urgente, Club or Normal
                        if (item.priority == "URGENTE") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PriorityUrgente
                            ) {
                                Text(
                                    text = "⚡ Urgente",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (item.priority == "CLUB") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PriorityClub
                            ) {
                                Text(
                                    text = "⭐ Club",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Offer Badge
                        if (item.hasOffer) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = OfferBadgeColor
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Oferta",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.SemiBold,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )

                    // Offer Details Text
                    if (item.hasOffer && item.offerDetails.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🎁 ${item.offerDetails}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = OfferBadgeColor
                        )
                    }

                    // Notes
                    if (item.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "📝 ${item.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Añadido por ${item.addedByMemberName} · ${item.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quantity +/- Controls
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { onQtyChange(-1.0) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(14.dp))
                        }

                        val qtyString = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                        Text(
                            text = "$qtyString ${item.unit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        FilledTonalIconButton(
                            onClick = { onQtyChange(1.0) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar de la lista",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SupermarketFilterChip(
    name: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("supermarket_filter_$name")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
