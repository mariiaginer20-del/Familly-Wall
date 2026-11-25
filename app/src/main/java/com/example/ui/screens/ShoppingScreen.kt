package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryScope
import com.example.data.model.CustomList
import com.example.data.model.CustomListItem
import com.example.data.model.FamilyMember
import com.example.data.model.ShoppingItem
import com.example.data.model.Supermarket
import com.example.data.preferences.AppDesignStyle
import com.example.ui.CustomListItemDraft
import com.example.ui.FamilyWallViewModel
import com.example.ui.theme.ColorCarrefour
import com.example.ui.theme.ColorLidl
import com.example.ui.theme.ColorMercadona
import com.example.ui.theme.ColorOtherSupermarket
import com.example.ui.theme.LocalDesignStyle
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

    val customLists by viewModel.customLists.collectAsStateWithLifecycle()
    val customListItems by viewModel.customListItems.collectAsStateWithLifecycle()
    val members by viewModel.allMembers.collectAsStateWithLifecycle()

    val isBento = LocalDesignStyle.current == AppDesignStyle.NORDIC_BENTO
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Compra, 1 = Otras listas
    var activeCustomListId by remember { mutableStateOf<String?>(null) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var showCustomItemDialog by remember { mutableStateOf(false) }
    var editingCustomItem by remember { mutableStateOf<CustomListItem?>(null) }

    val activeCustomList = remember(customLists, activeCustomListId) {
        customLists.firstOrNull { it.id == activeCustomListId }
    }

    if (selectedSubTab == 1 && activeCustomList != null) {
        BackHandler {
            activeCustomListId = null
        }
    }

    val pendingCount = allItems.count { !it.checked }
    val completedCount = allItems.count { it.checked }

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

            // Top Segmented Switcher: Compra vs Otras listas
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
                    val isCompra = selectedSubTab == 0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCompra) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedSubTab = 0 }
                            .testTag("subtab_shopping")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        ) {
                            Text(
                                text = "Compra ($pendingCount)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isCompra) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isCompra) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isCompra) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedSubTab = 1 }
                            .testTag("subtab_custom_lists")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        ) {
                            Text(
                                text = "Otras listas (${customLists.size})",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (!isCompra) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (!isCompra) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedSubTab == 0) {
                // --- COMPRA ---
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.openManageCategories(CategoryScope.SHOPPING) },
                            modifier = Modifier.testTag("shopping_manage_categories_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Category,
                                contentDescription = "Gestionar categorías de compra",
                                tint = MaterialTheme.colorScheme.primary
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

                Spacer(modifier = Modifier.height(10.dp))

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

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Toca el producto para editarlo · Pulsa el check para marcarlo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

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
                                text = "Añade alimentos eligiendo supermercado, cantidades, ofertas y prioridad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (isBento) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                ShoppingItemRow(
                                    item = item,
                                    isDark = isDark,
                                    onToggle = { viewModel.toggleShoppingItemChecked(item) },
                                    onEdit = { viewModel.startEditingShoppingItem(item) },
                                    onQtyChange = { delta ->
                                        viewModel.updateShoppingItemQuantity(item, item.quantity + delta)
                                    },
                                    onDelete = { viewModel.deleteShoppingItem(item) }
                                )
                            }
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
                                            isDark = isDark,
                                            onToggle = { viewModel.toggleShoppingItemChecked(item) },
                                            onEdit = { viewModel.startEditingShoppingItem(item) },
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
                            Spacer(modifier = Modifier.height(92.dp))
                        }
                    }
                }
            } else {
                // --- OTRAS LISTAS (LISTAS GENERALES) ---
                if (activeCustomList == null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ORGANIZACIÓN GENERAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Otras listas familiares",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Maletas de viaje, farmacia, bricolaje, deseos o recados compartidos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (customLists.isEmpty()) {
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
                                        imageVector = Icons.Outlined.Checklist,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(46.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Aún no tienes listas generales",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Crea listas como 'Maleta fin de semana', 'Farmacia' o 'Reformas'.",
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
                                contentPadding = PaddingValues(bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(customLists, key = { it.id }) { list ->
                                    val listItems = customListItems.filter { it.listId == list.id }
                                    val doneCount = listItems.count { it.checked }
                                    val total = listItems.size
                                    val ratio = if (total > 0) doneCount.toFloat() / total.toFloat() else 0f
                                    val accent = runCatching {
                                        Color(android.graphics.Color.parseColor(list.colorHex))
                                    }.getOrDefault(MaterialTheme.colorScheme.primary)

                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.medium)
                                            .clickable { activeCustomListId = list.id }
                                            .testTag("custom_list_card_${list.id}")
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = accent.copy(alpha = 0.16f),
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(text = list.emoji, fontSize = 19.sp)
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = list.title,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = if (total == 0) "Lista vacía" else "$doneCount / $total completados",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteCustomList(list) },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        contentDescription = "Eliminar lista",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            if (total > 0) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                LinearProgressIndicator(
                                                    progress = { ratio },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(5.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = accent,
                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Detail view of a CustomList
                    val itemsInList = remember(customListItems, activeCustomList.id) {
                        customListItems.filter { it.listId == activeCustomList.id }
                            .sortedWith(compareBy<CustomListItem> { it.checked }.thenByDescending { it.createdAt?.seconds ?: 0L })
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { activeCustomListId = null },
                                modifier = Modifier.testTag("custom_list_back_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a listas")
                            }
                            Text(text = activeCustomList.emoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeCustomList.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Toca un elemento para editarlo · Check para completar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (itemsInList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlaylistAdd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Lista vacía",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Añade el primer elemento pulsando 'Añadir elemento'",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(if (isBento) 8.dp else 0.dp)
                            ) {
                                if (isBento) {
                                    items(itemsInList, key = { it.id }) { item ->
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            CustomListItemRow(
                                                item = item,
                                                onToggle = { viewModel.toggleCustomListItemChecked(item) },
                                                onEdit = {
                                                    editingCustomItem = item
                                                    showCustomItemDialog = true
                                                },
                                                onDelete = { viewModel.deleteCustomListItem(item) }
                                            )
                                        }
                                    }
                                } else {
                                    item {
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                itemsInList.forEachIndexed { idx, item ->
                                                    CustomListItemRow(
                                                        item = item,
                                                        onToggle = { viewModel.toggleCustomListItemChecked(item) },
                                                        onEdit = {
                                                            editingCustomItem = item
                                                            showCustomItemDialog = true
                                                        },
                                                        onDelete = { viewModel.deleteCustomListItem(item) }
                                                    )
                                                    if (idx < itemsInList.lastIndex) {
                                                        HorizontalDivider(
                                                            modifier = Modifier.padding(horizontal = 16.dp),
                                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
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
                }
            }
        }

        // FAB adapts to Compra vs Otras Listas vs Detalle de Lista
        ExtendedFloatingActionButton(
            onClick = {
                when {
                    selectedSubTab == 0 -> viewModel.setShowAddShoppingDialog(true)
                    activeCustomList == null -> showCreateListDialog = true
                    else -> {
                        editingCustomItem = null
                        showCustomItemDialog = true
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = {
                Text(
                    text = when {
                        selectedSubTab == 0 -> "Añadir producto"
                        activeCustomList == null -> "Nueva lista"
                        else -> "Añadir elemento"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag(
                    when {
                        selectedSubTab == 0 -> "add_shopping_fab"
                        activeCustomList == null -> "create_custom_list_fab"
                        else -> "add_custom_list_item_fab"
                    }
                )
        )
    }

    if (showCreateListDialog) {
        CreateCustomListDialog(
            onDismiss = { showCreateListDialog = false },
            onSave = { title, emoji, colorHex ->
                viewModel.createCustomList(title, emoji, colorHex)
                showCreateListDialog = false
            }
        )
    }

    if (showCustomItemDialog && activeCustomList != null) {
        AddCustomListItemDialog(
            listId = activeCustomList.id,
            members = members,
            initialItem = editingCustomItem,
            onDismiss = {
                showCustomItemDialog = false
                editingCustomItem = null
            },
            onSave = { draft ->
                viewModel.saveCustomListItem(draft, editingCustomItem)
                showCustomItemDialog = false
                editingCustomItem = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    isDark: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onQtyChange: (Double) -> Unit,
    onDelete: () -> Unit
) {
    val normalizedSuper = Supermarket.fromString(item.supermarket)
    val superColor = when (normalizedSuper) {
        Supermarket.MERCADONA -> if (isDark) Color(0xFF4ADE80) else ColorMercadona
        Supermarket.CARREFOUR -> if (isDark) Color(0xFF60A5FA) else ColorCarrefour
        Supermarket.LIDL -> if (isDark) Color(0xFF93C5FD) else ColorLidl
        Supermarket.OTRO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val urgentFg = if (isDark) Color(0xFFFF8A80) else PriorityUrgente
    val urgentBg = if (isDark) PriorityUrgente.copy(alpha = 0.22f) else PriorityUrgenteBg
    val clubFg = if (isDark) Color(0xFFCBB2FF) else PriorityClub
    val clubBg = if (isDark) PriorityClub.copy(alpha = 0.22f) else PriorityClubBg
    val offerFg = if (isDark) Color(0xFFFBBF24) else OfferBadgeColor
    val offerBg = if (isDark) OfferBadgeColor.copy(alpha = 0.22f) else OfferBadgeBg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("shopping_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.checked,
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
                    color = superColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = normalizedSuper.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = superColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (item.priority == "URGENTE") {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = urgentBg
                    ) {
                        Text(
                            text = "URGENTE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = urgentFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (item.priority == "CLUB") {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = clubBg
                    ) {
                        Text(
                            text = "CLUB",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = clubFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.hasOffer) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = offerBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.LocalOffer,
                                contentDescription = null,
                                tint = offerFg,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "OFERTA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = offerFg
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (item.checked) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )

            if (item.hasOffer && item.offerDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.offerDetails,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = offerFg
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

            val ownerLabel = if (item.assignedMemberName.isNotBlank() && item.assignedMemberName != "Todos") {
                "Para ${item.assignedMemberName} · Por ${item.addedByMemberName} · ${item.category}"
            } else {
                "${item.addedByMemberName} · ${item.category}"
            }
            Text(
                text = ownerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

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

            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Editar producto",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
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
}

@Composable
private fun CustomListItemRow(
    item: CustomListItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("custom_list_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.testTag("custom_list_checkbox_${item.id}")
        )

        Spacer(modifier = Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (item.checked) FontWeight.Normal else FontWeight.SemiBold,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (item.quantity.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = item.quantity,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
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
                text = "Responsable: ${item.assignedMemberName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Editar elemento",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Eliminar elemento",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateCustomListDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, emoji: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📝") }
    var colorHex by remember { mutableStateOf("#1B4D3E") }

    val emojiOptions = listOf("📝", "🧳", "💊", "🛠️", "🎁", "🏖️", "🎒", "🏡", "🚗")
    val colorOptions = listOf("#1B4D3E", "#C25934", "#2847D6", "#0D9488", "#D97706", "#6D3FB8")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva lista general", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre (ej: Maleta, Farmacia, Bricolaje)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_custom_list_title_input")
                )
                Text("Icono", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojiOptions.forEach { e ->
                        val picked = emoji == e
                        Surface(
                            shape = CircleShape,
                            color = if (picked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { emoji = e }
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text(e, fontSize = 18.sp) }
                        }
                    }
                }
                Text("Color", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { hex ->
                        val c = runCatching {
                            Color(android.graphics.Color.parseColor(hex))
                        }.getOrDefault(MaterialTheme.colorScheme.primary)
                        Surface(
                            shape = CircleShape,
                            color = c,
                            border = if (colorHex == hex) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { colorHex = hex }
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onSave(title.trim(), emoji, colorHex) },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_custom_list_button")
            ) {
                Text("Crear lista")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCustomListItemDialog(
    listId: String,
    members: List<FamilyMember>,
    initialItem: CustomListItem?,
    onDismiss: () -> Unit,
    onSave: (CustomListItemDraft) -> Unit
) {
    var title by remember(initialItem) { mutableStateOf(initialItem?.title ?: "") }
    var quantity by remember(initialItem) { mutableStateOf(initialItem?.quantity ?: "") }
    var notes by remember(initialItem) { mutableStateOf(initialItem?.notes ?: "") }
    var assigned by remember(initialItem) { mutableStateOf(initialItem?.assignedMemberName ?: "Todos") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialItem == null) "Añadir elemento" else "Editar elemento",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Elemento") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_item_title_input")
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Cantidad (opcional, ej: 2 uds, 1 caja)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (members.isNotEmpty()) {
                    Text("Responsable", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val opts = listOf("Todos" to "👨‍👩‍👧‍👦") + members.map { it.name to it.avatarEmoji }
                        opts.forEach { (name, em) ->
                            val picked = assigned == name
                            Surface(
                                shape = CircleShape,
                                color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { assigned = name }
                            ) {
                                Text(
                                    text = "$em $name",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (picked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    if (title.isNotBlank()) {
                        onSave(
                            CustomListItemDraft(
                                listId = listId,
                                title = title,
                                quantity = quantity,
                                notes = notes,
                                assignedMemberName = assigned
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_custom_item_button")
            ) {
                Text(if (initialItem == null) "Añadir" else "Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
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
