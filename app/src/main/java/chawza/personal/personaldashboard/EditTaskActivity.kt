package chawza.personal.personaldashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chawza.personal.personaldashboard.core.USER_TOKEN_KEY
import chawza.personal.personaldashboard.core.userStore
import chawza.personal.personaldashboard.repository.NewTodo
import chawza.personal.personaldashboard.services.TodosService
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TodoFormViewModel: ViewModel() {
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
}
class EditTaskActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val model = remember { TodoFormViewModel() }
            var isLoading by remember { mutableStateOf(false) }
            val snackBar = remember { SnackbarHostState() }

            val title by model.title.collectAsState()
            val note by model.note.collectAsState()
            
            PersonalDashboardTheme {
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackBar)}
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
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "New Task", style = MaterialTheme.typography.titleMedium)
                                if (isLoading) {
                                    CircularProgressIndicator()
                                }
                            }
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = title,
                                singleLine = true,
                                onValueChange = { model.setTitle(it) },
                                label = { Text(text = "Title") }
                            )
                            OutlinedTextField(
                                modifier = Modifier
                                    .height(56.dp * 4)
                                    .fillMaxWidth(),
                                value = note,
                                onValueChange = { model.setNote(it) },
                                label = { Text(text = "Note") }
                            )
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                onClick = {
                                    model.viewModelScope.launch {
                                        isLoading = true

                                        this@EditTaskActivity.userStore.data.collect { data ->
                                            val token = data[USER_TOKEN_KEY]!!
                                            val service = TodosService(token)

                                            service.create(NewTodo(title, note))
                                                .onFailure { error ->
                                                    launch {
                                                        snackBar.showSnackbar(
                                                            error.message ?: "Failed to create new task",
                                                            duration  = SnackbarDuration.Short
                                                        )
                                                    }
                                                    isLoading = false
                                                }
                                                .onSuccess {
                                                    isLoading = false
                                                    this@EditTaskActivity.intent.putExtra("message", "Task created!")
                                                    this@EditTaskActivity.setResult(RESULT_OK)
                                                    this@EditTaskActivity.finish()
                                                }
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(text = "Create")
                            }
                        }
                    }
                }
            }
        }
    }
}