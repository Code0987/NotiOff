package com.ilusons.notioff.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val DATASTORE_NAME = "notioff_prefs"

val Context.notiOffDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)
