package com.ilusons.notioff.util

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

/**
 * Lists launchable apps via launcher intent query (parity with legacy PackagesDialog).
 * Relies on manifest &lt;queries&gt; for MAIN/LAUNCHER — no QUERY_ALL_PACKAGES.
 */
class PackageCatalog(
    private val packageManager: PackageManager,
) {
    fun loadLaunchableApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        return resolveInfos
            .mapNotNull { info ->
                val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
                val packageName = appInfo.packageName ?: return@mapNotNull null
                val label = try {
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    packageName
                }
                val icon = try {
                    packageManager.getApplicationIcon(appInfo)
                } catch (_: Exception) {
                    null
                }
                LaunchableApp(packageName = packageName, label = label, icon = icon)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
