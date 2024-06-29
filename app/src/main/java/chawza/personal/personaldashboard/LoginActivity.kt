package chawza.personal.personaldashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import chawza.personal.personaldashboard.core.API
import chawza.personal.personaldashboard.core.USER_TOKEN_KEY
import chawza.personal.personaldashboard.core.userStore
import chawza.personal.personaldashboard.ui.theme.PersonalDashboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class LoginActivity : ComponentActivity() {
    private var snackBar: SnackbarHostState = SnackbarHostState()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalDashboardTheme {
                val username = remember { mutableStateOf("Admin") }
                val password = remember { mutableStateOf("mountain") }

                val isLoggingIn = remember { mutableStateOf(false) }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackBar) }
                ) {
                    Box(
                        modifier = Modifier.padding(it).fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.width(400.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = username.value,
                                onValueChange = {value -> username.value = value},
                                label = { Text(text = "Username")}
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = password.value,
                                onValueChange = {value -> password.value = value},
                                label = { Text(text = "Password")},
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                            )
                            Button(
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                onClick = {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        isLoggingIn.value = true
                                        login(username.value, password.value)
                                        isLoggingIn.value = false
                                    }
                                },
                                shape = MaterialTheme.shapes.small,
                                enabled = !isLoggingIn.value
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Login")
                                    if (isLoggingIn.value) {
                                        CircularProgressIndicator(color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun login(username: String, password: String) = withContext(Dispatchers.Default) {
        val url = API.basicUrl()
            .addPathSegments(API.GET_TOKEN)
            .build()

        val requestBody = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token ${API.TOKEN}")
            .post(requestBody)
            .build()

        lateinit var response: Response
        try {
            withContext(Dispatchers.IO) {
                delay(2000)
                response = OkHttpClient().newCall(request).execute()
            }
        } catch (e: IOException) {
            // Login Error
            withContext(Dispatchers.Main) {
                snackBar.showSnackbar("Connection Error")
            }
            return@withContext
        }

        if (!response.isSuccessful) {
            // Login Error
            withContext(Dispatchers.Main) {
                val message = when(response.code) {
                    in 500 .. 599 -> "Server Error"
                    else -> "Invalid Credential"
                }
                snackBar.showSnackbar(message)
            }
            return@withContext
        }

        val resJson = JSONObject(response.body!!.string())

        if (!resJson.has("token")) {
            snackBar.showSnackbar("Something went wrong")
            return@withContext
        }

        val token = resJson.getString("token")
        this@LoginActivity.userStore.edit {
            it[USER_TOKEN_KEY] = token
        }
        goToHome()
    }

    private fun goToHome() {
        this@LoginActivity.startActivity(Intent(this@LoginActivity, MainActivity::class.java))
    }
}
