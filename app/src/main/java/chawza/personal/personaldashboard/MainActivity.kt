package chawza.personal.personaldashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import chawza.personal.personaldashboard.core.USER_TOKEN_KEY
import chawza.personal.personaldashboard.core.userStore
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import chawza.personal.personaldashboard.view.LoginActivity
import chawza.personal.personaldashboard.model.TodoListVIewModel
import chawza.personal.personaldashboard.repository.Todo
import chawza.personal.personaldashboard.services.TodosService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Composable
fun AddButton(
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(),
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Add button")
    }
}

@Composable
@Preview
fun AddButtonPreview() {
    AddButton {
    }
}


@Composable
fun AccountMenu(show: Boolean = false, dismiss: () -> Unit, requestLogout: () -> Unit) {
    DropdownMenu(expanded = show, onDismissRequest = dismiss) {
        DropdownMenuItem(
            text = { Text(text = "Logout", color = Color.Black) },
            onClick = requestLogout,
            trailingIcon = { Icons.Filled.ExitToApp }
        )
    }
}


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
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = "Users",
                        modifier = Modifier.size(60.dp)
                    )
                }
                AccountMenu(
                    showMenu.value,
                    dismiss = { showMenu.value = false },
                    requestLogout = requestLogout
                )
            }
        }
    }
}

@Composable
fun TaskListItem(
    todo: Todo,
    onClick: () -> Unit,
    onCheckBoxClick: (Boolean) -> Unit,
    onDeleteRequest: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        leadingContent = {
            Checkbox(
                checked = todo.done != null,
                onCheckedChange = onCheckBoxClick
            )
        },
        headlineContent = { Text(text = todo.title) },
        trailingContent = {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete Todo",
                tint = Color.Red,
                modifier = Modifier.clickable { onDeleteRequest() }
            )
        }
    )
}

@Composable
@Preview
fun PreviewTaskListItem() {
    TaskListItem(
        todo = Todo(1, "ASDASD", "asdasd"),
        onClick = { /*TODO*/ },
        onCheckBoxClick = {}) {}
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userToken = runBlocking { this@MainActivity.userStore.data.first()[USER_TOKEN_KEY] }

        if (userToken == null) {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
            return
        }

        val todoService = TodosService(userToken)
        val viewModel = TodoListVIewModel(todoService)

        setContent {
            val snackBar = remember { SnackbarHostState() }
            val isLoading = remember { mutableStateOf(false) }
            val todos = viewModel.todos.collectAsState()

            fun handleDeleteTask(todo: Todo) {
                viewModel.viewModelScope.launch {
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

                            viewModel.syncTodos()
                        }
                }
            }

            LaunchedEffect(Unit) {
                viewModel.syncTodos()
            }

            val editTaskLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode == RESULT_OK && it.data != null && it.data!!.hasExtra("message")) {
                    viewModel.viewModelScope.launch {
                        // Does not work
                        launch {
                            snackBar.showSnackbar(it.data!!.getStringExtra("message")!!)
                        }
                    }
                }
                viewModel.syncTodos()
            }

            PersonalDashboardTheme {
                Scaffold(
                    floatingActionButton = {
                        AddButton(
                            onClick = {
                                editTaskLauncher.launch(Intent(this, EditTaskActivity::class.java))
                            }
                        )
                    },
                    topBar = {
                        TopBar(
                            isLoading = isLoading.value,
                            requestLogout = {
                                viewModel.viewModelScope.launch {
                                    this@MainActivity.userStore.edit { data ->
                                        data.remove(USER_TOKEN_KEY)
                                    }
                                    this@MainActivity.startActivity(
                                        Intent(this@MainActivity, LoginActivity::class.java)
                                    )
                                }
                            },
                            requestRefresh = {
                                viewModel.syncTodos()
                            }
                        )
                    },
                    snackbarHost = {
                        SnackbarHost(hostState = snackBar)
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxHeight()
                            .animateContentSize()
                    ) {
                        if (todos.value.isNotEmpty()) {
                            LazyColumn {
                                items(todos.value.size) { idx ->
                                    val todo = todos.value[idx]
                                    TaskListItem(
                                        todo,
                                        onClick = {
                                            val intent = Intent(
                                                this@MainActivity,
                                                EditTaskActivity::class.java
                                            )
                                            intent.putExtra("todo", Json.encodeToString(todo))
                                            editTaskLauncher.launch(intent)
                                        }, onDeleteRequest = {
                                            handleDeleteTask(todo)
                                        }, onCheckBoxClick = { checked ->

                                        }
                                    )
                                    Divider()
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "No Todos")
                            }
                        }
                    }
                }
            }
        }
    }
}
