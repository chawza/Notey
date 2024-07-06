package chawza.personal.personaldashboard.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.findViewTreeLifecycleOwner
import chawza.personal.personaldashboard.MainActivity
import chawza.personal.personaldashboard.core.API
import chawza.personal.personaldashboard.core.USER_TOKEN_KEY
import chawza.personal.personaldashboard.core.userStore
import chawza.personal.personaldashboard.view.ui.theme.PersonalDashboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

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
    val context = LocalContext.current
    val snackBar = remember { SnackbarHostState() }

    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val isLoggingIn = remember { mutableStateOf(false) }

    suspend fun login(email: String, password: String): Result<String> {
        val url = API.basicUrl()
            .addPathSegments(API.AUTH_LOGIN)
            .build()

        val requestBody = JSONObject()
            .put("identity", email)
            .put("password", password)
            .toString()
            .toRequestBody(
                "application/json".toMediaType()
            )

        return runCatching {
            val response = withContext(Dispatchers.IO) {
                val request: Request = Request.Builder().url(url).post(requestBody).build()
                val client = OkHttpClient()
                client.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val responseJSON = JSONObject(response.body!!.string())
                return Result.success(responseJSON.getString("token"))
            }
            else if (response.code in 400..499 && response.body != null) {
                val jsonResponse = JSONObject(response.body!!.string())
                return Result.failure(Exception(jsonResponse["message"].toString()))
            }
            return Result.failure(Exception(response.message))
        }
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
                            val result = login(email.value, password.value)
                            result.onSuccess { token ->
                                context.userStore.edit { data ->
                                    data[USER_TOKEN_KEY] = token
                                    context.startActivity(Intent(context, MainActivity::class.java))
                                }
                            }
                            .onFailure { error ->
                                snackBar.showSnackbar(error.message ?: "Something went wrong")
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

@Preview
@Composable
fun Preview() {
    LoginView()
}