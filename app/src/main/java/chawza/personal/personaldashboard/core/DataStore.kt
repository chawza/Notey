package chawza.personal.personaldashboard.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore


val Context.userStore: DataStore<Preferences> by preferencesDataStore(name = "user-data")

val USER_TOKEN_KEY = stringPreferencesKey("token")
val USER_ID = stringPreferencesKey("collection_user_Id")
val USER_EMAIL = stringPreferencesKey("collection_user_email")
