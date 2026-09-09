package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class KTPStatus {
  UPLOADED,
  PROCESSING,
  ANALYZED,
  REVIEW_REQUIRED,
  CONFIRMED,
  FAILED
}

enum class KTPConfidence {
  HIGH,
  MEDIUM,
  LOW
}

enum class KTPEntryStatus {
  VALID,
  WARNING_DATE_WEEKDAY,
  WARNING_CLASS_MISMATCH,
  WARNING_SCHEDULE_MISMATCH,
  CONFIRMED
}

@Entity(
  tableName = "ktp_documents",
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
    Index(value = ["teacherId", "subjectId"])
  ]
)
data class KTPDocumentEntity(
  @PrimaryKey
  val id: String,
  val teacherId: String,
  val fileName: String,
  val fileUrl: String = "",
  val subjectId: String, // e.g. "Matematika"
  val academicYear: String = "2026/2027",
  val status: KTPStatus = KTPStatus.ANALYZED,
  val lessonCount: Int = 0,
  val warningCount: Int = 0,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "ktp_entries",
  foreignKeys = [
    ForeignKey(
      entity = KTPDocumentEntity::class,
      parentColumns = ["id"],
      childColumns = ["ktpDocumentId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = UserEntity::class,
      parentColumns = ["userId"],
      childColumns = ["teacherId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["ktpDocumentId"]),
    Index(value = ["teacherId"]),
    Index(value = ["classId"]),
    Index(value = ["date"])
  ]
)
data class KTPEntryEntity(
  @PrimaryKey
  val id: String,
  val ktpDocumentId: String,
  val teacherId: String,
  val classId: String, // Associated Class ID (e.g. 7-A)
  val classNameDisplay: String, // e.g. "7-A"
  val subjectId: String, // e.g. "Matematika"
  val date: String, // YYYY-MM-DD
  val weekNumber: Int = 1,
  val lessonNumber: Int = 1, // 1..8
  val topic: String, // e.g. "Oddiy kasrlar"
  val hours: Int = 1,
  val room: String = "24-xona",
  val confidence: KTPConfidence = KTPConfidence.HIGH,
  val source: String = "KTP", // "KTP", "SCHEDULE", "MANUAL"
  val manualOverride: Boolean = false,
  val status: KTPEntryStatus = KTPEntryStatus.VALID,
  val warningMessage: String? = null,
  val isConfirmed: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

data class KTPDocumentWithEntries(
  val document: KTPDocumentEntity,
  val entries: List<KTPEntryEntity>
)
