package chawza.personal.personaldashboard.model

import androidx.compose.material3.SnackbarDuration
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
    val isSyncing = MutableStateFlow(false)
    private val snackBar = SnackbarHostState()

    private val _todos = MutableStateFlow<List<Todo>>(listOf())
    val todos = _todos.asStateFlow()

    fun setTodos(todos: List<Todo>) {
        _todos.update { todos }
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

    fun handleDeleteTask(todo: Todo) {
        viewModelScope.launch {
            val result = todoService.delete(todo.id)

            result
                .onFailure { error ->
                    snackBar.showSnackbar(
                        error.message ?: "Something wrong happened"
                    )
                    return@launch
                }
                .onSuccess {
                    // TODO: Add button to undo deleted task by clicking trailing icon
                    launch {
                        snackBar.showSnackbar(
                            "Task \"${todo.title}\" task has been deleted",
                            duration = SnackbarDuration.Short
                        )
                    }

                    syncTodos()
                }
        }
    }
}
