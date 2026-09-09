package com.example.data.repository

import com.example.data.local.AttendanceDao
import com.example.data.local.AutomatedAttendanceDao
import com.example.data.local.ClassDao
import com.example.data.local.LessonDao
import com.example.data.local.SessionDao
import com.example.data.local.StudentDao
import com.example.data.model.AttendanceAuditEntity
import com.example.data.model.AttendanceDetectionState
import com.example.data.model.AttendanceStatus
import com.example.data.model.CameraEntity
import com.example.data.model.StudentCardEntity
import com.example.data.model.StudentFaceEnrollmentEntity
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class AttendanceScanResult(
  val studentId: String,
  val studentName: String,
  val status: AttendanceStatus,
  val source: String, // FACE, QR, NFC
  val detectionState: AttendanceDetectionState,
  val confidence: Float? = null,
  val lateMinutes: Int? = null,
  val message: String
)

class AutomatedAttendanceRepository(
  private val automatedAttendanceDao: AutomatedAttendanceDao,
  private val attendanceDao: AttendanceDao,
  private val lessonDao: LessonDao,
  private val classDao: ClassDao,
  private val studentDao: StudentDao,
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

  // ==========================================
  // CAMERAS
  // ==========================================

  suspend fun getCameras(): Result<List<CameraEntity>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    return Result.success(automatedAttendanceDao.getCamerasByTeacher(teacherId))
  }

  suspend fun registerCamera(
    name: String,
    location: String,
    deviceId: String,
    secretToken: String,
    linkedClassId: String? = null
  ): Result<CameraEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    if (name.isBlank()) return Result.failure(IllegalArgumentException("Kamera nomi kiritilishi shart"))
    if (deviceId.isBlank()) return Result.failure(IllegalArgumentException("Qurilma ID (Device ID) kiritilishi shart"))
    if (secretToken.isBlank()) return Result.failure(IllegalArgumentException("Maxfiy token kiritilishi shart"))

    val existing = automatedAttendanceDao.getCameraByDeviceId(deviceId.trim(), teacherId)
    if (existing != null) {
      return Result.failure(IllegalArgumentException("Ushbu Device ID bilan kamera allaqachon ro'yxatdan o'tgan"))
    }

    val camera = CameraEntity(
      id = "cam_" + UUID.randomUUID().toString().replace("-", "").take(10),
      teacherId = teacherId,
      name = name.trim(),
      location = location.trim().ifBlank { "Sinfxona" },
      deviceId = deviceId.trim(),
      secretToken = secretToken.trim(),
      status = "ACTIVE",
      linkedClassId = linkedClassId,
      lastSeen = System.currentTimeMillis(),
      createdAt = System.currentTimeMillis()
    )
    automatedAttendanceDao.insertCamera(camera)
    return Result.success(camera)
  }

  suspend fun deleteCamera(id: String): Result<Unit> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val count = automatedAttendanceDao.deleteCamera(id, teacherId)
    return if (count > 0) Result.success(Unit)
    else Result.failure(NoSuchElementException("Kamera topilmadi yoki o'chirish huquqi yo'q"))
  }

  suspend fun toggleCameraStatus(id: String, active: Boolean): Result<Unit> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val newStatus = if (active) "ACTIVE" else "INACTIVE"
    automatedAttendanceDao.updateCameraStatus(id, teacherId, newStatus, System.currentTimeMillis())
    return Result.success(Unit)
  }

  suspend fun testCameraConnection(id: String): Result<Boolean> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val cameras = automatedAttendanceDao.getCamerasByTeacher(teacherId)
    val cam = cameras.find { it.id == id }
      ?: return Result.failure(NoSuchElementException("Kamera topilmadi"))
    // Update heartbeat
    automatedAttendanceDao.updateCameraStatus(id, teacherId, "ACTIVE", System.currentTimeMillis())
    return Result.success(true)
  }

  // ==========================================
  // STUDENT CARDS (NFC/RFID)
  // ==========================================

  suspend fun getCards(): Result<List<StudentCardEntity>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    return Result.success(automatedAttendanceDao.getCardsByTeacher(teacherId))
  }

  suspend fun registerCard(
    studentId: String,
    cardUid: String,
    cardLabel: String = "NFC Karta"
  ): Result<StudentCardEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val cleanUid = cardUid.trim().uppercase()
    if (cleanUid.isBlank()) {
      return Result.failure(IllegalArgumentException("Karta UID kodi bo'sh bo'lishi mumkin emas"))
    }
    // Verify student exists and belongs to teacher
    val student = studentDao.getById(studentId)
      ?: return Result.failure(NoSuchElementException("O'quvchi topilmadi"))
    if (student.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu o'quvchi sizga tegishli emas"))
    }

    // Check if card UID is assigned to another student
    val existingCard = automatedAttendanceDao.getActiveCardByUid(cleanUid, teacherId)
    if (existingCard != null && existingCard.studentId != studentId) {
      return Result.failure(IllegalArgumentException("Bu NFC karta boshqa o'quvchiga biriktirilgan"))
    }

    val card = StudentCardEntity(
      id = "card_" + UUID.randomUUID().toString().replace("-", "").take(10),
      teacherId = teacherId,
      studentId = studentId,
      cardUid = cleanUid,
      cardLabel = cardLabel.trim().ifBlank { "NFC Karta" },
      isActive = true,
      registeredAt = System.currentTimeMillis()
    )
    automatedAttendanceDao.insertCard(card)
    logAudit(teacherId, "global", studentId, "CARD_REGISTERED", "NFC karta biriktirildi: $cleanUid")
    return Result.success(card)
  }

  suspend fun deactivateCard(id: String): Result<Unit> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val count = automatedAttendanceDao.deleteCard(id, teacherId)
    return if (count > 0) Result.success(Unit)
    else Result.failure(NoSuchElementException("Karta topilmadi"))
  }

  suspend fun getCardForStudent(studentId: String): Result<StudentCardEntity?> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    return Result.success(automatedAttendanceDao.getCardByStudent(studentId, teacherId))
  }

  // ==========================================
  // FACE ENROLLMENT (BIOMETRIC CONSENT & TEMPLATES)
  // ==========================================

  suspend fun getFaceEnrollments(): Result<List<StudentFaceEnrollmentEntity>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    return Result.success(automatedAttendanceDao.getFaceEnrollmentsByTeacher(teacherId))
  }

  suspend fun enrollStudentFace(
    studentId: String,
    faceTemplateHash: String
  ): Result<StudentFaceEnrollmentEntity> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val student = studentDao.getById(studentId)
      ?: return Result.failure(NoSuchElementException("O'quvchi topilmadi"))
    if (student.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: O'quvchi sizga tegishli emas"))
    }

    val enrollment = StudentFaceEnrollmentEntity(
      id = "face_" + UUID.randomUUID().toString().replace("-", "").take(10),
      teacherId = teacherId,
      studentId = studentId,
      hasConsent = true,
      faceTemplateHash = faceTemplateHash,
      registeredAt = System.currentTimeMillis()
    )
    automatedAttendanceDao.insertFaceEnrollment(enrollment)
    logAudit(teacherId, "global", studentId, "FACE_ENROLLED", "Face ID biometrik shablon xavfsiz saqlandi")
    return Result.success(enrollment)
  }

  suspend fun deleteFaceEnrollment(studentId: String): Result<Unit> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    automatedAttendanceDao.deleteFaceEnrollment(studentId, teacherId)
    logAudit(teacherId, "global", studentId, "FACE_DELETED", "Biometrik ma'lumot to'liq o'chirildi")
    return Result.success(Unit)
  }

  suspend fun getFaceEnrollmentForStudent(studentId: String): Result<StudentFaceEnrollmentEntity?> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    return Result.success(automatedAttendanceDao.getFaceEnrollmentByStudent(studentId, teacherId))
  }

  // ==========================================
  // ATTENDANCE SCANNING & AUTOMATION
  // ==========================================

  suspend fun processQrScan(lessonId: String, qrRawContent: String): Result<AttendanceScanResult> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val lesson = lessonDao.getById(lessonId)
      ?: return Result.failure(NoSuchElementException("Dars topilmadi"))
    if (lesson.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu dars sizga tegishli emas"))
    }

    // Expected QR Format: USTOZ_ATTENDANCE:<studentId>:<token>
    val parts = qrRawContent.trim().split(":")
    val studentId = if (parts.size >= 2 && parts[0] == "USTOZ_ATTENDANCE") {
      parts[1]
    } else if (parts.size == 1 && parts[0].startsWith("std_")) {
      parts[0]
    } else {
      return Result.failure(IllegalArgumentException("Noto'g'ri QR format: Ustoz AI o'quvchi QR kartasi talab qilinadi"))
    }

    val student = studentDao.getById(studentId)
      ?: return Result.failure(NoSuchElementException("O'quvchi topilmadi"))

    if (student.classId != lesson.classId) {
      return Result.failure(IllegalArgumentException("${student.fullName} ushbu dars sinfiga tegishli emas (${student.classId})"))
    }

    // Determine status based on lesson start time
    val (status, lateMinutes) = calculateAttendanceStatus(lesson.startTime)

    logAudit(
      teacherId = teacherId,
      lessonId = lessonId,
      studentId = studentId,
      action = "QR_SCANNED",
      details = "QR skanerlandi: $status, kechikish: ${lateMinutes ?: 0} min"
    )

    return Result.success(
      AttendanceScanResult(
        studentId = studentId,
        studentName = student.fullName,
        status = status,
        source = "QR",
        detectionState = AttendanceDetectionState.DETECTED,
        confidence = 1.0f,
        lateMinutes = lateMinutes,
        message = "${student.fullName} QR orqali aniqlandi (${if (status == AttendanceStatus.PRESENT) "Kelgan" else "Kechikkan: $lateMinutes daq"})"
      )
    )
  }

  suspend fun processCardTap(lessonId: String, cardUid: String): Result<AttendanceScanResult> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val lesson = lessonDao.getById(lessonId)
      ?: return Result.failure(NoSuchElementException("Dars topilmadi"))
    if (lesson.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu dars sizga tegishli emas"))
    }

    val cleanUid = cardUid.trim().uppercase()
    val card = automatedAttendanceDao.getActiveCardByUid(cleanUid, teacherId)
      ?: return Result.failure(NoSuchElementException("Ushbu NFC/RFID karta ($cleanUid) tizimda ro'yxatdan o'tmagan"))

    val student = studentDao.getById(card.studentId)
      ?: return Result.failure(NoSuchElementException("Kartaga biriktirilgan o'quvchi topilmadi"))

    if (student.classId != lesson.classId) {
      return Result.failure(IllegalArgumentException("${student.fullName} ushbu dars sinfiga tegishli emas"))
    }

    val (status, lateMinutes) = calculateAttendanceStatus(lesson.startTime)

    logAudit(
      teacherId = teacherId,
      lessonId = lessonId,
      studentId = student.id,
      action = "CARD_SCANNED",
      details = "NFC karta bosildi: $cleanUid -> $status"
    )

    return Result.success(
      AttendanceScanResult(
        studentId = student.id,
        studentName = student.fullName,
        status = status,
        source = "NFC",
        detectionState = AttendanceDetectionState.DETECTED,
        confidence = 1.0f,
        lateMinutes = lateMinutes,
        message = "${student.fullName} NFC karta orqali qayd etildi (${if (status == AttendanceStatus.PRESENT) "Kelgan" else "Kechikkan: $lateMinutes daq"})"
      )
    )
  }

  suspend fun processFaceDetections(
    lessonId: String,
    detectedFaceTemplateHashes: List<Pair<String, Float>> // Pair(faceHash, confidence)
  ): Result<List<AttendanceScanResult>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val lesson = lessonDao.getById(lessonId)
      ?: return Result.failure(NoSuchElementException("Dars topilmadi"))
    if (lesson.teacherId != teacherId) {
      return Result.failure(SecurityException("403 Forbidden: Ushbu dars sizga tegishli emas"))
    }

    val enrollments = automatedAttendanceDao.getFaceEnrollmentsByTeacher(teacherId)
    val studentsInClass = studentDao.getStudentsByClass(lesson.classId, teacherId)
    val studentMap = studentsInClass.associateBy { it.id }

    val results = mutableListOf<AttendanceScanResult>()
    val (status, lateMinutes) = calculateAttendanceStatus(lesson.startTime)

    for ((hash, confidence) in detectedFaceTemplateHashes) {
      val enrollment = enrollments.find { it.faceTemplateHash == hash } ?: continue
      val student = studentMap[enrollment.studentId] ?: continue

      val detectionState = if (confidence >= 0.85f) {
        AttendanceDetectionState.DETECTED
      } else {
        AttendanceDetectionState.UNCERTAIN
      }

      logAudit(
        teacherId = teacherId,
        lessonId = lessonId,
        studentId = student.id,
        action = "FACE_DETECTED",
        details = "Face AI aniqladi: holat=$detectionState, ishonch=${(confidence * 100).toInt()}%"
      )

      results.add(
        AttendanceScanResult(
          studentId = student.id,
          studentName = student.fullName,
          status = status,
          source = "FACE",
          detectionState = detectionState,
          confidence = confidence,
          lateMinutes = lateMinutes,
          message = if (detectionState == AttendanceDetectionState.DETECTED) {
            "${student.fullName} Face ID orqali aniqlandi (${(confidence * 100).toInt()}%)"
          } else {
            "${student.fullName} noaniq aniqlandi (${(confidence * 100).toInt()}%) — O'qituvchi tekshiruvi talab etiladi"
          }
        )
      )
    }

    return Result.success(results)
  }

  // ==========================================
  // AUDIT LOGS
  // ==========================================

  suspend fun getAuditLogs(lessonId: String? = null): Result<List<AttendanceAuditEntity>> {
    val teacherId = getAuthenticatedTeacherId().getOrElse { return Result.failure(it) }
    val list = if (lessonId != null) {
      automatedAttendanceDao.getAuditsByLesson(lessonId)
    } else {
      automatedAttendanceDao.getAuditsByTeacher(teacherId)
    }
    return Result.success(list)
  }

  private suspend fun logAudit(
    teacherId: String,
    lessonId: String,
    studentId: String,
    action: String,
    details: String
  ) {
    try {
      automatedAttendanceDao.insertAudit(
        AttendanceAuditEntity(
          id = "aud_" + UUID.randomUUID().toString().replace("-", "").take(10),
          teacherId = teacherId,
          lessonId = lessonId,
          studentId = studentId,
          action = action,
          details = details,
          timestamp = System.currentTimeMillis()
        )
      )
    } catch (_: Exception) {}
  }

  private fun calculateAttendanceStatus(lessonStartTime: String): Pair<AttendanceStatus, Int?> {
    return try {
      val now = LocalTime.now()
      val formatter = DateTimeFormatter.ofPattern("HH:mm")
      val start = LocalTime.parse(lessonStartTime, formatter)
      val diffMinutes = java.time.Duration.between(start, now).toMinutes().toInt()
      if (diffMinutes > 5) {
        Pair(AttendanceStatus.LATE, diffMinutes)
      } else {
        Pair(AttendanceStatus.PRESENT, null)
      }
    } catch (_: Exception) {
      Pair(AttendanceStatus.PRESENT, null)
    }
  }
}
