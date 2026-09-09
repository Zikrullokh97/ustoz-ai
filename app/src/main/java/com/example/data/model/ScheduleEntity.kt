package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "schedules",
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
    )
  ],
  indices = [
    Index(value = ["teacherId"]),
    Index(value = ["classId"]),
    Index(value = ["teacherId", "dayOfWeek", "startTime"])
  ]
)
data class ScheduleEntity(
  @PrimaryKey
  val id: String,
  val teacherId: String,
  val classId: String,
  val subjectId: String,
  val dayOfWeek: Int, // 1 = Dushanba, 2 = Seshanba, ..., 6 = Shanba, 7 = Yakshanba
  val startTime: String, // "09:00"
  val endTime: String, // "09:45"
  val room: String, // "24-xona"
  val lessonNumber: Int, // 1..8
  val active: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
