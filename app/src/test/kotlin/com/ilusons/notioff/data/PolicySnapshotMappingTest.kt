package com.ilusons.notioff.data

import com.google.common.truth.Truth.assertThat
import com.ilusons.notioff.domain.model.Profile
import com.ilusons.notioff.domain.model.ProfileItem
import com.ilusons.notioff.domain.model.ProfilesState
import org.junit.Test

class PolicySnapshotMappingTest {

    private val work = Profile(
        title = "Work",
        items = listOf(
            ProfileItem("com.a", "A"),
            ProfileItem("com.b", "B"),
        ),
    )
    private val emptyProfile = Profile(title = "Empty", items = emptyList())
    private val state = ProfilesState(profiles = listOf(work, emptyProfile))

    @Test
    fun nullActive_packagesForMutePolicy_null() {
        val snap = DataStoreSettingsRepository.buildPolicySnapshot(false, null, state)
        assertThat(snap.activeProfileTitle).isNull()
        assertThat(snap.packagesForMutePolicy()).isNull()
    }

    @Test
    fun activeWithPackages_returnsSet() {
        val snap = DataStoreSettingsRepository.buildPolicySnapshot(true, "Work", state)
        assertThat(snap.globalMute).isTrue()
        assertThat(snap.activeProfileTitle).isEqualTo("Work")
        assertThat(snap.packagesForMutePolicy()).containsExactly("com.a", "com.b")
    }

    @Test
    fun activeEmptyProfile_returnsEmptySet_notNull() {
        val snap = DataStoreSettingsRepository.buildPolicySnapshot(false, "Empty", state)
        assertThat(snap.packagesForMutePolicy()).isNotNull()
        assertThat(snap.packagesForMutePolicy()).isEmpty()
    }

    @Test
    fun danglingActiveKey_treatedAsNoActive() {
        val snap = DataStoreSettingsRepository.buildPolicySnapshot(false, "Missing", state)
        assertThat(snap.activeProfileTitle).isNull()
        assertThat(snap.packagesForMutePolicy()).isNull()
    }

    @Test
    fun neverUnionsAllProfiles() {
        val snap = DataStoreSettingsRepository.buildPolicySnapshot(false, "Empty", state)
        assertThat(snap.activePackages).doesNotContain("com.a")
    }
}
