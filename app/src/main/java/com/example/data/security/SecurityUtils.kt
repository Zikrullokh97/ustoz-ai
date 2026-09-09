package com.example.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

object SecurityUtils {

  private val secureRandom = SecureRandom()

  fun generateSalt(bytes: Int = 16): String {
    val salt = ByteArray(bytes)
    secureRandom.nextBytes(salt)
    return Base64.getEncoder().encodeToString(salt)
  }

  fun hashPassword(password: String, salt: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(salt.toByteArray(Charsets.UTF_8))
    val hashedBytes = md.digest(password.toByteArray(Charsets.UTF_8))
    return Base64.getEncoder().encodeToString(hashedBytes)
  }

  fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
    val computedHash = hashPassword(password, salt)
    return constantTimeEquals(computedHash, expectedHash)
  }

  fun hashPin(pin: String, salt: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    md.update("ustoz_pin_salt_".toByteArray(Charsets.UTF_8))
    md.update(salt.toByteArray(Charsets.UTF_8))
    val hashedBytes = md.digest(pin.toByteArray(Charsets.UTF_8))
    return Base64.getEncoder().encodeToString(hashedBytes)
  }

  fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
    val computedHash = hashPin(pin, salt)
    return constantTimeEquals(computedHash, expectedHash)
  }

  fun generateSecureToken(): String {
    val randomBytes = ByteArray(32)
    secureRandom.nextBytes(randomBytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
  }

  fun generateUserId(): String {
    val uuid = UUID.randomUUID().toString().replace("-", "").take(8)
    return "ustoz_$uuid"
  }

  fun generateProfileId(): String {
    val uuid = UUID.randomUUID().toString().replace("-", "").take(8)
    return "prof_$uuid"
  }

  private fun constantTimeEquals(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    var result = 0
    for (i in a.indices) {
      result = result or (a[i].code xor b[i].code)
    }
    return result == 0
  }
}
