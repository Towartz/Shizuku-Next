package moe.shizuku.manager.watchdog

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import moe.shizuku.manager.AppConstants
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.starter.SelfStarterService
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.utils.EnvironmentUtils
import rikka.shizuku.Shizuku

class WatchdogService : Service() {

    companion object {
        const val ACTION_START = "moe.shizuku.manager.watchdog.action.START"
        const val ACTION_STOP = "moe.shizuku.manager.watchdog.action.STOP"
        const val ACTION_RESET_ATTEMPTS = "moe.shizuku.manager.watchdog.action.RESET_ATTEMPTS"
        private const val MAX_RESTART_ATTEMPTS = 5
        private const val STABLE_WINDOW_MILLIS = 180_000L
        private const val RESTART_IN_FLIGHT_RESET_MILLIS = 20_000L
        private const val HEARTBEAT_INTERVAL_MILLIS = 15_000L
        private const val BACKOFF_POLL_INTERVAL_MILLIS = 45_000L
        private const val NOTIFICATION_ID = AppConstants.NOTIFICATION_ID_STATUS + 1
        private const val CHANNEL_ID = "watchdog_status"

        fun start(context: Context) {
            val intent = Intent(context, WatchdogService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, WatchdogService::class.java).setAction(ACTION_STOP)
            context.stopService(intent)
        }

        fun resetAttemptsAndPoke(context: Context) {
            if (!ShizukuSettings.getPreferences().getBoolean(ShizukuSettings.WATCHDOG_ENABLED_ADB, false)) return
            val intent = Intent(context, WatchdogService::class.java).setAction(ACTION_RESET_ATTEMPTS)
            runCatching { context.startForegroundService(intent) }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var restartAttempts = 0
    private var isRestartInFlight = false
    private var listenersRegistered = false
    private var stableResetJob: Job? = null
    private var restartInFlightResetJob: Job? = null
    private var heartbeatJob: Job? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(AppConstants.TAG, "Watchdog observed Shizuku binder received")
        isRestartInFlight = false
        restartInFlightResetJob?.cancel()
        updateNotification(getString(R.string.notification_watchdog_content))
        scheduleStableReset()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w(AppConstants.TAG, "Watchdog observed Shizuku binder death")
        stableResetJob?.cancel()
        handleBinderDown()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundWithCompatibleType(buildNotification(getString(R.string.notification_watchdog_content)))
    }

    private fun startForegroundWithCompatibleType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_RESET_ATTEMPTS) {
            Log.i(AppConstants.TAG, "Watchdog reset attempts requested via intent")
            restartAttempts = 0
            isRestartInFlight = false
            if (!Shizuku.pingBinder()) {
                handleBinderDown()
            }
            return START_STICKY
        }

        if (!isWatchdogEnabled()) {
            Log.i(AppConstants.TAG, "Watchdog start ignored because setting is disabled")
            stopSelf()
            return START_NOT_STICKY
        }

        if (!listenersRegistered) {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            listenersRegistered = true
        }

        startHeartbeat()

        if (Shizuku.pingBinder()) {
            binderReceivedListener.onBinderReceived()
        } else {
            handleBinderDown()
        }

        return START_STICKY
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                val delayMs = if (restartAttempts >= MAX_RESTART_ATTEMPTS) BACKOFF_POLL_INTERVAL_MILLIS else HEARTBEAT_INTERVAL_MILLIS
                delay(delayMs)

                if (Shizuku.pingBinder()) {
                    if (restartAttempts > 0) {
                        restartAttempts = 0
                        updateNotification(getString(R.string.notification_watchdog_content))
                    }
                } else {
                    Log.w(AppConstants.TAG, "Watchdog heartbeat detected Shizuku binder is down")
                    handleBinderDown()
                }
            }
        }
    }

    private fun handleBinderDown() {
        if (!isWatchdogEnabled()) {
            stopSelf()
            return
        }

        if (isRestartInFlight) {
            Log.i(AppConstants.TAG, "Watchdog restart already in flight, ignoring duplicate trigger")
            return
        }

        val launchMode = ShizukuSettings.getLastLaunchMode()
        if (launchMode == ShizukuSettings.LaunchMethod.ROOT && EnvironmentUtils.isRooted()) {
            Log.i(AppConstants.TAG, "Watchdog triggering Root restart")
            isRestartInFlight = true
            updateNotification(getString(R.string.notification_service_starting))
            serviceScope.launch(Dispatchers.IO) {
                try {
                    if (Shell.getShell().isRoot) {
                        Shell.cmd(Starter.internalCommand).exec()
                    }
                } catch (tr: Throwable) {
                    Log.w(AppConstants.TAG, "Root restart failed", tr)
                } finally {
                    scheduleRestartInFlightReset()
                }
            }
            return
        }

        if (restartAttempts >= MAX_RESTART_ATTEMPTS) {
            Log.w(AppConstants.TAG, "Watchdog reached restart limit ($MAX_RESTART_ATTEMPTS), entering backoff state")
            updateNotification(getString(R.string.notification_watchdog_waiting))
            return
        }

        restartAttempts += 1
        isRestartInFlight = true
        updateNotification(getString(R.string.notification_watchdog_reconnecting, restartAttempts, MAX_RESTART_ATTEMPTS))
        startSelfStarterService()
        scheduleRestartInFlightReset()
    }

    private fun isWatchdogEnabled(): Boolean {
        return ShizukuSettings.getPreferences().getBoolean(ShizukuSettings.WATCHDOG_ENABLED_ADB, false)
    }

    private fun scheduleStableReset() {
        stableResetJob?.cancel()
        stableResetJob = serviceScope.launch {
            delay(STABLE_WINDOW_MILLIS)
            if (Shizuku.pingBinder()) {
                restartAttempts = 0
                Log.i(AppConstants.TAG, "Watchdog reset restart attempts after stable window")
            }
        }
    }

    private fun scheduleRestartInFlightReset() {
        restartInFlightResetJob?.cancel()
        restartInFlightResetJob = serviceScope.launch {
            delay(RESTART_IN_FLIGHT_RESET_MILLIS)
            isRestartInFlight = false
        }
    }

    private fun startSelfStarterService() {
        Log.i(
            AppConstants.TAG,
            "Watchdog requesting SelfStarterService restart attempt=$restartAttempts/$MAX_RESTART_ATTEMPTS"
        )
        val hasSecureSettings = checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        val intent = Intent(this, SelfStarterService::class.java).apply {
            putExtra(SelfStarterService.EXTRA_AUTO_ENABLE_WIRELESS_DEBUGGING, hasSecureSettings)
            putExtra(SelfStarterService.EXTRA_FORCE_RESTART, false)
            putExtra(SelfStarterService.EXTRA_DISABLE_WIRELESS_DEBUGGING_WHEN_FINISHED, false)
            putExtra(SelfStarterService.EXTRA_STARTED_BY_WATCHDOG, true)
        }
        startForegroundService(intent)
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_watchdog),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun buildNotification(contentText: String): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_system_icon)
            .setColor(getColor(R.color.notification))
            .setContentTitle(getString(R.string.notification_watchdog_title))
            .setContentText(contentText)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        heartbeatJob?.cancel()
        stableResetJob?.cancel()
        restartInFlightResetJob?.cancel()
        serviceScope.cancel()
        if (listenersRegistered) {
            runCatching { Shizuku.removeBinderReceivedListener(binderReceivedListener) }
            runCatching { Shizuku.removeBinderDeadListener(binderDeadListener) }
            listenersRegistered = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
