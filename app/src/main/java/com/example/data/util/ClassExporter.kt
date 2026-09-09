package com.example.data.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ClassEntity
import com.example.data.model.StudentEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
  CSV("csv", "text/csv", "CSV (.csv)"),
  XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel (.xlsx)"),
  TXT("txt", "text/plain", "Matn (.txt)")
}

object ClassExporter {

  /**
   * Generates a real exported file for class students.
   * Performs ownership validation and returns generated file.
   */
  fun exportClassStudents(
    context: Context,
    classEntity: ClassEntity,
    students: List<StudentEntity>,
    format: ExportFormat,
    currentTeacherId: String
  ): Result<File> {
    // 1. Strict Ownership Check
    if (classEntity.teacherId != currentTeacherId) {
      return Result.failure(
        SecurityException("403 Forbidden: Siz faqat o'zingizga tegishli sinf ma'lumotlarini eksport qila olasiz.")
      )
    }

    val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val sanitizedClassName = classEntity.name.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_")
    val fileName = "UstozAI_${sanitizedClassName}_Oquvchilar_${dateStr}.${format.extension}"

    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val targetFile = File(exportDir, fileName)

    return try {
      when (format) {
        ExportFormat.CSV -> generateCsvFile(targetFile, classEntity, students)
        ExportFormat.XLSX -> generateXlsxFile(targetFile, classEntity, students)
        ExportFormat.TXT -> generateTxtFile(targetFile, classEntity, students)
      }
      Result.success(targetFile)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Writes the exported bytes directly to a user-selected SAF Uri (e.g. ActivityResultContracts.CreateDocument).
   */
  fun exportToUri(
    context: Context,
    classEntity: ClassEntity,
    students: List<StudentEntity>,
    format: ExportFormat,
    currentTeacherId: String,
    targetUri: Uri
  ): Result<Unit> {
    if (classEntity.teacherId != currentTeacherId) {
      return Result.failure(
        SecurityException("403 Forbidden: Siz faqat o'zingizga tegishli sinf ma'lumotlarini eksport qila olasiz.")
      )
    }

    return try {
      val tempFile = exportClassStudents(context, classEntity, students, format, currentTeacherId).getOrThrow()
      context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
        tempFile.inputStream().use { inStream ->
          inStream.copyTo(outStream)
        }
      } ?: return Result.failure(IllegalStateException("Faylga yozib bo'lmadi"))
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Creates a Share Intent with FileProvider URI.
   */
  fun createShareIntent(context: Context, file: File, mimeType: String): Intent {
    val contentUri: Uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )

    return Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, contentUri)
      putExtra(Intent.EXTRA_SUBJECT, file.name)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }

  private fun generateCsvFile(file: File, classEntity: ClassEntity, students: List<StudentEntity>) {
    FileOutputStream(file).use { fos ->
      // UTF-8 BOM for proper Cyrillic / Uzbek character rendering in MS Excel
      fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

      val sb = StringBuilder()
      // Headers
      sb.append("№,F.I.Sh.,O'quvchi raqami,Tug'ilgan sana,Jinsi,Telefon\n")

      students.sortedBy { it.fullName }.forEachIndexed { index, student ->
        val num = index + 1
        val name = escapeCsv(student.fullName)
        val studentNumber = escapeCsv(student.studentNumber)
        val birthDate = escapeCsv(student.birthDate ?: "")
        val gender = escapeCsv(student.gender ?: "")
        val phone = escapeCsv(student.phone ?: "")
        sb.append("$num,$name,$studentNumber,$birthDate,$gender,$phone\n")
      }

      fos.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
    }
  }

  private fun generateTxtFile(file: File, classEntity: ClassEntity, students: List<StudentEntity>) {
    FileOutputStream(file).use { fos ->
      val sb = StringBuilder()
      sb.append("USTOZ AI — SINF O'QUVCHILARI RO'YXATI\n")
      sb.append("Sinf: ${classEntity.name}\n")
      sb.append("Sana: ${LocalDate.now()}\n")
      sb.append("Jami o'quvchilar: ${students.size}\n\n")
      sb.append(String.format("%-4s %-30s %-15s %-12s %-8s %s\n", "№", "F.I.Sh.", "Raqami", "Tug'ilgan", "Jinsi", "Telefon"))
      sb.append("-".repeat(85)).append("\n")

      students.sortedBy { it.fullName }.forEachIndexed { index, student ->
        sb.append(
          String.format(
            "%-4d %-30s %-15s %-12s %-8s %s\n",
            index + 1,
            student.fullName.take(28),
            (student.studentNumber).take(13),
            (student.birthDate ?: "-").take(10),
            (student.gender ?: "-").take(6),
            student.phone ?: "-"
          )
        )
      }
      fos.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
    }
  }

  /**
   * Generates authentic, standard OpenXML Spreadsheet (XLSX).
   */
  private fun generateXlsxFile(file: File, classEntity: ClassEntity, students: List<StudentEntity>) {
    val sortedStudents = students.sortedBy { it.fullName }

    // Build sheet1.xml
    val sheetXml = StringBuilder()
    sheetXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    sheetXml.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
    sheetXml.append("""<sheetData>""")

    // Row 1: Title
    sheetXml.append("""<row r="1">""")
    sheetXml.append("""<c r="A1" t="inlineStr"><is><t>${escapeXml("Ustoz AI — ${classEntity.name} o'quvchilari ro'yxati (${LocalDate.now()})")}</t></is></c>""")
    sheetXml.append("""</row>""")

    // Row 2: Empty
    sheetXml.append("""<row r="2"/>""")

    // Row 3: Table Headers
    sheetXml.append("""<row r="3">""")
    sheetXml.append("""<c r="A3" t="inlineStr"><is><t>№</t></is></c>""")
    sheetXml.append("""<c r="B3" t="inlineStr"><is><t>F.I.Sh.</t></is></c>""")
    sheetXml.append("""<c r="C3" t="inlineStr"><is><t>O'quvchi raqami</t></is></c>""")
    sheetXml.append("""<c r="D3" t="inlineStr"><is><t>Tug'ilgan sana</t></is></c>""")
    sheetXml.append("""<c r="E3" t="inlineStr"><is><t>Jinsi</t></is></c>""")
    sheetXml.append("""<c r="F3" t="inlineStr"><is><t>Telefon</t></is></c>""")
    sheetXml.append("""</row>""")

    // Rows 4+: Student records
    sortedStudents.forEachIndexed { index, student ->
      val rowNum = index + 4
      sheetXml.append("""<row r="$rowNum">""")
      sheetXml.append("""<c r="A$rowNum"><v>${index + 1}</v></c>""")
      sheetXml.append("""<c r="B$rowNum" t="inlineStr"><is><t>${escapeXml(student.fullName)}</t></is></c>""")
      sheetXml.append("""<c r="C$rowNum" t="inlineStr"><is><t>${escapeXml(student.studentNumber)}</t></is></c>""")
      sheetXml.append("""<c r="D$rowNum" t="inlineStr"><is><t>${escapeXml(student.birthDate ?: "")}</t></is></c>""")
      sheetXml.append("""<c r="E$rowNum" t="inlineStr"><is><t>${escapeXml(student.gender ?: "")}</t></is></c>""")
      sheetXml.append("""<c r="F$rowNum" t="inlineStr"><is><t>${escapeXml(student.phone ?: "")}</t></is></c>""")
      sheetXml.append("""</row>""")
    }

    sheetXml.append("""</sheetData>""")
    sheetXml.append("""</worksheet>""")

    // Package standard OpenXML zip archive
    FileOutputStream(file).use { fos ->
      ZipOutputStream(fos).use { zos ->
        addZipEntry(zos, "[Content_Types].xml", CONTENT_TYPES_XML)
        addZipEntry(zos, "_rels/.rels", ROOT_RELS_XML)
        addZipEntry(zos, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
        addZipEntry(zos, "xl/workbook.xml", WORKBOOK_XML)
        addZipEntry(zos, "xl/styles.xml", STYLES_XML)
        addZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml.toString())
      }
    }
  }

  private fun addZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
    val entry = ZipEntry(entryName)
    zos.putNextEntry(entry)
    zos.write(content.toByteArray(StandardCharsets.UTF_8))
    zos.closeEntry()
  }

  private fun escapeCsv(value: String): String {
    val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
    return if (containsSpecial) {
      "\"" + value.replace("\"", "\"\"") + "\""
    } else {
      value
    }
  }

  private fun escapeXml(text: String): String {
    return text.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")
  }

  // Minimal standard OpenXML parts required for valid Excel spreadsheet
  private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

  private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

  private const val WORKBOOK_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

  private const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="O'quvchilar" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

  private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="1">
    <font><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="1">
    <fill><patternFill patternType="none"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
  </cellXfs>
</styleSheet>"""
}
