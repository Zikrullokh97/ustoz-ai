package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceStatus {
  PRESENT, // Kelgan
  LATE,    // Kech qolgan
  ABSENT,  // Yo'q
  EXCUSED  // Sababli
}

@Entity(
  tableName = "attendances",
  foreignKeys = [
    ForeignKey(
      entity = LessonEntity::class,
      parentColumns = ["id"],
      childColumns = ["lessonId"],
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
    Index(value = ["lessonId"]),
    Index(value = ["studentId"]),
    Index(value = ["lessonId", "studentId"], unique = true)
  ]
)
data class AttendanceEntity(
  @PrimaryKey
  val id: String,
  val lessonId: String,
  val studentId: String,
  val status: AttendanceStatus,
  val lateMinutes: Int? = null,
  val note: String? = null,
  val source: String = "MANUAL", // MANUAL, FACE, QR, NFC
  val manualOverride: Boolean = false,
  val originalSource: String? = null,
  val confidence: Float? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
