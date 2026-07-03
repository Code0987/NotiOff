package com.ilusons.notioff.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.ilusons.notioff.BuildConfig
import com.ilusons.notioff.MainActivity
import com.ilusons.notioff.NotiOffApp
import com.ilusons.notioff.R
import com.ilusons.notioff.domain.MutePolicy
import com.ilusons.notioff.domain.model.PolicySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Cancels clearable, non-ongoing notifications per global mute / active profile (K7, K14, K18).
 */
class NotiOffNotificationListenerService : NotificationListenerService() {

    private val cache = PolicySnapshotCache()
    private var serviceScope: CoroutineScope? = null
    private var collectJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "listener connected")
        startCollectingPolicy()
    }

    override fun onListenerDisconnected() {
        stopCollectingPolicy()
        super.onListenerDisconnected()
        Log.i(TAG, "listener disconnected")
    }

    override fun onDestroy() {
        stopCollectingPolicy()
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val snap = cache.get()
        val decision = MutePolicy.shouldMute(
            packageName = sbn.packageName,
            isOngoing = sbn.isOngoing,
            isClearable = sbn.isClearable,
            selfPackageName = packageName,
            globalMute = snap.globalMute,
            activeProfilePackages = snap.packagesForMutePolicy(),
        )

        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "posted ${sbn.packageName} cancel=${decision.shouldCancel} reason=${decision.reason}",
            )
        }

        if (decision.shouldCancel) {
            // Release: no per-cancel status spam (K14). No blank FSI hack (K7).
            cancelNotification(sbn.key)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op
    }

    private fun startCollectingPolicy() {
        val app = application as? NotiOffApp ?: return
        val scope = serviceScope ?: return
        collectJob?.cancel()
        collectJob = scope.launch {
            app.container.settingsRepository.policySnapshot.collect { snapshot ->
                cache.set(snapshot)
                refreshOngoingStatusNotification(snapshot)
            }
        }
    }

    private fun stopCollectingPolicy() {
        collectJob?.cancel()
        collectJob = null
    }

    private fun refreshOngoingStatusNotification(snapshot: PolicySnapshot) {
        NotificationChannels.ensureCreated(this)
        val manager = getSystemService<NotificationManager>() ?: return

        val text = when {
            snapshot.globalMute -> getString(R.string.status_global)
            snapshot.activeProfileTitle != null ->
                getString(R.string.status_profile, snapshot.activeProfileTitle)
            else -> getString(R.string.status_idle)
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.STATUS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Status UI may be blocked without POST_NOTIFICATIONS on API 33+; mute still works.
        runCatching {
            manager.notify(STATUS_NOTIFICATION_ID, notification)
        }.onFailure { e ->
            Log.w(TAG, "status notification failed (permission?)", e)
        }
    }

    companion object {
        private const val TAG = "NotiOffNLS"
        private const val STATUS_NOTIFICATION_ID = 1001
    }
}
