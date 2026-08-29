package rikka.shizuku.ktx

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.IInterface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Result data class for remote process execution.
 */
data class ShizukuExecResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

/**
 * Flow emitting the alive status of the Shizuku server binder.
 */
val Shizuku.Companion.binderAliveFlow: Flow<Boolean>
    get() = callbackFlow {
        val receivedListener = Shizuku.OnBinderReceivedListener {
            trySend(true)
        }
        val deadListener = Shizuku.OnBinderDeadListener {
            trySend(false)
        }

        Shizuku.addBinderReceivedListenerSticky(receivedListener)
        Shizuku.addBinderDeadListener(deadListener)

        awaitClose {
            Shizuku.removeBinderReceivedListener(receivedListener)
            Shizuku.removeBinderDeadListener(deadListener)
        }
    }

/**
 * Suspends until the Shizuku server binder is ready and received.
 *
 * @param timeoutMillis Maximum time in milliseconds to wait before timing out.
 * @return The server [IBinder].
 * @throws ShizukuNotRunningException If the timeout expires before the binder is received.
 */
suspend fun Shizuku.Companion.awaitBinder(timeoutMillis: Long = 10_000L): IBinder {
    if (Shizuku.pingBinder()) {
        return Shizuku.getBinder() ?: throw ShizukuNotRunningException()
    }

    return try {
        withTimeout(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : Shizuku.OnBinderReceivedListener {
                    override fun onBinderReceived() {
                        Shizuku.removeBinderReceivedListener(this)
                        val b = Shizuku.getBinder()
                        if (b != null && continuation.isActive) {
                            continuation.resume(b)
                        } else if (continuation.isActive) {
                            continuation.resumeWithException(ShizukuNotRunningException())
                        }
                    }
                }
                Shizuku.addBinderReceivedListenerSticky(listener)
                continuation.invokeOnCancellation {
                    Shizuku.removeBinderReceivedListener(listener)
                }
            }
        }
    } catch (e: Exception) {
        if (e is CancellationException && e !is kotlinx.coroutines.TimeoutCancellationException) {
            throw e
        }
        throw ShizukuNotRunningException("Timed out waiting for Shizuku server binder after ${timeoutMillis}ms")
    }
}

/**
 * Request Shizuku permission asynchronously.
 *
 * @param requestCode Application specific request code (default 1001).
 * @return True if permission is granted, false otherwise.
 */
suspend fun Shizuku.Companion.requestPermissionResult(requestCode: Int = 1001): Boolean {
    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
        return true
    }

    return suspendCancellableCoroutine { continuation ->
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(reqCode: Int, grantResult: Int) {
                if (reqCode == requestCode) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    if (continuation.isActive) {
                        continuation.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                    }
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        continuation.invokeOnCancellation {
            Shizuku.removeRequestPermissionResultListener(listener)
        }

        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Throwable) {
            Shizuku.removeRequestPermissionResultListener(listener)
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }
}

/**
 * Bind to a Shizuku UserService using coroutines.
 *
 * @param args UserService configuration arguments.
 * @param asInterface Lambda converting [IBinder] to the AIDL interface [T].
 * @return The AIDL interface [T] once connected.
 */
suspend fun <T : IInterface> Shizuku.Companion.bindUserService(
    args: Shizuku.UserServiceArgs,
    asInterface: (IBinder) -> T
): T {
    return suspendCancellableCoroutine { continuation ->
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (service != null && continuation.isActive) {
                    try {
                        val iface = asInterface(service)
                        continuation.resume(iface)
                    } catch (e: Throwable) {
                        continuation.resumeWithException(e)
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // Disconnected
            }
        }

        continuation.invokeOnCancellation {
            runCatching {
                Shizuku.unbindUserService(args, connection, true)
            }
        }

        try {
            Shizuku.bindUserService(args, connection)
        } catch (e: Throwable) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }
}

/**
 * Stream lines of stdout from a [ShizukuRemoteProcess] as a reactive [Flow].
 */
fun ShizukuRemoteProcess.stdoutLinesFlow(): Flow<String> = flow {
    BufferedReader(InputStreamReader(inputStream)).use { reader ->
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            emit(line!!)
        }
    }
}.flowOn(Dispatchers.IO)

/**
 * Stream lines of stderr from a [ShizukuRemoteProcess] as a reactive [Flow].
 */
fun ShizukuRemoteProcess.stderrLinesFlow(): Flow<String> = flow {
    BufferedReader(InputStreamReader(errorStream)).use { reader ->
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            emit(line!!)
        }
    }
}.flowOn(Dispatchers.IO)
