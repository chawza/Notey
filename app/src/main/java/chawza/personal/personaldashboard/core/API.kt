package chawza.personal.personaldashboard.core

import okhttp3.HttpUrl

object API {
    val HOST = "192.168.1.15"
    val PORT = 8000
    val SCHEMA = "http"

    val GET_TOKEN = "api/auth/get-token"
    val GET_TODO_LIST = "api/todos/"

    val TOKEN = "45dccb135d6583657922bfdf8bb06dda11133bc0"

    fun basicUrl(): HttpUrl.Builder {
        return HttpUrl.Builder()
            .host(HOST)
            .port(PORT)
            .scheme(SCHEMA)
    }
}