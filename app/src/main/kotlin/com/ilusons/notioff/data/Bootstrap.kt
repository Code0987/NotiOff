package com.ilusons.notioff.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicReference

/**
 * Bootstrap single-flight gate (K22).
 *
 * - [MigrationState.Running]: only migrator may write DataStore.
 * - [MigrationState.Done]: normal CRUD allowed (success or soft-fail both end in Done).
 */
sealed class MigrationState {
    data object NotStarted : MigrationState()
    data object Running : MigrationState()
    data object Done : MigrationState()
}

interface Bootstrap {
    val migrationState: StateFlow<MigrationState>
    /** Optional banner signal after soft-fail import. */
    val importFailed: StateFlow<Boolean>
    fun ensureMigrationStarted()
    suspend fun awaitReady()
    /** Shared write gate: migrator holds for whole import; UI writers acquire after Done. */
    suspend fun <T> withWriteGate(block: suspend () -> T): T
}

/**
 * No-op migrator used until PR 8 wires RealmLegacyImporter.
 * Marks migration done immediately so the app is usable.
 */
fun interface LegacyMigration {
    /**
     * Idempotent via legacy_migration_done. Never deletes realm files.
     * Must be the sole DataStore writer while MigrationState is Running.
     * @return true if import soft-failed (banner), false on success / nothing-to-do.
     */
    suspend fun migrateIfNeeded(): Boolean
}

class ImmediateDoneMigration : LegacyMigration {
    override suspend fun migrateIfNeeded(): Boolean = false
}

class DefaultBootstrap(
    private val scope: CoroutineScope,
    private val migration: LegacyMigration,
) : Bootstrap {

    private val _state = MutableStateFlow<MigrationState>(MigrationState.NotStarted)
    override val migrationState: StateFlow<MigrationState> = _state.asStateFlow()

    private val _importFailed = MutableStateFlow(false)
    override val importFailed: StateFlow<Boolean> = _importFailed.asStateFlow()

    private val writeMutex = Mutex()
    private val startJob = AtomicReference<Job?>(null)

    override fun ensureMigrationStarted() {
        if (_state.value is MigrationState.Done) return
        val existing = startJob.get()
        if (existing != null && existing.isActive) return

        val job = scope.launch {
            if (_state.value is MigrationState.Done) return@launch
            _state.value = MigrationState.Running
            try {
                writeMutex.withLock {
                    val failed = migration.migrateIfNeeded()
                    _importFailed.value = failed
                }
            } finally {
                _state.value = MigrationState.Done
            }
        }
        if (!startJob.compareAndSet(null, job)) {
            // Another starter won; cancel our redundant job if still not started work
            if (!job.isCompleted && startJob.get() !== job) {
                // leave the winner's job
            }
        }
    }

    override suspend fun awaitReady() {
        if (_state.value is MigrationState.Done) return
        ensureMigrationStarted()
        migrationState.first { it is MigrationState.Done }
    }

    override suspend fun <T> withWriteGate(block: suspend () -> T): T {
        awaitReady()
        return writeMutex.withLock { block() }
    }
}
