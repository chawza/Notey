package chawza.personal.personaldashboard.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chawza.personal.personaldashboard.model.TodoListVIewModel
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException


@Serializable
data class Todo(
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
fun TodoListView(
    viewModel: TodoListVIewModel,
    modifier: Modifier = Modifier,
) {
    val todos = viewModel.todos.collectAsState()
    val snackBar = remember { SnackbarHostState() }

    LaunchedEffect(true) {
        try {
            viewModel.fetchAll()
        } catch (e: IOException){
            snackBar.showSnackbar("Unable to fetch todos")
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = { AddButton(onCLick = {}) },
        topBar = {
            Text(text = "My Todos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBar)
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(), contentAlignment = Alignment.Center) {
            LazyColumn {
                items(todos.value) { todo ->
                    ListItem(
                        headlineContent = { Text(text = todo.title) },
                        overlineContent = { }
                    )
                    Divider()
                }
            }
            if (todos.value.isEmpty()) {
                Text(text = "No Todos") 
            }
        }
    }
}

@Preview
@Composable
fun TodoListPreview() {
    val viewModel = TodoListVIewModel()
    PersonalDashboardTheme {
        val todos = listOf(
            Todo("lmao", null, null, null),
            Todo("Task Two", null, null, null),
        )
        viewModel.setTodos(todos)
        TodoListView(modifier = Modifier.fillMaxSize(), viewModel=viewModel)
    }
}