package chawza.personal.personaldashboard.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chawza.personal.personaldashboard.repository.NewTodo
import chawza.personal.personaldashboard.repository.Todo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun TodoFormView(
    modifier: Modifier = Modifier,
    initial: Todo?,
    onUpdateRequest: suspend (Todo) -> Unit,
    onAddRequest: suspend (NewTodo) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    val isLoading = remember { mutableStateOf(false) }

    suspend fun handleCreate() {
        onAddRequest(
            NewTodo("", title, note)
        )
    }

    suspend fun handleUpdate() {
        val new = initial!!.copy(title=title, note=note)
        onUpdateRequest(new)
    }

    Surface(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    CoroutineScope(Dispatchers.Main).launch {
                        isLoading.value = true
                        if (initial == null) {
                            handleCreate()
                        }
                        else {
                            handleUpdate()
                        }
                        isLoading.value = false
                    }
                },
                enabled = !isLoading.value,
                shape = MaterialTheme.shapes.small
            ) {
                Text(text = if (initial == null) "Create" else "Update")
                Spacer(modifier.width(20.dp))
            }
        }

    }

}