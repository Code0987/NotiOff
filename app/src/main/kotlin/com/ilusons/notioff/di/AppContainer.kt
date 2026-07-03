package com.ilusons.notioff.di

import android.content.Context
import com.ilusons.notioff.data.Bootstrap
import com.ilusons.notioff.data.DataStoreProfileRepository
import com.ilusons.notioff.data.DataStoreSettingsRepository
import com.ilusons.notioff.data.DefaultBootstrap
import com.ilusons.notioff.data.ImmediateDoneMigration
import com.ilusons.notioff.data.LegacyMigration
import com.ilusons.notioff.data.ProfileRepository
import com.ilusons.notioff.data.SettingsRepository
import com.ilusons.notioff.data.notiOffDataStore
import kotlinx.coroutines.CoroutineScope

/**
 * Manual DI graph (K16).
 */
interface AppContainer {
    val applicationScope: CoroutineScope
    val bootstrap: Bootstrap
    val profileRepository: ProfileRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(
    appContext: Context,
    override val applicationScope: CoroutineScope,
    migration: LegacyMigration = ImmediateDoneMigration(),
) : AppContainer {

    private val dataStore = appContext.notiOffDataStore

    override val bootstrap: Bootstrap = DefaultBootstrap(applicationScope, migration)

    override val profileRepository: ProfileRepository =
        DataStoreProfileRepository(dataStore, bootstrap)

    override val settingsRepository: SettingsRepository =
        DataStoreSettingsRepository(dataStore, bootstrap)
}
