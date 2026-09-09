package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.ClassDao
import com.example.data.local.SessionDao
import com.example.data.local.StudentDao
import com.example.data.local.TeacherProfileDao
import com.example.data.local.UstozDatabase
import com.example.data.model.ClassEntity
import com.example.data.model.ClassWithStudentCount
import com.example.data.model.ParsedStudentDraft
import com.example.data.model.ScheduleEntity
import com.example.data.model.StudentEntity
import com.example.data.model.TeacherProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class TeacherRepository(
  private val sessionDao: SessionDao,
  private val teacherProfileDao: TeacherProfileDao,
  private val classDao: ClassDao,
  private val studentDao: StudentDao,
  private val authRepository: AuthRepository? = null,
  private val database: UstozDatabase? = null
) {

  /**
   * Identifies the current authenticated user securely via AuthRepository or the active unlocked session.
   * Never trusts any client-supplied userId or teacherId.
   */
  private suspend fun getAuthenticatedTeacherId(): Result<String> {
    if (authRepository != null) {
      return authRepository.getAuthenticatedUserId()
    }
    val activeSession = sessionDao.getActiveSession()
      ?: return Result.failure(SecurityException("401 Unauthorized: Faol sessiya topilmadi"))
    if (!activeSession.isActive || !activeSession.isUnlocked) {
      return Result.failure(SecurityException("401 Unauthorized: Sessiya qulflangan yoki faol emas"))
    }
    if (activeSession.expiresAt < System.currentTimeMillis()) {
      sessionDao.deleteSession(activeSession.sessionId)
      return Result.failure(SecurityException("401 Unauthorized: Sessiya muddati tugagan"))
    }
    return Result.success(activeSession.userId)
  }

  suspend fun getCurrentTeacherId(): Result<String> = getAuthenticatedTeacherId()

  // ==========================================
  // TEACHER PROFILE
  // ==========================================

  suspend fun getProfile(): Result<TeacherProfileEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val profile = teacherProfileDao.getByUserId(teacherId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: O'qituvchi profili topilmadi"))
    return Result.success(profile)
  }

  suspend fun updateProfile(
    firstName: String,
    lastName: String,
    phone: String,
    email: String,
    language: String,
    schoolName: String,
    subject: String,
    experienceYears: Int,
    photo: String? = null
  ): Result<TeacherProfileEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val current = teacherProfileDao.getByUserId(teacherId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: O'qituvchi profili topilmadi"))

    if (firstName.isBlank() || lastName.isBlank()) {
      return Result.failure(IllegalArgumentException("Ism va familiya bo'sh bo'lishi mumkin emas"))
    }

    val updated = current.copy(
      firstName = firstName.trim(),
      lastName = lastName.trim(),
      phone = phone.trim(),
      email = email.trim(),
      language = language.trim(),
      school = schoolName.trim(),
      schoolName = schoolName.trim(),
      subject = subject.trim(),
      experienceYears = experienceYears,
      experience = if (experienceYears > 0) "$experienceYears yil" else current.experience,
      photo = photo ?: current.photo
    )
    teacherProfileDao.update(updated)
    return Result.success(updated)
  }

  // ==========================================
  // DASHBOARD STATS (REAL DATA ONLY)
  // ==========================================

  suspend fun getDashboardStats(): Result<Pair<Int, Int>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val classCount = classDao.countClasses(teacherId)
    val studentCount = studentDao.countAllStudents(teacherId)
    return Result.success(Pair(classCount, studentCount))
  }

  // ==========================================
  // CLASSES (STRICT TEACHER OWNERSHIP)
  // ==========================================

  suspend fun getClasses(): Result<List<ClassWithStudentCount>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val classes = classDao.getClasses(teacherId)
    val listWithCounts = classes.map { cls ->
      val count = studentDao.countStudentsInClass(cls.id, teacherId)
      ClassWithStudentCount(cls, count)
    }
    return Result.success(listWithCounts)
  }

  suspend fun createClass(
    grade: Int,
    section: String,
    academicYear: String
  ): Result<ClassEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    if (grade !in 1..11) {
      return Result.failure(IllegalArgumentException("Sinf 1 dan 11 gacha bo'lishi kerak"))
    }
    val cleanSection = section.trim().uppercase()
    if (cleanSection.isBlank()) {
      return Result.failure(IllegalArgumentException("Sinf harfi (section) kiritilishi shart"))
    }
    val cleanAcademicYear = academicYear.trim().ifBlank { "2024-2025" }
    val className = "$grade-$cleanSection"

    val classId = "cls_" + UUID.randomUUID().toString().replace("-", "").take(10)
    val newClass = ClassEntity(
      id = classId,
      teacherId = teacherId,
      name = className,
      grade = grade,
      section = cleanSection,
      academicYear = cleanAcademicYear,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )
    classDao.insert(newClass)
    return Result.success(newClass)
  }

  suspend fun createClassWithSchedule(
    grade: Int,
    section: String,
    academicYear: String,
    subject: String,
    slots: List<DayScheduleSlot>
  ): Result<Pair<ClassEntity, List<ScheduleEntity>>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    if (grade !in 1..11) {
      return Result.failure(IllegalArgumentException("Sinf 1 dan 11 gacha bo'lishi kerak"))
    }
    val cleanSection = section.trim().uppercase()
    if (cleanSection.isBlank()) {
      return Result.failure(IllegalArgumentException("Sinf harfi (section) kiritilishi shart"))
    }
    val cleanAcademicYear = academicYear.trim().ifBlank { "2024-2025" }
    val cleanSubject = subject.trim().ifBlank { "Asosiy fan" }

    for (slot in slots) {
      if (slot.dayOfWeek !in 1..7) {
        return Result.failure(IllegalArgumentException("Hafta kuni noto'g'ri (1..6 oraliqda bo'lishi kerak)"))
      }
      if (slot.startTime.isBlank() || slot.endTime.isBlank()) {
        return Result.failure(IllegalArgumentException("Dars vaqtlari bo'sh bo'lishi mumkin emas"))
      }
      if (slot.lessonNumber !in 1..8) {
        return Result.failure(IllegalArgumentException("Para raqami 1 dan 8 gacha bo'lishi kerak"))
      }
    }

    val className = "$grade-$cleanSection"
    val classId = "cls_" + UUID.randomUUID().toString().replace("-", "").take(10)
    val newClass = ClassEntity(
      id = classId,
      teacherId = teacherId,
      name = className,
      grade = grade,
      section = cleanSection,
      academicYear = cleanAcademicYear,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )

    val scheduleEntities = slots.map { slot ->
      ScheduleEntity(
        id = "sch_" + UUID.randomUUID().toString().replace("-", "").take(10),
        teacherId = teacherId,
        classId = classId,
        subjectId = cleanSubject,
        dayOfWeek = slot.dayOfWeek,
        startTime = slot.startTime.trim(),
        endTime = slot.endTime.trim(),
        room = slot.room.trim().ifBlank { "Sinfxona" },
        lessonNumber = slot.lessonNumber,
        active = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
      )
    }

    if (database != null) {
      database.withTransaction {
        classDao.insert(newClass)
        val sDao = database.scheduleDao()
        scheduleEntities.forEach { sDao.insert(it) }
      }
    } else {
      classDao.insert(newClass)
    }

    return Result.success(Pair(newClass, scheduleEntities))
  }

  suspend fun getClassById(classId: String): Result<ClassEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val cls = classDao.getById(classId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Sinf topilmadi"))

    // Strict Cross-Access Ownership Check
    if (cls.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu sinf boshqa o'qituvchiga tegishli"))
    }
    return Result.success(cls)
  }

  suspend fun updateClass(
    classId: String,
    grade: Int,
    section: String,
    academicYear: String
  ): Result<ClassEntity> {
    val existing = getClassById(classId).getOrElse { return Result.failure(it) }
    val cleanSection = section.trim().uppercase()
    val updated = existing.copy(
      grade = grade,
      section = cleanSection,
      name = "$grade-$cleanSection",
      academicYear = academicYear.trim(),
      updatedAt = System.currentTimeMillis()
    )
    classDao.update(updated)
    return Result.success(updated)
  }

  suspend fun deleteClass(classId: String): Result<Unit> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    // Verify ownership first
    getClassById(classId).getOrElse { return Result.failure(it) }
    val rows = classDao.delete(classId, teacherId)
    return if (rows > 0) Result.success(Unit)
    else Result.failure(IllegalStateException("Sinf o'chirilmadi"))
  }

  // ==========================================
  // STUDENTS (STRICT TEACHER & CLASS OWNERSHIP)
  // ==========================================

  suspend fun getStudents(
    classId: String? = null,
    searchQuery: String = ""
  ): Result<List<StudentEntity>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }

    val students = when {
      !classId.isNullOrBlank() -> {
        // Validate class ownership before querying students
        getClassById(classId).getOrElse { return Result.failure(it) }
        studentDao.getStudentsByClass(classId, teacherId)
      }
      searchQuery.isNotBlank() -> {
        studentDao.searchStudents(teacherId, searchQuery.trim())
      }
      else -> {
        studentDao.getStudents(teacherId)
      }
    }

    val filtered = if (!classId.isNullOrBlank() && searchQuery.isNotBlank()) {
      val q = searchQuery.trim().lowercase()
      students.filter {
        it.firstName.lowercase().contains(q) ||
            it.lastName.lowercase().contains(q) ||
            it.studentNumber.lowercase().contains(q)
      }
    } else {
      students
    }

    return Result.success(filtered)
  }

  suspend fun createStudent(
    classId: String,
    firstName: String,
    lastName: String,
    studentNumber: String,
    birthDate: String,
    gender: String,
    phone: String = ""
  ): Result<StudentEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }

    // Validate class belongs to current teacher
    getClassById(classId).getOrElse { return Result.failure(it) }

    if (firstName.isBlank() || lastName.isBlank()) {
      return Result.failure(IllegalArgumentException("O'quvchi ismi va familiyasi kiritilishi shart"))
    }

    val cleanNumber = studentNumber.trim().ifBlank {
      "ST-" + (1000 + (Math.random() * 9000).toInt())
    }

    val studentId = "std_" + UUID.randomUUID().toString().replace("-", "").take(10)
    val newStudent = StudentEntity(
      id = studentId,
      classId = classId,
      teacherId = teacherId,
      firstName = firstName.trim(),
      lastName = lastName.trim(),
      studentNumber = cleanNumber,
      birthDate = birthDate.trim(),
      gender = gender.trim(),
      phone = phone.trim(),
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )
    studentDao.insert(newStudent)
    return Result.success(newStudent)
  }

  suspend fun getStudentById(studentId: String): Result<StudentEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val student = studentDao.getById(studentId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: O'quvchi topilmadi"))

    // Strict Cross-Access Ownership Check
    if (student.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu o'quvchi boshqa o'qituvchiga tegishli"))
    }
    return Result.success(student)
  }

  suspend fun updateStudent(
    studentId: String,
    firstName: String,
    lastName: String,
    studentNumber: String,
    birthDate: String,
    gender: String,
    phone: String = "",
    classId: String? = null
  ): Result<StudentEntity> {
    val existing = getStudentById(studentId).getOrElse { return Result.failure(it) }

    val targetClassId = if (!classId.isNullOrBlank() && classId != existing.classId) {
      // Verify target class ownership
      getClassById(classId).getOrElse { return Result.failure(it) }
      classId
    } else {
      existing.classId
    }

    val updated = existing.copy(
      classId = targetClassId,
      firstName = firstName.trim(),
      lastName = lastName.trim(),
      studentNumber = studentNumber.trim().ifBlank { existing.studentNumber },
      birthDate = birthDate.trim(),
      gender = gender.trim(),
      phone = phone.trim(),
      updatedAt = System.currentTimeMillis()
    )
    studentDao.update(updated)
    return Result.success(updated)
  }

  suspend fun deleteStudent(studentId: String): Result<Unit> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    // Verify ownership first
    getStudentById(studentId).getOrElse { return Result.failure(it) }
    val rows = studentDao.delete(studentId, teacherId)
    return if (rows > 0) Result.success(Unit)
    else Result.failure(IllegalStateException("O'quvchi o'chirilmadi"))
  }

  /**
   * Bulk imports parsed student drafts into a class within a Room database transaction.
   * Enforces strict teacher ownership of class and students.
   */
  suspend fun importStudents(
    classId: String,
    drafts: List<ParsedStudentDraft>
  ): Result<Int> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }

    // Validate class belongs to current teacher
    getClassById(classId).getOrElse { return Result.failure(it) }

    val toImport = drafts.filter { it.isSelectedForImport && it.firstName.isNotBlank() }
    if (toImport.isEmpty()) {
      return Result.failure(IllegalArgumentException("Import qilish uchun birorta ham o'quvchi tanlanmadi."))
    }

    val existingStudents = studentDao.getStudentsByClass(classId, teacherId)
    var nextNumberSeq = existingStudents.size + 1
    val now = System.currentTimeMillis()

    val entities = toImport.map { draft ->
      val cleanNumber = draft.studentNumber.trim().ifBlank {
        "ST-" + (1000 + nextNumberSeq++)
      }
      val studentId = "std_" + UUID.randomUUID().toString().replace("-", "").take(10)
      StudentEntity(
        id = studentId,
        classId = classId,
        teacherId = teacherId,
        firstName = draft.firstName.trim(),
        lastName = draft.lastName.trim(),
        studentNumber = cleanNumber,
        birthDate = draft.birthDate.trim(),
        gender = draft.gender.trim(),
        phone = draft.phone.trim(),
        createdAt = now,
        updatedAt = now
      )
    }

    try {
      if (database != null) {
        database.withTransaction {
          studentDao.insertAll(entities)
        }
      } else {
        studentDao.insertAll(entities)
      }
    } catch (e: Exception) {
      return Result.failure(e)
    }

    return Result.success(entities.size)
  }
}
