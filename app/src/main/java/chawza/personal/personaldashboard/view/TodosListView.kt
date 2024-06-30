package chawza.personal.personaldashboard.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chawza.personal.personaldashboard.core.USER_TOKEN_KEY
import chawza.personal.personaldashboard.core.userStore
import chawza.personal.personaldashboard.model.TodoListVIewModel
import chawza.personal.personaldashboard.repository.TodoService
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException


@Serializable
data class Todo(
    val id: Int? = null,
    val title: String,
    val note: String? = null,
    @SerialName("target_date")
    val targetDate: String? = null,
    val created: String? = null,
)


@Composable
fun AddButton(onCLick: () -> Unit) {
    IconButton(onClick = onCLick) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add new Todo")
    }
}

@Composable
fun AddTodoModal(
    modifier: Modifier = Modifier,
    dismiss: (sync: Boolean) -> Unit,
    onAddNewTodo: suspend (Todo) -> Boolean,
) {
    var isLoading by remember { mutableStateOf(false) }
    val closable by remember { derivedStateOf { !isLoading } }

    Dialog(
        onDismissRequest = { dismiss(false) },
        properties = DialogProperties(dismissOnClickOutside = closable, dismissOnBackPress = closable)
    ) {
        var title by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var targetDate by remember { mutableStateOf("") }

        Surface(
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(text = "New Task", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = title,
                    singleLine = true,
                    onValueChange = { title = it },
                    label = { Text(text = "Title") }
                )
                OutlinedTextField(
                    modifier = Modifier
                        .height(56.dp * 4)
                        .fillMaxWidth(),
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(text = "Note") }
                )
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    onClick = {
                        val newTodo = Todo(title = title, note = note.ifEmpty { null })
                        isLoading = true

                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val result = withContext(Dispatchers.Default) {
                                    onAddNewTodo(newTodo)
                                }
                                if (result) {
                                    dismiss(true)
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = "Create")
                    Spacer(modifier.width(20.dp))
                }
            }

        }

    }
}

@Composable
fun TodoListView(
    viewModel: TodoListVIewModel,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val todos = viewModel.todos.collectAsState()
    val snackBar = remember { SnackbarHostState() }
    val showAddTodoModal = remember { mutableStateOf(false) }
    val userToken = remember {
        runBlocking {
            ctx.userStore.data.first()[USER_TOKEN_KEY]!!
        }
    }

    LaunchedEffect(true) {
        try {
            viewModel.syncTodos(userToken)
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                snackBar.showSnackbar("Unable connected to Server")
            }
        } catch (e: Error) {
            e.message?.let {
                snackBar.showSnackbar(it)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            AddButton(onCLick = {
                showAddTodoModal.value = true
            })
        },
        topBar = {
            Row {
                Text(
                    text = "My Todos",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBar)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            LazyColumn {
                items(todos.value) { todo ->
                    ListItem(
                        headlineContent = { Text(text = todo.title) },
                        trailingContent = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete Todo",
                                tint = Color.Red,
                                modifier = Modifier.clickable { 
                                    CoroutineScope(Dispatchers.Default).launch {
                                        val todoService = TodoService(userToken)

                                        val response = try {
                                            todoService.deleteTodo(todo)
                                        } catch (e: IOException) {
                                            snackBar.showSnackbar("Unable to connect to Server")
                                            return@launch
                                        }

                                        if (response.isSuccessful) {
                                            viewModel.syncTodos(userToken)
                                        } else {
                                            snackBar.showSnackbar("Delete Failed")
                                        }
                                    }
                                })
                        }
                    )
                    Divider()
                }
            }
            if (todos.value.isEmpty()) {
                Text(text = "No Todos")
            }
        }
    }

    if (showAddTodoModal.value) {
        AddTodoModal(
            dismiss = { sync ->
                showAddTodoModal.value = false
                if (sync)
                    CoroutineScope(Dispatchers.Default).launch {
                        viewModel.syncTodos(userToken)
                    }
            },
            onAddNewTodo = { todo ->
                withContext(Dispatchers.Default) {
                    val todoService = TodoService(userToken)
                    val response = try {
                        todoService.addTodo(todo)
                    } catch (e: IOException) {
                        snackBar.showSnackbar("Unable to connect to Server")
                        return@withContext false
                    }

                    if (!response.isSuccessful) {
                        when(response.code) {
                            in 400..499 -> snackBar.showSnackbar("Client Error")
                            in 500..599 -> snackBar.showSnackbar("Server Error")
                            else -> { }
                        }
                        return@withContext false
                    }
                    return@withContext true
                }
            }
        )
    }
}

@Preview
@Composable
fun TodoListPreview() {
    val viewModel = TodoListVIewModel()
    PersonalDashboardTheme {
        val todos = listOf(
            Todo(null, "lmao"),
            Todo(null, "Task Two"),
        )
        viewModel.setTodos(todos)
        TodoListView(modifier = Modifier.fillMaxSize(), viewModel = viewModel)
    }
}