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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalendarEvent
import com.example.data.model.CategoryScope
import com.example.data.model.DefaultCategories
import com.example.data.model.EventDateUtils
import com.example.data.model.EventReminderOptions
import com.example.data.model.FamilyCategory
import com.example.data.model.FamilyMember
import com.example.data.model.resolvedReminders
import com.example.ui.CalendarEventDraft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddCalendarEventDialog(
    members: List<FamilyMember>,
    categories: List<FamilyCategory>,
    initialDateMillis: Long,
    initialEvent: CalendarEvent? = null,
    onManageCategories: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (CalendarEventDraft) -> Unit
) {
    val availableCategories = remember(categories) {
        categories.ifEmpty { DefaultCategories.forScope(CategoryScope.EVENT) }
    }

    var title by remember(initialEvent) { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember(initialEvent) { mutableStateOf(initialEvent?.description ?: "") }
    var location by remember(initialEvent) { mutableStateOf(initialEvent?.location ?: "") }
    var startMillis by remember(initialEvent, initialDateMillis) {
        mutableLongStateOf(EventDateUtils.normalizeNoonMillis(initialEvent?.dateEpochMillis ?: initialDateMillis))
    }
    var hasRangeEnd by remember(initialEvent) {
        mutableStateOf(
            initialEvent != null &&
                initialEvent.endDateEpochMillis > 0L &&
                EventDateUtils.dayKey(initialEvent.endDateEpochMillis) > EventDateUtils.dayKey(initialEvent.dateEpochMillis)
        )
    }
    var endMillis by remember(initialEvent, initialDateMillis) {
        mutableLongStateOf(
            if (initialEvent != null && initialEvent.endDateEpochMillis > 0L) {
                EventDateUtils.normalizeNoonMillis(initialEvent.endDateEpochMillis)
            } else {
                EventDateUtils.normalizeNoonMillis(initialEvent?.dateEpochMillis ?: initialDateMillis)
            }
        )
    }
    var extraDates by remember(initialEvent) {
        mutableStateOf(initialEvent?.extraDatesEpochMillis ?: emptyList())
    }
    var allDay by remember(initialEvent) { mutableStateOf(initialEvent?.allDay ?: false) }
    var startTime by remember(initialEvent) {
        mutableStateOf(
            if (initialEvent?.allDay == true) "10:00" else (initialEvent?.startTime ?: "17:00")
        )
    }
    var endTime by remember(initialEvent) {
        mutableStateOf(
            if (initialEvent?.allDay == true) "11:00" else (initialEvent?.endTime ?: "18:00")
        )
    }
    var assignedMemberName by remember(initialEvent) { mutableStateOf(initialEvent?.assignedMemberName ?: "Todos") }
    val initialColorHex = remember(initialEvent, members) {
        initialEvent?.memberColorHex?.takeIf { it.isNotBlank() }
            ?: members.firstOrNull { it.name == (initialEvent?.assignedMemberName ?: "Todos") }?.colorHex
            ?: "#1B4D3E"
    }
    var selectedColorHex by remember(initialEvent) { mutableStateOf(initialColorHex) }
    var hasCustomColor by remember(initialEvent) { mutableStateOf(initialEvent != null) }

    val colorPalette = remember(members, initialEvent) {
        val base = mutableListOf(
            "#1B4D3E" to "Verde bosque",
            "#C25934" to "Terracota",
            "#2847D6" to "Azul cobalto",
            "#0D9488" to "Turquesa",
            "#D97706" to "Ámbar",
            "#8B5CF6" to "Violeta",
            "#DB2777" to "Rosa",
            "#DC2626" to "Rojo coral",
            "#157A4B" to "Esmeralda",
            "#475569" to "Gris pizarra"
        )
        val extraHexes = buildList {
            initialEvent?.memberColorHex?.takeIf { it.isNotBlank() }?.let { add(it to "Color actual") }
            members.forEach { m ->
                if (m.colorHex.isNotBlank()) add(m.colorHex to "Color de ${m.name}")
            }
        }
        extraHexes.forEach { (hex, label) ->
            if (base.none { it.first.equals(hex, ignoreCase = true) }) {
                base.add(hex to label)
            }
        }
        base.toList()
    }

    var selectedCategory by remember(initialEvent, availableCategories) {
        mutableStateOf(
            initialEvent?.category?.takeIf { it.isNotBlank() }
                ?: availableCategories.firstOrNull()?.name
                ?: "General"
        )
    }
    var selectedReminders by remember(initialEvent) {
        mutableStateOf(
            initialEvent?.resolvedReminders()?.toSet() ?: setOf(15)
        )
    }

    var datePickerMode by remember { mutableStateOf<String?>(null) } // "START", "END", "EXTRA"
    var timePickerTarget by remember { mutableStateOf<String?>(null) } // "START", "END"

    val formatLongDate: (Long) -> String = { millis ->
        SimpleDateFormat("EEE, d MMM yyyy", Locale("es", "ES"))
            .format(Date(millis))
            .replaceFirstChar { it.uppercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialEvent == null) "Nuevo Evento Familiar" else "Editar Evento",
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
                    label = { Text("Título del evento (ej: Tutoría, Viaje, Médico)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Fechas del evento",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { datePickerMode = "START" },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("event_pick_date_button")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasRangeEnd) "Desde: ${formatLongDate(startMillis)}" else formatLongDate(startMillis),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Evento de varios días seguidos",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = hasRangeEnd,
                        onCheckedChange = { enabled ->
                            hasRangeEnd = enabled
                            if (enabled && EventDateUtils.dayKey(endMillis) <= EventDateUtils.dayKey(startMillis)) {
                                endMillis = startMillis + 86_400_000L
                            }
                        },
                        modifier = Modifier.testTag("event_multiday_switch")
                    )
                }

                if (hasRangeEnd) {
                    OutlinedButton(
                        onClick = { datePickerMode = "END" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("event_pick_end_date_button")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hasta: ${formatLongDate(endMillis)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    extraDates.forEach { extra ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { extraDates = extraDates.filterNot { it == extra } }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = EventDateUtils.shortDate(extra),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Quitar día",
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { datePickerMode = "EXTRA" }
                            .testTag("event_add_extra_day_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Añadir otro día suelto",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
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
                    Column {
                        Text(
                            text = "Todo el día",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (allDay) "Sin hora fija (00:00 – 23:59)" else "Selecciona hora de inicio y fin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = allDay,
                        onCheckedChange = { allDay = it },
                        modifier = Modifier.testTag("event_all_day_switch")
                    )
                }

                if (!allDay) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { timePickerTarget = "START" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("event_start_time_picker")
                        ) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Inicio: $startTime", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = { timePickerTarget = "END" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("event_end_time_picker")
                        ) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fin: $endTime", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Miembro asignado",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allOptions = listOf("Todos" to "👨‍👩‍👧‍👦") + members.map { it.name to it.avatarEmoji }
                    allOptions.forEach { (name, emoji) ->
                        val isSelected = assignedMemberName == name
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    assignedMemberName = name
                                    if (!hasCustomColor) {
                                        selectedColorHex = members.firstOrNull { it.name == name }?.colorHex ?: "#1B4D3E"
                                    }
                                }
                                .testTag("event_member_$name")
                        ) {
                            Text(
                                text = "$emoji $name",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Color en el calendario",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    colorPalette.forEach { (hex, label) ->
                        val swatchColor = runCatching {
                            Color(android.graphics.Color.parseColor(hex))
                        }.getOrDefault(MaterialTheme.colorScheme.primary)
                        val isColorSelected = selectedColorHex.equals(hex, ignoreCase = true)
                        val checkTint = if (swatchColor.luminance() > 0.5f) Color.Black else Color.White
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .clickable {
                                    selectedColorHex = hex
                                    hasCustomColor = true
                                }
                                .semantics { contentDescription = "Color $label" }
                                .testTag("event_color_$hex"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = swatchColor,
                                border = if (isColorSelected) {
                                    BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                if (isColorSelected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = checkTint,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
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
                            .testTag("manage_event_categories_btn")
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
                        val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat.name }
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

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Notificaciones previas (puedes elegir varias)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EventReminderOptions.options.forEach { (mins, label) ->
                        val isSelected = mins in selectedReminders
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedReminders = if (isSelected) {
                                        if (selectedReminders.size > 1) selectedReminders - mins else selectedReminders
                                    } else {
                                        selectedReminders + mins
                                    }
                                }
                                .testTag("reminder_chip_$mins")
                        ) {
                            Text(
                                text = if (isSelected) "✓ $label" else label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lugar (ej: Colegio, Polideportivo, Clínica)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detalles o notas adicionales") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val sortedReminders = selectedReminders.toList().sorted().ifEmpty { listOf(15) }
                        val finalEndMillis = if (hasRangeEnd && EventDateUtils.dayKey(endMillis) > EventDateUtils.dayKey(startMillis)) {
                            endMillis
                        } else {
                            0L
                        }
                        onSave(
                            CalendarEventDraft(
                                title = title,
                                description = description,
                                location = location,
                                dateEpochMillis = startMillis,
                                endDateEpochMillis = finalEndMillis,
                                extraDatesEpochMillis = extraDates,
                                allDay = allDay,
                                startTime = startTime,
                                endTime = endTime,
                                assignedMemberName = assignedMemberName,
                                memberColorHex = selectedColorHex,
                                category = selectedCategory,
                                reminderMinutesBefore = sortedReminders.first(),
                                reminderMinutesList = sortedReminders
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_event_button")
            ) {
                Text(if (initialEvent == null) "Guardar Evento" else "Actualizar Evento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (datePickerMode != null) {
        val initialSelection = when (datePickerMode) {
            "END" -> endMillis
            "EXTRA" -> startMillis
            else -> startMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelection)
        DatePickerDialog(
            onDismissRequest = { datePickerMode = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { picked ->
                            val normalized = EventDateUtils.normalizeNoonMillis(picked)
                            when (datePickerMode) {
                                "START" -> {
                                    startMillis = normalized
                                    if (hasRangeEnd && EventDateUtils.dayKey(endMillis) < EventDateUtils.dayKey(normalized)) {
                                        endMillis = normalized
                                    }
                                }
                                "END" -> {
                                    if (EventDateUtils.dayKey(normalized) >= EventDateUtils.dayKey(startMillis)) {
                                        endMillis = normalized
                                    } else {
                                        endMillis = startMillis
                                        startMillis = normalized
                                    }
                                }
                                "EXTRA" -> {
                                    val pickedKey = EventDateUtils.dayKey(normalized)
                                    if (pickedKey != EventDateUtils.dayKey(startMillis) &&
                                        extraDates.none { EventDateUtils.dayKey(it) == pickedKey } &&
                                        extraDates.size < 31
                                    ) {
                                        extraDates = (extraDates + normalized).sorted()
                                    }
                                }
                            }
                        }
                        datePickerMode = null
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerMode = null }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (timePickerTarget != null) {
        val sourceTime = if (timePickerTarget == "START") startTime else endTime
        val parts = sourceTime.split(":")
        val initHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 10
        val initMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initHour,
            initialMinute = initMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { timePickerTarget = null },
            title = {
                Text(
                    text = if (timePickerTarget == "START") "Hora de inicio" else "Hora de fin",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formatted = String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                        if (timePickerTarget == "START") {
                            startTime = formatted
                        } else {
                            endTime = formatted
                        }
                        timePickerTarget = null
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { timePickerTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
