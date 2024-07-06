package chawza.personal.personaldashboard.model

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chawza.personal.personaldashboard.repository.TodoAPIRepository
import chawza.personal.personaldashboard.repository.TodoRepository
import chawza.personal.personaldashboard.view.Todo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class TodoListVIewModel(
    private val todoRepository: TodoRepository,
    val snackBar: SnackbarHostState = SnackbarHostState()
) : ViewModel() {
    private val _todos = MutableStateFlow<List<Todo>>(listOf())

    val todos = _todos.asStateFlow()

    fun setTodos(todos: List<Todo>) {
        _todos.update { todos }
    }

    suspend fun syncTodos() {
        viewModelScope.launch {
            val todos = todoRepository.fetchAll()
            todos
                .onSuccess {
                    setTodos(it)
                }
                .onFailure {
                    viewModelScope.launch {
                        snackBar.showSnackbar(it.message ?: "Something went wrong")
                    }
                }
        }
    }
}
