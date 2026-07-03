package com.ilusons.notioff.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ilusons.notioff.data.Bootstrap
import com.ilusons.notioff.data.DuplicateProfileTitleException
import com.ilusons.notioff.data.InvalidProfileTitleException
import com.ilusons.notioff.data.MigrationState
import com.ilusons.notioff.data.ProfileRepository
import com.ilusons.notioff.data.SettingsRepository
import com.ilusons.notioff.domain.model.Profile
import com.ilusons.notioff.domain.model.ProfileItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val isBootstrapReady: Boolean = false,
    val importFailed: Boolean = false,
    val profiles: List<Profile> = emptyList(),
    val activeProfileTitle: String? = null,
    val globalMute: Boolean = false,
    val showCreateDialog: Boolean = false,
    val createTitleDraft: String = "",
    val createError: String? = null,
    val isCreateSubmitting: Boolean = false,
    val profilePendingDelete: String? = null,
)

sealed interface ProfilesEvent {
    data class Message(val text: String) : ProfilesEvent
    data object OpenHelp : ProfilesEvent
    data object OpenContact : ProfilesEvent
    data object ExitApp : ProfilesEvent
    data class OpenPackagePicker(val profileTitle: String) : ProfilesEvent
}

class ProfilesViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val bootstrap: Bootstrap,
) : ViewModel() {

    private data class DialogState(
        val showCreateDialog: Boolean = false,
        val createTitleDraft: String = "",
        val createError: String? = null,
        val isCreateSubmitting: Boolean = false,
        val profilePendingDelete: String? = null,
        val dismissImportBanner: Boolean = false,
    )

    private val _dialogState = MutableStateFlow(DialogState())

    private data class CoreState(
        val isBootstrapReady: Boolean,
        val importFailed: Boolean,
        val profiles: List<Profile>,
        val activeProfileTitle: String?,
        val globalMute: Boolean,
    )

    private val coreState = combine(
        bootstrap.migrationState,
        bootstrap.importFailed,
        profileRepository.profiles,
        settingsRepository.activeProfileTitle,
        settingsRepository.globalMute,
    ) { migration, importFailed, profiles, active, global ->
        CoreState(
            isBootstrapReady = migration is MigrationState.Done,
            importFailed = importFailed,
            profiles = profiles,
            activeProfileTitle = active,
            globalMute = global,
        )
    }

    val uiState: StateFlow<ProfilesUiState> = combine(coreState, _dialogState) { core, dialog ->
        ProfilesUiState(
            isBootstrapReady = core.isBootstrapReady,
            importFailed = core.importFailed && !dialog.dismissImportBanner,
            profiles = core.profiles,
            activeProfileTitle = core.activeProfileTitle,
            globalMute = core.globalMute,
            showCreateDialog = dialog.showCreateDialog,
            createTitleDraft = dialog.createTitleDraft,
            createError = dialog.createError,
            isCreateSubmitting = dialog.isCreateSubmitting,
            profilePendingDelete = dialog.profilePendingDelete,
        )
    }.stateIn(
        scope = viewModelScope,
        // Eagerly so uiState.value is accurate without an active UI collector (tests + early actions).
        started = SharingStarted.Eagerly,
        initialValue = ProfilesUiState(),
    )

    private val _events = MutableSharedFlow<ProfilesEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ProfilesEvent> = _events.asSharedFlow()

    fun openCreateDialog() {
        if (!uiState.value.isBootstrapReady) return
        _dialogState.update {
            it.copy(
                showCreateDialog = true,
                createTitleDraft = "",
                createError = null,
                isCreateSubmitting = false,
            )
        }
    }

    fun dismissCreateDialog() {
        _dialogState.update {
            it.copy(showCreateDialog = false, createError = null, isCreateSubmitting = false)
        }
    }

    fun onCreateTitleChange(value: String) {
        _dialogState.update { it.copy(createTitleDraft = value, createError = null) }
    }

    fun submitCreate() {
        if (!uiState.value.isBootstrapReady || _dialogState.value.isCreateSubmitting) return
        // Read draft from source of truth — uiState may lag one combine emission behind.
        val draft = _dialogState.value.createTitleDraft
        viewModelScope.launch {
            _dialogState.update { it.copy(isCreateSubmitting = true, createError = null) }
            try {
                profileRepository.create(draft)
                _dialogState.update {
                    it.copy(
                        showCreateDialog = false,
                        createTitleDraft = "",
                        createError = null,
                        isCreateSubmitting = false,
                    )
                }
                _events.emit(ProfilesEvent.Message("Profile created"))
            } catch (e: InvalidProfileTitleException) {
                _dialogState.update {
                    it.copy(isCreateSubmitting = false, createError = e.message ?: "Invalid name")
                }
            } catch (e: DuplicateProfileTitleException) {
                _dialogState.update {
                    it.copy(isCreateSubmitting = false, createError = e.message ?: "Already exists")
                }
            } catch (e: Exception) {
                _dialogState.update {
                    it.copy(isCreateSubmitting = false, createError = e.message ?: "Could not create")
                }
            }
        }
    }

    fun requestDelete(title: String) {
        if (!uiState.value.isBootstrapReady) return
        _dialogState.update { it.copy(profilePendingDelete = title) }
    }

    fun dismissDelete() {
        _dialogState.update { it.copy(profilePendingDelete = null) }
    }

    fun confirmDelete() {
        val title = _dialogState.value.profilePendingDelete ?: return
        viewModelScope.launch {
            try {
                profileRepository.delete(title)
                _dialogState.update { it.copy(profilePendingDelete = null) }
                _events.emit(ProfilesEvent.Message("Deleted “$title”"))
            } catch (e: Exception) {
                _dialogState.update { it.copy(profilePendingDelete = null) }
                _events.emit(ProfilesEvent.Message(e.message ?: "Delete failed"))
            }
        }
    }

    fun setActive(title: String?) {
        if (!uiState.value.isBootstrapReady) return
        viewModelScope.launch {
            try {
                val current = uiState.value.activeProfileTitle
                val next = if (title != null && title == current) null else title
                settingsRepository.setActiveProfileTitle(next)
            } catch (e: Exception) {
                _events.emit(ProfilesEvent.Message(e.message ?: "Could not update active profile"))
            }
        }
    }

    fun setGlobalMute(enabled: Boolean) {
        if (!uiState.value.isBootstrapReady) return
        viewModelScope.launch {
            try {
                settingsRepository.setGlobalMute(enabled)
            } catch (e: Exception) {
                _events.emit(ProfilesEvent.Message(e.message ?: "Could not update global mute"))
            }
        }
    }

    fun dismissImportBanner() {
        _dialogState.update { it.copy(dismissImportBanner = true) }
    }

    fun openPackagePicker(title: String) {
        viewModelScope.launch {
            _events.emit(ProfilesEvent.OpenPackagePicker(title))
        }
    }

    fun saveProfileItems(title: String, items: List<ProfileItem>) {
        if (!uiState.value.isBootstrapReady) return
        viewModelScope.launch {
            try {
                profileRepository.setItems(title, items)
                _events.emit(ProfilesEvent.Message("Updated apps for “$title”"))
            } catch (e: Exception) {
                _events.emit(ProfilesEvent.Message(e.message ?: "Could not save apps"))
            }
        }
    }

    fun openHelp() {
        viewModelScope.launch { _events.emit(ProfilesEvent.OpenHelp) }
    }

    fun openContact() {
        viewModelScope.launch { _events.emit(ProfilesEvent.OpenContact) }
    }

    fun exitApp() {
        viewModelScope.launch { _events.emit(ProfilesEvent.ExitApp) }
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val settingsRepository: SettingsRepository,
        private val bootstrap: Bootstrap,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProfilesViewModel::class.java))
            return ProfilesViewModel(profileRepository, settingsRepository, bootstrap) as T
        }
    }
}
