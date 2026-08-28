package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import moe.shizuku.manager.AppConstants
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.watchdog.WatchdogService
import rikka.shizuku.Shizuku

class WirelessAdbStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = ShizukuSettings.getPreferences()
        val autoStartEnabled = prefs.getBoolean(ShizukuSettings.KEEP_START_ON_BOOT_WIRELESS, false)
        val watchdogEnabled = prefs.getBoolean(ShizukuSettings.WATCHDOG_ENABLED_ADB, false)

        if (!autoStartEnabled && !watchdogEnabled) return

        if (intent.action == "android.net.wifi.STATE_CHANGE" ||
            intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return
            val capabilities = cm.getNetworkCapabilities(network) ?: return

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i(AppConstants.TAG, "Wi-Fi connected, checking Shizuku state")
                if (watchdogEnabled) {
                    WatchdogService.resetAttemptsAndPoke(context)
                }
                if (autoStartEnabled && !Shizuku.pingBinder()) {
                    ShizukuReceiverStarter.startWireless(context, force = false, requireBootSupport = false)
                }
            }
        }
    }
}
