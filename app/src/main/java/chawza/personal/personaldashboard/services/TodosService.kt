package chawza.personal.personaldashboard.services

import android.util.Log
import chawza.personal.personaldashboard.core.API
import chawza.personal.personaldashboard.repository.NewTodo
import chawza.personal.personaldashboard.repository.Todo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class TodosService(private val token: String) {
    private val encoder = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json".toMediaType()
    private val client = OkHttpClient()

    suspend fun fetch(): Result<List<Todo>> = withContext(Dispatchers.IO) {
        val url = API.basicUrl()
            .addPathSegments(API.TODO_LIST_VIEWSET)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $token")
            .get()
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            return@withContext Result.failure(IOException("Failed to fetch todos"))
        }

        val todos = response.body!!.string().let { encoder.decodeFromString<List<Todo>>(it) }
        return@withContext Result.success(todos)
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun create(todo: NewTodo): Result<Todo> = withContext(Dispatchers.IO) {
        val url = API.basicUrl()
            .addPathSegments(API.TODO_LIST_VIEWSET)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(encoder.encodeToString(todo).toRequestBody(jsonMediaType))
            .addHeader("Authorization", "Token $token")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return@withContext Result.failure(Exception("Task not created"))
        }

        val newTodo = encoder.decodeFromStream<Todo>(response.body!!.byteStream())
        response.close()
        return@withContext Result.success(newTodo)
    }

    suspend fun update(todo: Todo): Result<Todo> = withContext(Dispatchers.IO) {
        val url = API.basicUrl()
            .addPathSegments(API.TODO_LIST_VIEWSET)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $token")
            .patch(encoder.encodeToString(todo).toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            return@withContext Result.failure(IOException("Failed to update task"))
        }

        return@withContext Result.success(
            encoder.decodeFromString(response.body!!.string())
        )
    }

    suspend fun delete(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val url = API.basicUrl()
            .addPathSegments(API.TODO_LIST_VIEWSET)
            .addPathSegments("${id}/")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $token")
            .delete()
            .build()

        val response = client.newCall(request).execute()

        if (response.code !in 200..299) {
            return@withContext Result.failure(IOException("Failed to delete task"))
        }

        return@withContext Result.success(Unit)
    }
}