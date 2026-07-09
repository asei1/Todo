package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getIntExtra("TODO_ID", -1)
        val title = intent.getStringExtra("TODO_TITLE") ?: "할 일 일정이 있습니다!"
        val timeString = intent.getStringExtra("TODO_TIME")
        val repeatMode = intent.getStringExtra("TODO_REPEAT") ?: "NONE"

        Log.d("TodoReminderReceiver", "Alarm received for Todo: $todoId, Title: $title, Repeat: $repeatMode")

        if (todoId != -1) {
            // Show notification
            NotificationScheduler.showNotification(context, todoId, title)

            // Reschedule if periodic/repeating
            if (repeatMode != "NONE" && timeString != null) {
                NotificationScheduler.scheduleReminder(
                    context = context,
                    todoId = todoId,
                    title = title,
                    timeString = timeString,
                    repeatMode = repeatMode
                )
            }
        }
    }
}
