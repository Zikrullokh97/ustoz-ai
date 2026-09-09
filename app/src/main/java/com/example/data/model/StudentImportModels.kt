package com.example.data.model

enum class StudentImportStatus {
  NEW,
  EXISTING,
  SUSPECTED_DUPLICATE,
  INVALID
}

data class ParsedStudentDraft(
  val tempId: String,
  val firstName: String,
  val lastName: String,
  val studentNumber: String,
  val birthDate: String,
  val gender: String, // "O'g'il", "Qiz", or ""
  val phone: String,
  val detectedClass: String? = null,
  val status: StudentImportStatus = StudentImportStatus.NEW,
  val statusNote: String? = null,
  val isSelectedForImport: Boolean = true
) {
  val fullName: String
    get() = "$firstName $lastName".trim()
}

data class StudentImportPreview(
  val totalFound: Int,
  val newCount: Int,
  val existingCount: Int,
  val suspectedDuplicateCount: Int,
  val invalidCount: Int,
  val detectedClassName: String?,
  val drafts: List<ParsedStudentDraft>
)
