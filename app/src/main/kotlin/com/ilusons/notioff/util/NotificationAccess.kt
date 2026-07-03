package com.ilusons.notioff.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.TextUtils
import com.ilusons.notioff.service.NotiOffNotificationListenerService

object NotificationAccess {

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        if (TextUtils.isEmpty(flat)) return false
        val names = flat.split(":").mapNotNull { ComponentName.unflattenFromString(it) }
        val expected = ComponentName(context, NotiOffNotificationListenerService::class.java)
        return names.any { it.packageName == expected.packageName && it.className == expected.className }
    }

    fun createListenerSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    /**
     * Optional OEM troubleshoot (K17) — not run automatically on every launch.
     */
    fun rebindListenerComponent(context: Context) {
        val pm = context.packageManager
        val component = ComponentName(context, NotiOffNotificationListenerService::class.java)
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
