package com.ilusons.notioff.service

import com.google.common.truth.Truth.assertThat
import com.ilusons.notioff.domain.model.PolicySnapshot
import org.junit.Test

class PolicySnapshotCacheTest {
    @Test
    fun defaultIsEmpty() {
        val cache = PolicySnapshotCache()
        assertThat(cache.get()).isEqualTo(PolicySnapshot.EMPTY)
    }

    @Test
    fun setAndGet() {
        val cache = PolicySnapshotCache()
        val snap = PolicySnapshot(globalMute = true, activeProfileTitle = "Work")
        cache.set(snap)
        assertThat(cache.get().globalMute).isTrue()
        assertThat(cache.get().activeProfileTitle).isEqualTo("Work")
    }
}
