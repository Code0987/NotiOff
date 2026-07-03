package com.ilusons.notioff.data

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class BootstrapTest {

    @Test
    fun ensureMigrationStarted_isSingleFlight() = runTest {
        val starts = AtomicInteger(0)
        val migration = LegacyMigration {
            starts.incrementAndGet()
            delay(50)
            false
        }
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val bootstrap = DefaultBootstrap(scope, migration)

        bootstrap.ensureMigrationStarted()
        bootstrap.ensureMigrationStarted()
        bootstrap.ensureMigrationStarted()
        scope.advanceUntilIdle()

        assertThat(bootstrap.migrationState.value).isEqualTo(MigrationState.Done)
        assertThat(starts.get()).isEqualTo(1)
    }

    @Test
    fun awaitReady_completesAfterDone() = runTest {
        val migration = LegacyMigration {
            delay(20)
            false
        }
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val bootstrap = DefaultBootstrap(scope, migration)

        val ready = async(dispatcher) { bootstrap.awaitReady() }
        scope.advanceUntilIdle()
        ready.await()
        assertThat(bootstrap.migrationState.value).isEqualTo(MigrationState.Done)
    }

    @Test
    fun withWriteGate_blocksUntilMigrationDone_thenRunsOnce() = runTest {
        val order = mutableListOf<String>()
        val migration = LegacyMigration {
            order += "migrate"
            delay(100)
            false
        }
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val bootstrap = DefaultBootstrap(scope, migration)

        bootstrap.ensureMigrationStarted()
        val createJob = scope.launch {
            bootstrap.withWriteGate {
                order += "create"
            }
        }
        scope.advanceTimeBy(50)
        assertThat(order).containsExactly("migrate")
        assertThat(order).doesNotContain("create")

        scope.advanceUntilIdle()
        createJob.join()
        assertThat(order).containsExactly("migrate", "create").inOrder()
    }

    @Test
    fun softFail_setsImportFailed_andEndsDone() = runTest {
        val migration = LegacyMigration { true }
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val bootstrap = DefaultBootstrap(scope, migration)
        bootstrap.ensureMigrationStarted()
        scope.advanceUntilIdle()
        assertThat(bootstrap.migrationState.value).isEqualTo(MigrationState.Done)
        assertThat(bootstrap.importFailed.value).isTrue()
    }
}
