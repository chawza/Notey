package chawza.personal.personaldashboard.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chawza.personal.personaldashboard.core.API
import chawza.personal.personaldashboard.view.Todo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class TodoListVIewModel: ViewModel() {
    private val _todos = MutableStateFlow<List<Todo>>(listOf())

    val todos = _todos.asStateFlow()

    fun setTodos(todos: List<Todo>) {
        _todos.update { todos }
    }

    suspend fun fetchAll(token: String) {
        val client = OkHttpClient()
        val url = API.basicUrl()
            .addPathSegments(API.TODO_LIST_VIEWSET)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $token")
            .get()
            .build()

        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            println("Error request")
        }
        val json = Json { ignoreUnknownKeys = true }

        val todos = response.body?.let {
            json.decodeFromString<List<Todo>>(it.string())
        }
        response.close()

        viewModelScope.launch {
            setTodos(todos ?: listOf())
        }
    }

    suspend fun addTodo(todo: Todo, token: String): Boolean {
        val client = OkHttpClient()
        val url = API.basicUrl()
            .addPathSegments(API.TODO_LIST_VIEWSET)
            .build()

        val json = Json { ignoreUnknownKeys = true }
        val requestBody = json.encodeToString(todo).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $token")
            .post(requestBody)
            .build()

        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
        response.close()

        if (response.isSuccessful) {
            return true
        }
        // TODO: Handle form error
        when(response.code) {
            in 400..499 -> {
                // Form Error}
            }
            in 500 .. 599 -> {
                // Server Error
            }
        }
        return false
    }
}