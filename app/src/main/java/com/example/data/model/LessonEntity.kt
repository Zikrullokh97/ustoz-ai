package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LessonStatus {
  UPCOMING,
  CURRENT,
  COMPLETED,
  CANCELLED
}

@Entity(
  tableName = "lessons",
  foreignKeys = [
    ForeignKey(
      entity = UserEntity::class,
      parentColumns = ["userId"],
      childColumns = ["teacherId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = ClassEntity::class,
      parentColumns = ["id"],
      childColumns = ["classId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = ScheduleEntity::class,
      parentColumns = ["id"],
      childColumns = ["scheduleId"],
      onDelete = ForeignKey.SET_NULL
    )
  ],
  indices = [
    Index(value = ["teacherId"]),
    Index(value = ["classId"]),
    Index(value = ["scheduleId"]),
    Index(value = ["date"]),
    Index(value = ["teacherId", "classId", "subjectId", "date", "startTime"], unique = true)
  ]
)
data class LessonEntity(
  @PrimaryKey
  val id: String,
  val teacherId: String,
  val classId: String,
  val subjectId: String,
  val scheduleId: String? = null,
  val date: String, // YYYY-MM-DD
  val startTime: String, // "09:00"
  val endTime: String, // "09:45"
  val room: String, // "24-xona"
  val topic: String, // "Kasrlar"
  val status: LessonStatus = LessonStatus.UPCOMING,
  val lessonNumber: Int = 1,
  val source: String = "MANUAL", // "KTP", "SCHEDULE", "MANUAL"
  val manualOverride: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
