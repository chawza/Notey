package chawza.personal.personaldashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chawza.personal.personaldashboard.core.USER_TOKEN_KEY
import chawza.personal.personaldashboard.core.userStore
import chawza.personal.personaldashboard.repository.NewTodo
import chawza.personal.personaldashboard.repository.Todo
import chawza.personal.personaldashboard.services.TodosService
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class TodoFormViewModel(private val todoService: TodosService) : ViewModel() {
    var isLoading = MutableStateFlow(false)
    val snackBar = SnackbarHostState()

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _note = MutableStateFlow("")
    val note = _note.asStateFlow()

    fun setTitle(value: String) {
        _title.value = value
    }

    fun setNote(value: String) {
        _note.value = value
    }

    suspend fun handleCreate(): Result<Todo> {
        return viewModelScope.async {
            isLoading.value = true

            todoService.create(NewTodo(title.value, note.value))
                .onFailure { error ->
                    launch {
                        snackBar.showSnackbar(
                            error.message ?: "Failed to create new task",
                            duration = SnackbarDuration.Short
                        )
                    }
                    isLoading.value = false
                    return@async Result.failure(Exception("ASDAS"))
                }
                .onSuccess { newTodo ->
                    isLoading.value = false
                    return@async Result.success(newTodo)
                }
        }.await()
    }

    fun handleUpdate(updateTodo: Todo) {
        viewModelScope.launch {
            isLoading.value = true

            val currTitle = title.value
            val currNote = note.value

            if (currTitle.isEmpty()) {
                async {
                    snackBar.showSnackbar("Title must be filled")
                }.await()

                return@launch
            }

            val updated = updateTodo.copy(title = currTitle, notes = currNote)

            todoService.update(updated)
                .onFailure { error ->
                    launch {
                        snackBar.showSnackbar(
                            error.message ?: "Failed to update task",
                            duration = SnackbarDuration.Short
                        )
                    }
                    isLoading.value = false
                }
                .onSuccess {
                    isLoading.value = false
                    snackBar.showSnackbar("Task updated!")
                }
        }
    }

}

class EditTaskActivity : ComponentActivity() {
    private var updateTodo: Todo? = null
    private lateinit var userToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (this.intent.hasExtra("todo")) {
            updateTodo = Json.decodeFromString<Todo>(this.intent.getStringExtra("todo")!!)
        }

        userToken = runBlocking { this@EditTaskActivity.userStore.data.first()[USER_TOKEN_KEY]!! }

        val isUpdate = updateTodo != null
        val service = TodosService(this.userToken)
        val viewModel = TodoFormViewModel(service)

        setContent {
            val isLoading = viewModel.isLoading.collectAsState()
            val title by viewModel.title.collectAsState()
            val note by viewModel.note.collectAsState()

            LaunchedEffect(Unit) {
                launch {
                    this@EditTaskActivity.userStore.data.collect { data ->
                        userToken = data[USER_TOKEN_KEY]!!
                    }
                }

                if (isUpdate) {
                    viewModel.setTitle(updateTodo!!.title)
                    viewModel.setNote(updateTodo!!.notes ?: "")
                }
            }

            PersonalDashboardTheme {
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = viewModel.snackBar) }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier.padding(paddingValues),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isUpdate) "Update Task" else "New Task",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (isLoading.value) {
                                    CircularProgressIndicator()
                                }
                                if (isUpdate) {
                                    Checkbox(
                                        checked = updateTodo!!.done != null,
                                        onCheckedChange = {})
                                }
                            }
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = title,
                                singleLine = true,
                                onValueChange = { viewModel.setTitle(it) },
                                label = { Text(text = "Title") }
                            )
                            OutlinedTextField(
                                modifier = Modifier
                                    .height(56.dp * 4)
                                    .fillMaxWidth(),
                                value = note,
                                onValueChange = { viewModel.setNote(it) },
                                label = { Text(text = "Note") }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    onClick = {
                                        if (isUpdate)
                                            viewModel.handleUpdate(updateTodo!!)
                                        else {
                                            viewModel.viewModelScope.launch {
                                                viewModel.handleCreate()
                                                    .onSuccess {
                                                        this@EditTaskActivity.finish()
                                                    }
                                            }
                                        }
                                    },
                                    enabled = !isLoading.value,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(text = if (isUpdate) "Update" else "Create")
                                }
                                if (isUpdate) {
                                    Button(
                                        onClick = {
                                            viewModel.viewModelScope.launch {
                                                TodosService(userToken).delete(updateTodo!!.id)
                                                    .onFailure {
                                                        launch {
                                                            viewModel.snackBar.showSnackbar("Failed to Delete task")
                                                        }
                                                    }
                                                    .onSuccess {
                                                        this@EditTaskActivity.finish()
                                                    }
                                            }
                                        },
                                        modifier = Modifier.size(56.dp),
                                        shape = MaterialTheme.shapes.small,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        contentPadding = PaddingValues(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "delete task",
                                            tint = Color.White,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}