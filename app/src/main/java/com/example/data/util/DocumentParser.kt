package com.example.data.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.zip.InflaterInputStream
import java.util.zip.ZipInputStream

object DocumentParser {

  private const val MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024 // 15 MB

  /**
   * Retrieves display name and size from SAF Uri.
   */
  fun readFileNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
    var name = "fayl"
    var size = -1L

    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (nameIndex != -1) {
            name = cursor.getString(nameIndex) ?: name
          }
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
            size = cursor.getLong(sizeIndex)
          }
        }
      }
    } catch (_: Exception) {
      // Fallback to uri path if cursor query fails
      name = uri.lastPathSegment?.substringAfterLast('/') ?: "fayl"
    }

    if (size <= 0) {
      try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          size = stream.available().toLong()
        }
      } catch (_: Exception) {
        // Ignore
      }
    }

    return Pair(name, size)
  }

  /**
   * Validates and parses document content into text.
   */
  fun parseDocumentText(
    context: Context,
    uri: Uri,
    fileName: String,
    allowPdf: Boolean = true
  ): Result<String> {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    val supported = if (allowPdf) {
      listOf("csv", "txt", "tsv", "xlsx", "xls", "docx", "doc", "pdf")
    } else {
      listOf("csv", "txt", "tsv", "xlsx", "xls", "docx", "doc")
    }

    if (extension !in supported) {
      return Result.failure(
        IllegalArgumentException("Fayl formati qo'llab-quvvatlanmaydi: .$extension. Iltimos XLSX, DOCX, CSV yoki TXT fayl tanlang.")
      )
    }

    val (_, reportedSize) = readFileNameAndSize(context, uri)
    if (reportedSize > MAX_FILE_SIZE_BYTES) {
      return Result.failure(
        IllegalArgumentException("Fayl hajmi juda katta (maksimal 15 MB).")
      )
    }

    return try {
      val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: return Result.failure(IllegalArgumentException("Faylni o'qib bo'lmadi."))

      if (bytes.isEmpty()) {
        return Result.failure(IllegalArgumentException("Fayl bo'sh."))
      }

      if (bytes.size > MAX_FILE_SIZE_BYTES) {
        return Result.failure(IllegalArgumentException("Fayl hajmi juda katta (maksimal 15 MB)."))
      }

      val text = when (extension) {
        "csv", "txt", "tsv" -> parsePlainText(bytes)
        "xlsx" -> parseXlsx(bytes)
        "docx" -> parseDocx(bytes)
        "pdf" -> parsePdf(bytes)
        else -> parsePlainText(bytes)
      }

      if (text.isBlank()) {
        Result.failure(IllegalArgumentException("Fayldan ma'lumot topilmadi yoki fayl shifrlangan/zararlangan."))
      } else {
        Result.success(text)
      }
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("Faylni o'qishda xatolik yuz berdi: ${e.localizedMessage ?: "Faylni ochib bo'lmadi"}"))
    }
  }

  private fun parsePlainText(bytes: ByteArray): String {
    // Check UTF-8 BOM
    val content = if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
      String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
    } else {
      // Try UTF-8 first, fallback to Windows-1251 if cyrillic characters are garbled
      try {
        String(bytes, Charsets.UTF_8)
      } catch (_: Exception) {
        String(bytes, Charset.forName("windows-1251"))
      }
    }
    return content.trim()
  }

  /**
   * Real XLSX parsing from ZIP bytes without heavy external POI dependencies.
   * Extracts shared strings and sheet cell contents.
   */
  private fun parseXlsx(bytes: ByteArray): String {
    val sharedStrings = mutableListOf<String>()
    val sheetXmlBytes = mutableMapOf<String, ByteArray>()

    ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        val name = entry.name
        when {
          name == "xl/sharedStrings.xml" -> {
            val sstBytes = zis.readBytes()
            sharedStrings.addAll(extractSharedStrings(sstBytes))
          }
          name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml") -> {
            sheetXmlBytes[name] = zis.readBytes()
          }
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }

    val firstSheet = sheetXmlBytes["xl/worksheets/sheet1.xml"]
      ?: sheetXmlBytes.values.firstOrNull()
      ?: return ""

    return extractRowsFromSheetXml(firstSheet, sharedStrings)
  }

  private fun extractSharedStrings(xmlBytes: ByteArray): List<String> {
    val xml = String(xmlBytes, Charsets.UTF_8)
    val list = mutableListOf<String>()
    // Match <si>...</si> blocks
    val siRegex = Regex("""<si>(.*?)</si>""", RegexOption.DOT_MATCHES_ALL)
    val tRegex = Regex("""<t(?:\s+[^>]*)?>(.*?)</t>""", RegexOption.DOT_MATCHES_ALL)

    siRegex.findAll(xml).forEach { match ->
      val siContent = match.groupValues[1]
      val textPieces = tRegex.findAll(siContent).map { unescapeXml(it.groupValues[1]) }.joinToString("")
      list.add(textPieces)
    }
    return list
  }

  private fun extractRowsFromSheetXml(xmlBytes: ByteArray, sharedStrings: List<String>): String {
    val xml = String(xmlBytes, Charsets.UTF_8)
    val sb = StringBuilder()

    val rowRegex = Regex("""<row\b[^>]*>(.*?)</row>""", RegexOption.DOT_MATCHES_ALL)
    val cellRegex = Regex("""<c\b([^>]*)>(.*?)</c>""", RegexOption.DOT_MATCHES_ALL)
    val valRegex = Regex("""<v>(.*?)</v>""", RegexOption.DOT_MATCHES_ALL)
    val inlineTextRegex = Regex("""<t(?:\s+[^>]*)?>(.*?)</t>""", RegexOption.DOT_MATCHES_ALL)

    rowRegex.findAll(xml).forEach { rowMatch ->
      val rowContent = rowMatch.groupValues[1]
      val cellValues = mutableListOf<String>()

      cellRegex.findAll(rowContent).forEach { cellMatch ->
        val cellAttrs = cellMatch.groupValues[1]
        val cellInner = cellMatch.groupValues[2]

        val isSharedString = cellAttrs.contains("""t="s"""")
        val isInlineStr = cellAttrs.contains("""t="inlineStr"""") || cellAttrs.contains("""t="str"""")

        val value: String = when {
          isSharedString -> {
            val vMatch = valRegex.find(cellInner)
            val idx = vMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1
            if (idx in sharedStrings.indices) sharedStrings[idx] else ""
          }
          isInlineStr -> {
            val tMatch = inlineTextRegex.find(cellInner)
            unescapeXml(tMatch?.groupValues?.get(1) ?: "")
          }
          else -> {
            val vMatch = valRegex.find(cellInner)
            unescapeXml(vMatch?.groupValues?.get(1) ?: "")
          }
        }
        cellValues.add(value.trim())
      }

      if (cellValues.any { it.isNotBlank() }) {
        sb.append(cellValues.joinToString("\t")).append("\n")
      }
    }

    return sb.toString().trim()
  }

  /**
   * Real DOCX parsing from ZIP bytes.
   * Extracts paragraphs and table rows from word/document.xml.
   */
  private fun parseDocx(bytes: ByteArray): String {
    var documentXml: ByteArray? = null

    ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        if (entry.name == "word/document.xml") {
          documentXml = zis.readBytes()
          break
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }

    if (documentXml == null) return ""
    val xml = String(documentXml!!, Charsets.UTF_8)
    val sb = StringBuilder()

    // Table rows: <w:tr> containing <w:tc> (cells)
    val tableRowRegex = Regex("""<w:tr\b[^>]*>(.*?)</w:tr>""", RegexOption.DOT_MATCHES_ALL)
    val cellRegex = Regex("""<w:tc\b[^>]*>(.*?)</w:tc>""", RegexOption.DOT_MATCHES_ALL)
    val tRegex = Regex("""<w:t(?:\s+[^>]*)?>(.*?)</w:t>""", RegexOption.DOT_MATCHES_ALL)

    val rows = tableRowRegex.findAll(xml).toList()
    if (rows.isNotEmpty()) {
      for (row in rows) {
        val rowContent = row.groupValues[1]
        val cellValues = mutableListOf<String>()
        cellRegex.findAll(rowContent).forEach { cell ->
          val cellText = tRegex.findAll(cell.groupValues[1]).map { unescapeXml(it.groupValues[1]) }.joinToString("")
          cellValues.add(cellText.trim())
        }
        if (cellValues.any { it.isNotBlank() }) {
          sb.append(cellValues.joinToString("\t")).append("\n")
        }
      }
    } else {
      // Paragraphs: <w:p>
      val pRegex = Regex("""<w:p\b[^>]*>(.*?)</w:p>""", RegexOption.DOT_MATCHES_ALL)
      pRegex.findAll(xml).forEach { p ->
        val text = tRegex.findAll(p.groupValues[1]).map { unescapeXml(it.groupValues[1]) }.joinToString("")
        if (text.isNotBlank()) {
          sb.append(text.trim()).append("\n")
        }
      }
    }

    return sb.toString().trim()
  }

  /**
   * Basic PDF text extraction for standard text objects.
   */
  private fun parsePdf(bytes: ByteArray): String {
    val sb = StringBuilder()
    val content = String(bytes, Charsets.ISO_8859_1)

    // Match text showing operators Tj and TJ
    val tjRegex = Regex("""\(([^)]*)\)\s*Tj""")
    val tjArrayRegex = Regex("""\[(.*?)\]\s*TJ""")

    tjRegex.findAll(content).forEach { match ->
      val text = match.groupValues[1].replace("\\n", "\n").replace("\\r", "").replace("\\t", " ")
      if (text.isNotBlank()) {
        sb.append(text).append(" ")
      }
    }

    tjArrayRegex.findAll(content).forEach { match ->
      val arrayContent = match.groupValues[1]
      val subTj = Regex("""\(([^)]*)\)""").findAll(arrayContent).map { it.groupValues[1] }.joinToString("")
      if (subTj.isNotBlank()) {
        sb.append(subTj).append("\n")
      }
    }

    if (sb.isBlank()) {
      // Attempt flate streams
      val streamRegex = Regex("""stream[\r\n]+(.*?)[\r\n]+endstream""", RegexOption.DOT_MATCHES_ALL)
      streamRegex.findAll(content).take(20).forEach { streamMatch ->
        try {
          val streamRaw = streamMatch.groupValues[1].toByteArray(Charsets.ISO_8859_1)
          val inflated = InflaterInputStream(ByteArrayInputStream(streamRaw)).use { it.readBytes() }
          val inflatedText = String(inflated, Charsets.UTF_8)
          tjRegex.findAll(inflatedText).forEach { m ->
            sb.append(m.groupValues[1]).append(" ")
          }
          sb.append("\n")
        } catch (_: Exception) {
          // Ignore non-flate streams
        }
      }
    }

    return sb.toString().trim()
  }

  private fun unescapeXml(text: String): String {
    return text.replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&apos;", "'")
  }
}
