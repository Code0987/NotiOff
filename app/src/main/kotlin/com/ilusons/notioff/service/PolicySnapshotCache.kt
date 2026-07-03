package com.ilusons.notioff.service

import com.ilusons.notioff.domain.model.PolicySnapshot
import java.util.concurrent.atomic.AtomicReference

/**
 * Lock-free hot-path cache for [NotiOffNotificationListenerService].
 * Updated from a coroutine collecting [com.ilusons.notioff.data.SettingsRepository.policySnapshot].
 */
class PolicySnapshotCache {
    private val ref = AtomicReference(PolicySnapshot.EMPTY)

    fun get(): PolicySnapshot = ref.get()

    fun set(snapshot: PolicySnapshot) {
        ref.set(snapshot)
    }
}
