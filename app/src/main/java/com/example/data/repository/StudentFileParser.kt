package com.example.data.repository

import com.example.data.model.ParsedStudentDraft
import com.example.data.model.StudentEntity
import com.example.data.model.StudentImportPreview
import com.example.data.model.StudentImportStatus
import java.util.UUID

object StudentFileParser {

  private val CLASS_REGEX = Regex("""(?i)\b([1-9]|1[0-1])\s*[-_ ]\s*([A-Za-zА-Яа-яЎўҚқҒғҲҳ])\b""")

  /**
   * Parses raw file content text and extracts students, detecting class name if present.
   */
  fun parseStudents(
    content: String,
    fileName: String,
    existingStudentsInClass: List<StudentEntity>,
    allTeacherStudents: List<StudentEntity>
  ): Result<StudentImportPreview> {
    val extension = fileName.substringAfterLast('.', "").lowercase()

    // Format validation
    val supportedExtensions = listOf("csv", "txt", "tsv", "xlsx", "xls", "docx", "doc", "pdf")
    if (extension !in supportedExtensions) {
      return Result.failure(
        IllegalArgumentException(".$extension formati qo'llab-quvvatlanmaydi. Iltimos CSV, TXT, XLSX yoki DOCX faylidan foydalaning.")
      )
    }

    val lines = content.lines()
      .map { it.trim() }
      .filter { it.isNotBlank() }

    if (lines.isEmpty()) {
      return Result.failure(IllegalArgumentException("Fayl bo'sh yoki o'qib bo'lmadi."))
    }

    // Try to detect class from entire text
    var detectedClass: String? = null
    for (line in lines.take(10)) {
      val match = CLASS_REGEX.find(line)
      if (match != null) {
        val grade = match.groupValues[1]
        val sec = match.groupValues[2].uppercase()
        detectedClass = "$grade-$sec"
        break
      }
    }

    val drafts = mutableListOf<ParsedStudentDraft>()
    val existingNameSet = existingStudentsInClass.map { normalizeName(it.fullName) }.toSet()
    val existingNumberSet = existingStudentsInClass.map { it.studentNumber.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    val allTeacherNameSet = allTeacherStudents.map { normalizeName(it.fullName) }.toSet()

    for (line in lines) {
      // Skip header lines
      val lower = line.lowercase()
      if (lower.startsWith("ism") || lower.startsWith("№") || lower.startsWith("no") ||
        lower.startsWith("familya") || lower.startsWith("id") || lower.startsWith("student") ||
        lower.contains("o'quvchi") && lower.contains("sinf")
      ) {
        continue
      }

      val draft = parseLine(line, detectedClass) ?: continue
      val normFullName = normalizeName(draft.fullName)
      val cleanNumber = draft.studentNumber.trim().lowercase()

      val status: StudentImportStatus
      val note: String?

      if (cleanNumber.isNotBlank() && existingNumberSet.contains(cleanNumber)) {
        status = StudentImportStatus.EXISTING
        note = "Bu raqamli o'quvchi sinfda allaqachon mavjud"
      } else if (existingNameSet.contains(normFullName)) {
        status = StudentImportStatus.SUSPECTED_DUPLICATE
        note = "Shu ism-familiyadagi o'quvchi allaqachon ushbu sinfda mavjud"
      } else if (allTeacherNameSet.contains(normFullName)) {
        status = StudentImportStatus.SUSPECTED_DUPLICATE
        note = "Boshqa sinfingizda xuddi shunday ismli o'quvchi bor"
      } else if (draft.firstName.isBlank()) {
        status = StudentImportStatus.INVALID
        note = "O'quvchi ismi aniqlanmadi"
      } else {
        status = StudentImportStatus.NEW
        note = null
      }

      drafts.add(
        draft.copy(
          status = status,
          statusNote = note,
          isSelectedForImport = (status == StudentImportStatus.NEW)
        )
      )
    }

    if (drafts.isEmpty()) {
      return Result.failure(
        IllegalArgumentException("Fayl tuzilmasini aniqlab bo'lmadi. O'quvchilar ro'yxati topilmadi.")
      )
    }

    val preview = StudentImportPreview(
      totalFound = drafts.size,
      newCount = drafts.count { it.status == StudentImportStatus.NEW },
      existingCount = drafts.count { it.status == StudentImportStatus.EXISTING },
      suspectedDuplicateCount = drafts.count { it.status == StudentImportStatus.SUSPECTED_DUPLICATE },
      invalidCount = drafts.count { it.status == StudentImportStatus.INVALID },
      detectedClassName = detectedClass,
      drafts = drafts
    )

    return Result.success(preview)
  }

  private fun parseLine(line: String, detectedClass: String?): ParsedStudentDraft? {
    // Delimiters: comma, semicolon, tab, pipe
    val tokens: List<String> = when {
      line.contains("\t") -> line.split("\t")
      line.contains(";") -> line.split(";")
      line.contains(",") -> line.split(",")
      line.contains("|") -> line.split("|")
      else -> listOf(line)
    }.map { it.trim().removeSurrounding("\"").removeSurrounding("'") }

    val tempId = "draft_" + UUID.randomUUID().toString().take(8)

    if (tokens.size >= 2) {
      // Possible structured CSV: e.g. [No/ID, LastName, FirstName, BirthDate, Gender, Phone]
      // or [ID, FullName, ...]
      val firstToken = tokens[0]
      val secondToken = tokens[1]

      // Check if token 0 is a number / ID
      val isFirstTokenNumber = firstToken.matches(Regex("""^\d+$""")) || firstToken.startsWith("ST-")

      val studentNumber: String
      val lastName: String
      val firstName: String

      if (isFirstTokenNumber && tokens.size >= 3) {
        studentNumber = firstToken
        // Check if token 1 is LastName and token 2 is FirstName
        lastName = secondToken
        firstName = tokens[2]
      } else if (isFirstTokenNumber && tokens.size == 2) {
        studentNumber = firstToken
        val parts = secondToken.split(" ").filter { it.isNotBlank() }
        lastName = parts.getOrNull(0) ?: ""
        firstName = parts.drop(1).joinToString(" ").ifBlank { "" }
      } else {
        studentNumber = ""
        val parts = firstToken.split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) {
          lastName = parts[0]
          firstName = parts.drop(1).joinToString(" ")
        } else {
          lastName = firstToken
          firstName = secondToken
        }
      }

      val birthDate = tokens.firstOrNull { it.matches(Regex("""^\d{4}-\d{2}-\d{2}$|^\d{2}\.\d{2}\.\d{4}$""")) } ?: ""
      val phone = tokens.firstOrNull { it.matches(Regex("""^\+?\d{9,13}$""")) } ?: ""
      val gender = when {
        tokens.any { it.equals("o'g'il", ignoreCase = true) || it.equals("m", ignoreCase = true) || it.equals("erkak", ignoreCase = true) } -> "O'g'il"
        tokens.any { it.equals("qiz", ignoreCase = true) || it.equals("f", ignoreCase = true) || it.equals("ayol", ignoreCase = true) } -> "Qiz"
        else -> ""
      }

      if (firstName.isNotBlank() || lastName.isNotBlank()) {
        return ParsedStudentDraft(
          tempId = tempId,
          firstName = cleanWord(firstName),
          lastName = cleanWord(lastName),
          studentNumber = studentNumber,
          birthDate = birthDate,
          gender = gender,
          phone = phone,
          detectedClass = detectedClass
        )
      }
    }

    // Fallback: Numbered list e.g. "1. Aziz Aliyev" or "2) Karimova Nilufar"
    val cleanLine = line.replace(Regex("""^\d+[\.\)\-\s]+"""), "").trim()
    val words = cleanLine.split(" ").filter { it.isNotBlank() }
    if (words.size >= 2) {
      val lastName = words[0]
      val firstName = words.drop(1).joinToString(" ")
      return ParsedStudentDraft(
        tempId = tempId,
        firstName = cleanWord(firstName),
        lastName = cleanWord(lastName),
        studentNumber = "",
        birthDate = "",
        gender = "",
        phone = "",
        detectedClass = detectedClass
      )
    } else if (words.size == 1 && words[0].length >= 3) {
      return ParsedStudentDraft(
        tempId = tempId,
        firstName = cleanWord(words[0]),
        lastName = "",
        studentNumber = "",
        birthDate = "",
        gender = "",
        phone = "",
        detectedClass = detectedClass
      )
    }

    return null
  }

  private fun cleanWord(word: String): String {
    return word.replace(Regex("""[^\p{L}\p{Nd}'ʼ`\-\s]"""), "").trim()
  }

  private fun normalizeName(name: String): String {
    return name.lowercase()
      .replace("ʻ", "'")
      .replace("ʼ", "'")
      .replace("`", "'")
      .replace("'", "")
      .replace(Regex("""\s+"""), " ")
      .trim()
  }
}
