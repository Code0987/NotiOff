package com.ilusons.notioff.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ilusons.notioff.domain.model.PolicySnapshot
import com.ilusons.notioff.domain.model.ProfilesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val activeProfileTitle: Flow<String?>
    val globalMute: Flow<Boolean>
    /** Combined snapshot with correct null-vs-empty active package mapping. */
    val policySnapshot: Flow<PolicySnapshot>

    suspend fun setActiveProfileTitle(title: String?)
    suspend fun setGlobalMute(enabled: Boolean)

    /** Bootstrap-only: write settings without awaiting gate (caller holds lock). */
    suspend fun setSettingsInternal(activeProfileTitle: String?, globalMute: Boolean)
    suspend fun markLegacyMigrationDoneInternal()
}

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val bootstrap: Bootstrap,
) : SettingsRepository {

    override val activeProfileTitle: Flow<String?> = dataStore.data.map { prefs ->
        prefs[NotiOffPreferences.ACTIVE_PROFILE]
    }

    override val globalMute: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NotiOffPreferences.TURN_OFF_GLOBALLY] ?: false
    }

    override val policySnapshot: Flow<PolicySnapshot> = dataStore.data
        .map { prefs -> mapSnapshot(prefs) }
        .distinctUntilChanged()

    private fun mapSnapshot(prefs: Preferences): PolicySnapshot {
        val global = prefs[NotiOffPreferences.TURN_OFF_GLOBALLY] ?: false
        val activeKey = prefs[NotiOffPreferences.ACTIVE_PROFILE]?.takeIf { it.isNotBlank() }
        val state = ProfilesJson.decode(prefs[NotiOffPreferences.PROFILES_JSON])
        return buildPolicySnapshot(global, activeKey, state)
    }

    override suspend fun setActiveProfileTitle(title: String?) {
        bootstrap.withWriteGate {
            dataStore.edit { prefs ->
                if (title.isNullOrBlank()) {
                    prefs.remove(NotiOffPreferences.ACTIVE_PROFILE)
                } else {
                    prefs[NotiOffPreferences.ACTIVE_PROFILE] = title
                }
            }
        }
    }

    override suspend fun setGlobalMute(enabled: Boolean) {
        bootstrap.withWriteGate {
            dataStore.edit { prefs ->
                prefs[NotiOffPreferences.TURN_OFF_GLOBALLY] = enabled
            }
        }
    }

    override suspend fun setSettingsInternal(activeProfileTitle: String?, globalMute: Boolean) {
        dataStore.edit { prefs ->
            if (activeProfileTitle.isNullOrBlank()) {
                prefs.remove(NotiOffPreferences.ACTIVE_PROFILE)
            } else {
                prefs[NotiOffPreferences.ACTIVE_PROFILE] = activeProfileTitle
            }
            prefs[NotiOffPreferences.TURN_OFF_GLOBALLY] = globalMute
        }
    }

    override suspend fun markLegacyMigrationDoneInternal() {
        dataStore.edit { prefs ->
            prefs[NotiOffPreferences.LEGACY_MIGRATION_DONE] = true
        }
    }

    companion object {
        /**
         * Pure mapping for unit tests.
         * Dangling active key (title not in profiles) → treat as no active profile.
         */
        fun buildPolicySnapshot(
            globalMute: Boolean,
            activeProfileTitle: String?,
            state: ProfilesState,
        ): PolicySnapshot {
            if (activeProfileTitle.isNullOrBlank()) {
                return PolicySnapshot(globalMute = globalMute)
            }
            val profile = state.profiles.find { it.title == activeProfileTitle }
                ?: return PolicySnapshot(globalMute = globalMute)
            return PolicySnapshot(
                globalMute = globalMute,
                activeProfileTitle = profile.title,
                activePackages = profile.items.map { it.packageName }.toSet(),
            )
        }
    }
}
