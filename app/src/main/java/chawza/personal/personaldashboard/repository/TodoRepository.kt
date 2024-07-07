package chawza.personal.personaldashboard.repository

import chawza.personal.personaldashboard.core.API
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

@Serializable
data class NewTodo(
    @SerialName("user_id")
    var userId: String,
    val title: String,
    val note: String? = null,
)

@Serializable
data class Todo(
    val id: String,
    val title: String,
    val note: String? = null,
//    @SerialName("target_date")
//    val targetDate: String? = null,
//    val created: String? = null,
)


interface TodoRepository {
    suspend fun fetchAll(): Result<List<Todo>>
    suspend fun deleteTodo(todo: Todo): Result<Unit>
    suspend fun addTodo(todo: NewTodo):  Result<Todo>
}
