package chawza.personal.personaldashboard.repository

import chawza.personal.personaldashboard.core.API
import chawza.personal.personaldashboard.view.Todo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class TodoService(private val userToken: String) {
    suspend fun fetchAll(): Response {
        val client = OkHttpClient()
        val url = API.basicUrl()
            .addPathSegments(API.TODO_LIST_VIEWSET)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $userToken")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
    }

    suspend fun addTodo(todo: Todo): Response{
        val client = OkHttpClient()
        val url = API.basicUrl()
            .addPathSegments(API.TODO_LIST_VIEWSET)
            .build()

        val json = Json { ignoreUnknownKeys = true }
        val requestBody = json.encodeToString(todo).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $userToken")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
    }

    suspend fun deleteTodo(todo: Todo): Response {
        val client = OkHttpClient()
        val url = API.basicUrl()
            .addPathSegments(API.TODO_LIST_VIEWSET)
            .addPathSegment(todo.id.toString())
            .addPathSegment("")  // for some reason should ends with `/`
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $userToken")
            .delete()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
    }
}