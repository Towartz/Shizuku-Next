package rikka.shizuku.ktx

/**
 * Base class for all Shizuku-related exceptions.
 */
open class ShizukuException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Thrown when attempting an operation while the Shizuku server binder is dead or unavailable.
 */
class ShizukuNotRunningException(
    message: String = "Shizuku server is not running or binder is unavailable"
) : ShizukuException(message)

/**
 * Thrown when the caller does not have permission to communicate with Shizuku.
 */
class ShizukuPermissionDeniedException(
    message: String = "Shizuku permission has not been granted to this package"
) : ShizukuException(message)

/**
 * Thrown when the server API version does not satisfy the client requirement.
 */
class ShizukuVersionMismatchException(
    val requiredVersion: Int,
    val actualVersion: Int
) : ShizukuException("Shizuku server version $actualVersion is lower than required version $requiredVersion")

/**
 * Thrown when a remote process execution fails or returns an unexpected exit code.
 */
class ShizukuProcessException(
    val exitCode: Int,
    val stderr: String,
    message: String = "Process execution failed with exit code $exitCode: $stderr"
) : ShizukuException(message)
