package com.ilusons.notioff

import android.app.Application
import com.ilusons.notioff.di.AppContainer
import com.ilusons.notioff.di.DefaultAppContainer
import com.ilusons.notioff.service.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application entry point. Real Realm migration lands in PR 8;
 * PR 3 wires ImmediateDoneMigration under the K22 write gate.
 */
class NotiOffApp : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        container = DefaultAppContainer(this, applicationScope)
        container.bootstrap.ensureMigrationStarted()
    }
}
