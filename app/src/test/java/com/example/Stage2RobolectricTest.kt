package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UstozDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.TeacherRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Stage2RobolectricTest {

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
      studentDao = database.studentDao()
    )
  }

  @After
  fun tearDown() {
    database.close()
  }

  private suspend fun registerAndUnlock(
    firstName: String,
    lastName: String,
    email: String,
    subject: String = "Matematika",
    pin: String = "1234"
  ): String {
    val reg = authRepository.register(
      firstName = firstName,
      lastName = lastName,
      emailOrPhone = email,
      password = "securePassword123",
      subject = subject
    ).getOrThrow()
    authRepository.setupPin(pin)
    return reg.userId
  }

  @Test
  fun test1_authenticatedUserLoadsProfile() = runBlocking {
    // 1. Authenticated user -> Teacher Profile yuklanadi
    val userId = registerAndUnlock(
      firstName = "Dilnoza",
      lastName = "Karimova",
      email = "dilnoza@maktab.uz",
      subject = "Ona tili va adabiyot"
    )

    val profileResult = teacherRepository.getProfile()
    assertTrue("Profil muvaffaqiyatli yuklanishi kerak", profileResult.isSuccess)

    val profile = profileResult.getOrThrow()
    assertEquals(userId, profile.userId)
    assertEquals("Dilnoza", profile.firstName)
    assertEquals("Karimova", profile.lastName)
    assertEquals("Dilnoza Karimova", profile.fullName)
    assertEquals("Ona tili va adabiyot", profile.subject)
  }

  @Test
  fun test2_profileEditUpdatesDatabase() = runBlocking {
    // 2. Profile edit -> DB'da yangilanadi -> qayta chaqirganda yangi ma'lumot chiqadi
    registerAndUnlock(
      firstName = "Otabek",
      lastName = "Nazarov",
      email = "otabek@maktab.uz",
      subject = "Fizika"
    )

    val updateResult = teacherRepository.updateProfile(
      firstName = "Otabekjon",
      lastName = "Nazarov",
      phone = "+998901234567",
      email = "otabek.new@maktab.uz",
      language = "O'zbekcha",
      schoolName = "34-maktab",
      subject = "Fizika va Astronomiya",
      experienceYears = 8
    )
    assertTrue("Profil yangilanishi muvaffaqiyatli bo'lishi kerak", updateResult.isSuccess)

    val updatedProfile = updateResult.getOrThrow()
    assertEquals("Otabekjon", updatedProfile.firstName)
    assertEquals("34-maktab", updatedProfile.effectiveSchoolName)
    assertEquals("+998901234567", updatedProfile.effectivePhone)
    assertEquals("Fizika va Astronomiya", updatedProfile.subject)
    assertEquals(8, updatedProfile.experienceYears)

    // DB'dan qayta o'qilganda ham yangilangan qiymatlar chiqishi kerak
    val reloaded = teacherRepository.getProfile().getOrThrow()
    assertEquals("Otabekjon Nazarov", reloaded.fullName)
    assertEquals(8, reloaded.experienceYears)
    assertEquals("34-maktab", reloaded.effectiveSchoolName)
  }

  @Test
  fun test3_createClassTiedToTeacher() = runBlocking {
    // 3. Create Class -> joriy o'qituvchining teacherId bilan DB ga saqlanadi
    val teacherId = registerAndUnlock(
      firstName = "Zulayho",
      lastName = "Sobirova",
      email = "zulayho@maktab.uz"
    )

    val classResult = teacherRepository.createClass(
      grade = 7,
      section = "A",
      academicYear = "2024-2025"
    )
    assertTrue("Sinf yaratilishi muvaffaqiyatli bo'lishi kerak", classResult.isSuccess)

    val createdClass = classResult.getOrThrow()
    assertEquals("7-A", createdClass.name)
    assertEquals(7, createdClass.grade)
    assertEquals("A", createdClass.section)
    assertEquals(teacherId, createdClass.teacherId)

    // Database'dagi class yozuvini tekshirish
    val classInDb = database.classDao().getById(createdClass.id)
    assertNotNull(classInDb)
    assertEquals(teacherId, classInDb?.teacherId)
  }

  @Test
  fun test4_classListShowsOnlyCurrentTeacherClasses() = runBlocking {
    // 4. Class List: Teacher A 7-A va 7-B ni ko'radi, Teacher B esa 8-B ni ko'radi
    // Setup Teacher A
    val teacherAId = registerAndUnlock("Teacher", "A", "teacherA@maktab.uz", pin = "1111")
    val class7A = teacherRepository.createClass(7, "A", "2024-2025").getOrThrow()
    val class7B = teacherRepository.createClass(7, "B", "2024-2025").getOrThrow()

    val classesForA = teacherRepository.getClasses().getOrThrow()
    assertEquals(2, classesForA.size)
    val namesForA = classesForA.map { it.classEntity.name }
    assertTrue(namesForA.contains("7-A"))
    assertTrue(namesForA.contains("7-B"))

    // Teacher A logout -> Teacher B register & login
    authRepository.logout()
    val teacherBId = registerAndUnlock("Teacher", "B", "teacherB@maktab.uz", pin = "2222")
    val class8B = teacherRepository.createClass(8, "B", "2024-2025").getOrThrow()

    val classesForB = teacherRepository.getClasses().getOrThrow()
    assertEquals(1, classesForB.size)
    assertEquals("8-B", classesForB[0].classEntity.name)
    assertFalse(classesForB.any { it.classEntity.name == "7-A" || it.classEntity.name == "7-B" })
  }

  @Test
  fun test5_addStudentAndCountUpdates() = runBlocking {
    // 5. Add Student: sinfga o'quvchi qo'shilganda sinfdagi o'quvchilar soni yangilanadi
    registerAndUnlock("Jamshid", "Qodirov", "jamshid@maktab.uz")
    val class7A = teacherRepository.createClass(7, "A", "2024-2025").getOrThrow()

    // Dastlab o'quvchilar soni 0 bo'lishi kerak
    val initialClasses = teacherRepository.getClasses().getOrThrow()
    assertEquals(0, initialClasses[0].studentCount)

    // O'quvchi qo'shish
    val studentResult = teacherRepository.createStudent(
      classId = class7A.id,
      firstName = "Aziz",
      lastName = "Alimov",
      studentNumber = "ST-1001",
      birthDate = "2011-04-12",
      gender = "Erkak",
      phone = "+998911112233"
    )
    assertTrue("O'quvchi yaratilishi muvaffaqiyatli bo'lishi kerak", studentResult.isSuccess)

    val student = studentResult.getOrThrow()
    assertEquals("Aziz Alimov", student.fullName)
    assertEquals(class7A.id, student.classId)

    // Sinfdagi o'quvchilar soni 1 ga teng bo'lishi kerak
    val updatedClasses = teacherRepository.getClasses().getOrThrow()
    assertEquals(1, updatedClasses[0].studentCount)

    // Dashboard stats (real classCount = 1, studentCount = 1)
    val (classCount, studentCount) = teacherRepository.getDashboardStats().getOrThrow()
    assertEquals(1, classCount)
    assertEquals(1, studentCount)
  }

  @Test
  fun test6_searchAndFilterStudents() = runBlocking {
    // 6. O'quvchilarni qidirish va sinf bo'yicha filtrlash
    registerAndUnlock("Madina", "Saidova", "madina@maktab.uz")
    val class9A = teacherRepository.createClass(9, "A", "2024-2025").getOrThrow()
    val class9B = teacherRepository.createClass(9, "B", "2024-2025").getOrThrow()

    teacherRepository.createStudent(class9A.id, "Sherzod", "Toirov", "ST-2001", "2010-01-01", "Erkak")
    teacherRepository.createStudent(class9A.id, "Nodira", "Kamalova", "ST-2002", "2010-02-02", "Ayol")
    teacherRepository.createStudent(class9B.id, "Bobur", "Shermatov", "ST-2003", "2010-03-03", "Erkak")

    // Filter by 9-A
    val students9A = teacherRepository.getStudents(classId = class9A.id).getOrThrow()
    assertEquals(2, students9A.size)
    assertTrue(students9A.all { it.classId == class9A.id })

    // Filter by 9-B
    val students9B = teacherRepository.getStudents(classId = class9B.id).getOrThrow()
    assertEquals(1, students9B.size)
    assertEquals("Bobur Shermatov", students9B[0].fullName)

    // Search query "Kamalova"
    val searchKamalova = teacherRepository.getStudents(searchQuery = "Kamalova").getOrThrow()
    assertEquals(1, searchKamalova.size)
    assertEquals("Nodira Kamalova", searchKamalova[0].fullName)

    // Search query by student ID "ST-2003"
    val searchId = teacherRepository.getStudents(searchQuery = "ST-2003").getOrThrow()
    assertEquals(1, searchId.size)
    assertEquals("Bobur Shermatov", searchId[0].fullName)
  }

  @Test
  fun test7_strictDataIsolationBetweenTeachers() = runBlocking {
    // 7. MANDATORY DATA ISOLATION TEST:
    // Teacher A: Class 7-A, Student Aziz
    // Teacher B: Class 8-B, Student Bobur
    // Teacher A -> faqat 7-A va Aziz. 8-B va Bobur ko'rinmaydi!
    // Teacher B -> faqat 8-B va Bobur. 7-A va Aziz ko'rinmaydi!

    // Teacher A ro'yxatdan o'tadi va ma'lumot yaratadi
    val teacherAId = registerAndUnlock("Teacher", "A", "teacherA@test.uz", pin = "1111")
    val class7A = teacherRepository.createClass(7, "A", "2024-2025").getOrThrow()
    val studentAziz = teacherRepository.createStudent(
      classId = class7A.id,
      firstName = "Aziz",
      lastName = "Alimov",
      studentNumber = "ST-A01",
      birthDate = "2012-01-01",
      gender = "Erkak"
    ).getOrThrow()

    // Teacher A tekshiruvi
    val classesA = teacherRepository.getClasses().getOrThrow()
    val studentsA = teacherRepository.getStudents().getOrThrow()
    assertEquals(1, classesA.size)
    assertEquals("7-A", classesA[0].classEntity.name)
    assertEquals(1, studentsA.size)
    assertEquals("Aziz Alimov", studentsA[0].fullName)

    // Teacher A logout qiladi
    authRepository.logout()

    // Teacher B ro'yxatdan o'tadi va ma'lumot yaratadi
    val teacherBId = registerAndUnlock("Teacher", "B", "teacherB@test.uz", pin = "2222")
    val class8B = teacherRepository.createClass(8, "B", "2024-2025").getOrThrow()
    val studentBobur = teacherRepository.createStudent(
      classId = class8B.id,
      firstName = "Bobur",
      lastName = "Nematov",
      studentNumber = "ST-B01",
      birthDate = "2011-05-05",
      gender = "Erkak"
    ).getOrThrow()

    // Teacher B tekshiruvi: Teacher A ma'lumotlari zarracha ko'rinmasligi SHART!
    val classesB = teacherRepository.getClasses().getOrThrow()
    val studentsB = teacherRepository.getStudents().getOrThrow()

    assertEquals(1, classesB.size)
    assertEquals("8-B", classesB[0].classEntity.name)
    assertNotEquals("7-A", classesB[0].classEntity.name)

    assertEquals(1, studentsB.size)
    assertEquals("Bobur Nematov", studentsB[0].fullName)
    assertNotEquals("Aziz Alimov", studentsB[0].fullName)

    // Teacher B dagi barcha sinflar va o'quvchilar teacherBId ga tegishli bo'lishi shart
    assertTrue(classesB.all { it.classEntity.teacherId == teacherBId })
    assertTrue(studentsB.all { it.teacherId == teacherBId })
  }

  @Test
  fun test8_crossAccessIsRejectedWithSecurityException() = runBlocking {
    // 8. MANDATORY CROSS-ACCESS TEST:
    // Teacher B Teacher A'ning class ID'sini so'raganda: 403 Forbidden / SecurityException
    // Teacher B Teacher A'ning student ID'sini so'raganda: 403 Forbidden / SecurityException

    // Teacher A
    registerAndUnlock("Teacher", "A", "teacherA@cross.uz", pin = "1111")
    val classA = teacherRepository.createClass(10, "A", "2024-2025").getOrThrow()
    val studentA = teacherRepository.createStudent(
      classId = classA.id,
      firstName = "Jasur",
      lastName = "Qodirov",
      studentNumber = "ST-CROSS-01",
      birthDate = "2009-08-08",
      gender = "Erkak"
    ).getOrThrow()

    authRepository.logout()

    // Teacher B
    registerAndUnlock("Teacher", "B", "teacherB@cross.uz", pin = "2222")

    // Teacher B attempts to query Teacher A's class by ID
    val classAccessResult = teacherRepository.getClassById(classA.id)
    assertFalse("Teacher B Teacher A'ning sinfiga kira olmasligi shart", classAccessResult.isSuccess)
    val classEx = classAccessResult.exceptionOrNull()
    assertTrue(
      "Xatolik SecurityException bo'lishi kerak",
      classEx is SecurityException && classEx.message?.contains("403") == true
    )

    // Teacher B attempts to query Teacher A's student by ID
    val studentAccessResult = teacherRepository.getStudentById(studentA.id)
    assertFalse("Teacher B Teacher A'ning o'quvchisiga kira olmasligi shart", studentAccessResult.isSuccess)
    val studentEx = studentAccessResult.exceptionOrNull()
    assertTrue(
      "Xatolik SecurityException bo'lishi kerak",
      studentEx is SecurityException && studentEx.message?.contains("403") == true
    )

    // Teacher B attempts to add a student to Teacher A's class
    val hijackStudentResult = teacherRepository.createStudent(
      classId = classA.id,
      firstName = "Hacker",
      lastName = "Student",
      studentNumber = "ST-HACK",
      birthDate = "2010-01-01",
      gender = "Erkak"
    )
    assertFalse("Teacher B Teacher A'ning sinfiga o'quvchi qo'sha olmasligi shart", hijackStudentResult.isSuccess)
  }

  @Test
  fun test9_logoutLoginSwitchDataIsolation() = runBlocking {
    // 9. LOGOUT -> LOGIN CYCLE:
    // Teacher A login -> A data -> Logout -> Teacher B login -> B data -> No leak
    val teacherAId = registerAndUnlock("Alisher", "Navoiy", "alisher@switch.uz", pin = "1111")
    val classA = teacherRepository.createClass(5, "A", "2024-2025").getOrThrow()
    teacherRepository.createStudent(classA.id, "Ulug'bek", "Mirzo", "ST-SW1", "2013-01-01", "Erkak")

    authRepository.logout()

    val teacherBId = registerAndUnlock("Bobur", "Zahiriddin", "bobur@switch.uz", pin = "2222")
    val classB = teacherRepository.createClass(6, "B", "2024-2025").getOrThrow()

    val (classCountB, studentCountB) = teacherRepository.getDashboardStats().getOrThrow()
    assertEquals(1, classCountB)
    assertEquals(0, studentCountB)

    // Logout B, Login A again
    authRepository.logout()
    val loginAResult = authRepository.login("alisher@switch.uz", "securePassword123").getOrThrow()
    authRepository.unlockWithPin("1111")

    val profileA = teacherRepository.getProfile().getOrThrow()
    assertEquals(teacherAId, profileA.userId)
    assertEquals("Alisher", profileA.firstName)

    val (classCountA, studentCountA) = teacherRepository.getDashboardStats().getOrThrow()
    assertEquals(1, classCountA)
    assertEquals(1, studentCountA)

    val studentsA = teacherRepository.getStudents().getOrThrow()
    assertEquals(1, studentsA.size)
    assertEquals("Ulug'bek Mirzo", studentsA[0].fullName)
  }
}
