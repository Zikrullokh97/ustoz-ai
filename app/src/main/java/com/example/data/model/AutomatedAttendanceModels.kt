package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceMethod {
  MANUAL,
  FACE,
  QR,
  NFC
}

enum class AttendanceDetectionState {
  DETECTED,     // Aniq aniqlandi
  NOT_DETECTED, // Hali aniqlanmadi (avtomatik yo'q deb hisoblanmaydi)
  UNCERTAIN     // Noaniq / Qo'lda tekshirish kerak
}

@Entity(
  tableName = "cameras",
  foreignKeys = [
    ForeignKey(
      entity = UserEntity::class,
      parentColumns = ["userId"],
      childColumns = ["teacherId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["teacherId"]),
    Index(value = ["deviceId"]),
    Index(value = ["teacherId", "deviceId"], unique = true)
  ]
)
data class CameraEntity(
  @PrimaryKey
  val id: String,
  val teacherId: String,
  val name: String,
  val location: String,
  val deviceId: String,
  val secretToken: String,
  val status: String = "ACTIVE",
  val linkedClassId: String? = null,
  val lastSeen: Long = System.currentTimeMillis(),
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "student_cards",
  foreignKeys = [
    ForeignKey(
      entity = UserEntity::class,
      parentColumns = ["userId"],
      childColumns = ["teacherId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = StudentEntity::class,
      parentColumns = ["id"],
      childColumns = ["studentId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["teacherId"]),
    Index(value = ["studentId"]),
    Index(value = ["cardUid"])
  ]
)
data class StudentCardEntity(
  @PrimaryKey
  val id: String,
  val teacherId: String,
  val studentId: String,
  val cardUid: String,
  val cardLabel: String = "NFC Karta",
  val isActive: Boolean = true,
  val registeredAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "face_enrollments",
  foreignKeys = [
    ForeignKey(
      entity = UserEntity::class,
      parentColumns = ["userId"],
      childColumns = ["teacherId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = StudentEntity::class,
      parentColumns = ["id"],
      childColumns = ["studentId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["teacherId"]),
    Index(value = ["studentId"], unique = true)
  ]
)
data class StudentFaceEnrollmentEntity(
  @PrimaryKey
  val id: String,
  val teacherId: String,
  val studentId: String,
  val hasConsent: Boolean = true,
  val faceTemplateHash: String,
  val registeredAt: Long = System.currentTimeMillis(),
  val deletedAt: Long? = null
)

@Entity(
  tableName = "attendance_audits",
  indices = [
    Index(value = ["teacherId"]),
    Index(value = ["lessonId"]),
    Index(value = ["studentId"]),
    Index(value = ["timestamp"])
  ]
)
data class AttendanceAuditEntity(
  @PrimaryKey
  val id: String,
  val teacherId: String,
  val lessonId: String,
  val studentId: String,
  val action: String,
  val details: String,
  val timestamp: Long = System.currentTimeMillis()
)
