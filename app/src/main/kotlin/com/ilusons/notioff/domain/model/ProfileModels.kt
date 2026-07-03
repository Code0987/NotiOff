package com.ilusons.notioff.domain.model

import kotlinx.serialization.Serializable

/**
 * On-disk / in-memory profile models (JSON via kotlinx.serialization in PR 3).
 */
@Serializable
data class ProfilesState(
    val schemaVersion: Int = ProfilesState.CURRENT_SCHEMA,
    val profiles: List<Profile> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA: Int = 1
    }
}

@Serializable
data class Profile(
    val title: String,
    val items: List<ProfileItem> = emptyList(),
)

@Serializable
data class ProfileItem(
    val packageName: String,
    val title: String,
)

/**
 * Combined mute policy inputs for the notification listener hot path.
 *
 * [activePackages] is the package set of the active profile only (never a union of all profiles).
 * Use [packagesForMutePolicy] when calling [com.ilusons.notioff.domain.MutePolicy.shouldMute].
 */
data class PolicySnapshot(
    val globalMute: Boolean = false,
    val activeProfileTitle: String? = null,
    /**
     * Packages of the active profile only.
     * Empty if the active profile exists but has no items.
     * Ignored for mute when [activeProfileTitle] is null — do not pass this set blindly.
     */
    val activePackages: Set<String> = emptySet(),
) {
    /**
     * Correct argument for [com.ilusons.notioff.domain.MutePolicy.shouldMute]'s
     * `activeProfilePackages`: null when no active profile, else the (possibly empty) set.
     */
    fun packagesForMutePolicy(): Set<String>? =
        activeProfileTitle?.let { activePackages }

    companion object {
        /** Safe default before first DataStore emission (global off, no active profile). */
        val EMPTY = PolicySnapshot()
    }
}

data class MuteDecision(
    val shouldCancel: Boolean,
    val reason: String? = null,
)
