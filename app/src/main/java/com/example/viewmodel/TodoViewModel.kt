package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TodoDatabase
import com.example.data.TodoEntity
import com.example.data.TodoRepository
import com.example.receiver.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TodoDatabase.getDatabase(application)
    private val repository = TodoRepository(database.todoDao())

    private val sharedPrefs = application.getSharedPreferences("todo_settings", Context.MODE_PRIVATE)

    // Raw stream of todos from Room
    val todos: StateFlow<List<TodoEntity>> = repository.allTodos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter states
    val selectedCategoryFilter = MutableStateFlow("전체")
    val selectedPriorityFilter = MutableStateFlow("전체")
    val searchQuery = MutableStateFlow("")

    // Derived filtered list of todos
    val filteredTodos: StateFlow<List<TodoEntity>> = combine(
        todos,
        selectedCategoryFilter,
        selectedPriorityFilter,
        searchQuery
    ) { list, category, priority, query ->
        list.filter { item ->
            val matchesCategory = (category == "전체") || (item.category == category)
            
            val mappedPriority = when (priority) {
                "높음" -> "HIGH"
                "보통" -> "MEDIUM"
                "낮음" -> "LOW"
                else -> "전체"
            }
            val matchesPriority = (mappedPriority == "전체") || (item.priority == mappedPriority)
            
            val matchesSearch = query.isEmpty() || 
                    item.title.contains(query, ignoreCase = true) || 
                    item.description.contains(query, ignoreCase = true)
            
            matchesCategory && matchesPriority && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // App theme state: "SYSTEM", "LIGHT", "DARK"
    val themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")

    fun setThemeMode(mode: String) {
        sharedPrefs.edit().putString("theme_mode", mode).apply()
        themeMode.value = mode
    }

    // Insert Todo
    fun addTodo(
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDate: Long?,
        isPeriodic: Boolean,
        repeatMode: String,
        reminderTime: String?,
        isReminderEnabled: Boolean
    ) {
        viewModelScope.launch {
            val newTodo = TodoEntity(
                title = title,
                description = description,
                category = category,
                priority = priority,
                dueDate = dueDate,
                isPeriodic = isPeriodic,
                repeatMode = repeatMode,
                reminderTime = reminderTime,
                isReminderEnabled = isReminderEnabled
            )
            val generatedId = repository.insert(newTodo)

            // Setup system notification alarm if enabled
            if (isReminderEnabled && !reminderTime.isNullOrEmpty()) {
                NotificationScheduler.scheduleReminder(
                    context = getApplication(),
                    todoId = generatedId.toInt(),
                    title = title,
                    timeString = reminderTime,
                    repeatMode = repeatMode
                )
            }
        }
    }

    // Update Todo
    fun updateTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.update(todo)

            // Check if alarm needs rescheduling or canceling
            if (todo.isCompleted || !todo.isReminderEnabled || todo.reminderTime.isNullOrEmpty()) {
                NotificationScheduler.cancelReminder(getApplication(), todo.id)
            } else {
                NotificationScheduler.scheduleReminder(
                    context = getApplication(),
                    todoId = todo.id,
                    title = todo.title,
                    timeString = todo.reminderTime,
                    repeatMode = todo.repeatMode
                )
            }
        }
    }

    // Quick complete toggle
    fun toggleComplete(todo: TodoEntity) {
        val updatedTodo = todo.copy(isCompleted = !todo.isCompleted)
        updateTodo(updatedTodo)
    }

    // Delete Todo
    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.delete(todo)
            NotificationScheduler.cancelReminder(getApplication(), todo.id)
        }
    }
}
