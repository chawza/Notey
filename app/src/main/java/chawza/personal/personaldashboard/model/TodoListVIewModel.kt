package chawza.personal.personaldashboard.model

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chawza.personal.personaldashboard.repository.Todo
import chawza.personal.personaldashboard.services.TodosService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class TodoListVIewModel(private val todoService: TodosService): ViewModel() {
    private val isSyncing = MutableStateFlow(false)
    private val snackBar = SnackbarHostState()

    private val _todos = MutableStateFlow<List<Todo>>(listOf())
    val todos = _todos.asStateFlow()

    fun setTodos(todos: List<Todo>) {
        _todos.update { todos }
    }

    fun deleteById(id: Int) {
        _todos.update {
            it.filter { todo -> todo.id != id}
        }
    }

    fun syncTodos() {
        viewModelScope.launch {
            isSyncing.value = true
            val fetchedResult = todoService.fetch()
            fetchedResult
                .onSuccess { todos ->
                    setTodos(todos)
                }
                .onFailure { error ->
                    launch {
                        snackBar.showSnackbar(error.message ?: "Something went wrong")
                    }
                }
            isSyncing.value = false
        }
    }
}
