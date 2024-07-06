package chawza.personal.personaldashboard.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.findViewTreeLifecycleOwner
import chawza.personal.personaldashboard.view.ui.theme.PersonalDashboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalDashboardTheme {
                LoginView()
            }
        }
    }
}


@Composable
fun LoginView() {
    val email = remember {
        mutableStateOf("")
    }
    val password = remember {
        mutableStateOf("")
    }

    val isLoggingIn = remember {
        mutableStateOf(false)
    }

    val snackBar = remember {
        SnackbarHostState()
    }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBar)}
    ) {
        Box(modifier = Modifier.padding(it).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .width(400.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Welcome", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
                Text(text = "you may login", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Email") },
                    value = email.value,
                    onValueChange = { value -> email.value = value }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Password") },
                    value = password.value,
                    onValueChange = { value -> password.value = value},
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false),
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        isLoggingIn.value = true
                        CoroutineScope(Dispatchers.Main).launch {
                            snackBar.showSnackbar("Logging in")
                            isLoggingIn.value = false
                        }
                    },
                    enabled = !isLoggingIn.value,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = "Login")
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview() {
    LoginView()
}