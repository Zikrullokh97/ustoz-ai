package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AttendanceDao
import com.example.data.local.ClassDao
import com.example.data.local.LessonDao
import com.example.data.local.ScheduleDao
import com.example.data.local.SessionDao
import com.example.data.local.StudentDao
import com.example.data.local.TeacherProfileDao
import com.example.data.local.UstozDatabase
import com.example.data.model.AttendanceDetectionState
import com.example.data.model.AttendanceEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceSummary
import com.example.data.model.BulkAttendanceRecord
import com.example.data.model.LessonEntity
import com.example.data.model.LessonStatus
import com.example.data.model.LessonWithDetails
import com.example.data.model.ScheduleEntity
import com.example.data.model.ScheduleWithClass
import com.example.data.model.StudentAttendanceItem
import com.example.data.model.StudentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class DayScheduleSlot(
  val dayOfWeek: Int, // 1 = Dushanba, 2 = Seshanba, ..., 6 = Shanba
  val lessonNumber: Int = 1,
  val startTime: String = "08:30",
  val endTime: String = "09:15",
  val room: String = "Sinfxona"
)

class LessonRepository(
  private val database: UstozDatabase,
  private val scheduleDao: ScheduleDao,
  private val lessonDao: LessonDao,
  private val attendanceDao: AttendanceDao,
  private val classDao: ClassDao,
  private val studentDao: StudentDao,
  private val sessionDao: SessionDao,
  private val teacherProfileDao: TeacherProfileDao,
  private val authRepository: AuthRepository? = null
) {

  /**
   * Strictly identifies the current authenticated teacher. Never trusts client-supplied teacherId.
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

  // ==========================================
  // SCHEDULE MANAGEMENT
  // ==========================================

  suspend fun getSchedules(): Result<List<ScheduleWithClass>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val schedules = scheduleDao.getSchedulesByTeacher(teacherId)
    val result = schedules.map { sch ->
      val cls = classDao.getById(sch.classId)
      ScheduleWithClass(schedule = sch, className = cls?.name ?: "Sinf")
    }
    return Result.success(result)
  }

  suspend fun getSchedulesByClass(classId: String): Result<List<ScheduleWithClass>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val cls = classDao.getById(classId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Sinf topilmadi"))
    if (cls.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu sinf boshqa o'qituvchiga tegishli"))
    }

    val schedules = scheduleDao.getSchedulesByClass(classId, teacherId)
    return Result.success(schedules.map { ScheduleWithClass(it, cls.name) })
  }

  suspend fun createSchedule(
    classId: String,
    subjectId: String,
    dayOfWeek: Int,
    startTime: String,
    endTime: String,
    room: String,
    lessonNumber: Int
  ): Result<ScheduleEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }

    // Validate class ownership
    val cls = classDao.getById(classId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Sinf topilmadi"))
    if (cls.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu sinf boshqa o'qituvchiga tegishli"))
    }

    // Validation
    if (dayOfWeek !in 1..7) {
      return Result.failure(IllegalArgumentException("Hafta kuni 1 dan 7 gacha bo'lishi kerak"))
    }
    if (startTime >= endTime) {
      return Result.failure(IllegalArgumentException("Dars boshlanish vaqti tugash vaqtidan oldin bo'lishi kerak"))
    }
    if (subjectId.isBlank()) {
      return Result.failure(IllegalArgumentException("Fan nomi kiritilishi shart"))
    }

    // Check for schedule conflict
    val conflicts = scheduleDao.findConflicts(
      teacherId = teacherId,
      dayOfWeek = dayOfWeek,
      startTime = startTime,
      endTime = endTime,
      lessonNumber = lessonNumber
    )
    if (conflicts.isNotEmpty()) {
      val conflict = conflicts.first()
      val confClass = classDao.getById(conflict.classId)?.name ?: "Boshqa sinf"
      return Result.failure(IllegalStateException("Bu vaqtda boshqa dars mavjud: $confClass, ${conflict.subjectId} (${conflict.startTime}-${conflict.endTime})"))
    }

    val scheduleId = "sch_" + UUID.randomUUID().toString().replace("-", "").take(10)
    val schedule = ScheduleEntity(
      id = scheduleId,
      teacherId = teacherId,
      classId = classId,
      subjectId = subjectId.trim(),
      dayOfWeek = dayOfWeek,
      startTime = startTime.trim(),
      endTime = endTime.trim(),
      room = room.trim().ifBlank { "Sinfxona" },
      lessonNumber = lessonNumber,
      active = true,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )
    scheduleDao.insert(schedule)
    return Result.success(schedule)
  }

  suspend fun createMultiDaySchedule(
    classId: String,
    subjectId: String,
    slots: List<DayScheduleSlot>
  ): Result<List<ScheduleEntity>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }

    // Validate class ownership
    val cls = classDao.getById(classId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Sinf topilmadi"))
    if (cls.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu sinf boshqa o'qituvchiga tegishli"))
    }

    if (subjectId.isBlank()) {
      return Result.failure(IllegalArgumentException("Fan nomi kiritilishi shart"))
    }
    if (slots.isEmpty()) {
      return Result.failure(IllegalArgumentException("Kamida bitta kun tanlanishi kerak"))
    }

    // Validate all slots
    for (slot in slots) {
      if (slot.dayOfWeek !in 1..7) {
        return Result.failure(IllegalArgumentException("Hafta kuni noto'g'ri: ${slot.dayOfWeek}"))
      }
      if (slot.startTime >= slot.endTime) {
        return Result.failure(IllegalArgumentException("Boshlanish vaqti tugash vaqtidan oldin bo'lishi kerak (${slot.startTime} - ${slot.endTime})"))
      }

      val conflicts = scheduleDao.findConflicts(
        teacherId = teacherId,
        dayOfWeek = slot.dayOfWeek,
        startTime = slot.startTime,
        endTime = slot.endTime,
        lessonNumber = slot.lessonNumber
      )
      if (conflicts.isNotEmpty()) {
        val conf = conflicts.first()
        val confClass = classDao.getById(conf.classId)?.name ?: "Boshqa sinf"
        val dayName = when (slot.dayOfWeek) {
          1 -> "Dushanba"
          2 -> "Seshanba"
          3 -> "Chorshanba"
          4 -> "Payshanba"
          5 -> "Juma"
          6 -> "Shanba"
          else -> "Yakshanba"
        }
        return Result.failure(IllegalStateException("$dayName kuni bu vaqtda boshqa dars mavjud: $confClass, ${conf.subjectId} (${conf.startTime}-${conf.endTime})"))
      }
    }

    val createdEntities = mutableListOf<ScheduleEntity>()
    database.withTransaction {
      for (slot in slots) {
        val scheduleId = "sch_" + UUID.randomUUID().toString().replace("-", "").take(10)
        val schedule = ScheduleEntity(
          id = scheduleId,
          teacherId = teacherId,
          classId = classId,
          subjectId = subjectId.trim(),
          dayOfWeek = slot.dayOfWeek,
          startTime = slot.startTime.trim(),
          endTime = slot.endTime.trim(),
          room = slot.room.trim().ifBlank { "Sinfxona" },
          lessonNumber = slot.lessonNumber,
          active = true,
          createdAt = System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis()
        )
        scheduleDao.insert(schedule)
        createdEntities.add(schedule)
      }
    }

    return Result.success(createdEntities)
  }

  suspend fun updateSchedule(
    scheduleId: String,
    classId: String,
    subjectId: String,
    dayOfWeek: Int,
    startTime: String,
    endTime: String,
    room: String,
    lessonNumber: Int
  ): Result<ScheduleEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val existing = scheduleDao.getById(scheduleId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Jadval topilmadi"))
    if (existing.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Jadval boshqa o'qituvchiga tegishli"))
    }

    val cls = classDao.getById(classId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Sinf topilmadi"))
    if (cls.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu sinf boshqa o'qituvchiga tegishli"))
    }

    if (dayOfWeek !in 1..7) {
      return Result.failure(IllegalArgumentException("Hafta kuni 1 dan 7 gacha bo'lishi kerak"))
    }
    if (startTime >= endTime) {
      return Result.failure(IllegalArgumentException("Dars boshlanish vaqti tugash vaqtidan oldin bo'lishi kerak"))
    }
    if (subjectId.isBlank()) {
      return Result.failure(IllegalArgumentException("Fan nomi kiritilishi shart"))
    }

    val conflicts = scheduleDao.findConflicts(
      teacherId = teacherId,
      dayOfWeek = dayOfWeek,
      startTime = startTime,
      endTime = endTime,
      lessonNumber = lessonNumber,
      excludeId = scheduleId
    )
    if (conflicts.isNotEmpty()) {
      val conflict = conflicts.first()
      val confClass = classDao.getById(conflict.classId)?.name ?: "Boshqa sinf"
      return Result.failure(IllegalStateException("Bu vaqtda boshqa dars mavjud: $confClass, ${conflict.subjectId} (${conflict.startTime}-${conflict.endTime})"))
    }

    val updated = existing.copy(
      classId = classId,
      subjectId = subjectId.trim(),
      dayOfWeek = dayOfWeek,
      startTime = startTime.trim(),
      endTime = endTime.trim(),
      room = room.trim().ifBlank { "Sinfxona" },
      lessonNumber = lessonNumber,
      updatedAt = System.currentTimeMillis()
    )
    scheduleDao.update(updated)
    return Result.success(updated)
  }

  suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val existing = scheduleDao.getById(scheduleId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Jadval topilmadi"))
    if (existing.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Jadval boshqa o'qituvchiga tegishli"))
    }

    val rows = scheduleDao.delete(scheduleId, teacherId)
    return if (rows > 0) Result.success(Unit)
    else Result.failure(IllegalStateException("Jadval o'chirilmadi"))
  }

  // ==========================================
  // LESSON MANAGEMENT
  // ==========================================

  suspend fun getTodayLessons(): Result<List<LessonWithDetails>> {
    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    return getLessonsByDate(today)
  }

  suspend fun getLessonsByDate(date: String): Result<List<LessonWithDetails>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val lessons = lessonDao.getLessonsByDate(teacherId, date)

    val listWithDetails = lessons.map { lesson ->
      val updatedLesson = syncLessonStatusWithCurrentTime(lesson)
      if (updatedLesson.status != lesson.status) {
        lessonDao.update(updatedLesson)
      }

      val cls = classDao.getById(updatedLesson.classId)
      val studentCount = studentDao.countStudentsInClass(updatedLesson.classId, teacherId)
      val attendances = attendanceDao.getByLesson(updatedLesson.id)

      val present = attendances.count { it.status == AttendanceStatus.PRESENT }
      val late = attendances.count { it.status == AttendanceStatus.LATE }
      val absent = attendances.count { it.status == AttendanceStatus.ABSENT }
      val excused = attendances.count { it.status == AttendanceStatus.EXCUSED }

      LessonWithDetails(
        lesson = updatedLesson,
        className = cls?.name ?: "Sinf",
        studentCount = studentCount,
        attendanceTaken = attendances.isNotEmpty(),
        presentCount = present,
        lateCount = late,
        absentCount = absent,
        excusedCount = excused
      )
    }
    return Result.success(listWithDetails)
  }

  suspend fun getLessonById(lessonId: String): Result<LessonWithDetails> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val lesson = lessonDao.getById(lessonId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Dars topilmadi"))

    // Strict Cross-Access Ownership Check
    if (lesson.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu dars boshqa o'qituvchiga tegishli"))
    }

    val updatedLesson = syncLessonStatusWithCurrentTime(lesson)
    if (updatedLesson.status != lesson.status) {
      lessonDao.update(updatedLesson)
    }

    val cls = classDao.getById(updatedLesson.classId)
    val studentCount = studentDao.countStudentsInClass(updatedLesson.classId, teacherId)
    val attendances = attendanceDao.getByLesson(updatedLesson.id)

    val present = attendances.count { it.status == AttendanceStatus.PRESENT }
    val late = attendances.count { it.status == AttendanceStatus.LATE }
    val absent = attendances.count { it.status == AttendanceStatus.ABSENT }
    val excused = attendances.count { it.status == AttendanceStatus.EXCUSED }

    return Result.success(
      LessonWithDetails(
        lesson = updatedLesson,
        className = cls?.name ?: "Sinf",
        studentCount = studentCount,
        attendanceTaken = attendances.isNotEmpty(),
        presentCount = present,
        lateCount = late,
        absentCount = absent,
        excusedCount = excused
      )
    )
  }

  suspend fun createLesson(
    classId: String,
    subjectId: String,
    date: String,
    startTime: String,
    endTime: String,
    room: String,
    topic: String,
    scheduleId: String? = null
  ): Result<LessonEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }

    // Validate class belongs to current teacher
    val cls = classDao.getById(classId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Sinf topilmadi"))
    if (cls.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu sinf boshqa o'qituvchiga tegishli"))
    }

    if (startTime >= endTime) {
      return Result.failure(IllegalArgumentException("Dars boshlanish vaqti tugash vaqtidan oldin bo'lishi kerak"))
    }
    if (subjectId.isBlank()) {
      return Result.failure(IllegalArgumentException("Fan nomi kiritilishi shart"))
    }

    // Check duplicate prevention
    val existingDuplicate = lessonDao.findDuplicate(
      teacherId = teacherId,
      classId = classId,
      subjectId = subjectId.trim(),
      date = date.trim(),
      startTime = startTime.trim()
    )
    if (existingDuplicate != null) {
      return Result.success(existingDuplicate)
    }

    // Check time conflict with another lesson for the teacher
    val conflicts = lessonDao.findTimeConflicts(
      teacherId = teacherId,
      date = date.trim(),
      startTime = startTime.trim(),
      endTime = endTime.trim()
    )
    if (conflicts.isNotEmpty()) {
      return Result.failure(IllegalStateException("Bu vaqtda boshqa dars mavjud."))
    }

    val lessonId = "les_" + UUID.randomUUID().toString().replace("-", "").take(10)
    val initialStatus = computeLessonStatus(date.trim(), startTime.trim(), endTime.trim())

    val lesson = LessonEntity(
      id = lessonId,
      teacherId = teacherId,
      classId = classId,
      subjectId = subjectId.trim(),
      scheduleId = scheduleId,
      date = date.trim(),
      startTime = startTime.trim(),
      endTime = endTime.trim(),
      room = room.trim().ifBlank { "Sinfxona" },
      topic = topic.trim().ifBlank { "Mavzu belgilanmagan" },
      status = initialStatus,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )
    lessonDao.insert(lesson)
    return Result.success(lesson)
  }

  suspend fun deleteLesson(lessonId: String): Result<Unit> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val lesson = lessonDao.getById(lessonId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Dars topilmadi"))
    if (lesson.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu dars boshqa o'qituvchiga tegishli"))
    }
    val rows = lessonDao.delete(lessonId, teacherId)
    return if (rows > 0) Result.success(Unit)
    else Result.failure(IllegalStateException("Dars o'chirilmadi"))
  }

  suspend fun getLessonsByClass(classId: String): Result<List<LessonWithDetails>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val cls = classDao.getById(classId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Sinf topilmadi"))
    if (cls.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu sinf boshqa o'qituvchiga tegishli"))
    }

    val lessons = lessonDao.getLessonsByClass(classId, teacherId)
    val studentCount = studentDao.countStudentsInClass(classId, teacherId)

    val list = lessons.map { lesson ->
      val attendances = attendanceDao.getByLesson(lesson.id)
      LessonWithDetails(
        lesson = lesson,
        className = cls.name,
        studentCount = studentCount,
        attendanceTaken = attendances.isNotEmpty(),
        presentCount = attendances.count { it.status == AttendanceStatus.PRESENT },
        lateCount = attendances.count { it.status == AttendanceStatus.LATE },
        absentCount = attendances.count { it.status == AttendanceStatus.ABSENT },
        excusedCount = attendances.count { it.status == AttendanceStatus.EXCUSED }
      )
    }
    return Result.success(list)
  }

  // ==========================================
  // ATTENDANCE MANAGEMENT (DAVOMAT = LESSON + STUDENT)
  // ==========================================

  /**
   * Retrieves students of the lesson's class together with their current attendance record.
   * Enforces strict teacher and lesson ownership.
   */
  suspend fun getLessonAttendance(lessonId: String): Result<Pair<LessonWithDetails, List<StudentAttendanceItem>>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }

    val lesson = lessonDao.getById(lessonId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Dars topilmadi"))
    if (lesson.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu dars boshqa o'qituvchiga tegishli"))
    }

    val cls = classDao.getById(lesson.classId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Sinf topilmadi"))
    val students = studentDao.getStudentsByClass(lesson.classId, teacherId)
      .sortedWith(compareBy({ it.studentNumber }, { it.lastName }, { it.firstName }))

    val attendances = attendanceDao.getByLesson(lessonId)
    val attendanceMap = attendances.associateBy { it.studentId }

    val items = students.map { student ->
      val att = attendanceMap[student.id]
      StudentAttendanceItem(
        student = student,
        status = att?.status,
        lateMinutes = att?.lateMinutes,
        note = att?.note,
        source = att?.source ?: "MANUAL",
        detectionState = if (att != null) AttendanceDetectionState.DETECTED else AttendanceDetectionState.NOT_DETECTED,
        confidence = att?.confidence,
        manualOverride = att?.manualOverride ?: false,
        originalSource = att?.originalSource
      )
    }

    val present = attendances.count { it.status == AttendanceStatus.PRESENT }
    val late = attendances.count { it.status == AttendanceStatus.LATE }
    val absent = attendances.count { it.status == AttendanceStatus.ABSENT }
    val excused = attendances.count { it.status == AttendanceStatus.EXCUSED }

    val details = LessonWithDetails(
      lesson = lesson,
      className = cls.name,
      studentCount = students.size,
      attendanceTaken = attendances.isNotEmpty(),
      presentCount = present,
      lateCount = late,
      absentCount = absent,
      excusedCount = excused
    )

    return Result.success(Pair(details, items))
  }

  /**
   * Saves attendance records for a lesson in an atomic database transaction.
   * Enforces:
   * 1. Current teacher ownership of the lesson.
   * 2. All students belong to the teacher AND to the lesson's class.
   * 3. Database uniqueness constraint: UNIQUE(lessonId, studentId) - updates existing, inserts new.
   */
  suspend fun saveBulkAttendance(
    lessonId: String,
    records: List<BulkAttendanceRecord>
  ): Result<LessonWithDetails> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }

    // 1. Verify Lesson Ownership
    val lesson = lessonDao.getById(lessonId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: Dars topilmadi"))
    if (lesson.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu dars boshqa o'qituvchiga tegishli"))
    }

    // 2. Validate all student ownerships
    for (record in records) {
      val student = studentDao.getById(record.studentId)
        ?: return Result.failure(NoSuchElementException("404 Not Found: O'quvchi topilmadi (ID: ${record.studentId})"))
      if (student.teacherId != teacherId || student.classId != lesson.classId) {
        return Result.failure(SecurityException("403 Forbidden: O'quvchi dars sinfiga yoki o'qituvchiga tegishli emas"))
      }
    }

    // 3. Execute Transaction
    try {
      database.withTransaction {
        val existingAttendances = attendanceDao.getByLesson(lessonId).associateBy { it.studentId }
        val now = System.currentTimeMillis()

        val entitiesToSave = records.map { record ->
          val existing = existingAttendances[record.studentId]
          val cleanLate = if (record.status == AttendanceStatus.LATE) {
            record.lateMinutes ?: 5
          } else {
            null
          }
          val cleanNote = if (record.status == AttendanceStatus.ABSENT || record.status == AttendanceStatus.EXCUSED) {
            record.note?.trim()
          } else {
            null
          }

          if (existing != null) {
            existing.copy(
              status = record.status,
              lateMinutes = cleanLate,
              note = cleanNote,
              source = record.source,
              manualOverride = record.manualOverride,
              originalSource = record.originalSource ?: existing.source,
              confidence = record.confidence,
              updatedAt = now
            )
          } else {
            AttendanceEntity(
              id = "att_" + UUID.randomUUID().toString().replace("-", "").take(10),
              lessonId = lessonId,
              studentId = record.studentId,
              status = record.status,
              lateMinutes = cleanLate,
              note = cleanNote,
              source = record.source,
              manualOverride = record.manualOverride,
              originalSource = record.originalSource,
              confidence = record.confidence,
              createdAt = now,
              updatedAt = now
            )
          }
        }

        attendanceDao.insertAll(entitiesToSave)
      }
    } catch (e: Exception) {
      return Result.failure(e)
    }

    // Return updated Lesson details
    return getLessonById(lessonId)
  }

  suspend fun getStudentAttendanceHistory(studentId: String): Result<List<AttendanceEntity>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val student = studentDao.getById(studentId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: O'quvchi topilmadi"))
    if (student.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu o'quvchi boshqa o'qituvchiga tegishli"))
    }
    val history = attendanceDao.getByStudent(studentId)
    return Result.success(history)
  }

  /**
   * Calculates pending attendance count for real KPI in Dashboard.
   */
  suspend fun getPendingAttendanceCount(date: String = LocalDate.now().toString()): Result<Int> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val lessons = lessonDao.getLessonsByDate(teacherId, date)
    var pendingCount = 0
    for (lesson in lessons) {
      if (lesson.status != LessonStatus.CANCELLED) {
        val count = attendanceDao.countAttendanceForLesson(lesson.id)
        if (count == 0) {
          pendingCount++
        }
      }
    }
    return Result.success(pendingCount)
  }

  // ==========================================
  // HELPERS FOR TIME & STATUS COMPUTATION
  // ==========================================

  private fun computeLessonStatus(dateStr: String, startTimeStr: String, endTimeStr: String): LessonStatus {
    val today = LocalDate.now()
    val lessonDate = try {
      LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: Exception) {
      today
    }

    if (lessonDate.isBefore(today)) return LessonStatus.COMPLETED
    if (lessonDate.isAfter(today)) return LessonStatus.UPCOMING

    val now = LocalTime.now()
    val start = try { LocalTime.parse(startTimeStr) } catch (_: Exception) { LocalTime.MIN }
    val end = try { LocalTime.parse(endTimeStr) } catch (_: Exception) { LocalTime.MAX }

    return when {
      now.isBefore(start) -> LessonStatus.UPCOMING
      !now.isBefore(start) && now.isBefore(end) -> LessonStatus.CURRENT
      else -> LessonStatus.COMPLETED
    }
  }

  private fun syncLessonStatusWithCurrentTime(lesson: LessonEntity): LessonEntity {
    if (lesson.status == LessonStatus.CANCELLED) return lesson
    val computed = computeLessonStatus(lesson.date, lesson.startTime, lesson.endTime)
    return if (lesson.status != computed) {
      lesson.copy(status = computed, updatedAt = System.currentTimeMillis())
    } else {
      lesson
    }
  }

  /**
   * Generates real lesson occurrences from weekly recurring schedules for a date range.
   * Prevents duplicates and sets source = "SCHEDULE".
   */
  suspend fun generateLessonsFromSchedule(
    startDate: LocalDate,
    endDate: LocalDate,
    classId: String? = null
  ): Result<Int> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }

    if (endDate.isBefore(startDate)) {
      return Result.failure(IllegalArgumentException("Tugash sanasi boshlanish sanasidan keyin bo'lishi kerak"))
    }

    val schedules = if (!classId.isNullOrBlank()) {
      scheduleDao.getSchedulesByClass(classId, teacherId)
    } else {
      scheduleDao.getSchedulesByTeacher(teacherId)
    }.filter { it.active }

    if (schedules.isEmpty()) {
      return Result.failure(IllegalStateException("Faol dars jadvali topilmadi. Avval jadval yarating."))
    }

    var generatedCount = 0
    var curr = startDate
    val now = System.currentTimeMillis()

    database.withTransaction {
      while (!curr.isAfter(endDate)) {
        val dayOfWeek = curr.dayOfWeek.value
        val daySchedules = schedules.filter { it.dayOfWeek == dayOfWeek }
        val dateStr = curr.format(DateTimeFormatter.ISO_LOCAL_DATE)

        for (sch in daySchedules) {
          val duplicate = lessonDao.findDuplicate(
            teacherId = teacherId,
            classId = sch.classId,
            subjectId = sch.subjectId,
            date = dateStr,
            startTime = sch.startTime
          )

          if (duplicate == null) {
            val lessonId = "les_" + UUID.randomUUID().toString().replace("-", "").take(10)
            val lesson = LessonEntity(
              id = lessonId,
              teacherId = teacherId,
              classId = sch.classId,
              subjectId = sch.subjectId,
              scheduleId = sch.id,
              date = dateStr,
              startTime = sch.startTime,
              endTime = sch.endTime,
              room = sch.room,
              topic = "Dars mashg'uloti",
              status = computeLessonStatus(dateStr, sch.startTime, sch.endTime),
              lessonNumber = sch.lessonNumber,
              source = "SCHEDULE",
              manualOverride = false,
              createdAt = now,
              updatedAt = now
            )
            lessonDao.insert(lesson)
            generatedCount++
          }
        }
        curr = curr.plusDays(1)
      }
    }

    return Result.success(generatedCount)
  }
}
