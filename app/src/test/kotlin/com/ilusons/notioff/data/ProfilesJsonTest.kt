package com.ilusons.notioff.data

import com.google.common.truth.Truth.assertThat
import com.ilusons.notioff.domain.model.Profile
import com.ilusons.notioff.domain.model.ProfileItem
import com.ilusons.notioff.domain.model.ProfilesState
import org.junit.Test

class ProfilesJsonTest {
    @Test
    fun roundTrip_preservesProfiles() {
        val state = ProfilesState(
            profiles = listOf(
                Profile(
                    title = "Work",
                    items = listOf(ProfileItem("com.slack", "Slack")),
                ),
                Profile(title = "Unicode 🔔", items = emptyList()),
            ),
        )
        val encoded = ProfilesJson.encode(state)
        val decoded = ProfilesJson.decode(encoded)
        assertThat(decoded.profiles).isEqualTo(state.profiles)
        assertThat(decoded.schemaVersion).isEqualTo(ProfilesState.CURRENT_SCHEMA)
    }

    @Test
    fun decode_nullOrBlank_returnsEmpty() {
        assertThat(ProfilesJson.decode(null).profiles).isEmpty()
        assertThat(ProfilesJson.decode("").profiles).isEmpty()
        assertThat(ProfilesJson.decode("   ").profiles).isEmpty()
    }

    @Test
    fun decode_invalidJson_returnsEmpty() {
        assertThat(ProfilesJson.decode("{not-json").profiles).isEmpty()
    }
}
