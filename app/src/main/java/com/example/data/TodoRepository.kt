package com.example.data

import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {
    val allTodos: Flow<List<TodoEntity>> = todoDao.getAllTodos()

    suspend fun getTodoById(id: Int): TodoEntity? {
        return todoDao.getTodoById(id)
    }

    suspend fun insert(todo: TodoEntity): Long {
        return todoDao.insertTodo(todo)
    }

    suspend fun update(todo: TodoEntity) {
        todoDao.updateTodo(todo)
    }

    suspend fun delete(todo: TodoEntity) {
        todoDao.deleteTodo(todo)
    }

    suspend fun deleteById(id: Int) {
        todoDao.deleteTodoById(id)
    }
}
