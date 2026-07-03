package com.ilusons.notioff.data

import com.ilusons.notioff.domain.model.ProfilesState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ProfilesJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: ProfilesState): String = json.encodeToString(state)

    fun decode(raw: String?): ProfilesState {
        if (raw.isNullOrBlank()) return ProfilesState()
        return runCatching { json.decodeFromString<ProfilesState>(raw) }
            .getOrElse { ProfilesState() }
    }
}
