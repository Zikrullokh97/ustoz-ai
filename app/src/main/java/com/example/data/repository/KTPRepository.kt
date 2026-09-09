package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.ClassDao
import com.example.data.local.KTPDao
import com.example.data.local.LessonDao
import com.example.data.local.ScheduleDao
import com.example.data.local.SessionDao
import com.example.data.local.UstozDatabase
import com.example.data.model.ClassEntity
import com.example.data.model.KTPConfidence
import com.example.data.model.KTPDocumentEntity
import com.example.data.model.KTPDocumentWithEntries
import com.example.data.model.KTPEntryEntity
import com.example.data.model.KTPEntryStatus
import com.example.data.model.KTPStatus
import com.example.data.model.LessonEntity
import com.example.data.model.LessonStatus
import com.example.data.model.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class KTPRepository(
  private val database: UstozDatabase,
  private val ktpDao: KTPDao,
  private val scheduleDao: ScheduleDao,
  private val lessonDao: LessonDao,
  private val classDao: ClassDao,
  private val sessionDao: SessionDao,
  private val authRepository: AuthRepository? = null
) {

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

  suspend fun getDocuments(): Result<List<KTPDocumentEntity>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val docs = ktpDao.getDocumentsByTeacher(teacherId)
    return Result.success(docs)
  }

  fun getDocumentsFlow(): Flow<List<KTPDocumentEntity>> {
    return flow {
      val teacherId = getAuthenticatedTeacherId().getOrNull() ?: ""
      emitAll(ktpDao.getDocumentsByTeacherFlow(teacherId))
    }
  }

  suspend fun getDocumentWithEntries(documentId: String): Result<KTPDocumentWithEntries> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val doc = ktpDao.getDocumentById(documentId, teacherId)
      ?: return Result.failure(NoSuchElementException("404 Not Found: KTP hujjati topilmadi"))
    val entries = ktpDao.getEntriesByDocument(documentId, teacherId)
    return Result.success(KTPDocumentWithEntries(doc, entries))
  }

  /**
   * Uploads and analyzes a KTP file/text, performing semantic structural parsing,
   * matching with Teacher's weekly Schedule, calculating dates, detecting conflicts and warnings.
   * Does NOT write to production LessonEntity until teacher reviews and confirms!
   */
  suspend fun uploadAndAnalyzeKTP(
    fileName: String,
    subjectId: String,
    academicYear: String,
    content: String,
    explicitClassId: String? = null
  ): Result<KTPDocumentWithEntries> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }

    val teacherClasses = classDao.getClasses(teacherId)
    val teacherSchedules = scheduleDao.getSchedulesByTeacher(teacherId)

    // 1. Structural Parse
    val parseResult = parseKTPContent(
      content = content,
      fileName = fileName,
      defaultSubject = subjectId,
      teacherClasses = teacherClasses,
      explicitClassId = explicitClassId
    ).getOrElse { return Result.failure(it) }

    val docId = "ktp_doc_" + UUID.randomUUID().toString().replace("-", "").take(10)
    val now = System.currentTimeMillis()

    // 2. Schedule Matching & Date Calculation
    val entries = matchEntriesWithSchedule(
      docId = docId,
      teacherId = teacherId,
      rawItems = parseResult.rawItems,
      targetClass = parseResult.targetClass,
      detectedClassName = parseResult.detectedClassName,
      subjectId = subjectId,
      teacherClasses = teacherClasses,
      teacherSchedules = teacherSchedules
    )

    val warningCount = entries.count { it.status != KTPEntryStatus.VALID && it.status != KTPEntryStatus.CONFIRMED }

    val document = KTPDocumentEntity(
      id = docId,
      teacherId = teacherId,
      fileName = fileName.trim(),
      subjectId = subjectId.trim(),
      academicYear = academicYear.trim().ifBlank { "2026/2027" },
      status = if (warningCount > 0) KTPStatus.REVIEW_REQUIRED else KTPStatus.ANALYZED,
      lessonCount = entries.size,
      warningCount = warningCount,
      createdAt = now,
      updatedAt = now
    )

    // Save draft in transaction (draft only, NOT lessons yet!)
    database.withTransaction {
      ktpDao.insertDocument(document)
      ktpDao.insertEntries(entries)
    }

    return Result.success(KTPDocumentWithEntries(document, entries))
  }

  /**
   * Teacher manually updates a KTP entry row.
   * Marks manualOverride = true and preserves changes against automated recalculations.
   */
  suspend fun updateKTPEntry(
    entryId: String,
    date: String,
    lessonNumber: Int,
    topic: String,
    room: String,
    classId: String? = null
  ): Result<KTPEntryEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val existing = ktpDao.getEntryById(entryId, teacherId)
      ?: return Result.failure(NoSuchElementException("KTP bandi topilmadi"))

    val targetClass = if (!classId.isNullOrBlank()) {
      classDao.getById(classId)
    } else {
      classDao.getById(existing.classId)
    }

    // Validate date against weekday
    val dayOfWeek = try {
      LocalDate.parse(date.trim(), DateTimeFormatter.ISO_LOCAL_DATE).dayOfWeek.value
    } catch (_: Exception) {
      0
    }

    val updated = existing.copy(
      date = date.trim(),
      lessonNumber = lessonNumber,
      topic = topic.trim(),
      room = room.trim().ifBlank { "Sinfxona" },
      classId = targetClass?.id ?: existing.classId,
      classNameDisplay = targetClass?.name ?: existing.classNameDisplay,
      manualOverride = true,
      source = "MANUAL",
      status = if (dayOfWeek == 0) KTPEntryStatus.WARNING_DATE_WEEKDAY else KTPEntryStatus.VALID,
      warningMessage = if (dayOfWeek == 0) "Sana formati noto'g'ri (YYYY-MM-DD bo'lishi kerak)" else null,
      updatedAt = System.currentTimeMillis()
    )

    ktpDao.updateEntry(updated)

    // Update document warning count
    val allEntries = ktpDao.getEntriesByDocument(existing.ktpDocumentId, teacherId)
    val doc = ktpDao.getDocumentById(existing.ktpDocumentId, teacherId)
    if (doc != null) {
      val warnCount = allEntries.count { it.status != KTPEntryStatus.VALID && it.status != KTPEntryStatus.CONFIRMED }
      ktpDao.updateDocument(doc.copy(warningCount = warnCount, updatedAt = System.currentTimeMillis()))
    }

    return Result.success(updated)
  }

  /**
   * Confirms KTP document and generates REAL LessonEntity occurrences in database.
   * Enforces duplicate protection and traceability (source = KTP).
   */
  suspend fun confirmAndGenerateLessons(documentId: String): Result<Int> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val doc = ktpDao.getDocumentById(documentId, teacherId)
      ?: return Result.failure(NoSuchElementException("KTP hujjati topilmadi"))

    val entries = ktpDao.getEntriesByDocument(documentId, teacherId)
    if (entries.isEmpty()) {
      return Result.failure(IllegalStateException("Tasdiqlash uchun KTP bandlari mavjud emas."))
    }

    val criticalErrors = entries.filter { it.status == KTPEntryStatus.WARNING_CLASS_MISMATCH }
    if (criticalErrors.isNotEmpty()) {
      return Result.failure(IllegalStateException("Ayrim bandlarda sinf aniqlanmagan. Iltimos sinfni moslang."))
    }

    var createdCount = 0
    val now = System.currentTimeMillis()

    database.withTransaction {
      // Fetch teacher schedules for time lookup
      val schedules = scheduleDao.getSchedulesByTeacher(teacherId)

      for (entry in entries) {
        val entryDate = LocalDate.parse(entry.date, DateTimeFormatter.ISO_LOCAL_DATE)
        val dayOfWeek = entryDate.dayOfWeek.value

        // Find schedule for time slots
        val matchedSchedule = schedules.firstOrNull {
          it.classId == entry.classId &&
              it.dayOfWeek == dayOfWeek &&
              (it.lessonNumber == entry.lessonNumber || it.subjectId.equals(entry.subjectId, ignoreCase = true))
        }

        val startTime = matchedSchedule?.startTime ?: getStandardStartTime(entry.lessonNumber)
        val endTime = matchedSchedule?.endTime ?: getStandardEndTime(entry.lessonNumber)
        val room = entry.room.ifBlank { matchedSchedule?.room ?: "Sinfxona" }

        // Duplicate check: teacherId + classId + subjectId + date + startTime
        val existingLesson = lessonDao.findDuplicate(
          teacherId = teacherId,
          classId = entry.classId,
          subjectId = entry.subjectId,
          date = entry.date,
          startTime = startTime
        )

        if (existingLesson != null) {
          // If lesson exists and teacher hasn't manually overridden it, update topic from KTP
          if (!existingLesson.manualOverride) {
            val updatedLesson = existingLesson.copy(
              topic = entry.topic,
              source = "KTP",
              scheduleId = matchedSchedule?.id ?: existingLesson.scheduleId,
              updatedAt = now
            )
            lessonDao.update(updatedLesson)
          }
        } else {
          val lessonId = "les_" + UUID.randomUUID().toString().replace("-", "").take(10)
          val newLesson = LessonEntity(
            id = lessonId,
            teacherId = teacherId,
            classId = entry.classId,
            subjectId = entry.subjectId,
            scheduleId = matchedSchedule?.id,
            date = entry.date,
            startTime = startTime,
            endTime = endTime,
            room = room,
            topic = entry.topic,
            status = LessonStatus.UPCOMING,
            lessonNumber = entry.lessonNumber,
            source = "KTP",
            manualOverride = entry.manualOverride,
            createdAt = now,
            updatedAt = now
          )
          lessonDao.insert(newLesson)
          createdCount++
        }

        // Mark entry as confirmed
        ktpDao.updateEntry(entry.copy(isConfirmed = true, status = KTPEntryStatus.CONFIRMED, updatedAt = now))
      }

      // Update document status
      ktpDao.updateDocument(doc.copy(status = KTPStatus.CONFIRMED, warningCount = 0, updatedAt = now))
    }

    return Result.success(createdCount)
  }

  suspend fun deleteDocument(documentId: String): Result<Unit> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    ktpDao.deleteEntriesByDocument(documentId, teacherId)
    val rows = ktpDao.deleteDocument(documentId, teacherId)
    return if (rows > 0) Result.success(Unit) else Result.failure(IllegalStateException("KTP o'chirilmadi"))
  }

  // ==========================================
  // PARSING & SCHEDULE MATCHING HELPERS
  // ==========================================

  private data class RawKTPItem(
    val sequenceNumber: Int,
    val explicitDate: String?, // YYYY-MM-DD if available
    val weekNumber: Int,
    val lessonNumber: Int,
    val topic: String,
    val hours: Int
  )

  private data class KTPParseResult(
    val rawItems: List<RawKTPItem>,
    val targetClass: ClassEntity?,
    val detectedClassName: String?
  )

  private fun parseKTPContent(
    content: String,
    fileName: String,
    defaultSubject: String,
    teacherClasses: List<ClassEntity>,
    explicitClassId: String?
  ): Result<KTPParseResult> {
    val lines = content.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) {
      return Result.failure(IllegalArgumentException("KTP fayli bo'sh yoki o'qib bo'lmadi."))
    }

    // Try detecting class from text
    var detectedClassName: String? = null
    val classRegex = Regex("""(?i)\b([1-9]|1[0-1])\s*[-_ ]\s*([A-Za-zА-Яа-яЎўҚқҒғҲҳ])\b""")
    for (line in lines.take(15)) {
      val match = classRegex.find(line)
      if (match != null) {
        detectedClassName = "${match.groupValues[1]}-${match.groupValues[2].uppercase()}"
        break
      }
    }

    val matchedClass = when {
      !explicitClassId.isNullOrBlank() -> teacherClasses.firstOrNull { it.id == explicitClassId }
      detectedClassName != null -> teacherClasses.firstOrNull { it.name.equals(detectedClassName, ignoreCase = true) }
      teacherClasses.size == 1 -> teacherClasses.first()
      else -> null
    }

    val rawItems = mutableListOf<RawKTPItem>()
    var seq = 1
    val dateRegex = Regex("""\b(\d{4}-\d{2}-\d{2}|\d{2}\.\d{2}\.\d{4})\b""")

    for (line in lines) {
      val lower = line.lowercase()
      if (lower.startsWith("mavzu") || lower.startsWith("№") || lower.startsWith("soat") ||
        lower.startsWith("dars") && lower.contains("reja") || lower.startsWith("kalendar")
      ) {
        continue
      }

      // Check date
      val dateMatch = dateRegex.find(line)
      val explicitDate = dateMatch?.value?.let { rawDate ->
        if (rawDate.contains(".")) {
          val parts = rawDate.split(".")
          if (parts.size == 3) "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}" else null
        } else rawDate
      }

      // Remove date from topic string
      var cleanLine = if (dateMatch != null) line.replace(dateMatch.value, "") else line

      // Clean leading numbering: "1.", "1-mavzu", "1)"
      cleanLine = cleanLine.replace(Regex("""^\d+[\.\)\-\s]+"""), "")
        .replace(Regex("""^\d+\s*-\s*mavzu\s*[:\-]?\s*""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""^\d+\s*-\s*dars\s*[:\-]?\s*""", RegexOption.IGNORE_CASE), "")
        .trim()

      if (cleanLine.length < 3) continue

      val topicParts = cleanLine.split("|", ";", "\t").map { it.trim() }.filter { it.isNotBlank() }
      val topicText = topicParts.firstOrNull { it.length >= 3 && !it.matches(Regex("""^\d+$""")) } ?: cleanLine

      rawItems.add(
        RawKTPItem(
          sequenceNumber = seq++,
          explicitDate = explicitDate,
          weekNumber = ((seq - 1) / 2) + 1,
          lessonNumber = 1,
          topic = topicText,
          hours = 1
        )
      )
    }

    if (rawItems.isEmpty()) {
      return Result.failure(IllegalArgumentException("KTP formatini to'liq aniqlab bo'lmadi. Mavzular ro'yxatini tekshiring."))
    }

    return Result.success(KTPParseResult(rawItems, matchedClass, detectedClassName))
  }

  private fun matchEntriesWithSchedule(
    docId: String,
    teacherId: String,
    rawItems: List<RawKTPItem>,
    targetClass: ClassEntity?,
    detectedClassName: String?,
    subjectId: String,
    teacherClasses: List<ClassEntity>,
    teacherSchedules: List<ScheduleEntity>
  ): List<KTPEntryEntity> {
    val entries = mutableListOf<KTPEntryEntity>()
    val now = System.currentTimeMillis()

    val assignedClass = targetClass ?: teacherClasses.firstOrNull()
    val classId = assignedClass?.id ?: "unassigned_class"
    val classNameDisplay = targetClass?.name ?: (detectedClassName ?: "Noma'lum sinf")

    // Class mismatch warning
    val classMismatch = targetClass == null && detectedClassName != null

    // Get schedules for this class
    val classSchedules = teacherSchedules
      .filter { it.classId == classId && it.active }
      .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))

    val scheduleDays = if (classSchedules.isNotEmpty()) {
      classSchedules.map { it.dayOfWeek }.distinct()
    } else {
      listOf(1, 3, 5) // Fallback: Dushanba, Chorshanba, Juma
    }

    // Date calculation start (Academic year start: first Monday of September 2026)
    var currentDate = LocalDate.of(2026, 9, 7)
    var scheduleDayIndex = 0

    for ((index, item) in rawItems.withIndex()) {
      val entryId = "ktp_entry_" + UUID.randomUUID().toString().replace("-", "").take(10)

      var entryDate: String
      var entryLessonNumber = item.lessonNumber
      var entryConfidence: KTPConfidence
      var entrySource: String
      var entryStatus = KTPEntryStatus.VALID
      var warningMsg: String? = null

      if (item.explicitDate != null) {
        entryDate = item.explicitDate
        entrySource = "KTP"
        entryConfidence = KTPConfidence.HIGH

        // Weekday validation
        try {
          val parsedDate = LocalDate.parse(entryDate, DateTimeFormatter.ISO_LOCAL_DATE)
          val actualDay = parsedDate.dayOfWeek.value
          if (classSchedules.isNotEmpty() && !scheduleDays.contains(actualDay)) {
            entryStatus = KTPEntryStatus.WARNING_SCHEDULE_MISMATCH
            warningMsg = "Jadval va KTP o'rtasida mos kelmaslik aniqlandi (${getDayName(actualDay)} jadvalda yo'q)."
          }
        } catch (_: Exception) {
          entryStatus = KTPEntryStatus.WARNING_DATE_WEEKDAY
          warningMsg = "Sana va hafta kuni mos kelmaydi."
        }
      } else {
        // Compute deterministic date using schedule
        entrySource = "SCHEDULE"
        entryConfidence = if (classSchedules.isNotEmpty()) KTPConfidence.MEDIUM else KTPConfidence.LOW

        val targetDayOfWeek = scheduleDays[scheduleDayIndex % scheduleDays.size]
        while (currentDate.dayOfWeek.value != targetDayOfWeek) {
          currentDate = currentDate.plusDays(1)
        }

        // Find schedule for this day to get lesson number and room
        val schForDay = classSchedules.firstOrNull { it.dayOfWeek == targetDayOfWeek }
        if (schForDay != null) {
          entryLessonNumber = schForDay.lessonNumber
        }

        entryDate = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Advance to next schedule slot
        scheduleDayIndex++
        currentDate = currentDate.plusDays(1)
      }

      if (classMismatch) {
        entryStatus = KTPEntryStatus.WARNING_CLASS_MISMATCH
        warningMsg = "$detectedClassName sinfi Teacher profilingizda topilmadi."
      }

      val sch = classSchedules.firstOrNull { it.dayOfWeek == try { LocalDate.parse(entryDate).dayOfWeek.value } catch (_: Exception) { 1 } }
      val room = sch?.room ?: "24-xona"

      entries.add(
        KTPEntryEntity(
          id = entryId,
          ktpDocumentId = docId,
          teacherId = teacherId,
          classId = classId,
          classNameDisplay = classNameDisplay,
          subjectId = subjectId,
          date = entryDate,
          weekNumber = (index / scheduleDays.size.coerceAtLeast(1)) + 1,
          lessonNumber = entryLessonNumber,
          topic = item.topic,
          hours = item.hours,
          room = room,
          confidence = entryConfidence,
          source = entrySource,
          manualOverride = false,
          status = entryStatus,
          warningMessage = warningMsg,
          isConfirmed = false,
          createdAt = now,
          updatedAt = now
        )
      )
    }

    return entries
  }

  private fun getStandardStartTime(lessonNumber: Int): String {
    return when (lessonNumber) {
      1 -> "08:00"
      2 -> "08:50"
      3 -> "09:40"
      4 -> "10:40"
      5 -> "11:30"
      6 -> "12:20"
      7 -> "13:10"
      else -> "14:00"
    }
  }

  private fun getStandardEndTime(lessonNumber: Int): String {
    return when (lessonNumber) {
      1 -> "08:45"
      2 -> "09:35"
      3 -> "10:25"
      4 -> "11:25"
      5 -> "12:15"
      6 -> "13:05"
      7 -> "13:55"
      else -> "14:45"
    }
  }

  private fun getDayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
      1 -> "Dushanba"
      2 -> "Seshanba"
      3 -> "Chorshanba"
      4 -> "Payshanba"
      5 -> "Juma"
      6 -> "Shanba"
      7 -> "Yakshanba"
      else -> ""
    }
  }
}
