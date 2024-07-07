package chawza.personal.personaldashboard.repository

import chawza.personal.personaldashboard.core.API
import chawza.personal.personaldashboard.view.NewTodo
import chawza.personal.personaldashboard.view.Todo
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
import org.json.JSONObject
import java.io.IOException

class TodoPocketBaseRepository(private val token: String): TodoRepository {
    private val jsonEncoder = Json { ignoreUnknownKeys = true }

    override suspend fun fetchAll(): Result<List<Todo>> = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val url = API.basicUrl()
            .addPathSegments(API.TODO_ENDPOINT)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", token)
            .get()
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            if (response.code in 400..499) {
                val responseJson = JSONObject(response.body!!.string())
                return@withContext Result.failure(Exception(responseJson.getString("message")))
            }
            return@withContext Result.failure(Exception(response.message))
        }

        val responseJson = JSONObject(response.body!!.string())
        response.close()

        return@withContext withContext(Dispatchers.Default) {
            val recordList = responseJson.getJSONArray("items")
            val todos: MutableList<Todo> = mutableListOf()

            for (idx in 0..< recordList.length()) {
                todos.add(jsonEncoder.decodeFromString<Todo>(recordList.getJSONObject(idx).toString()))
            }

            Result.success(todos)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun addTodo(todo: NewTodo): Result<Todo> = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val url = API.basicUrl()
            .addPathSegments(API.TODO_ENDPOINT)
            .build()

        val requestBody = jsonEncoder
            .encodeToString(todo)
            .toRequestBody(
                "application/json".toMediaType()
            )

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", token)
            .post(requestBody)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            return@withContext Result.failure(Exception("Unable to connect to server"))
        }

        if (!response.isSuccessful) {
            if (response.code in 400..499) {
                val responseJson = JSONObject(response.body!!.string())
                return@withContext Result.failure(Exception(responseJson.getString("message")))
            }
            return@withContext Result.failure(Exception(response.message))
        }

        val stream = response.body!!.byteStream()
        val createdTodo = jsonEncoder.decodeFromStream<Todo>(stream)

        response.close()
        Result.success(createdTodo)
    }

    override suspend fun deleteTodo(todo: Todo): Result<Unit> = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val url = API.basicUrl()
            .addPathSegments(API.TODO_ENDPOINT)
            .addPathSegment(todo.id)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", token)
            .delete()
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            return@withContext Result.failure(Exception("Unable to connect to server"))
        }

        if (!response.isSuccessful) {
            if (response.code in 400..499) {
                val responseJson = JSONObject(response.body!!.string())
                return@withContext Result.failure(Exception(responseJson.getString("message")))
            }
            return@withContext Result.failure(Exception(response.message))
        }

        Result.success(Unit)
    }
}
