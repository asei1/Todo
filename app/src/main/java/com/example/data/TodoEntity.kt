package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val category: String = "Others", // "Work", "Personal", "Shopping", "Health", "Study", "Others"
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val dueDate: Long? = null,       // Date timestamp
    val isPeriodic: Boolean = false,
    val repeatMode: String = "NONE", // "NONE", "DAILY", "WEEKLY", "MONTHLY"
    val reminderTime: String? = null, // "HH:mm" formatted time
    val isReminderEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
