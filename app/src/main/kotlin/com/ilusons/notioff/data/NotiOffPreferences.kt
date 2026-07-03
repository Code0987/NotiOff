package com.ilusons.notioff.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object NotiOffPreferences {
    val PROFILES_JSON = stringPreferencesKey("profiles_json")
    val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
    val TURN_OFF_GLOBALLY = booleanPreferencesKey("turn_off_globally")
    val LEGACY_MIGRATION_DONE = booleanPreferencesKey("legacy_migration_done")
}
