package chawza.personal.personaldashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import chawza.personal.personaldashboard.core.USER_TOKEN_KEY
import chawza.personal.personaldashboard.core.userStore
import chawza.personal.personaldashboard.model.TodoListVIewModel
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import chawza.personal.personaldashboard.view.TodoListView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LaunchedEffect(key1 = true) {
                this@MainActivity.userStore.data
                    .collect {
                        if (it[USER_TOKEN_KEY] == null) {
                            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        }
                    }
            }
            val todoListViewModel = TodoListVIewModel()
            PersonalDashboardTheme {
                TodoListView(todoListViewModel)
            }
        }
    }
}
