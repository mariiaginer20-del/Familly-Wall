package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CalendarEvent
import com.example.data.model.EventCategory
import com.example.data.preferences.AppDesignStyle
import com.example.ui.FamilyWallViewModel
import com.example.ui.theme.LocalDesignStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun dayKey(epochMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.get(Calendar.YEAR) * 10000L + (cal.get(Calendar.MONTH) + 1) * 100L + cal.get(Calendar.DAY_OF_MONTH)
}

@Composable
fun CalendarScreen(
    viewModel: FamilyWallViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.filteredEvents.collectAsStateWithLifecycle()
    val selectedMember by viewModel.selectedMemberFilter.collectAsStateWithLifecycle()
    val selectedDateMillis by viewModel.selectedCalendarDateMillis.collectAsStateWithLifecycle()

    val isBento = LocalDesignStyle.current == AppDesignStyle.NORDIC_BENTO

    val initialCal = remember { Calendar.getInstance() }
    var displayedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
    var showAllEvents by remember { mutableStateOf(false) }

    val todayKey = remember { dayKey(System.currentTimeMillis()) }
    val selectedDayKey = remember(selectedDateMillis) { dayKey(selectedDateMillis) }

    val eventsByDay = remember(events) {
        events.groupBy { dayKey(it.dateEpochMillis) }
    }

    val monthTitle = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
            .format(cal.time)
            .replaceFirstChar { it.uppercase() }
    }

    val calendarCells = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val firstDayDow = cal.get(Calendar.DAY_OF_WEEK) // Sun=1..Sat=7
        val leadingEmpty = if (firstDayDow == Calendar.SUNDAY) 6 else firstDayDow - 2
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<Pair<Int?, Long?>>()
        repeat(leadingEmpty) { cells.add(null to null) }
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            cells.add(day to cal.timeInMillis)
        }
        while (cells.size % 7 != 0) {
            cells.add(null to null)
        }
        cells.chunked(7)
    }

    val visibleEvents = remember(events, selectedDayKey, showAllEvents) {
        if (showAllEvents) events
        else events.filter { dayKey(it.dateEpochMillis) == selectedDayKey }
    }

    val selectedDateLabel = remember(selectedDateMillis) {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
            .format(Date(selectedDateMillis))
            .replaceFirstChar { it.uppercase() }
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
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
                            text = if (selectedMember == "Todos") "CALENDARIO FAMILIAR" else "AGENDA · ${selectedMember.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Agenda mensual",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (!showAllEvents) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showAllEvents = !showAllEvents }
                            .testTag("calendar_toggle_all_button")
                    ) {
                        Text(
                            text = if (showAllEvents) "Ver solo día" else "Ver todos (${events.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (!showAllEvents) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Monthly Calendar Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Month controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (displayedMonth == 0) {
                                        displayedMonth = 11
                                        displayedYear -= 1
                                    } else {
                                        displayedMonth -= 1
                                    }
                                },
                                modifier = Modifier.testTag("calendar_prev_month")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Mes anterior"
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = monthTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val now = Calendar.getInstance()
                                            displayedYear = now.get(Calendar.YEAR)
                                            displayedMonth = now.get(Calendar.MONTH)
                                            viewModel.selectCalendarDate(now.timeInMillis)
                                        }
                                        .testTag("calendar_today_button")
                                ) {
                                    Text(
                                        text = "Hoy",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (displayedMonth == 11) {
                                        displayedMonth = 0
                                        displayedYear += 1
                                    } else {
                                        displayedMonth += 1
                                    }
                                },
                                modifier = Modifier.testTag("calendar_next_month")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Mes siguiente"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Weekday headers (L M X J V S D)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("L", "M", "X", "J", "V", "S", "D").forEach { dow ->
                                Text(
                                    text = dow,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Calendar weeks
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            calendarCells.forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    week.forEach { (dayNum, cellMillis) ->
                                        if (dayNum == null || cellMillis == null) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                            )
                                        } else {
                                            val k = dayKey(cellMillis)
                                            val isSelected = k == selectedDayKey
                                            val isToday = k == todayKey
                                            val dayEvents = eventsByDay[k].orEmpty()

                                            val cellShape = if (isBento) RoundedCornerShape(12.dp) else CircleShape
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(cellShape)
                                                    .background(
                                                        when {
                                                            isSelected -> MaterialTheme.colorScheme.primary
                                                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .then(
                                                        if (isToday && !isSelected) Modifier.border(
                                                            1.dp,
                                                            MaterialTheme.colorScheme.primary,
                                                            cellShape
                                                        ) else Modifier
                                                    )
                                                    .clickable {
                                                        viewModel.selectCalendarDate(cellMillis)
                                                        showAllEvents = false
                                                    }
                                                    .testTag("calendar_cell_$dayNum"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = "$dayNum",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                                                        ),
                                                        color = when {
                                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                                            isToday -> MaterialTheme.colorScheme.primary
                                                            else -> MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )
                                                    if (dayEvents.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                            dayEvents.take(3).forEach { ev ->
                                                                val dotColor = if (isSelected) {
                                                                    MaterialTheme.colorScheme.onPrimary
                                                                } else {
                                                                    runCatching {
                                                                        Color(android.graphics.Color.parseColor(ev.memberColorHex))
                                                                    }.getOrDefault(MaterialTheme.colorScheme.secondary)
                                                                }
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(5.dp)
                                                                        .clip(CircleShape)
                                                                        .background(dotColor)
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
            }

            // Events section header
            item {
                Text(
                    text = if (showAllEvents) "Todos los eventos (${visibleEvents.size})" else "$selectedDateLabel (${visibleEvents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (visibleEvents.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Día despejado",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pulsa 'Nuevo evento' para programar citas o actividades en esta fecha.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(visibleEvents, key = { it.id }) { event ->
                    CalendarEventCard(
                        event = event,
                        dateLabel = viewModel.formatEventDate(event.dateEpochMillis),
                        onEdit = { viewModel.startEditingEvent(event) },
                        onTriggerNotification = { viewModel.triggerEventNotification(event) },
                        onDelete = { viewModel.deleteCalendarEvent(event) }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { viewModel.setShowAddEventDialog(true) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Nuevo evento", style = MaterialTheme.typography.labelLarge) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_event_fab")
        )
    }
}

@Composable
fun CalendarEventCard(
    event: CalendarEvent,
    dateLabel: String,
    onEdit: () -> Unit,
    onTriggerNotification: () -> Unit,
    onDelete: () -> Unit
) {
    val memberColor = runCatching {
        Color(android.graphics.Color.parseColor(event.memberColorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)
    val cat = EventCategory.fromString(event.category)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onEdit)
            .testTag("calendar_event_${event.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.width(56.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = event.startTime,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = event.endTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(memberColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${cat.emoji} ${dateLabel.uppercase()} · ${event.assignedMemberName.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.7.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = memberColor,
                        modifier = Modifier.weight(1f)
                    )

                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Editar evento",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Eliminar evento",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (event.location.isNotBlank()) {
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
                            text = event.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aviso ${event.reminderMinutesBefore} min antes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onTriggerNotification,
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Probar aviso",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
