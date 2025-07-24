package com.prof18.feedflow.desktop.telemetry

import com.prof18.feedflow.core.utils.AppDataPathBuilder
import com.prof18.feedflow.core.utils.AppEnvironment
import java.io.File
import java.security.MessageDigest
import java.util.*

class UserIdManager(
    private val salt: String,
    appEnv: AppEnvironment,
) {
    private val userIdFile = File(
        AppDataPathBuilder.getAppDataPath(appEnv),
        "id",
    )

    private var userId: String? = null

    fun getHashedUserId(): String {
        if (userId != null) {
            return requireNotNull(userId)
        }
        val userId = if (userIdFile.exists()) {
            userIdFile.readText().trim()
        } else {
            val rawUserId = UUID.randomUUID().toString()
            val hashedUserId = hashString("$salt$rawUserId")
            userIdFile.writeText(hashedUserId)
            hashedUserId
        }
        this.userId = userId
        return userId
    }

    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
