package moe.shizuku.manager.hide

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Process
import android.util.Log
import moe.shizuku.manager.AppConstants
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.ShizukuSettings
import java.util.concurrent.ConcurrentHashMap

object HideAppsManager {

    private val cachedHiddenPackages = ConcurrentHashMap.newKeySet<String>()
    private val uidCache = ConcurrentHashMap<Int, Boolean>()
    @Volatile
    private var initialized = false

    private fun ensureInitialized(context: Context) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    ShizukuSettings.initialize(context)
                    val prefs = ShizukuSettings.getPreferences()
                    val savedSet = prefs.getStringSet(ShizukuSettings.HIDDEN_APPS_SET, emptySet()) ?: emptySet()
                    cachedHiddenPackages.clear()
                    cachedHiddenPackages.addAll(savedSet)
                    uidCache.clear()
                    initialized = true
                }
            }
        }
    }

    fun getHiddenPackages(context: Context): Set<String> {
        ensureInitialized(context)
        return cachedHiddenPackages.toSet()
    }

    fun isPackageHidden(context: Context, packageName: String): Boolean {
        ensureInitialized(context)
        return cachedHiddenPackages.contains(packageName)
    }

    fun isUidHidden(context: Context, uid: Int): Boolean {
        if (uid <= 0 || uid == Process.myUid()) return false
        ensureInitialized(context)

        return uidCache.computeIfAbsent(uid) {
            val pm = context.packageManager
            val packages = pm.getPackagesForUid(uid) ?: return@computeIfAbsent false
            packages.any { cachedHiddenPackages.contains(it) }
        }
    }

    fun setPackageHidden(context: Context, packageName: String, hidden: Boolean) {
        ensureInitialized(context)
        if (hidden) {
            cachedHiddenPackages.add(packageName)
        } else {
            cachedHiddenPackages.remove(packageName)
        }
        uidCache.clear()

        val prefs = ShizukuSettings.getPreferences()
        prefs.edit().putStringSet(ShizukuSettings.HIDDEN_APPS_SET, HashSet(cachedHiddenPackages)).apply()
        Log.i(AppConstants.TAG, "HideAppsManager: updated $packageName hidden=$hidden (total hidden=${cachedHiddenPackages.size})")
    }

    fun getInstalledApps(context: Context): List<PackageInfo> {
        val pm = context.packageManager
        val installed = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        return installed.filter {
            it.packageName != BuildConfig.APPLICATION_ID &&
                it.packageName != "moe.shizuku.privileged.api" &&
                it.applicationInfo != null &&
                (it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }.sortedBy { it.applicationInfo.loadLabel(pm).toString().lowercase() }
    }
}
