package moe.shizuku.manager.hide

import android.content.ComponentName
import android.content.Context
import android.util.Log
import moe.shizuku.manager.AppConstants
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.ktx.setComponentEnabled
import moe.shizuku.manager.legacy.LegacyIsNotSupportedActivity
import moe.shizuku.manager.legacy.ShellRequestHandlerActivity
import moe.shizuku.manager.receiver.BootCompleteReceiver
import moe.shizuku.manager.receiver.ManualStartReceiver
import moe.shizuku.manager.receiver.ManualStopReceiver
import moe.shizuku.manager.receiver.ShizukuReceiver

object StealthModeManager {

    private val DISCOVERABLE_COMPONENTS = listOf(
        ShellRequestHandlerActivity::class.java,
        LegacyIsNotSupportedActivity::class.java,
        ManualStartReceiver::class.java,
        ManualStopReceiver::class.java,
        ShizukuReceiver::class.java,
        BootCompleteReceiver::class.java
    )

    fun isStealthModeEnabled(context: Context): Boolean {
        ShizukuSettings.initialize(context)
        return ShizukuSettings.getPreferences().getBoolean(ShizukuSettings.STEALTH_MODE_ENABLED, false)
    }

    fun setStealthModeEnabled(context: Context, enabled: Boolean) {
        ShizukuSettings.initialize(context)
        ShizukuSettings.getPreferences().edit().putBoolean(ShizukuSettings.STEALTH_MODE_ENABLED, enabled).apply()

        val componentEnabled = !enabled // Stealth enabled means components disabled to conceal them
        for (clazz in DISCOVERABLE_COMPONENTS) {
            val component = ComponentName(context, clazz)
            context.packageManager.setComponentEnabled(component, componentEnabled)
        }
        Log.i(AppConstants.TAG, "StealthModeManager: stealth mode set to $enabled (components active=$componentEnabled)")
    }
}
