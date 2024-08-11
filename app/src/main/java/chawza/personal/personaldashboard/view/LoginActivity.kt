package chawza.personal.personaldashboard.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chawza.personal.personaldashboard.MainActivity
import chawza.personal.personaldashboard.core.API
import chawza.personal.personaldashboard.core.USER_TOKEN_KEY
import chawza.personal.personaldashboard.core.userStore
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LoginViewModel: ViewModel() {
    var username = MutableStateFlow("")
    var password = MutableStateFlow("")
}

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PersonalDashboardTheme {
                val context = LocalContext.current
                val snackBar = remember { SnackbarHostState() }
                val viewModel = remember { LoginViewModel() }
                val isLoggingIn = remember { mutableStateOf(false) }

                val username = viewModel.username.collectAsState()
                val password = viewModel.password.collectAsState()

                LaunchedEffect(true) {
                    viewModel.viewModelScope.launch {
                        this@LoginActivity.userStore.data.collect { data ->
                            val token = data[USER_TOKEN_KEY]
                            if (token != null) {
                                this@LoginActivity.startActivity(
                                    Intent(
                                        context,
                                        MainActivity::class.java
                                    )
                                )
                            }
                        }
                    }
                }

                suspend fun login(username: String, password: String): Result<String> {
                    val url = API.basicUrl()
                        .addPathSegments(API.GET_TOKEN)
                        .build()

                    val requestBody = JSONObject()
                        .put("username", username)
                        .put("password", password)
                        .toString()
                        .toRequestBody(
                            "application/json".toMediaType()
                        )

                    return runCatching {
                        val response = withContext(Dispatchers.IO) {
                            val request: Request =
                                Request.Builder().url(url).post(requestBody).build()
                            val client = OkHttpClient()
                            client.newCall(request).execute()
                        }

                        if (!response.isSuccessful) {
                            val message = "login failed [${response.code}]"
                            return Result.failure(Exception(message))
                        }

                        val responseJson = JSONObject(response.body!!.string())
                        response.close()
                        return Result.success(responseJson.getString("token"))
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackBar) }
                ) {
                    Box(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxWidth(), contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .width(400.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Welcome",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "you may login",
                                style = MaterialTheme.typography.titleSmall
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(text = "Username") },
                                value = username.value,
                                singleLine = true,
                                onValueChange = { value -> viewModel.username.value = value },
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(text = "Password") },
                                value = password.value,
                                singleLine = true,
                                onValueChange = { value -> viewModel.password.value = value },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    autoCorrect = false
                                ),
                                visualTransformation = PasswordVisualTransformation()
                            )
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    isLoggingIn.value = true
                                    viewModel.viewModelScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            login(username.value, password.value)
                                        }

                                        result
                                            .onSuccess { token ->
                                                context.userStore.edit { data ->
                                                    data[USER_TOKEN_KEY] = token
                                                }
                                                context.startActivity(
                                                    Intent(
                                                        context,
                                                        MainActivity::class.java
                                                    )
                                                )
                                            }
                                            .onFailure { error ->
                                                snackBar.showSnackbar(
                                                    error.message ?: "Something went wrong"
                                                )
                                            }
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
        }
    }
}
