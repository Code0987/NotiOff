package com.ilusons.notioff.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ilusons.notioff.domain.model.Profile
import com.ilusons.notioff.domain.model.ProfileItem
import com.ilusons.notioff.domain.model.ProfilesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ProfileRepository {
    val profiles: Flow<List<Profile>>

    /**
     * Creates a profile.
     * - Trims [title]; blank after trim → [InvalidProfileTitleException]
     * - Duplicate title (exact match after trim) → [DuplicateProfileTitleException]
     * - Length > 64 → [InvalidProfileTitleException]
     * - Suspends until bootstrap Ready (K22)
     */
    suspend fun create(title: String): Profile

    /** Deletes profile; if it was active, clears active title in the same edit. */
    suspend fun delete(title: String)

    suspend fun setItems(title: String, items: List<ProfileItem>)

    /** Migration-only / bootstrap path; not for UI. Does not awaitReady — caller holds gate. */
    suspend fun replaceAllInternal(profiles: List<Profile>)
}

class DataStoreProfileRepository(
    private val dataStore: DataStore<Preferences>,
    private val bootstrap: Bootstrap,
) : ProfileRepository {

    override val profiles: Flow<List<Profile>> = dataStore.data.map { prefs ->
        ProfilesJson.decode(prefs[NotiOffPreferences.PROFILES_JSON]).profiles
    }

    override suspend fun create(title: String): Profile {
        val trimmed = validateTitle(title)
        return bootstrap.withWriteGate {
            var created: Profile? = null
            dataStore.edit { prefs ->
                val state = ProfilesJson.decode(prefs[NotiOffPreferences.PROFILES_JSON])
                if (state.profiles.any { it.title == trimmed }) {
                    throw DuplicateProfileTitleException("Profile already exists")
                }
                val profile = Profile(title = trimmed, items = emptyList())
                prefs[NotiOffPreferences.PROFILES_JSON] =
                    ProfilesJson.encode(state.copy(profiles = state.profiles + profile))
                created = profile
            }
            created!!
        }
    }

    override suspend fun delete(title: String) {
        bootstrap.withWriteGate {
            dataStore.edit { prefs ->
                val state = ProfilesJson.decode(prefs[NotiOffPreferences.PROFILES_JSON])
                val next = state.profiles.filterNot { it.title == title }
                prefs[NotiOffPreferences.PROFILES_JSON] =
                    ProfilesJson.encode(state.copy(profiles = next))
                if (prefs[NotiOffPreferences.ACTIVE_PROFILE] == title) {
                    prefs.remove(NotiOffPreferences.ACTIVE_PROFILE)
                }
            }
        }
    }

    override suspend fun setItems(title: String, items: List<ProfileItem>) {
        bootstrap.withWriteGate {
            dataStore.edit { prefs ->
                val state = ProfilesJson.decode(prefs[NotiOffPreferences.PROFILES_JSON])
                val next = state.profiles.map { profile ->
                    if (profile.title == title) profile.copy(items = items) else profile
                }
                prefs[NotiOffPreferences.PROFILES_JSON] =
                    ProfilesJson.encode(state.copy(profiles = next))
            }
        }
    }

    override suspend fun replaceAllInternal(profiles: List<Profile>) {
        dataStore.edit { prefs ->
            prefs[NotiOffPreferences.PROFILES_JSON] =
                ProfilesJson.encode(ProfilesState(profiles = profiles))
        }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 64

        /** @return trimmed title or throws */
        fun validateTitle(title: String): String {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) {
                throw InvalidProfileTitleException("Name required")
            }
            if (trimmed.length > MAX_TITLE_LENGTH) {
                throw InvalidProfileTitleException("Name too long")
            }
            return trimmed
        }
    }
}
