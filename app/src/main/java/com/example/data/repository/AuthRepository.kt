package com.example.data.repository

import com.example.data.local.SessionDao
import com.example.data.local.TeacherProfileDao
import com.example.data.local.UserDao
import com.example.data.model.SessionEntity
import com.example.data.model.TeacherProfileEntity
import com.example.data.model.UserEntity
import com.example.data.security.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

sealed interface SessionStatus {
  object NoSession : SessionStatus
  data class NeedsPinSetup(val userId: String) : SessionStatus
  data class Locked(val userId: String, val teacherName: String) : SessionStatus
  data class Authenticated(val userId: String, val profile: TeacherProfileEntity) : SessionStatus
}

class AuthRepository(
  private val userDao: UserDao,
  private val teacherProfileDao: TeacherProfileDao,
  private val sessionDao: SessionDao
) {

  suspend fun checkSession(): SessionStatus {
    val activeSession = sessionDao.getActiveSession() ?: return SessionStatus.NoSession
    if (activeSession.expiresAt < System.currentTimeMillis()) {
      sessionDao.deleteSession(activeSession.sessionId)
      return SessionStatus.NoSession
    }

    val profile = teacherProfileDao.getByUserId(activeSession.userId)
    val teacherName = profile?.let { "${it.firstName} ${it.lastName}".trim() } ?: "O'qituvchi"

    return when {
      !activeSession.isPinSet -> SessionStatus.NeedsPinSetup(activeSession.userId)
      !activeSession.isUnlocked -> SessionStatus.Locked(activeSession.userId, teacherName)
      profile != null -> SessionStatus.Authenticated(activeSession.userId, profile)
      else -> SessionStatus.NoSession
    }
  }

  suspend fun register(
    firstName: String,
    lastName: String,
    emailOrPhone: String,
    password: String,
    language: String = "O'zbekcha",
    subject: String = "Umumiy ta'lim",
    school: String = ""
  ): Result<TeacherProfileEntity> {
    val cleanEmailOrPhone = emailOrPhone.trim().lowercase()
    if (cleanEmailOrPhone.isBlank()) {
      return Result.failure(IllegalArgumentException("Telefon yoki email kiritilishi shart"))
    }
    if (password.length < 6) {
      return Result.failure(IllegalArgumentException("Parol kamida 6 ta belgidan iborat bo'lishi kerak"))
    }
    if (firstName.isBlank() || lastName.isBlank()) {
      return Result.failure(IllegalArgumentException("Ism va familiya kiritilishi shart"))
    }

    val existingUser = userDao.findByEmailOrPhone(cleanEmailOrPhone)
    if (existingUser != null) {
      return Result.failure(IllegalStateException("Bu telefon yoki email orqali allaqachon ro'yxatdan o'tilgan"))
    }

    // 1. Create unique User ID & User Entity
    val userId = SecurityUtils.generateUserId()
    val salt = SecurityUtils.generateSalt()
    val passwordHash = SecurityUtils.hashPassword(password, salt)
    val user = UserEntity(
      userId = userId,
      emailOrPhone = cleanEmailOrPhone,
      passwordHash = passwordHash,
      salt = salt
    )
    userDao.insert(user)

    // 2. Create linked Teacher Profile (1-to-1 relationship with User ID)
    val profileId = SecurityUtils.generateProfileId()
    val profile = TeacherProfileEntity(
      profileId = profileId,
      userId = userId,
      firstName = firstName.trim(),
      lastName = lastName.trim(),
      emailOrPhone = cleanEmailOrPhone,
      language = language,
      subject = subject.trim().ifBlank { "Umumiy ta'lim" },
      school = school.trim()
    )
    teacherProfileDao.insert(profile)

    // 3. Invalidate any prior sessions to guarantee single active user session
    sessionDao.deactivateAllSessions()

    // 4. Create new Session requiring PIN setup
    val sessionId = UUID.randomUUID().toString()
    val accessToken = SecurityUtils.generateSecureToken()
    val refreshToken = SecurityUtils.generateSecureToken()
    val session = SessionEntity(
      sessionId = sessionId,
      userId = userId,
      accessToken = accessToken,
      refreshToken = refreshToken,
      isPinSet = false,
      isUnlocked = false,
      isActive = true
    )
    sessionDao.insert(session)

    return Result.success(profile)
  }

  suspend fun login(emailOrPhone: String, password: String): Result<SessionStatus> {
    val cleanEmailOrPhone = emailOrPhone.trim().lowercase()
    if (cleanEmailOrPhone.isBlank() || password.isBlank()) {
      return Result.failure(IllegalArgumentException("Telefon/email va parol kiritilishi shart"))
    }

    val user = userDao.findByEmailOrPhone(cleanEmailOrPhone)
      ?: return Result.failure(IllegalArgumentException("Telefon/email yoki parol noto'g'ri"))

    val isPasswordValid = SecurityUtils.verifyPassword(password, user.salt, user.passwordHash)
    if (!isPasswordValid) {
      return Result.failure(IllegalArgumentException("Telefon/email yoki parol noto'g'ri"))
    }

    // Invalidate previous sessions
    sessionDao.deactivateAllSessions()

    // Check if user has an existing PIN on this device
    val priorPinSession = sessionDao.getLatestConfiguredPinForUser(user.userId)

    val sessionId = UUID.randomUUID().toString()
    val accessToken = SecurityUtils.generateSecureToken()
    val refreshToken = SecurityUtils.generateSecureToken()

    val isPinSet = priorPinSession?.isPinSet == true && priorPinSession.pinHash != null
    val session = SessionEntity(
      sessionId = sessionId,
      userId = user.userId,
      accessToken = accessToken,
      refreshToken = refreshToken,
      pinHash = priorPinSession?.pinHash,
      pinSalt = priorPinSession?.pinSalt,
      isPinSet = isPinSet,
      isUnlocked = false,
      isActive = true
    )
    sessionDao.insert(session)

    val profile = teacherProfileDao.getByUserId(user.userId)
    val teacherName = profile?.let { "${it.firstName} ${it.lastName}".trim() } ?: "O'qituvchi"

    return if (!isPinSet) {
      Result.success(SessionStatus.NeedsPinSetup(user.userId))
    } else {
      Result.success(SessionStatus.Locked(user.userId, teacherName))
    }
  }

  suspend fun setupPin(pin: String): Result<Unit> {
    if (pin.length != 4 || !pin.all { it.isDigit() }) {
      return Result.failure(IllegalArgumentException("PIN kod aynan 4 ta raqamdan iborat bo'lishi shart"))
    }

    val activeSession = sessionDao.getActiveSession()
      ?: return Result.failure(IllegalStateException("Faol sessiya topilmadi. Qaytadan kiring."))

    val pinSalt = SecurityUtils.generateSalt()
    val pinHash = SecurityUtils.hashPin(pin, pinSalt)

    val updatedSession = activeSession.copy(
      pinHash = pinHash,
      pinSalt = pinSalt,
      isPinSet = true,
      isUnlocked = true
    )
    sessionDao.update(updatedSession)
    return Result.success(Unit)
  }

  suspend fun unlockWithPin(pin: String): Result<TeacherProfileEntity> {
    if (pin.length != 4) {
      return Result.failure(IllegalArgumentException("PIN kod 4 ta raqam bo'lishi kerak"))
    }

    val activeSession = sessionDao.getActiveSession()
      ?: return Result.failure(IllegalStateException("Faol sessiya topilmadi. Qaytadan kiring."))

    val storedHash = activeSession.pinHash
    val storedSalt = activeSession.pinSalt

    if (storedHash == null || storedSalt == null) {
      return Result.failure(IllegalStateException("Ushbu hisob uchun PIN o'rnatilmagan"))
    }

    val isPinValid = SecurityUtils.verifyPin(pin, storedSalt, storedHash)
    if (!isPinValid) {
      return Result.failure(IllegalArgumentException("PIN kod noto'g'ri"))
    }

    val updatedSession = activeSession.copy(isUnlocked = true)
    sessionDao.update(updatedSession)

    val profile = teacherProfileDao.getByUserId(activeSession.userId)
      ?: return Result.failure(IllegalStateException("O'qituvchi profili topilmadi"))

    return Result.success(profile)
  }

  suspend fun lockApp() {
    val activeSession = sessionDao.getActiveSession() ?: return
    sessionDao.update(activeSession.copy(isUnlocked = false))
  }

  suspend fun logout() {
    sessionDao.deactivateAllSessions()
    sessionDao.deleteActiveSession()
  }

  // ==========================================
  // MULTI-USER ISOLATION & DATA SCOPING
  // ==========================================

  /**
   * Retrieves the current authenticated User ID strictly from the active, unlocked session.
   * Fails if session does not exist, is locked, inactive, or expired.
   */
  suspend fun getAuthenticatedUserId(): Result<String> {
    val activeSession = sessionDao.getActiveSession()
      ?: return Result.failure(SecurityException("401 Unauthorized: Faol sessiya mavjud emas"))
    if (!activeSession.isActive || !activeSession.isUnlocked) {
      return Result.failure(SecurityException("401 Unauthorized: Sessiya qulflangan yoki faol emas"))
    }
    if (activeSession.expiresAt < System.currentTimeMillis()) {
      sessionDao.deleteSession(activeSession.sessionId)
      return Result.failure(SecurityException("401 Unauthorized: Sessiya muddati tugagan"))
    }
    return Result.success(activeSession.userId)
  }

  suspend fun requireAuthenticatedUserId(): String {
    return getAuthenticatedUserId().getOrThrow()
  }

  /**
   * Enforces multi-user isolation by verifying that the target user ID matches
   * the currently authenticated user session. Rejects cross-user access with 403 Forbidden.
   */
  suspend fun ensureUserIsolation(targetUserId: String): Result<Unit> {
    val currentUserId = getAuthenticatedUserId().getOrElse { return Result.failure(it) }
    if (currentUserId != targetUserId) {
      return Result.failure(SecurityException("403 Forbidden: Boshqa o'qituvchi ma'lumotlariga kirish taqiqlanadi"))
    }
    return Result.success(Unit)
  }

  suspend fun isUserAuthenticated(userId: String): Boolean {
    val activeSession = sessionDao.getActiveSession() ?: return false
    return activeSession.isActive &&
        activeSession.isUnlocked &&
        activeSession.userId == userId &&
        activeSession.expiresAt >= System.currentTimeMillis()
  }

  suspend fun <T> scopeQuery(query: suspend (authenticatedUserId: String) -> Result<T>): Result<T> {
    val currentUserId = getAuthenticatedUserId().getOrElse { return Result.failure(it) }
    return query(currentUserId)
  }

  suspend fun clearUserSessions(userId: String) {
    sessionDao.deleteSessionsForUser(userId)
  }

  // ==========================================
  // SECURE TOKEN STORAGE & MANAGEMENT
  // ==========================================

  /**
   * Retrieves the secure authentication access token only if the session is active and unlocked.
   */
  suspend fun getAccessToken(): Result<String> {
    val activeSession = sessionDao.getActiveSession()
      ?: return Result.failure(SecurityException("401 Unauthorized: Faol sessiya mavjud emas"))
    if (!activeSession.isActive || !activeSession.isUnlocked) {
      return Result.failure(SecurityException("401 Unauthorized: Sessiya qulflangan yoki faol emas"))
    }
    if (activeSession.expiresAt < System.currentTimeMillis()) {
      sessionDao.deleteSession(activeSession.sessionId)
      return Result.failure(SecurityException("401 Unauthorized: Token muddati o'tgan"))
    }
    return Result.success(activeSession.accessToken)
  }

  suspend fun getAuthToken(): Result<String> = getAccessToken()

  suspend fun getRefreshToken(): Result<String> {
    val activeSession = sessionDao.getActiveSession()
      ?: return Result.failure(SecurityException("401 Unauthorized: Faol sessiya mavjud emas"))
    if (!activeSession.isActive) {
      return Result.failure(SecurityException("401 Unauthorized: Faol sessiya mavjud emas"))
    }
    return Result.success(activeSession.refreshToken)
  }

  /**
   * Rotates access and refresh tokens securely and renews the expiration period.
   */
  suspend fun refreshAuthTokens(): Result<SessionEntity> {
    val activeSession = sessionDao.getActiveSession()
      ?: return Result.failure(SecurityException("401 Unauthorized: Faol sessiya mavjud emas"))

    val newAccessToken = SecurityUtils.generateSecureToken()
    val newRefreshToken = SecurityUtils.generateSecureToken()
    val newExpiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)

    val refreshedSession = activeSession.copy(
      accessToken = newAccessToken,
      refreshToken = newRefreshToken,
      expiresAt = newExpiresAt
    )
    sessionDao.update(refreshedSession)
    return Result.success(refreshedSession)
  }

  suspend fun validateToken(token: String): Boolean {
    val activeSession = sessionDao.getActiveSession() ?: return false
    if (!activeSession.isActive || !activeSession.isUnlocked) return false
    if (activeSession.expiresAt < System.currentTimeMillis()) return false
    return activeSession.accessToken == token
  }

  suspend fun revokeActiveSession(): Result<Unit> {
    sessionDao.deleteActiveSession()
    return Result.success(Unit)
  }

  // ==========================================
  // REACTIVE FLOWS
  // ==========================================

  // Reactive flow to observe session status changes in real-time
  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val sessionStatusFlow: Flow<SessionStatus> =
    sessionDao.getActiveSessionFlow()
      .distinctUntilChanged()
      .flatMapLatest { session ->
        if (session == null || !session.isActive) {
          flowOf(SessionStatus.NoSession)
        } else if (session.expiresAt < System.currentTimeMillis()) {
          flowOf(SessionStatus.NoSession)
        } else if (!session.isPinSet) {
          flowOf(SessionStatus.NeedsPinSetup(session.userId))
        } else if (!session.isUnlocked) {
          val profile = teacherProfileDao.getByUserId(session.userId)
          val teacherName = profile?.let { "${it.firstName} ${it.lastName}".trim() } ?: "O'qituvchi"
          flowOf(SessionStatus.Locked(session.userId, teacherName))
        } else {
          teacherProfileDao.getByUserIdFlow(session.userId).flatMapLatest { profile ->
            if (profile != null) {
              flowOf(SessionStatus.Authenticated(session.userId, profile))
            } else {
              flowOf(SessionStatus.NoSession)
            }
          }
        }
      }

  // Reactive flow to observe current authenticated teacher profile
  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val activeTeacherProfileFlow: Flow<TeacherProfileEntity?> =
    sessionDao.getActiveSessionFlow()
      .distinctUntilChanged()
      .flatMapLatest { session ->
        if (session != null && session.isUnlocked) {
          teacherProfileDao.getByUserIdFlow(session.userId)
        } else {
          flowOf(null)
        }
      }

  suspend fun getCurrentTeacherProfile(): TeacherProfileEntity? {
    val activeSession = sessionDao.getActiveSession() ?: return null
    if (!activeSession.isUnlocked) return null
    return teacherProfileDao.getByUserId(activeSession.userId)
  }

  suspend fun getActiveSession(): SessionEntity? {
    val session = sessionDao.getActiveSession() ?: return null
    if (session.expiresAt < System.currentTimeMillis()) {
      sessionDao.deleteSession(session.sessionId)
      return null
    }
    return session
  }
}
