package chawza.personal.personaldashboard.view

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import chawza.personal.personaldashboard.core.USER_ID
import chawza.personal.personaldashboard.core.USER_TOKEN_KEY
import chawza.personal.personaldashboard.core.userStore
import chawza.personal.personaldashboard.model.TodoListVIewModel
import chawza.personal.personaldashboard.repository.NewTodo
import chawza.personal.personaldashboard.repository.Todo
import chawza.personal.personaldashboard.repository.TodoRepository
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID


@Composable
fun TopBar(isLoading: Boolean = false, requestLogout: () -> Unit, requestRefresh: () -> Unit) {
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
                modifier = Modifier
                    .padding(16.dp)
                    .clickable { requestRefresh() }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {  // right area
            if (isLoading) {
                CircularProgressIndicator()
            }
            Box {
                IconButton(
                    onClick = { showMenu.value = true }
                ) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = "Users", modifier = Modifier.size(60.dp))
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
            TopBar(false, { }, {})
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
    isLoading: Boolean = false,
    onAddNewTodo: suspend (NewTodo) -> Unit,
) {
    val closable by remember { derivedStateOf { !isLoading } }

    Dialog(
        onDismissRequest = { dismiss() },
        properties = DialogProperties(
            dismissOnClickOutside = closable,
            dismissOnBackPress = closable
        )
    ) {
        TodoFormView(
            modifier = modifier,
            initial = null,
            onAddRequest = onAddNewTodo,
            onUpdateRequest = { throw Exception("Unhandled")},
        )
    }
}

@Composable
fun TodoListView(
    modifier: Modifier = Modifier,
    todoRepository: TodoRepository,
    viewModel: TodoListVIewModel = remember { TodoListVIewModel() }
) {
    val context = LocalContext.current
    val todos = viewModel.todos.collectAsState()
    val snackBar = remember { SnackbarHostState() }

    val showAddTodoPane = remember { mutableStateOf(false) }
    val selectedTodo = viewModel.selectedTodo.collectAsState()

    val isSyncing = remember { mutableStateOf(false) }
    val isUpdating = remember { mutableStateOf(false) }
    val isLoading = remember { derivedStateOf { isSyncing.value || isUpdating.value } }

    fun syncTodos() {
        viewModel.viewModelScope.launch {
            isSyncing.value = true
            val fetchedResult = todoRepository.fetchAll()
            fetchedResult
                .onSuccess { todos ->
                    viewModel.setTodos(todos)
                }
                .onFailure { error ->
                    CoroutineScope(Dispatchers.Main).launch {
                        snackBar.showSnackbar(error.message ?: "Something went wrong")
                    }
                }
            isSyncing.value = false
        }
    }

    LaunchedEffect(true) {
        syncTodos()
    }

    suspend fun handleAddTodo(todo: NewTodo) {
        withContext(Dispatchers.Main) {
            todo.userId = context.userStore.data.first()[USER_ID]!!
            val result = todoRepository.addTodo(todo)
            result.onFailure { error ->
                launch {
                    snackBar.showSnackbar(
                        error.message ?: "Something wrong happened"
                    )
                }
                return@withContext
            }

            val createdTodo: Todo = result.getOrThrow()
            showAddTodoPane.value = false

            launch {
                snackBar.showSnackbar(
                    "Task \"${createdTodo.title}\" task has been Added",
                    duration = SnackbarDuration.Short
                )
            }
            syncTodos()
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            AddButton(
                onCLick = {
                    showAddTodoPane.value = true
                }
            )
        },
        topBar = {
            TopBar(
                isLoading.value,
                requestLogout = {
                    CoroutineScope(Dispatchers.Main).launch {
                        context.userStore.edit { data ->
                            data.remove(USER_TOKEN_KEY)
                        }
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }
                },
                requestRefresh = {
                    syncTodos()
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBar)
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val leftAreaWidth = remember {
                derivedStateOf { if (selectedTodo.value == null) 1f else .5f }
            }
            // left size
            Surface(modifier = Modifier
                .fillMaxHeight()
                .animateContentSize()
                .fillMaxWidth(leftAreaWidth.value)) {
                LazyColumn {
                    items(todos.value) { todo ->
                        ListItem(
                            headlineContent = { Text(text = todo.title) },
                            modifier = Modifier.clickable {
                                if (selectedTodo.value != todo) {
                                    viewModel.selectTodo(todo)
                                } else {
                                    viewModel.unSelectTodo()
                                }
                            },
                            trailingContent = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete Todo",
                                    tint = Color.Red,
                                    modifier = Modifier.clickable {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val success = todoRepository.deleteTodo(todo)

                                            success.onFailure { error ->
                                                snackBar.showSnackbar(
                                                    error.message ?: "Something wrong happened"
                                                )
                                                return@launch
                                            }

                                            if (selectedTodo.value == todo) {
                                                viewModel.unSelectTodo()
                                            }

                                            syncTodos()

                                            // TODO: Add button to undo deleted task by clicking trailing icon
                                            snackBar.showSnackbar(
                                                "Task \"${todo.title}\" task has been deleted",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                )
                            }
                        )
                        Divider()
                    }
                }
                if (todos.value.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No Todos")
                    }
                }
            }

            if (selectedTodo.value != null) {  // Edit Pane
                AnimatedVisibility(visible = selectedTodo.value != null) {
                    key(selectedTodo.value) {
                        TodoFormView(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(1f),
                            initial = selectedTodo.value,
                            onUpdateRequest = {
                                withContext(Dispatchers.Main) {
                                    isUpdating.value = true
                                    delay(2000)
                                    isUpdating.value = false
                                }
                            },
                            onAddRequest = { }
                        )
                    }
                }
            }
        }
        if (showAddTodoPane.value) {
            Dialog(
                onDismissRequest = { showAddTodoPane.value = false },
            ) {
                TodoFormView(
                    initial = null,
                    onUpdateRequest = { throw Exception("Unhandled Error") },
                    onAddRequest = { newTodo ->
                        handleAddTodo(newTodo)
                    }
                )
            }
        }
    }
}

class MockRepository: TodoRepository {
    private val todos = mutableListOf<Todo>()
    override suspend fun fetchAll(): Result<List<Todo>> = Result.success(todos)

    override suspend fun deleteTodo(todo: Todo): Result<Unit> {
        todos.remove(todo)
        return Result.success(Unit)
    }

    override suspend fun addTodo(todo: NewTodo): Result<Todo>{
        todos.add(
            Todo(
                id = UUID.randomUUID().toString().slice(IntRange(0, 6)),
                title = todo.title,
                note = todo.note
            )
        )
        return Result.success(todos.last())
    }
}
@Preview
@Composable
fun TodoListPreview() {

    PersonalDashboardTheme {
        val repo = MockRepository()
        runBlocking {
            repo.addTodo(NewTodo("", "LMAO", "Notes"))
            repo.addTodo(NewTodo("", "LMAO", null))
        }
        TodoListView(modifier = Modifier.fillMaxSize(), todoRepository = repo)
    }
}