package chawza.personal.personaldashboard.core

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

val interceptor = HttpLoggingInterceptor()
    .setLevel(HttpLoggingInterceptor.Level.BODY)
    .apply { redactHeader("Authorization") }
val httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()