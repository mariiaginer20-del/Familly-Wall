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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryScope
import com.example.data.model.DefaultCategories
import com.example.data.model.EventReminderOptions
import com.example.data.model.FamilyCategory
import com.example.data.model.FamilyMember
import com.example.data.model.FamilyTask
import com.example.data.model.TaskPriority
import com.example.ui.TaskDraft
import com.example.ui.theme.PriorityUrgente
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun resolveDueEpochMillis(option: String): Long {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return when (option) {
        "Hoy" -> cal.timeInMillis
        "Mañana" -> {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.timeInMillis
        }
        "Esta semana" -> {
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val daysToFriday = when (dow) {
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 5
                else -> (Calendar.FRIDAY - dow).coerceAtLeast(1)
            }
            cal.add(Calendar.DAY_OF_YEAR, daysToFriday)
            cal.timeInMillis
        }
        "Fin de semana" -> {
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val daysToSat = (Calendar.SATURDAY - dow + 7) % 7
            cal.add(Calendar.DAY_OF_YEAR, daysToSat)
            cal.timeInMillis
        }
        "Sin fecha" -> 0L
        else -> cal.timeInMillis
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    members: List<FamilyMember>,
    categories: List<FamilyCategory>,
    initialTask: FamilyTask? = null,
    onManageCategories: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (TaskDraft) -> Unit
) {
    val availableCategories = remember(categories) {
        categories.ifEmpty { DefaultCategories.forScope(CategoryScope.TASK) }
    }

    var title by remember(initialTask) { mutableStateOf(initialTask?.title ?: "") }
    var description by remember(initialTask) { mutableStateOf(initialTask?.description ?: "") }
    var assignedMemberName by remember(initialTask) {
        mutableStateOf(initialTask?.assignedMemberName ?: members.firstOrNull()?.name ?: "Todos")
    }
    var assignedEmoji by remember(initialTask) {
        mutableStateOf(initialTask?.assignedMemberEmoji ?: members.firstOrNull()?.avatarEmoji ?: "👨‍👩‍👧‍👦")
    }
    var dueDate by remember(initialTask) { mutableStateOf(initialTask?.dueDate ?: "Sin fecha") }
    var dueEpochMillis by remember(initialTask) {
        val initialMillis = initialTask?.dueEpochMillis ?: 0L
        val resolved = if (initialMillis > 0L) initialMillis
        else if (initialTask != null && initialTask.dueDate != "Sin fecha") resolveDueEpochMillis(initialTask.dueDate)
        else 0L
        mutableLongStateOf(resolved)
    }
    var dueTime by remember(initialTask) {
        mutableStateOf(initialTask?.dueTime?.ifBlank { "09:00" } ?: "09:00")
    }
    var selectedReminders by remember(initialTask) {
        mutableStateOf(initialTask?.reminderMinutesList ?: emptyList())
    }
    var priority by remember(initialTask) {
        mutableStateOf(initialTask?.let { TaskPriority.fromString(it.priority) } ?: TaskPriority.MEDIA)
    }
    var category by remember(initialTask, availableCategories) {
        mutableStateOf(
            initialTask?.category?.takeIf { it.isNotBlank() }
                ?: availableCategories.firstOrNull()?.name
                ?: "Hogar"
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dueOptions = listOf("Hoy", "Mañana", "Esta semana", "Fin de semana", "Sin fecha")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialTask == null) "Nueva Tarea Familiar" else "Editar Tarea",
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
                    label = { Text("¿Qué hay que hacer?") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Asignar a:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allOptions = listOf(
                        Triple("Todos", "👨‍👩‍👧‍👦", "Todos"),
                        Triple("Cualquiera", "⭐", "Cualquiera")
                    ) + members.map { Triple(it.name, it.avatarEmoji, it.name) }
                    allOptions.forEach { (name, emoji, tag) ->
                        val isSelected = assignedMemberName == name
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    assignedMemberName = name
                                    assignedEmoji = emoji
                                }
                                .testTag("task_assignee_$tag")
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

                Text(
                    text = "Fecha límite",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dueOptions.forEach { option ->
                        val isSelected = dueDate == option
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    dueDate = option
                                    dueEpochMillis = resolveDueEpochMillis(option)
                                    if (option == "Sin fecha") {
                                        selectedReminders = emptyList()
                                    }
                                }
                                .testTag("task_due_${option.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = option,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp)
                            )
                        }
                    }

                    val isCustomDate = dueDate !in dueOptions
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCustomDate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDatePicker = true }
                            .testTag("task_due_custom_date")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCustomDate) dueDate else "Elegir día",
                                fontSize = 11.sp,
                                fontWeight = if (isCustomDate) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCustomDate) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (dueEpochMillis > 0L && dueDate != "Sin fecha") {
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_due_time_picker")
                    ) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hora límite / aviso: $dueTime", style = MaterialTheme.typography.labelLarge)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                            selectedReminders - mins
                                        } else {
                                            selectedReminders + mins
                                        }
                                    }
                                    .testTag("task_reminder_chip_$mins")
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
                } else {
                    Text(
                        text = "Elige una fecha límite arriba si quieres programar avisos previos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Prioridad",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.entries.forEach { p ->
                        val isSelected = priority == p
                        val pColor = when (p) {
                            TaskPriority.ALTA -> PriorityUrgente
                            TaskPriority.MEDIA -> Color(0xFFF59E0B)
                            TaskPriority.BAJA -> Color(0xFF10B981)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) pColor else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { priority = p }
                        ) {
                            Text(
                                text = p.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
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
                    Text(
                        text = "Área / Categoría",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onManageCategories)
                            .testTag("manage_task_categories_btn")
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
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detalles opcionales") },
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
                        onSave(
                            TaskDraft(
                                title = title,
                                description = description,
                                assignedMemberName = assignedMemberName,
                                assignedMemberEmoji = assignedEmoji,
                                dueDate = dueDate,
                                dueEpochMillis = if (dueDate == "Sin fecha") 0L else dueEpochMillis,
                                dueTime = dueTime,
                                reminderMinutesList = if (dueDate == "Sin fecha") emptyList() else selectedReminders.distinct(),
                                priority = priority,
                                category = category
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text(if (initialTask == null) "Guardar Tarea" else "Actualizar Tarea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = if (dueEpochMillis > 0L) dueEpochMillis else System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            dueEpochMillis = millis
                            dueDate = SimpleDateFormat("d MMM", Locale("es", "ES"))
                                .format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val parts = dueTime.split(":")
        val initHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val initMin = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val timeState = rememberTimePickerState(
            initialHour = initHour,
            initialMinute = initMin,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    text = "Hora límite / aviso",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                Button(
                    onClick = {
                        dueTime = String.format(Locale.US, "%02d:%02d", timeState.hour, timeState.minute)
                        showTimePicker = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            }
        )
    }
}
