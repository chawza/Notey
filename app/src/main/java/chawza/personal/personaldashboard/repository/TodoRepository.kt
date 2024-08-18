package chawza.personal.personaldashboard.repository

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class NewTodo(
    val title: String,
    val notes: String? = null,
    val done: LocalDateTime? = null
)

@Serializable
data class Todo(
    val id: Int,
    val title: String,
    val notes: String? = null,
    val created: LocalDateTime,
    val done: LocalDateTime? = null
//    @SerialName("target_date")
//    val targetDate: String? = null,
//    val created: String? = null,
)
