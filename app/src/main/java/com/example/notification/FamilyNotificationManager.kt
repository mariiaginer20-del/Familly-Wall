package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.CalendarEvent
import com.example.data.model.EventReminderOptions
import com.example.data.model.FamilyTask
import com.example.data.model.resolvedReminders
import java.util.Calendar

class FamilyNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "family_events_channel"
        const val CHANNEL_NAME = "Eventos Familiares"
        const val CHANNEL_DESCRIPTION = "Recordatorios de eventos y calendario familiar"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showEventNotification(event: CalendarEvent) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notifId = (event.id.hashCode().takeIf { it != 0 } ?: 101) and 0x7FFFFFFF
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val timeText = if (event.allDay) "Todo el día" else "${event.startTime} - ${event.endTime}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Evento próximo: ${event.title}")
            .setContentText("$timeText · ${event.assignedMemberName}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${event.title}\n$timeText\n${event.assignedMemberName}\n${if (event.location.isNotBlank()) event.location else "Sin ubicación específica"}\n${event.description}"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId.coerceAtLeast(1), notification)
        } catch (_: SecurityException) {
            // Handled safely
        }
    }

    fun showReminderNotification(title: String, message: String, notificationId: Int = 1001) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Handled safely
        }
    }

    fun scheduleEventReminders(event: CalendarEvent) {
        if (event.id.isBlank()) return
        cancelEventReminders(event.id)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val startCal = Calendar.getInstance().apply {
            timeInMillis = event.dateEpochMillis
            if (event.allDay) {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
            } else {
                val parts = event.startTime.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
                val min = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val eventStartMillis = startCal.timeInMillis
        val now = System.currentTimeMillis()
        val timeLabel = if (event.allDay) "Todo el día" else "${event.startTime} - ${event.endTime}"

        event.resolvedReminders().forEach { mins ->
            val triggerAt = eventStartMillis - mins * 60_000L
            if (triggerAt > now) {
                val reqCode = ("${event.id}_$mins".hashCode()) and 0x7FFFFFFF
                val remindText = EventReminderOptions.formatShort(mins)
                val intent = Intent(context, EventReminderReceiver::class.java).apply {
                    putExtra(EventReminderReceiver.EXTRA_TITLE, "Recordatorio ($remindText): ${event.title}")
                    putExtra(EventReminderReceiver.EXTRA_MESSAGE, "$timeLabel · ${event.assignedMemberName}")
                    putExtra(EventReminderReceiver.EXTRA_NOTIF_ID, reqCode)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                try {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } catch (_: SecurityException) {
                    // Handled safely without privileged exact-alarm permissions
                }
            }
        }
    }

    fun cancelEventReminders(eventId: String) {
        if (eventId.isBlank()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        EventReminderOptions.options.forEach { (mins, _) ->
            val reqCode = ("${eventId}_$mins".hashCode()) and 0x7FFFFFFF
            val intent = Intent(context, EventReminderReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
    }

    fun scheduleTaskReminders(task: FamilyTask) {
        if (task.id.isBlank()) return
        cancelTaskReminders(task.id)
        if (task.completed || task.dueEpochMillis <= 0L || task.reminderMinutesList.isEmpty()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val dueCal = Calendar.getInstance().apply {
            timeInMillis = task.dueEpochMillis
            val parts = task.dueTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
            val min = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dueMillis = dueCal.timeInMillis
        val now = System.currentTimeMillis()
        val timeLabel = task.dueTime.ifBlank { "09:00" }

        task.reminderMinutesList.distinct().forEach { mins ->
            val triggerAt = dueMillis - mins * 60_000L
            if (triggerAt > now) {
                val reqCode = ("task_${task.id}_$mins".hashCode()) and 0x7FFFFFFF
                val remindText = EventReminderOptions.formatShort(mins)
                val intent = Intent(context, EventReminderReceiver::class.java).apply {
                    putExtra(EventReminderReceiver.EXTRA_TITLE, "Tarea ($remindText): ${task.title}")
                    putExtra(EventReminderReceiver.EXTRA_MESSAGE, "${task.dueDate} $timeLabel · ${task.assignedMemberName}")
                    putExtra(EventReminderReceiver.EXTRA_NOTIF_ID, reqCode)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                try {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } catch (_: SecurityException) {
                    // Handled safely without privileged exact-alarm permissions
                }
            }
        }
    }

    fun cancelTaskReminders(taskId: String) {
        if (taskId.isBlank()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        EventReminderOptions.options.forEach { (mins, _) ->
            val reqCode = ("task_${taskId}_$mins".hashCode()) and 0x7FFFFFFF
            val intent = Intent(context, EventReminderReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
    }
}
