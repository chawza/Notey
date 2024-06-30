package chawza.personal.personaldashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import chawza.personal.personaldashboard.core.USER_TOKEN_KEY
import chawza.personal.personaldashboard.core.userStore
import chawza.personal.personaldashboard.model.TodoListVIewModel
import chawza.personal.personaldashboard.repository.TodoAPIRepository
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import chawza.personal.personaldashboard.view.TodoListView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userToken = runBlocking { this@MainActivity.userStore.data.first()[USER_TOKEN_KEY] }

        if (userToken == null) {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            return
        }

        setContent {
            val todoRepository = TodoAPIRepository(userToken)
            val todoListViewModel = TodoListVIewModel(todoRepository, SnackbarHostState())

            PersonalDashboardTheme {
                TodoListView(todoRepository = todoRepository, viewModel = todoListViewModel)
            }
        }
    }
}
