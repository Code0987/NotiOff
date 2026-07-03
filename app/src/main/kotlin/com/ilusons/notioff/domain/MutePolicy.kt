package com.ilusons.notioff.domain

import com.ilusons.notioff.domain.model.MuteDecision

/**
 * Pure mute decision logic shared by the notification listener and unit tests.
 * No Android dependencies.
 */
object MutePolicy {

    /**
     * @param activeProfilePackages null means no active profile is selected;
     *        an empty set means an active profile exists but lists no packages.
     */
    fun shouldMute(
        packageName: String,
        isOngoing: Boolean,
        isClearable: Boolean,
        selfPackageName: String,
        globalMute: Boolean,
        activeProfilePackages: Set<String>?,
    ): MuteDecision {
        if (packageName == selfPackageName) {
            return MuteDecision(shouldCancel = false, reason = "self")
        }
        if (isOngoing || !isClearable) {
            return MuteDecision(shouldCancel = false, reason = "ongoing_or_uncleared")
        }
        if (globalMute) {
            return MuteDecision(shouldCancel = true, reason = "global")
        }
        val packages = activeProfilePackages
            ?: return MuteDecision(shouldCancel = false, reason = "no_active_profile")
        // Exact match only (intentional change vs legacy equalsIgnoreCase)
        return if (packageName in packages) {
            MuteDecision(shouldCancel = true, reason = "profile")
        } else {
            MuteDecision(shouldCancel = false, reason = "not_in_profile")
        }
    }
}
