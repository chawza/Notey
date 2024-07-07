package chawza.personal.personaldashboard.repository

import chawza.personal.personaldashboard.core.API
import chawza.personal.personaldashboard.view.Todo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

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

    override suspend fun addTodo(todo: Todo): Todo {
        TODO("Not yet implemented")
    }

    override suspend fun deleteTodo(todo: Todo) {
        TODO("Not yet implemented")
    }
}
