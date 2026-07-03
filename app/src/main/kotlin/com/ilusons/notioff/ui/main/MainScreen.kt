package com.ilusons.notioff.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilusons.notioff.R
import com.ilusons.notioff.domain.model.Profile
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.ilusons.notioff.util.NotificationAccess
import com.ilusons.notioff.ui.permission.PermissionBanners
import com.ilusons.notioff.ui.packages.PackagePickerSheet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ProfilesViewModel,
    onOpenContact: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }
    var packagePickerProfile by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ProfilesEvent.Message -> snackbarHostState.showSnackbar(event.text)
                ProfilesEvent.OpenHelp -> showHelp = true
                ProfilesEvent.OpenContact -> onOpenContact()
                ProfilesEvent.ExitApp -> onExitApp()
                is ProfilesEvent.OpenPackagePicker -> packagePickerProfile = event.profileTitle
            }
        }
    }

    if (!state.isBootstrapReady) {
        BootstrapLoadingScreen(modifier = modifier)
        return
    }

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(24.dp),
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_create_profile)) },
                    selected = false,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.openCreateDialog()
                    },
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_help)) },
                    selected = false,
                    icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.openHelp()
                    },
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_contact)) },
                    selected = false,
                    icon = { Icon(Icons.Default.Email, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.openContact()
                    },
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_exit)) },
                    selected = false,
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.exitApp()
                    },
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("main_scaffold"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.cd_open_drawer))
                        }
                    },
                    actions = {
                        GlobalMuteAction(
                            enabled = state.globalMute,
                            onToggle = viewModel::setGlobalMute,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = viewModel::openCreateDialog,
                    modifier = Modifier.testTag("fab_create"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_create_profile))
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (state.importFailed) {
                    ImportFailedBanner(onDismiss = viewModel::dismissImportBanner)
                }
                PermissionBanners(
                    onOpenListenerSettings = {
                        context.startActivity(NotificationAccess.createListenerSettingsIntent())
                    },
                    onTroubleshootRebind = {
                        NotificationAccess.rebindListenerComponent(context)
                        Toast.makeText(
                            context,
                            context.getString(R.string.perm_rebind_done),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
                if (state.profiles.isEmpty()) {
                    EmptyProfiles(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("profile_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.profiles, key = { it.title }) { profile ->
                            ProfileCard(
                                profile = profile,
                                isActive = profile.title == state.activeProfileTitle,
                                onToggleActive = { viewModel.setActive(profile.title) },
                                onEditApps = { viewModel.openPackagePicker(profile.title) },
                                onDelete = { viewModel.requestDelete(profile.title) },
                            )
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }

    if (state.showCreateDialog) {
        CreateProfileDialog(
            titleDraft = state.createTitleDraft,
            error = state.createError,
            submitting = state.isCreateSubmitting,
            onTitleChange = viewModel::onCreateTitleChange,
            onDismiss = viewModel::dismissCreateDialog,
            onConfirm = viewModel::submitCreate,
        )
    }

    state.profilePendingDelete?.let { title ->
        DeleteProfileDialog(
            title = title,
            onDismiss = viewModel::dismissDelete,
            onConfirm = viewModel::confirmDelete,
        )
    }

    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }

    packagePickerProfile?.let { title ->
        val profile = state.profiles.find { it.title == title }
        PackagePickerSheet(
            profileTitle = title,
            initiallySelected = profile?.items?.map { it.packageName }?.toSet().orEmpty(),
            onDismiss = { packagePickerProfile = null },
            onSave = { items ->
                viewModel.saveProfileItems(title, items)
                packagePickerProfile = null
            },
        )
    }
}

@Composable
private fun BootstrapLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("bootstrap_loading"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.bootstrap_loading),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun GlobalMuteAction(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp),
    ) {
        Icon(
            imageVector = if (enabled) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.testTag("global_mute_switch"),
        )
    }
}

@Composable
private fun ImportFailedBanner(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.import_failed_banner),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_dismiss),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun EmptyProfiles(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.empty_profiles),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    isActive: Boolean,
    onToggleActive: () -> Unit,
    onEditApps: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_card_${profile.title}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            R.string.profile_app_count,
                            profile.items.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.testTag("profile_active_${profile.title}"),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEditApps) {
                    Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.action_edit_apps))
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateProfileDialog(
    titleDraft: String,
    error: String?,
    submitting: Boolean,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text(stringResource(R.string.create_profile_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = titleDraft,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.create_profile_hint)) },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    enabled = !submitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_profile_field"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !submitting,
                modifier = Modifier.testTag("create_profile_confirm"),
            ) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun DeleteProfileDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_profile_title)) },
        text = { Text(stringResource(R.string.delete_profile_message, title)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.help_title)) },
        text = { Text(stringResource(R.string.help_body)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
    )
}
