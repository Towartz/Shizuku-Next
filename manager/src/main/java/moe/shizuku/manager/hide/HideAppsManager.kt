package moe.shizuku.manager.hide

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Process
import android.util.Log
import moe.shizuku.manager.AppConstants
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.utils.Logger.LOGGER
import moe.shizuku.manager.utils.ShizukuSystemApis
import rikka.shizuku.Shizuku
import java.util.concurrent.ConcurrentHashMap

object HideAppsManager {

    const val FLAG_HIDDEN = 1 shl 3
    const val MASK_HIDDEN = FLAG_HIDDEN
    private const val FLAG_ALLOWED = 1 shl 1
    private const val FLAG_DENIED = 1 shl 2
    private const val MASK_PERMISSION = FLAG_ALLOWED or FLAG_DENIED

    private val cachedHiddenPackages = ConcurrentHashMap.newKeySet<String>()
    private val uidCache = ConcurrentHashMap<Int, Boolean>()
    @Volatile
    private var initialized = false
    @Volatile
    private var listenerRegistered = false

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

                    if (!listenerRegistered) {
                        listenerRegistered = true
                        try {
                            Shizuku.addBinderReceivedListenerSticky {
                                syncAllToService(context)
                            }
                        } catch (e: Throwable) {
                            LOGGER.w(e, "HideAppsManager: failed to register binder listener")
                        }
                    }
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

    fun setPackageHidden(context: Context, packageName: String, hidden: Boolean, explicitUid: Int? = null) {
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

        if (Shizuku.pingBinder()) {
            try {
                val uid = explicitUid ?: try {
                    context.packageManager.getApplicationInfo(packageName, 0).uid
                } catch (_: Throwable) {
                    -1
                }
                if (uid > 0) {
                    val mask = MASK_HIDDEN or MASK_PERMISSION
                    val value = if (hidden) FLAG_HIDDEN or FLAG_DENIED else 0
                    Shizuku.updateFlagsForUid(uid, mask, value)
                    if (hidden) {
                        try {
                            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                            am?.killBackgroundProcesses(packageName)
                        } catch (_: Throwable) {
                        }
                    }
                }
            } catch (e: Throwable) {
                LOGGER.w(e, "HideAppsManager: failed to sync updateFlagsForUid for $packageName")
            }
        }
    }

    fun syncAllToService(context: Context) {
        ensureInitialized(context)
        if (!Shizuku.pingBinder()) return

        val hiddenList = cachedHiddenPackages.toList()
        if (hiddenList.isEmpty()) return

        val pm = context.packageManager
        for (pkg in hiddenList) {
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                if (ai.uid > 0) {
                    val mask = MASK_HIDDEN or MASK_PERMISSION
                    val value = FLAG_HIDDEN or FLAG_DENIED
                    Shizuku.updateFlagsForUid(ai.uid, mask, value)
                }
            } catch (e: Throwable) {
                LOGGER.w(e, "HideAppsManager: syncAllToService failed for $pkg")
            }
        }
        LOGGER.i("HideAppsManager: synced ${hiddenList.size} hidden packages to Shizuku service")
    }

    fun getInstalledApps(context: Context): List<PackageInfo> {
        ensureInitialized(context)
        val pm = context.packageManager
        val installed = mutableListOf<PackageInfo>()

        if (Shizuku.pingBinder()) {
            try {
                installed.addAll(ShizukuSystemApis.getInstalledPackages(PackageManager.GET_META_DATA.toLong(), 0))
            } catch (e: Throwable) {
                LOGGER.w(e, "HideAppsManager: failed to get packages via ShizukuSystemApis, fallback to PackageManager")
            }
        }

        if (installed.isEmpty()) {
            try {
                installed.addAll(pm.getInstalledPackages(PackageManager.GET_META_DATA))
            } catch (e: Throwable) {
                LOGGER.e(e, "HideAppsManager: failed to get installed packages via PackageManager")
            }
        }

        return installed.filter {
            val ai = it.applicationInfo ?: return@filter false
            it.packageName != BuildConfig.APPLICATION_ID &&
                it.packageName != "moe.shizuku.privileged.api" &&
                (ai.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }.sortedBy {
            it.applicationInfo?.loadLabel(pm)?.toString()?.lowercase() ?: it.packageName
        }
    }
}
