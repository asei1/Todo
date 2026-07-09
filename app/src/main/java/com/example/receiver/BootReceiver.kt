package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.TodoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device reboot completed. Rescheduling active reminders.")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = TodoDatabase.getDatabase(context)
                    val dao = db.todoDao()
                    // Fetch the first emission of all items
                    val todos = dao.getAllTodos().first()
                    var count = 0
                    for (todo in todos) {
                        if (!todo.isCompleted && todo.isReminderEnabled && !todo.reminderTime.isNullOrEmpty()) {
                            NotificationScheduler.scheduleReminder(
                                context = context,
                                todoId = todo.id,
                                title = todo.title,
                                timeString = todo.reminderTime,
                                repeatMode = todo.repeatMode
                            )
                            count++
                        }
                    }
                    Log.d("BootReceiver", "Rescheduled $count reminders successfully.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling reminders", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
