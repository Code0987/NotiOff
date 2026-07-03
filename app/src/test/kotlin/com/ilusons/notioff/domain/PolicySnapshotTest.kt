package com.ilusons.notioff.domain

import com.google.common.truth.Truth.assertThat
import com.ilusons.notioff.domain.model.PolicySnapshot
import org.junit.Test

class PolicySnapshotTest {

    @Test
    fun empty_packagesForMutePolicy_isNull() {
        assertThat(PolicySnapshot.EMPTY.packagesForMutePolicy()).isNull()
    }

    @Test
    fun noActiveTitle_packagesForMutePolicy_isNull_evenIfPackagesSet() {
        val snap = PolicySnapshot(
            globalMute = false,
            activeProfileTitle = null,
            activePackages = setOf("com.foo"),
        )
        assertThat(snap.packagesForMutePolicy()).isNull()
    }

    @Test
    fun activeTitle_withPackages_returnsSet() {
        val pkgs = setOf("com.a", "com.b")
        val snap = PolicySnapshot(
            globalMute = false,
            activeProfileTitle = "Work",
            activePackages = pkgs,
        )
        assertThat(snap.packagesForMutePolicy()).isEqualTo(pkgs)
    }

    @Test
    fun activeTitle_emptyPackages_returnsEmptySet_notNull() {
        val snap = PolicySnapshot(
            globalMute = false,
            activeProfileTitle = "Empty",
            activePackages = emptySet(),
        )
        val result = snap.packagesForMutePolicy()
        assertThat(result).isNotNull()
        assertThat(result).isEmpty()
    }

    @Test
    fun mutePolicy_viaSnapshot_nullActive_noCancel() {
        val snap = PolicySnapshot.EMPTY
        val d = MutePolicy.shouldMute(
            packageName = "com.example",
            isOngoing = false,
            isClearable = true,
            selfPackageName = "com.ilusons.notioff",
            globalMute = snap.globalMute,
            activeProfilePackages = snap.packagesForMutePolicy(),
        )
        assertThat(d.shouldCancel).isFalse()
        assertThat(d.reason).isEqualTo("no_active_profile")
    }

    @Test
    fun mutePolicy_viaSnapshot_activeEmpty_noCancel_notInProfile() {
        val snap = PolicySnapshot(
            activeProfileTitle = "Work",
            activePackages = emptySet(),
        )
        val d = MutePolicy.shouldMute(
            packageName = "com.example",
            isOngoing = false,
            isClearable = true,
            selfPackageName = "com.ilusons.notioff",
            globalMute = snap.globalMute,
            activeProfilePackages = snap.packagesForMutePolicy(),
        )
        assertThat(d.shouldCancel).isFalse()
        assertThat(d.reason).isEqualTo("not_in_profile")
    }
}
