package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityCrypto {
    private val secureRandom = SecureRandom()

    fun generateSalt(): String {
        val bytes = ByteArray(12)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(salt: String, rawPassword: String): String {
        return sha256("$salt:$rawPassword")
    }

    fun computeAuditHash(
        prevHash: String,
        managerId: String,
        actionPerformed: String,
        recordType: String,
        recordId: String,
        previousValue: String,
        newValue: String,
        dateKey: String,
        timeDisplay: String
    ): String {
        val payload = listOf(
            prevHash,
            managerId,
            actionPerformed,
            recordType,
            recordId,
            previousValue,
            newValue,
            dateKey,
            timeDisplay
        ).joinToString("|")
        return sha256(payload)
    }
}
