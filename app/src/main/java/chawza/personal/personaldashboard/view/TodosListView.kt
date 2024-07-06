package chawza.personal.personaldashboard.view

import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
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
import androidx.datastore.preferences.core.edit
import chawza.personal.personaldashboard.core.USER_TOKEN_KEY
import chawza.personal.personaldashboard.core.userStore
import chawza.personal.personaldashboard.model.TodoListVIewModel
import chawza.personal.personaldashboard.repository.TodoRepository
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException


@Serializable
data class Todo(
    val id: String? = null,
    val title: String,
    val note: String? = null,
//    @SerialName("target_date")
//    val targetDate: String? = null,
//    val created: String? = null,
)

@Composable
fun TopBar(isLoading: Boolean = false, requestLogout: () -> Unit) {
    val showMenu = remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {  // left area
            Text(
                text = "My Todos",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            if (isLoading) {
                CircularProgressIndicator()
            }
        }
        Row {  // right area
            Box {
                IconButton(
                    onClick = { showMenu.value = true }
                ) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = "Users")
                }
                AccountMenu(showMenu.value, dismiss = {showMenu.value = false}, requestLogout=requestLogout)
            }
        }
    }
}

@Composable
fun AccountMenu(show: Boolean = false, dismiss: () -> Unit, requestLogout: () -> Unit) {
    DropdownMenu(expanded = show, onDismissRequest = dismiss) {
        DropdownMenuItem(
            text = { Text("Logout", color = Color.Black) },
            onClick = requestLogout,
            trailingIcon = { Icons.Filled.ExitToApp}
        )
    }
}

@Preview
@Composable
fun PreviewTopBar() {
    PersonalDashboardTheme {
        Surface(modifier = Modifier.width(400.dp)) {
            TopBar(false, { })
        }
    }
}
@Composable
fun AddButton(onCLick: () -> Unit) {
    IconButton(onClick = onCLick) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add new Todo")
    }
}

@Composable
fun AddTodoModal(
    modifier: Modifier = Modifier,
    dismiss: () -> Unit,
    onAddNewTodo: suspend (Todo) -> Boolean,
) {
    var isLoading by remember { mutableStateOf(false) }
    val closable by remember { derivedStateOf { !isLoading } }

    Dialog(
        onDismissRequest = { dismiss() },
        properties = DialogProperties(
            dismissOnClickOutside = closable,
            dismissOnBackPress = closable
        )
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
                                val success = onAddNewTodo(newTodo)
                                if (success)
                                    dismiss()
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
    modifier: Modifier = Modifier,
    todoRepository: TodoRepository,
    viewModel: TodoListVIewModel = TodoListVIewModel(todoRepository, SnackbarHostState())
) {
    val context = LocalContext.current
    val todos = viewModel.todos.collectAsState()
    val snackBar = viewModel.snackBar
    val showAddTodoModal = remember { mutableStateOf(false) }
    val isSyncing = remember { mutableStateOf(false) }

    val isLoading = isSyncing.value


    LaunchedEffect(true) {
        launch {
            isSyncing.value = true
            viewModel.syncTodos()
            isSyncing.value = false
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
            TopBar(
                isLoading,
                requestLogout = {
                    CoroutineScope(Dispatchers.Main).launch {
                        context.userStore.edit { data ->
                            data.remove(USER_TOKEN_KEY)
                        }
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBar)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.TopStart
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
                                        try {
                                            todoRepository.deleteTodo(todo)
                                        } catch (e: Exception) {
                                            val message = when (e) {
                                                is IOException -> "Unable to connect to Server"
                                                else -> e.message ?: "Unable to Delete Todo"
                                            }
                                            snackBar.showSnackbar(message)
                                            return@launch
                                        }

                                        viewModel.syncTodos()

                                        withContext(Dispatchers.Main) {
                                            val message =
                                                "Task \"${todo.title}\"task has been deleted"
                                            snackBar.showSnackbar(
                                                message,
                                                duration = SnackbarDuration.Short
                                            )
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
            dismiss = {
                showAddTodoModal.value = false
            },
            onAddNewTodo = { todo ->
                CoroutineScope(Dispatchers.Default).async {
                    val createdTodo = try {
                        todoRepository.addTodo(todo)
                    } catch (e: Exception) {
                        val message = when (e) {
                            is IOException -> "Unable to connect to Server"
                            else -> e.message ?: "Unable to add task"
                        }
                        snackBar.showSnackbar(message)
                        return@async false
                    }
                    viewModel.syncTodos()
                    return@async true
                }.await()
            }
        )
    }
}

class MockRepository: TodoRepository {
    private val todos = mutableListOf<Todo>()
    override suspend fun fetchAll(): Result<List<Todo>> = Result.success(todos)

    override suspend fun deleteTodo(todo: Todo) {
        todos.remove(todo)
    }

    override suspend fun addTodo(todo: Todo): Todo {
        todos.add(todo)
        return todos.find { it.id == todo.id }!!
    }
}
@Preview
@Composable
fun TodoListPreview() {

    PersonalDashboardTheme {
        val repo = MockRepository()
        runBlocking {
            repo.addTodo(Todo("1", "LMAO", "Notes"))
            repo.addTodo(Todo("2", "LMAO", null))
        }
        TodoListView(modifier = Modifier.fillMaxSize(), todoRepository = repo)
    }
}