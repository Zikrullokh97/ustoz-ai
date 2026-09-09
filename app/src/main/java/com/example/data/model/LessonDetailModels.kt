package com.example.data.model

data class LessonWithDetails(
  val lesson: LessonEntity,
  val className: String,
  val studentCount: Int,
  val attendanceTaken: Boolean,
  val presentCount: Int = 0,
  val lateCount: Int = 0,
  val absentCount: Int = 0,
  val excusedCount: Int = 0
)

data class StudentAttendanceItem(
  val student: StudentEntity,
  val status: AttendanceStatus?,
  val lateMinutes: Int? = null,
  val note: String? = null,
  val source: String? = "MANUAL",
  val detectionState: AttendanceDetectionState = AttendanceDetectionState.NOT_DETECTED,
  val confidence: Float? = null,
  val manualOverride: Boolean = false,
  val originalSource: String? = null
)

data class BulkAttendanceRecord(
  val studentId: String,
  val status: AttendanceStatus,
  val lateMinutes: Int? = null,
  val note: String? = null,
  val source: String = "MANUAL",
  val manualOverride: Boolean = false,
  val originalSource: String? = null,
  val confidence: Float? = null
)

data class AttendanceSummary(
  val totalStudents: Int,
  val presentCount: Int,
  val lateCount: Int,
  val absentCount: Int,
  val excusedCount: Int
) {
  val isAllMarked: Boolean
    get() = (presentCount + lateCount + absentCount + excusedCount) == totalStudents && totalStudents > 0
}

data class ScheduleWithClass(
  val schedule: ScheduleEntity,
  val className: String
)
