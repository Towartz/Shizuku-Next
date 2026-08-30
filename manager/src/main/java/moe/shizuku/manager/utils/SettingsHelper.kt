package moe.shizuku.manager.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.utils.Logger.LOGGER
import rikka.shizuku.Shizuku

object SettingsHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context, launcher: ActivityResultLauncher<Intent>? = null) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        if (launcher != null) {
            launcher.launch(intent)
        } else {
            try {
                context.startActivity(intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (_: Exception) {
                }
            }
        }
    }

    fun requestIgnoreBatteryOptimizationsPrivileged(
        context: Context,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val pkg = context.packageName
        val cmds = listOf(
            "cmd deviceidle whitelist +$pkg",
            "dumpsys deviceidle whitelist +$pkg",
            "cmd appops set $pkg RUN_IN_BACKGROUND allow",
            "cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow",
            "cmd appops set $pkg REQUEST_IGNORE_BATTERY_OPTIMIZATIONS allow",
            "cmd appops set $pkg AUTO_START allow"
        )

        CoroutineScope(Dispatchers.IO).launch {
            var success = false
            if (Shizuku.pingBinder()) {
                try {
                    val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    ).apply { isAccessible = true }

                    for (cmd in cmds) {
                        val proc = newProcessMethod.invoke(null, arrayOf("sh", "-c", cmd), null, null) as? Process
                        proc?.waitFor()
                    }
                    success = isIgnoringBatteryOptimizations(context)
                } catch (e: Throwable) {
                    LOGGER.w("requestIgnoreBatteryOptimizationsPrivileged via Shizuku failed", e)
                }
            }

            if (!success && com.topjohnwu.superuser.Shell.isAppGrantedRoot() == true) {
                try {
                    for (cmd in cmds) {
                        com.topjohnwu.superuser.Shell.cmd(cmd).exec()
                    }
                    success = isIgnoringBatteryOptimizations(context)
                } catch (e: Throwable) {
                    LOGGER.w("requestIgnoreBatteryOptimizationsPrivileged via root failed", e)
                }
            }

            withContext(Dispatchers.Main) {
                if (!success) {
                    requestIgnoreBatteryOptimizations(context)
                }
                onComplete?.invoke(success)
            }
        }
    }

    fun openWifiSettings(context: Context) {
        val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {
            }
        }
    }
}
