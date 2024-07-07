package chawza.personal.personaldashboard.model

import androidx.lifecycle.ViewModel
import chawza.personal.personaldashboard.repository.Todo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class TodoListVIewModel: ViewModel() {
    private val _todos = MutableStateFlow<List<Todo>>(listOf())

    val todos = _todos.asStateFlow()

    fun setTodos(todos: List<Todo>) {
        _todos.update { todos }
    }
}
