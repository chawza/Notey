package chawza.personal.personaldashboard.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chawza.personal.personaldashboard.core.API
import chawza.personal.personaldashboard.view.Todo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class TodoListVIewModel: ViewModel() {
    private val _todos = MutableStateFlow<List<Todo>>(listOf())

    val todos = _todos.asStateFlow()

    fun setTodos(todos: List<Todo>) {
        _todos.update { todos }
    }

    suspend fun fetchAll() {
        val client = OkHttpClient()
        val url = API.basicUrl()
            .addPathSegments(API.GET_TODO_LIST)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token ${API.TOKEN}")
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
        viewModelScope.launch {
            setTodos(todos ?: listOf())
        }

        response.close()
    }
    init {
        viewModelScope.run { CoroutineScope(Dispatchers.IO).launch{ fetchAll() } }
    }
}