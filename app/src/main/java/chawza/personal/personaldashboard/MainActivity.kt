package chawza.personal.personaldashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import chawza.personal.personaldashboard.model.TodoListVIewModel
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import chawza.personal.personaldashboard.view.TodoListPreview
import chawza.personal.personaldashboard.view.TodoListView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val todoListViewModel = TodoListVIewModel()
            PersonalDashboardTheme {
                TodoListView(todoListViewModel)
            }
        }
    }
}
