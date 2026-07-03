package com.ilusons.notioff.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MutePolicyTest {

    private val self = "com.ilusons.notioff"
    private val other = "com.example.app"

    @Test
    fun globalOn_clearable_cancelsWithGlobalReason() {
        val d = MutePolicy.shouldMute(
            packageName = other,
            isOngoing = false,
            isClearable = true,
            selfPackageName = self,
            globalMute = true,
            activeProfilePackages = null,
        )
        assertThat(d.shouldCancel).isTrue()
        assertThat(d.reason).isEqualTo("global")
    }

    @Test
    fun globalOff_packageInActiveProfile_cancelsWithProfileReason() {
        val d = MutePolicy.shouldMute(
            packageName = other,
            isOngoing = false,
            isClearable = true,
            selfPackageName = self,
            globalMute = false,
            activeProfilePackages = setOf(other, "com.foo"),
        )
        assertThat(d.shouldCancel).isTrue()
        assertThat(d.reason).isEqualTo("profile")
    }

    @Test
    fun globalOff_activeProfileEmptyPackages_doesNotCancel() {
        val d = MutePolicy.shouldMute(
            packageName = other,
            isOngoing = false,
            isClearable = true,
            selfPackageName = self,
            globalMute = false,
            activeProfilePackages = emptySet(),
        )
        assertThat(d.shouldCancel).isFalse()
        assertThat(d.reason).isEqualTo("not_in_profile")
    }

    @Test
    fun globalOff_nullActiveProfile_doesNotCancel_noCrash() {
        val d = MutePolicy.shouldMute(
            packageName = other,
            isOngoing = false,
            isClearable = true,
            selfPackageName = self,
            globalMute = false,
            activeProfilePackages = null,
        )
        assertThat(d.shouldCancel).isFalse()
        assertThat(d.reason).isEqualTo("no_active_profile")
    }

    @Test
    fun ongoing_notCancelledEvenIfGlobal() {
        val d = MutePolicy.shouldMute(
            packageName = other,
            isOngoing = true,
            isClearable = true,
            selfPackageName = self,
            globalMute = true,
            activeProfilePackages = setOf(other),
        )
        assertThat(d.shouldCancel).isFalse()
        assertThat(d.reason).isEqualTo("ongoing_or_uncleared")
    }

    @Test
    fun nonClearable_notCancelled() {
        val d = MutePolicy.shouldMute(
            packageName = other,
            isOngoing = false,
            isClearable = false,
            selfPackageName = self,
            globalMute = true,
            activeProfilePackages = null,
        )
        assertThat(d.shouldCancel).isFalse()
        assertThat(d.reason).isEqualTo("ongoing_or_uncleared")
    }

    @Test
    fun selfPackage_notCancelledEvenIfGlobal() {
        val d = MutePolicy.shouldMute(
            packageName = self,
            isOngoing = false,
            isClearable = true,
            selfPackageName = self,
            globalMute = true,
            activeProfilePackages = setOf(self),
        )
        assertThat(d.shouldCancel).isFalse()
        assertThat(d.reason).isEqualTo("self")
    }

    @Test
    fun mixedCasePackage_exactMatchOnly_doesNotCancel() {
        val d = MutePolicy.shouldMute(
            packageName = "Com.Example.App",
            isOngoing = false,
            isClearable = true,
            selfPackageName = self,
            globalMute = false,
            activeProfilePackages = setOf("com.example.app"),
        )
        assertThat(d.shouldCancel).isFalse()
        assertThat(d.reason).isEqualTo("not_in_profile")
    }

    @Test
    fun packageNotInProfile_doesNotCancel() {
        val d = MutePolicy.shouldMute(
            packageName = other,
            isOngoing = false,
            isClearable = true,
            selfPackageName = self,
            globalMute = false,
            activeProfilePackages = setOf("com.other.app"),
        )
        assertThat(d.shouldCancel).isFalse()
        assertThat(d.reason).isEqualTo("not_in_profile")
    }
}
