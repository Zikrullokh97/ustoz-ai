package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceAuditEntity
import com.example.data.model.CameraEntity
import com.example.data.model.StudentCardEntity
import com.example.data.model.StudentFaceEnrollmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomatedAttendanceDao {

  // ==========================================
  // CAMERAS
  // ==========================================

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCamera(camera: CameraEntity)

  @Update
  suspend fun updateCamera(camera: CameraEntity)

  @Query("DELETE FROM cameras WHERE id = :id AND teacherId = :teacherId")
  suspend fun deleteCamera(id: String, teacherId: String): Int

  @Query("SELECT * FROM cameras WHERE teacherId = :teacherId ORDER BY createdAt DESC")
  suspend fun getCamerasByTeacher(teacherId: String): List<CameraEntity>

  @Query("SELECT * FROM cameras WHERE teacherId = :teacherId ORDER BY createdAt DESC")
  fun getCamerasByTeacherFlow(teacherId: String): Flow<List<CameraEntity>>

  @Query("SELECT * FROM cameras WHERE deviceId = :deviceId AND teacherId = :teacherId LIMIT 1")
  suspend fun getCameraByDeviceId(deviceId: String, teacherId: String): CameraEntity?

  @Query("UPDATE cameras SET status = :status, lastSeen = :timestamp WHERE id = :id AND teacherId = :teacherId")
  suspend fun updateCameraStatus(id: String, teacherId: String, status: String, timestamp: Long)

  // ==========================================
  // STUDENT CARDS (NFC/RFID)
  // ==========================================

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCard(card: StudentCardEntity)

  @Update
  suspend fun updateCard(card: StudentCardEntity)

  @Query("DELETE FROM student_cards WHERE id = :id AND teacherId = :teacherId")
  suspend fun deleteCard(id: String, teacherId: String): Int

  @Query("SELECT * FROM student_cards WHERE teacherId = :teacherId ORDER BY registeredAt DESC")
  suspend fun getCardsByTeacher(teacherId: String): List<StudentCardEntity>

  @Query("SELECT * FROM student_cards WHERE cardUid = :cardUid AND teacherId = :teacherId AND isActive = 1 LIMIT 1")
  suspend fun getActiveCardByUid(cardUid: String, teacherId: String): StudentCardEntity?

  @Query("SELECT * FROM student_cards WHERE studentId = :studentId AND teacherId = :teacherId LIMIT 1")
  suspend fun getCardByStudent(studentId: String, teacherId: String): StudentCardEntity?

  // ==========================================
  // FACE ENROLLMENT (BIOMETRIC TEMPLATES)
  // ==========================================

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFaceEnrollment(enrollment: StudentFaceEnrollmentEntity)

  @Update
  suspend fun updateFaceEnrollment(enrollment: StudentFaceEnrollmentEntity)

  @Query("DELETE FROM face_enrollments WHERE studentId = :studentId AND teacherId = :teacherId")
  suspend fun deleteFaceEnrollment(studentId: String, teacherId: String): Int

  @Query("SELECT * FROM face_enrollments WHERE studentId = :studentId AND teacherId = :teacherId LIMIT 1")
  suspend fun getFaceEnrollmentByStudent(studentId: String, teacherId: String): StudentFaceEnrollmentEntity?

  @Query("SELECT * FROM face_enrollments WHERE teacherId = :teacherId")
  suspend fun getFaceEnrollmentsByTeacher(teacherId: String): List<StudentFaceEnrollmentEntity>

  // ==========================================
  // ATTENDANCE AUDITS
  // ==========================================

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAudit(audit: AttendanceAuditEntity)

  @Query("SELECT * FROM attendance_audits WHERE lessonId = :lessonId ORDER BY timestamp DESC")
  suspend fun getAuditsByLesson(lessonId: String): List<AttendanceAuditEntity>

  @Query("SELECT * FROM attendance_audits WHERE teacherId = :teacherId ORDER BY timestamp DESC LIMIT :limit")
  suspend fun getAuditsByTeacher(teacherId: String, limit: Int = 50): List<AttendanceAuditEntity>
}
