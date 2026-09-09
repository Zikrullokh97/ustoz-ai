package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UstozDatabase
import com.example.data.model.SessionEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.SessionStatus
import com.example.data.repository.TeacherRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthRepositoryIsolationTest {

  private lateinit var database: UstozDatabase
  private lateinit var authRepository: AuthRepository
  private lateinit var teacherRepository: TeacherRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, UstozDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    authRepository = AuthRepository(
      userDao = database.userDao(),
      teacherProfileDao = database.teacherProfileDao(),
      sessionDao = database.sessionDao()
    )

    teacherRepository = TeacherRepository(
      sessionDao = database.sessionDao(),
      teacherProfileDao = database.teacherProfileDao(),
      classDao = database.classDao(),
      studentDao = database.studentDao(),
      authRepository = authRepository
    )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun test1_sessionStateTransitions_and_TokenStorage() = runBlocking {
    // Check initial state -> NoSession
    val initialStatus = authRepository.checkSession()
    assertEquals(SessionStatus.NoSession, initialStatus)

    // Register user -> NeedsPinSetup
    val profile = authRepository.register(
      firstName = "Nodira",
      lastName = "Xoliqova",
      emailOrPhone = "nodira@ustoz.uz",
      password = "password123",
      subject = "Biologiya"
    ).getOrThrow()

    val afterRegStatus = authRepository.checkSession()
    assertTrue(afterRegStatus is SessionStatus.NeedsPinSetup)

    // Tokens exist in session table
    val rawSession = database.sessionDao().getActiveSession()
    assertNotNull(rawSession)
    assertNotNull(rawSession?.accessToken)
    assertNotNull(rawSession?.refreshToken)
    assertFalse(rawSession!!.isUnlocked)

    // Cannot get auth token before PIN unlock
    val tokenLocked = authRepository.getAccessToken()
    assertTrue(tokenLocked.isFailure)

    // Setup PIN -> Transitions to unlocked / Authenticated
    authRepository.setupPin("1234")
    val afterPinStatus = authRepository.checkSession()
    assertTrue(afterPinStatus is SessionStatus.Authenticated)

    // Access token is now accessible securely
    val tokenResult = authRepository.getAccessToken()
    assertTrue(tokenResult.isSuccess)
    val token = tokenResult.getOrThrow()
    assertTrue(token.isNotBlank())
    assertTrue(authRepository.validateToken(token))

    // Lock app -> Transitions to Locked
    authRepository.lockApp()
    val afterLockStatus = authRepository.checkSession()
    assertTrue(afterLockStatus is SessionStatus.Locked)
    assertTrue(authRepository.getAccessToken().isFailure)

    // Unlock with PIN -> Authenticated
    val unlockResult = authRepository.unlockWithPin("1234")
    assertTrue(unlockResult.isSuccess)
    assertTrue(authRepository.getAccessToken().isSuccess)
  }

  @Test
  fun test2_tokenRotation_and_Refresh() = runBlocking {
    authRepository.register(
      firstName = "Jasur",
      lastName = "Berdiyev",
      emailOrPhone = "jasur@ustoz.uz",
      password = "password123"
    ).getOrThrow()
    authRepository.setupPin("9876")

    val oldToken = authRepository.getAccessToken().getOrThrow()
    val oldRefreshToken = authRepository.getRefreshToken().getOrThrow()

    // Refresh tokens
    val refreshed = authRepository.refreshAuthTokens().getOrThrow()
    assertNotEquals(oldToken, refreshed.accessToken)
    assertNotEquals(oldRefreshToken, refreshed.refreshToken)

    // Old token should no longer validate
    assertFalse(authRepository.validateToken(oldToken))
    // New token validates
    assertTrue(authRepository.validateToken(refreshed.accessToken))
    assertEquals(refreshed.accessToken, authRepository.getAccessToken().getOrThrow())
  }

  @Test
  fun test3_multiUserIsolation_and_Scoping() = runBlocking {
    // Teacher 1 (Anvar)
    val teacher1Profile = authRepository.register(
      firstName = "Anvar",
      lastName = "Sodiqov",
      emailOrPhone = "anvar@ustoz.uz",
      password = "password123"
    ).getOrThrow()
    authRepository.setupPin("1111")
    val teacher1Id = authRepository.getAuthenticatedUserId().getOrThrow()
    assertEquals(teacher1Profile.userId, teacher1Id)

    // Teacher 1 creates a class
    val class1 = teacherRepository.createClass(grade = 7, section = "A", academicYear = "2024-2025").getOrThrow()
    assertEquals(teacher1Id, class1.teacherId)

    // ensureUserIsolation for Teacher 1 succeeds
    assertTrue(authRepository.ensureUserIsolation(teacher1Id).isSuccess)
    // ensureUserIsolation for arbitrary user fails with 403
    val isolationCheck = authRepository.ensureUserIsolation("ustoz_intruder")
    assertTrue(isolationCheck.isFailure)
    assertTrue(isolationCheck.exceptionOrNull() is SecurityException)

    // Teacher 2 (Shahlo) logs in on same device
    val teacher2Profile = authRepository.register(
      firstName = "Shahlo",
      lastName = "Rasulova",
      emailOrPhone = "shahlo@ustoz.uz",
      password = "password123"
    ).getOrThrow()
    authRepository.setupPin("2222")
    val teacher2Id = authRepository.getAuthenticatedUserId().getOrThrow()
    assertNotEquals(teacher1Id, teacher2Id)

    // Isolation: Current active user is strictly Teacher 2
    assertEquals(teacher2Id, authRepository.requireAuthenticatedUserId())
    assertTrue(authRepository.isUserAuthenticated(teacher2Id))
    assertFalse(authRepository.isUserAuthenticated(teacher1Id))

    // Attempting to access Teacher 1's ID via ensureUserIsolation fails with 403 Forbidden
    val crossAccess = authRepository.ensureUserIsolation(teacher1Id)
    assertTrue(crossAccess.isFailure)
    assertTrue(crossAccess.exceptionOrNull()?.message?.contains("403 Forbidden") == true)

    // TeacherRepository scoped to authRepository will reject Teacher 2 accessing Teacher 1's class
    val crossClassAccess = teacherRepository.getClassById(class1.id)
    assertTrue(crossClassAccess.isFailure)
    assertTrue(crossClassAccess.exceptionOrNull() is SecurityException)

    // Teacher 2's class list is empty
    val teacher2Classes = teacherRepository.getClasses().getOrThrow()
    assertTrue(teacher2Classes.isEmpty())
  }

  @Test
  fun test4_expiredSessionHandling() = runBlocking {
    val profile = authRepository.register(
      firstName = "Zarina",
      lastName = "Karimova",
      emailOrPhone = "zarina@ustoz.uz",
      password = "password123"
    ).getOrThrow()
    authRepository.setupPin("3333")

    val activeSession = database.sessionDao().getActiveSession()
    assertNotNull(activeSession)

    // Artificially expire the session
    val expiredSession = activeSession!!.copy(
      expiresAt = System.currentTimeMillis() - 1000 // in the past
    )
    database.sessionDao().update(expiredSession)

    // checkSession should detect expiration, delete session, and return NoSession
    val status = authRepository.checkSession()
    assertEquals(SessionStatus.NoSession, status)

    // getAuthenticatedUserId should return failure with 401 Unauthorized
    val authIdResult = authRepository.getAuthenticatedUserId()
    assertTrue(authIdResult.isFailure)
    assertTrue(authIdResult.exceptionOrNull() is SecurityException)

    // getAccessToken should return failure
    val tokenResult = authRepository.getAccessToken()
    assertTrue(tokenResult.isFailure)
  }

  @Test
  fun test5_scopeQuery_executesWithCurrentUserId() = runBlocking {
    val profile = authRepository.register(
      firstName = "Bekzod",
      lastName = "Ismoilov",
      emailOrPhone = "bekzod@ustoz.uz",
      password = "password123"
    ).getOrThrow()
    authRepository.setupPin("4444")

    val result = authRepository.scopeQuery { scopedUserId ->
      assertEquals(profile.userId, scopedUserId)
      Result.success("scoped_data_for_$scopedUserId")
    }

    assertTrue(result.isSuccess)
    assertEquals("scoped_data_for_${profile.userId}", result.getOrThrow())
  }
}
