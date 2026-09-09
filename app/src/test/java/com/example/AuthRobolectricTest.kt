package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UstozDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.SessionStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthRobolectricTest {

  private lateinit var database: UstozDatabase
  private lateinit var repository: AuthRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // In-memory database for isolated, reproducible tests
    database = Room.inMemoryDatabaseBuilder(context, UstozDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    repository = AuthRepository(
      userDao = database.userDao(),
      teacherProfileDao = database.teacherProfileDao(),
      sessionDao = database.sessionDao()
    )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun test1_teacherARegister_createsUserIdAndProfile() = runBlocking {
    // TEST 1: Teacher A register -> User ID yaratiladi -> Teacher Profile yaratiladi
    val result = repository.register(
      firstName = "Alisher",
      lastName = "Navoiy",
      emailOrPhone = "alisher@ustoz.uz",
      password = "secretPassword123",
      language = "O'zbekcha",
      subject = "Adabiyot",
      school = "1-IDUM"
    )

    assertTrue("Ro'yxatdan o'tish muvaffaqiyatli bo'lishi kerak", result.isSuccess)
    val profile = result.getOrThrow()

    // Unikal User ID tekshiruvi
    assertNotNull(profile.userId)
    assertTrue("User ID ustoz_ prefiksiga ega bo'lishi kerak", profile.userId.startsWith("ustoz_"))
    assertEquals("Alisher", profile.firstName)
    assertEquals("Navoiy", profile.lastName)
    assertEquals("Adabiyot", profile.subject)

    // Database'dagi User va Profile o'zaro 1-ga-1 bog'langanligini tekshirish
    val userInDb = database.userDao().findById(profile.userId)
    assertNotNull("Foydalanuvchi users jadvalida mavjud bo'lishi kerak", userInDb)
    assertEquals("alisher@ustoz.uz", userInDb?.emailOrPhone)

    val profileInDb = database.teacherProfileDao().getByUserId(profile.userId)
    assertNotNull("Teacher profile jadvalida user ID bilan bog'langan yozuv bo'lishi kerak", profileInDb)
    assertEquals(profile.userId, profileInDb?.userId)
  }

  @Test
  fun test2_teacherALogin_loadsProfile() = runBlocking {
    // Setup Teacher A
    val regResult = repository.register(
      firstName = "Alisher",
      lastName = "Navoiy",
      emailOrPhone = "alisher@ustoz.uz",
      password = "secretPassword123",
      language = "O'zbekcha",
      subject = "Adabiyot"
    )
    val registeredProfile = regResult.getOrThrow()

    // O'rnatilgan PIN
    repository.setupPin("1234")

    // Logout qilib, qayta login qilish
    repository.logout()

    // TEST 2: Teacher A login -> A profile yuklanadi
    val loginResult = repository.login("alisher@ustoz.uz", "secretPassword123")
    assertTrue("Login muvaffaqiyatli bo'lishi kerak", loginResult.isSuccess)

    val sessionStatus = loginResult.getOrThrow()
    assertTrue("Login qilgach, PIN bilan ochish talab qilinadi", sessionStatus is SessionStatus.Locked)

    // PIN bilan ochish
    val unlockResult = repository.unlockWithPin("1234")
    assertTrue(unlockResult.isSuccess)

    val loadedProfile = unlockResult.getOrThrow()
    assertEquals(registeredProfile.userId, loadedProfile.userId)
    assertEquals("Alisher", loadedProfile.firstName)
    assertEquals("Adabiyot", loadedProfile.subject)
  }

  @Test
  fun test3_teacherALogout_invalidatesSession() = runBlocking {
    // Setup Teacher A
    repository.register(
      firstName = "Alisher",
      lastName = "Navoiy",
      emailOrPhone = "alisher@ustoz.uz",
      password = "secretPassword123"
    )
    repository.setupPin("1234")

    // TEST 3: Teacher A logout -> session tugaydi
    repository.logout()

    val sessionStatus = repository.checkSession()
    assertTrue("Sessiya NoSession bo'lishi kerak", sessionStatus is SessionStatus.NoSession)

    val currentProfile = repository.getCurrentTeacherProfile()
    assertNull("Chiqilgandan so'ng aktiv profil mavjud bo'lmasligi kerak", currentProfile)

    val activeSession = database.sessionDao().getActiveSession()
    assertNull("Database'da faol sessiya qolmasligi kerak", activeSession)
  }

  @Test
  fun test4_teacherBLogin_loadsBProfile_notAProfile() = runBlocking {
    // 1. Teacher A register & setup
    val regA = repository.register(
      firstName = "O'qituvchi A",
      lastName = "Ahmadov",
      emailOrPhone = "teacherA@maktab.uz",
      password = "passwordA123",
      subject = "Matematika"
    ).getOrThrow()
    repository.setupPin("1111")
    repository.logout()

    // 2. Teacher B register & setup
    val regB = repository.register(
      firstName = "O'qituvchi B",
      lastName = "Boburiy",
      emailOrPhone = "teacherB@maktab.uz",
      password = "passwordB123",
      subject = "Informatika"
    ).getOrThrow()
    repository.setupPin("2222")

    // TEST 4: Teacher B login -> B profile yuklanadi -> A profile ko‘rinmaydi
    val currentProfile = repository.getCurrentTeacherProfile()
    assertNotNull(currentProfile)
    assertEquals("O'qituvchi B", currentProfile?.firstName)
    assertEquals("Informatika", currentProfile?.subject)
    assertEquals(regB.userId, currentProfile?.userId)

    // Teacher A ma'lumoti bilan taqqoslash: butunlay boshqa User ID bo'lishi shart!
    assertNotEquals(regA.userId, currentProfile?.userId)
    assertNotEquals("O'qituvchi A", currentProfile?.firstName)
    assertNotEquals("Matematika", currentProfile?.subject)
  }

  @Test
  fun test5_incorrectPassword_rejectsLogin() = runBlocking {
    repository.register(
      firstName = "Zulayho",
      lastName = "Sobirova",
      emailOrPhone = "zulayho@maktab.uz",
      password = "correctPassword123"
    )
    repository.logout()

    // TEST 5: Noto‘g‘ri password -> login rad etiladi
    val badPasswordResult = repository.login("zulayho@maktab.uz", "wrongPassword!")
    assertFalse("Noto'g'ri parol bilan kirish muvaffaqiyatsiz bo'lishi kerak", badPasswordResult.isSuccess)

    val nonExistentUserResult = repository.login("nobody@maktab.uz", "anyPassword")
    assertFalse("Mavjud bo'lmagan user login bo'lmasligi kerak", nonExistentUserResult.isSuccess)
  }

  @Test
  fun test6_incorrectPin_doesNotUnlock() = runBlocking {
    repository.register(
      firstName = "Sanjar",
      lastName = "Alimov",
      emailOrPhone = "sanjar@maktab.uz",
      password = "password123"
    )
    repository.setupPin("4321")

    // Ilovani qulflash
    repository.lockApp()

    // TEST 6: Noto‘g‘ri PIN -> app unlock bo‘lmaydi
    val wrongPinResult = repository.unlockWithPin("9999")
    assertFalse("Noto'g'ri PIN bilan ochilmasligi kerak", wrongPinResult.isSuccess)

    // Sessiya hali ham qulflangan holatda qolishi kerak
    val activeSession = repository.getActiveSession()
    assertNotNull(activeSession)
    assertFalse("Sessiya ochilmagan (isUnlocked = false) bo'lishi kerak", activeSession!!.isUnlocked)

    val profile = repository.getCurrentTeacherProfile()
    assertNull("Qulflangan holatda profil olinmasligi kerak", profile)
  }

  @Test
  fun test7_validPin_unlocksCurrentTeacherDashboard() = runBlocking {
    repository.register(
      firstName = "Sanjar",
      lastName = "Alimov",
      emailOrPhone = "sanjar@maktab.uz",
      password = "password123",
      subject = "Fizika"
    )
    repository.setupPin("4321")
    repository.lockApp()

    // TEST 7: Valid PIN -> current user dashboard ochiladi
    val validPinResult = repository.unlockWithPin("4321")
    assertTrue("To'g'ri PIN bilan ochilishi kerak", validPinResult.isSuccess)

    val unlockedProfile = validPinResult.getOrThrow()
    assertEquals("Sanjar", unlockedProfile.firstName)
    assertEquals("Fizika", unlockedProfile.subject)

    val sessionStatus = repository.checkSession()
    assertTrue("Sessiya Authenticated holatiga o'tishi kerak", sessionStatus is SessionStatus.Authenticated)
  }

  @Test
  fun test8_appRestart_restoresValidSession() = runBlocking {
    val regProfile = repository.register(
      firstName = "Malika",
      lastName = "Karimova",
      emailOrPhone = "malika@maktab.uz",
      password = "securePassword789",
      subject = "Kimyo"
    ).getOrThrow()
    repository.setupPin("5555")

    // TEST 8: App restart -> valid session bo‘lsa current user tiklanadi
    // Simulating app restart with a fresh Repository instance using same persisted DB
    val freshRepository = AuthRepository(
      userDao = database.userDao(),
      teacherProfileDao = database.teacherProfileDao(),
      sessionDao = database.sessionDao()
    )

    val restoredSession = freshRepository.checkSession()
    assertTrue("Aktiv sessiya tiklanishi kerak", restoredSession is SessionStatus.Authenticated)

    val authenticated = restoredSession as SessionStatus.Authenticated
    assertEquals(regProfile.userId, authenticated.userId)
    assertEquals("Malika", authenticated.profile.firstName)
    assertEquals("Kimyo", authenticated.profile.subject)
  }

  @Test
  fun test9_viewModelsInstantiation_viaAndroidViewModelFactory() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)

    // Verify AuthViewModel can be instantiated via default AndroidViewModelFactory
    val authViewModel = factory.create(com.example.ui.auth.AuthViewModel::class.java)
    assertNotNull("AuthViewModel instance should be created successfully", authViewModel)

    // Verify MainAppViewModel can be instantiated via default AndroidViewModelFactory
    val mainAppViewModel = factory.create(com.example.ui.main.MainAppViewModel::class.java)
    assertNotNull("MainAppViewModel instance should be created successfully", mainAppViewModel)
  }
}
