package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AppDesignStyle
import com.example.ui.FamilyWallViewModel
import com.example.ui.MainTab
import com.example.ui.theme.LocalDesignStyle
import com.example.ui.theme.PriorityClub
import com.example.ui.theme.PriorityClubBg
import com.example.ui.theme.PriorityUrgente
import com.example.ui.theme.PriorityUrgenteBg
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: FamilyWallViewModel,
    onNavigateToTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val events by viewModel.allEvents.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val shoppingItems by viewModel.rawShoppingItems.collectAsStateWithLifecycle()
    val customLists by viewModel.customLists.collectAsStateWithLifecycle()
    val customListItems by viewModel.customListItems.collectAsStateWithLifecycle()
    val meals by viewModel.allMealPlans.collectAsStateWithLifecycle()

    val isBento = LocalDesignStyle.current == AppDesignStyle.NORDIC_BENTO
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val pendingTasks = tasks.filter { !it.completed }
    val pendingShopping = shoppingItems.filter { !it.checked }
    val urgentShopping = pendingShopping.filter { it.priority == "URGENTE" || it.priority == "CLUB" }
    val nextEvent = events.firstOrNull()

    val calendar = Calendar.getInstance()
    val dayOfWeekSundayBased = calendar.get(Calendar.DAY_OF_WEEK)
    val dayOfWeekMondayBased = if (dayOfWeekSundayBased == Calendar.SUNDAY) 6 else dayOfWeekSundayBased - 2
    val todayMeals = meals.filter { it.dayOfWeekIndex == dayOfWeekMondayBased }

    val dateFormatted = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES")).format(Date())
        .replaceFirstChar { it.uppercase() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(if (isBento) 16.dp else 20.dp)
    ) {
        // Hero Summary (adapts between Editorial Bar & Nordic Bento 2x2 Grid)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = dateFormatted.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBento) "Panel del hogar" else "Resumen del día",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isBento) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BentoKpiTile(
                                count = events.size,
                                label = "Eventos",
                                icon = Icons.Outlined.CalendarMonth,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = { onNavigateToTab(MainTab.CALENDAR) },
                                modifier = Modifier.weight(1f)
                            )
                            BentoKpiTile(
                                count = pendingTasks.size,
                                label = "Tareas pend.",
                                icon = Icons.Outlined.Assignment,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = { onNavigateToTab(MainTab.TASKS) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BentoKpiTile(
                                count = pendingShopping.size,
                                label = "En la compra",
                                icon = Icons.Outlined.ShoppingCart,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                onClick = { onNavigateToTab(MainTab.SHOPPING) },
                                modifier = Modifier.weight(1f)
                            )
                            BentoKpiTile(
                                count = customLists.size,
                                label = "Otras listas",
                                icon = Icons.Outlined.Checklist,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                onClick = { onNavigateToTab(MainTab.SHOPPING) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SummaryStatColumn(
                                count = events.size,
                                label = "Eventos",
                                onClick = { onNavigateToTab(MainTab.CALENDAR) },
                                modifier = Modifier.weight(1f)
                            )
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            )
                            SummaryStatColumn(
                                count = pendingTasks.size,
                                label = "Tareas",
                                onClick = { onNavigateToTab(MainTab.TASKS) },
                                modifier = Modifier.weight(1f)
                            )
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            )
                            SummaryStatColumn(
                                count = pendingShopping.size,
                                label = "En compra",
                                onClick = { onNavigateToTab(MainTab.SHOPPING) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Next Upcoming Event
        if (nextEvent != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeaderRow(
                        eyebrow = "AGENDA COMPARTIDA",
                        title = "Próximo evento",
                        actionLabel = "Ver calendario",
                        onAction = { onNavigateToTab(MainTab.CALENDAR) }
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onNavigateToTab(MainTab.CALENDAR) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val accent = runCatching {
                                Color(android.graphics.Color.parseColor(nextEvent.memberColorHex))
                            }.getOrDefault(MaterialTheme.colorScheme.primary)

                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(accent)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${viewModel.formatEventDate(nextEvent.dateEpochMillis)} · ${nextEvent.startTime}–${nextEvent.endTime}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = nextEvent.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (nextEvent.location.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${nextEvent.location} · ${nextEvent.assignedMemberName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { viewModel.triggerEventNotification(nextEvent) },
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("home_test_notification_btn"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.NotificationsActive,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Enviar recordatorio", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Today's Menu
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeaderRow(
                    eyebrow = "PLANNING DE COMIDAS",
                    title = "Menú de hoy",
                    actionLabel = "Semana",
                    onAction = { onNavigateToTab(MainTab.MEALS) }
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onNavigateToTab(MainTab.MEALS) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        val mealTypes = listOf(
                            "DESAYUNO" to "Desayuno",
                            "COMIDA" to "Comida",
                            "MERIENDA" to "Merienda",
                            "CENA" to "Cena"
                        )
                        mealTypes.forEachIndexed { idx, (type, label) ->
                            val dish = todayMeals.firstOrNull { it.mealType == type }?.dishName?.takeIf { it.isNotBlank() } ?: "Sin definir"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(88.dp)
                                )
                                Text(
                                    text = dish,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (dish == "Sin definir") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (idx < mealTypes.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }

        // General Custom Lists Overview
        if (customLists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeaderRow(
                        eyebrow = "LISTAS GENERALES",
                        title = "Otras listas",
                        actionLabel = "Ver (${customLists.size})",
                        onAction = { onNavigateToTab(MainTab.SHOPPING) }
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onNavigateToTab(MainTab.SHOPPING) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            val shown = customLists.take(3)
                            shown.forEachIndexed { idx, list ->
                                val itemsInList = customListItems.filter { it.listId == list.id }
                                val pendingInList = itemsInList.count { !it.checked }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text(text = list.emoji, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = list.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "$pendingInList pend. / ${itemsInList.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (idx < shown.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Urgent & Club Shopping Items
        if (urgentShopping.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeaderRow(
                        eyebrow = "PRIORITARIOS EN COMPRA",
                        title = "Urgente & Club",
                        actionLabel = "Compra (${urgentShopping.size})",
                        onAction = { onNavigateToTab(MainTab.SHOPPING) }
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onNavigateToTab(MainTab.SHOPPING) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            val shown = urgentShopping.take(4)
                            shown.forEachIndexed { idx, item ->
                                val isUrgent = item.priority == "URGENTE"
                                val pillFg = if (isUrgent) {
                                    if (isDark) Color(0xFFFF8A80) else PriorityUrgente
                                } else {
                                    if (isDark) Color(0xFFCBB2FF) else PriorityClub
                                }
                                val pillBg = if (isUrgent) {
                                    if (isDark) PriorityUrgente.copy(alpha = 0.22f) else PriorityUrgenteBg
                                } else {
                                    if (isDark) PriorityClub.copy(alpha = 0.22f) else PriorityClubBg
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = pillBg
                                        ) {
                                            Text(
                                                text = if (isUrgent) "URGENTE" else "CLUB",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = pillFg,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                                    Text(
                                        text = "$qtyStr ${item.unit} · ${item.supermarket}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (idx < shown.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pending Tasks
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeaderRow(
                    eyebrow = "RESPONSABILIDADES",
                    title = "Tareas pendientes",
                    actionLabel = "Todas (${pendingTasks.size})",
                    onAction = { onNavigateToTab(MainTab.TASKS) }
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onNavigateToTab(MainTab.TASKS) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        if (pendingTasks.isEmpty()) {
                            Text(
                                text = "Todas las tareas familiares están completadas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            val shown = pendingTasks.take(4)
                            shown.forEachIndexed { idx, task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${task.dueDate} · ${task.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${task.assignedMemberEmoji} ${task.assignedMemberName}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                if (idx < shown.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BentoKpiTile(
    count: Int,
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
        border = borderColor?.let { BorderStroke(1.dp, it) }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.headlineMedium,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun SummaryStatColumn(
    count: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun SectionHeaderRow(
    eyebrow: String,
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAction)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
