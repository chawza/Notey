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
import okhttp3.Response

fun Response.raiseStatus(message: String? = null) {
    if (this.code in 400..599) {
        throw Exception(message?.let { "Request Error [${this.code}]" })
    }
}

interface TodoRepository {
    suspend fun fetchAll(): Result<List<Todo>>
    suspend fun deleteTodo(todo: Todo): Result<Unit>

    suspend fun addTodo(todo: NewTodo):  Result<Todo>
}

class TodoAPIRepository(private val userToken: String): TodoRepository {
    private val jsonEncoder = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun fetchAll(): Result<List<Todo>> {
        val client = OkHttpClient()
        val url = API.basicUrl()
            .addPathSegments(API.TODO_LIST_VIEWSET)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $userToken")
            .get()
            .build()

        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
        response.raiseStatus("Error fetching todos")

        val todos: List<Todo> = withContext(Dispatchers.Default) {
            jsonEncoder.decodeFromStream(response.body!!.byteStream())
        }
        response.close()

        return Result.success(todos)

    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun addTodo(todo: NewTodo): Result<Todo>{
        val client = OkHttpClient()
        val url = API.basicUrl()
            .addPathSegments(API.TODO_ENDPOINT)
            .build()

        val json = Json { ignoreUnknownKeys = true }
        val requestBody = json.encodeToString(todo).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $userToken")
            .post(requestBody)
            .build()

        val response = withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute()
            } catch (e: Error) {
                throw Error("Failed to add task")
            }
        }

        if (!response.isSuccessful) {
            throw Error("Failed to add task")
        }

        val newTodo = withContext(Dispatchers.Default) {
            json.decodeFromStream<Todo>(response.body!!.byteStream())
        }
        response.close()

        return Result.success(newTodo)
    }

    override suspend fun deleteTodo(todo: Todo): Result<Unit> {
        val client = OkHttpClient()
        val url = API.basicUrl()
            .addPathSegments(API.TODO_ENDPOINT)
            .addPathSegment(todo.id.toString())
            .addPathSegment("")  // for some reason should ends with `/`
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $userToken")
            .delete()
            .build()

        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            response.raiseStatus("Unable To Delete \"${todo.title}\"")
        }
        response.close()

        return Result.success(Unit)
    }
}
