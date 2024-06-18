package chawza.personal.personaldashboard.view

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.TimeZone

data class Todo(
    val title: String,
    val note: String?,
    val scheduleDate: Calendar?
) {
    val created = Calendar.getInstance(TimeZone.getDefault())
}


@Composable
fun AddButton(onCLick: () -> Unit) {
    IconButton(onClick = onCLick) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add new Todo")
    }
}


@Composable
fun TodoListView(
    modifier: Modifier = Modifier,
    todos: List<Todo> = listOf(),
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = { AddButton(onCLick = {}) },
        topBar = {
            Text(text = "My Todos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(todos) { todo ->
                ListItem(headlineContent = { Text(text = todo.title) })
                Divider()
            }
        }
    }
}

@Preview
@Composable
fun TodoListPreview() {
    val todos = listOf(
        Todo("lmao", null, null),
        Todo("Task Two", null, null),
    )
    TodoListView(modifier = Modifier.fillMaxSize(), todos = todos)
}