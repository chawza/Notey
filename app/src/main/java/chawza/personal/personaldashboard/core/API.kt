package chawza.personal.personaldashboard.core

import okhttp3.HttpUrl

object API {
//    val HOST = "127.0.0.1"
    val HOST = "192.168.1.18"
    val PORT = 8000
    val SCHEMA = "http"

    val GET_TOKEN = "api/auth/login"
    val TODO_LIST_VIEWSET = "api/todos/task/"

    // pocket base
    val AUTH_LOGIN = "api/auth/login"
    val TODO_ENDPOINT = "api/collections/todo/records"

    val TOKEN = "45dccb135d6583657922bfdf8bb06dda11133bc0"

    fun basicUrl(): HttpUrl.Builder {
        return HttpUrl.Builder()
            .host(HOST)
            .port(PORT)
            .scheme(SCHEMA)
    }
}