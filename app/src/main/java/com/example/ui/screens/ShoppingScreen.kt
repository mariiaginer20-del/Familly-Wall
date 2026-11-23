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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.ColorOtherSupermarket
import com.example.ui.theme.OfferBadgeBg
import com.example.ui.theme.OfferBadgeColor
import com.example.ui.theme.PriorityClub
import com.example.ui.theme.PriorityClubBg
import com.example.ui.theme.PriorityUrgente
import com.example.ui.theme.PriorityUrgenteBg
import com.example.ui.theme.SuccessGreen

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
                        text = "$pendingCount PENDIENTES · $completedCount EN CARRO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Lista de la compra",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (completedCount > 0) {
                    OutlinedButton(
                        onClick = { viewModel.clearCheckedShoppingItems() },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Outlined.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Limpiar ($completedCount)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setShoppingSearchQuery(it) },
                placeholder = {
                    Text(
                        "Buscar producto o categoría...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
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
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shopping_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Supermarket Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    SupermarketFilterChip(
                        name = "Todos",
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
                        Supermarket.OTRO -> ColorOtherSupermarket
                    }
                    SupermarketFilterChip(
                        name = smkt.displayName,
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
                    "Comprados" to SuccessGreen
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
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { viewModel.setShoppingPriorityFilter(filterName) }
                            .testTag("priority_filter_$filterName")
                    ) {
                        Text(
                            text = filterName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (items.isEmpty()) {
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
                            imageVector = Icons.Outlined.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Lista vacía con estos filtros",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Añade alimentos eligiendo supermercado, cantidades, ofertas y si es Urgente o Club.",
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
                        .weight(1f)
                ) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column {
                                items.forEachIndexed { index, item ->
                                    ShoppingItemRow(
                                        item = item,
                                        onToggle = { viewModel.toggleShoppingItemChecked(item) },
                                        onQtyChange = { delta ->
                                            viewModel.updateShoppingItemQuantity(item, item.quantity + delta)
                                        },
                                        onDelete = { viewModel.deleteShoppingItem(item) }
                                    )
                                    if (index < items.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { viewModel.setShowAddShoppingDialog(true) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Añadir producto", style = MaterialTheme.typography.labelLarge) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_shopping_fab")
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingItemRow(
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
        else -> ColorOtherSupermarket
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("shopping_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("shopping_checkbox_${item.id}")
        )

        Spacer(modifier = Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = superColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = item.supermarket.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = superColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (item.priority == "URGENTE") {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = PriorityUrgenteBg
                    ) {
                        Text(
                            text = "URGENTE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PriorityUrgente,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (item.priority == "CLUB") {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = PriorityClubBg
                    ) {
                        Text(
                            text = "CLUB",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PriorityClub,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.hasOffer) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = OfferBadgeBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.LocalOffer,
                                contentDescription = null,
                                tint = OfferBadgeColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "OFERTA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = OfferBadgeColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )

            if (item.hasOffer && item.offerDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.offerDetails,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = OfferBadgeColor
                )
            }

            if (item.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${item.addedByMemberName} · ${item.category}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Compact quantity stepper + delete
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onQtyChange(-1.0) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Reducir cantidad",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val qtyString = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                    Text(
                        text = "$qtyString ${item.unit}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onQtyChange(1.0) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Aumentar cantidad",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Eliminar de la lista",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SupermarketFilterChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("supermarket_filter_$name")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
