package chawza.personal.personaldashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import chawza.personal.personaldashboard.core.USER_ID
import chawza.personal.personaldashboard.core.USER_TOKEN_KEY
import chawza.personal.personaldashboard.core.userStore
import chawza.personal.personaldashboard.repository.TodoPocketBaseRepository
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import chawza.personal.personaldashboard.view.LoginActivity
import chawza.personal.personaldashboard.view.TodoListView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userToken = runBlocking { this@MainActivity.userStore.data.first()[USER_TOKEN_KEY] }
        val userDbID = runBlocking { this@MainActivity.userStore.data.first()[USER_ID] }

        if (userToken == null || userDbID == null) {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }

        setContent {
            PersonalDashboardTheme {
                TodoListView(todoRepository = remember { TodoPocketBaseRepository(userToken!!) })
            }
        }
    }
}
