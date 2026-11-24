package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class EventReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 2001)
        FamilyNotificationManager(context).showReminderNotification(
            title = title,
            message = message,
            notificationId = notifId
        )
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }
}
