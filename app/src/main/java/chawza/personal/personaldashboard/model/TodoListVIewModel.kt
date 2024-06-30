package chawza.personal.personaldashboard.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chawza.personal.personaldashboard.repository.TodoService
import chawza.personal.personaldashboard.view.Todo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class TodoListVIewModel: ViewModel() {
    private val _todos = MutableStateFlow<List<Todo>>(listOf())

    val todos = _todos.asStateFlow()

    fun setTodos(todos: List<Todo>) {
        _todos.update { todos }
    }

    suspend fun syncTodos(token: String) {
        val response = TodoService(token).fetchAll()

        if (!response.isSuccessful) {
            when(response.code) {
                in 400..499 -> throw Error("Client Error")
                in 500..599 -> throw Error("Serer Error")
            }
        }

        val json = Json { ignoreUnknownKeys = true }
        val todos = response.body?.let {
            json.decodeFromString<List<Todo>>(it.string())
        }
        response.close()

        viewModelScope.launch {
            setTodos(todos ?: listOf())
        }
    }
}