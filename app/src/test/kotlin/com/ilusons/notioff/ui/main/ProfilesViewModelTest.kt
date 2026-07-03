package com.ilusons.notioff.ui.main

import com.google.common.truth.Truth.assertThat
import com.ilusons.notioff.data.Bootstrap
import com.ilusons.notioff.data.DuplicateProfileTitleException
import com.ilusons.notioff.data.InvalidProfileTitleException
import com.ilusons.notioff.data.MigrationState
import com.ilusons.notioff.data.ProfileRepository
import com.ilusons.notioff.data.SettingsRepository
import com.ilusons.notioff.domain.model.PolicySnapshot
import com.ilusons.notioff.domain.model.Profile
import com.ilusons.notioff.domain.model.ProfileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun create_success_addsProfileAndClosesDialog() = runTest(dispatcher) {
        val repo = FakeProfileRepository()
        val settings = FakeSettingsRepository()
        val bootstrap = FakeBootstrap(ready = true)
        val vm = ProfilesViewModel(repo, settings, bootstrap)

        advanceUntilIdle()
        vm.openCreateDialog()
        advanceUntilIdle()
        assertThat(vm.uiState.value.showCreateDialog).isTrue()

        vm.onCreateTitleChange("Work")
        vm.submitCreate()
        advanceUntilIdle()

        assertThat(vm.uiState.value.showCreateDialog).isFalse()
        assertThat(vm.uiState.value.profiles.map { it.title }).contains("Work")
    }

    @Test
    fun create_duplicate_setsError() = runTest(dispatcher) {
        val repo = FakeProfileRepository(mutableListOf(Profile("Work")))
        val vm = ProfilesViewModel(repo, FakeSettingsRepository(), FakeBootstrap(true))
        advanceUntilIdle()

        vm.openCreateDialog()
        vm.onCreateTitleChange("Work")
        vm.submitCreate()
        advanceUntilIdle()

        assertThat(vm.uiState.value.showCreateDialog).isTrue()
        assertThat(vm.uiState.value.createError).isNotNull()
    }

    @Test
    fun create_blank_setsError() = runTest(dispatcher) {
        val vm = ProfilesViewModel(
            FakeProfileRepository(),
            FakeSettingsRepository(),
            FakeBootstrap(true),
        )
        advanceUntilIdle()
        vm.openCreateDialog()
        vm.onCreateTitleChange("   ")
        vm.submitCreate()
        advanceUntilIdle()
        assertThat(vm.uiState.value.createError).isNotNull()
    }

    @Test
    fun setActive_togglesOffWhenSame() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(active = "Work")
        val vm = ProfilesViewModel(
            FakeProfileRepository(mutableListOf(Profile("Work"))),
            settings,
            FakeBootstrap(true),
        )
        advanceUntilIdle()
        vm.setActive("Work")
        advanceUntilIdle()
        assertThat(settings.activeProfileTitle.first()).isNull()
    }

    @Test
    fun openCreate_blockedWhenBootstrapNotReady() = runTest(dispatcher) {
        val vm = ProfilesViewModel(
            FakeProfileRepository(),
            FakeSettingsRepository(),
            FakeBootstrap(ready = false),
        )
        advanceUntilIdle()
        vm.openCreateDialog()
        advanceUntilIdle()
        assertThat(vm.uiState.value.showCreateDialog).isFalse()
    }

    private class FakeBootstrap(ready: Boolean) : Bootstrap {
        private val _state = MutableStateFlow(
            if (ready) MigrationState.Done else MigrationState.Running,
        )
        override val migrationState: StateFlow<MigrationState> = _state.asStateFlow()
        private val _importFailed = MutableStateFlow(false)
        override val importFailed: StateFlow<Boolean> = _importFailed.asStateFlow()
        override fun ensureMigrationStarted() = Unit
        override suspend fun awaitReady() = Unit
        override suspend fun <T> withWriteGate(block: suspend () -> T): T = block()
    }

    private class FakeProfileRepository(
        initial: MutableList<Profile> = mutableListOf(),
    ) : ProfileRepository {
        private val _profiles = MutableStateFlow(initial.toList())
        override val profiles: Flow<List<Profile>> = _profiles

        override suspend fun create(title: String): Profile {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) throw InvalidProfileTitleException("Name required")
            if (_profiles.value.any { it.title == trimmed }) {
                throw DuplicateProfileTitleException("Profile already exists")
            }
            val profile = Profile(trimmed)
            _profiles.value = _profiles.value + profile
            return profile
        }

        override suspend fun delete(title: String) {
            _profiles.value = _profiles.value.filterNot { it.title == title }
        }

        override suspend fun setItems(title: String, items: List<ProfileItem>) {
            _profiles.value = _profiles.value.map {
                if (it.title == title) it.copy(items = items) else it
            }
        }

        override suspend fun replaceAllInternal(profiles: List<Profile>) {
            _profiles.value = profiles
        }
    }

    private class FakeSettingsRepository(
        active: String? = null,
        global: Boolean = false,
    ) : SettingsRepository {
        private val _active = MutableStateFlow(active)
        private val _global = MutableStateFlow(global)
        override val activeProfileTitle: Flow<String?> = _active
        override val globalMute: Flow<Boolean> = _global
        override val policySnapshot: Flow<PolicySnapshot> =
            combineFlows(_active, _global) { a, g ->
                PolicySnapshot(globalMute = g, activeProfileTitle = a)
            }

        override suspend fun setActiveProfileTitle(title: String?) {
            _active.value = title
        }

        override suspend fun setGlobalMute(enabled: Boolean) {
            _global.value = enabled
        }

        override suspend fun setSettingsInternal(activeProfileTitle: String?, globalMute: Boolean) {
            _active.value = activeProfileTitle
            _global.value = globalMute
        }

        override suspend fun markLegacyMigrationDoneInternal() = Unit
    }
}

private fun <A, B, R> combineFlows(
    a: Flow<A>,
    b: Flow<B>,
    transform: (A, B) -> R,
): Flow<R> = kotlinx.coroutines.flow.combine(a, b, transform)
