package com.example.ui.students

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ParsedStudentDraft
import com.example.data.model.StudentImportPreview
import com.example.data.model.StudentImportStatus
import com.example.data.util.DocumentParser
import com.example.ui.main.MainAppUiState
import com.example.ui.main.MainAppViewModel
import com.example.ui.main.StudentImportResultSummary
import com.example.ui.theme.AbsentColor
import com.example.ui.theme.LateColor
import com.example.ui.theme.PresentColor
import com.example.ui.theme.UstozPrimary
import com.example.ui.theme.UstozPrimaryContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentImportDialog(
  uiState: MainAppUiState,
  viewModel: MainAppViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var selectedClassId by remember {
    mutableStateOf(uiState.importTargetClassId ?: uiState.classes.firstOrNull()?.classEntity?.id ?: "")
  }
  var classExpanded by remember { mutableStateOf(false) }

  var selectedFileName by remember { mutableStateOf<String?>(null) }
  var selectedFileSizeFormatted by remember { mutableStateOf<String?>(null) }
  var isReadingFile by remember { mutableStateOf(false) }
  var fileErrorMessage by remember { mutableStateOf<String?>(null) }

  var editingDraft by remember { mutableStateOf<ParsedStudentDraft?>(null) }
  var previewFilter by remember { mutableStateOf("ALL") }

  // Real Android File Picker Launcher
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri == null) return@rememberLauncherForActivityResult

    fileErrorMessage = null
    isReadingFile = true

    coroutineScope.launch {
      val (fileName, sizeBytes) = withContext(Dispatchers.IO) {
        DocumentParser.readFileNameAndSize(context, uri)
      }

      val sizeStr = when {
        sizeBytes <= 0 -> ""
        sizeBytes < 1024 -> "$sizeBytes B"
        sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
        else -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
      }

      selectedFileName = fileName
      selectedFileSizeFormatted = sizeStr

      val parseResult = withContext(Dispatchers.IO) {
        DocumentParser.parseDocumentText(context, uri, fileName, allowPdf = false)
      }

      isReadingFile = false

      parseResult.fold(
        onSuccess = { extractedText ->
          if (selectedClassId.isNotBlank()) {
            viewModel.parseStudentImportFile(selectedClassId, fileName, extractedText)
          } else {
            fileErrorMessage = "Iltimos, avval sinfni tanlang"
          }
        },
        onFailure = { error ->
          fileErrorMessage = error.message ?: "Faylni o'qishda xatolik yuz berdi"
        }
      )
    }
  }

  // If previous import just completed with stats, show result summary dialog
  uiState.lastImportResult?.let { result ->
    ImportResultSummaryDialog(
      result = result,
      onClose = {
        viewModel.clearLastImportResult()
        onDismiss()
      }
    )
    return
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.92f)
        .testTag("student_import_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(UstozPrimaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.FileUpload, contentDescription = null, tint = UstozPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "O'quvchilarni fayldan yuklash",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "XLSX, XLS, CSV, DOCX, TXT formatlar qo'llab-quvvatlanadi",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val preview = uiState.importStudentsPreview

        if (preview == null) {
          // ================= STEP 1: FILE PICKER & CONFIG =================
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // Target Class Selector
            Text(
              text = "1. Qaysi sinfga import qilmoqchisiz?",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )

            ExposedDropdownMenuBox(
              expanded = classExpanded,
              onExpandedChange = { classExpanded = !classExpanded }
            ) {
              val currentClassName = uiState.classes.firstOrNull { it.classEntity.id == selectedClassId }?.classEntity?.name ?: "Sinfni tanlang"
              OutlinedTextField(
                value = currentClassName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Maqsadli sinf *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                  .testTag("import_class_selector")
              )
              ExposedDropdownMenu(
                expanded = classExpanded,
                onDismissRequest = { classExpanded = false }
              ) {
                uiState.classes.forEach { cls ->
                  DropdownMenuItem(
                    text = { Text("${cls.classEntity.name} (${cls.studentCount} o'quvchi)") },
                    onClick = {
                      selectedClassId = cls.classEntity.id
                      classExpanded = false
                    }
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // File Picker Action Area
            Text(
              text = "2. Telefoningizdan faylni tanlang",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Card(
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, UstozPrimary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Box(
                  modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(UstozPrimaryContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = UstozPrimary,
                    modifier = Modifier.size(32.dp)
                  )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                  text = "Faylni tanlash uchun bosing",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                  text = "Android xotirasidan .xlsx, .csv, .docx yoki .txt fayl",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                  onClick = {
                    if (selectedClassId.isBlank()) {
                      fileErrorMessage = "Iltimos, avval maqsadli sinfni tanlang"
                      return@Button
                    }
                    filePickerLauncher.launch(
                      arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "text/csv",
                        "text/plain",
                        "text/comma-separated-values",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/msword",
                        "*/*"
                      )
                    )
                  },
                  shape = CircleShape,
                  colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
                  modifier = Modifier.testTag("pick_student_file_button")
                ) {
                  Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Fayl tanlash")
                }
              }
            }

            // Selected file indicator / Loading state
            if (isReadingFile || uiState.isLoading) {
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = UstozPrimaryContainer.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = UstozPrimary)
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = "Fayl o'qilmoqda va tahlil qilinmoqda...",
                    fontSize = 13.sp,
                    color = UstozPrimary,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }

            // Selected File Details (if picked)
            selectedFileName?.let { name ->
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = UstozPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      selectedFileSizeFormatted?.let { size ->
                        Text(text = size, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                  }
                }
              }
            }

            // Error message display
            fileErrorMessage?.let { err ->
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = err, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        } else {
          // ================= STEP 2: PREVIEW, FILTER, EDIT & CONFIRM =================
          Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Summary KPI Badges
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ImportKpiCard(
                label = "Jami",
                count = preview.totalFound,
                color = MaterialTheme.colorScheme.onSurface,
                isSelected = previewFilter == "ALL",
                onClick = { previewFilter = "ALL" },
                modifier = Modifier.weight(1f)
              )
              ImportKpiCard(
                label = "Yangi",
                count = preview.newCount,
                color = PresentColor,
                isSelected = previewFilter == "NEW",
                onClick = { previewFilter = "NEW" },
                modifier = Modifier.weight(1f)
              )
              ImportKpiCard(
                label = "Dublikat",
                count = preview.existingCount + preview.suspectedDuplicateCount,
                color = LateColor,
                isSelected = previewFilter == "DUPLICATES",
                onClick = { previewFilter = "DUPLICATES" },
                modifier = Modifier.weight(1f)
              )
              ImportKpiCard(
                label = "Xatolik",
                count = preview.invalidCount,
                color = AbsentColor,
                isSelected = previewFilter == "ERRORS",
                onClick = { previewFilter = "ERRORS" },
                modifier = Modifier.weight(1f)
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filtered drafts list
            val filteredDrafts = preview.drafts.filter { draft ->
              when (previewFilter) {
                "NEW" -> draft.status == StudentImportStatus.NEW
                "DUPLICATES" -> draft.status == StudentImportStatus.EXISTING || draft.status == StudentImportStatus.SUSPECTED_DUPLICATE
                "ERRORS" -> draft.status == StudentImportStatus.INVALID
                else -> true
              }
            }

            Text(
              text = "O'quvchilar ro'yxati (${filteredDrafts.size} ta)",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(filteredDrafts, key = { it.tempId }) { draft ->
                ImportStudentRow(
                  draft = draft,
                  onToggleSelection = { viewModel.toggleStudentImportSelection(draft.tempId) },
                  onEditClick = { editingDraft = draft }
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            val selectedCount = preview.drafts.count { it.isSelectedForImport }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedButton(
                onClick = {
                  viewModel.openStudentImportDialog(selectedClassId)
                },
                shape = RoundedCornerShape(12.dp)
              ) {
                Text("Boshqa fayl tanlash", fontSize = 12.sp)
              }

              Button(
                onClick = {
                  viewModel.confirmStudentImport(selectedClassId)
                },
                enabled = selectedCount > 0 && !uiState.isImportingStudents,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
                modifier = Modifier.testTag("confirm_import_button")
              ) {
                if (uiState.isImportingStudents) {
                  CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Saqlanmoqda...", fontSize = 13.sp)
                } else {
                  Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("$selectedCount ta o'quvchini import qilish", fontSize = 13.sp)
                }
              }
            }
          }
        }
      }
    }
  }

  // Edit draft dialog
  editingDraft?.let { draft ->
    EditDraftStudentDialog(
      draft = draft,
      onDismiss = { editingDraft = null },
      onSave = { firstName, lastName, number ->
        viewModel.updateDraftStudent(draft.tempId, firstName, lastName, number)
        editingDraft = null
      }
    )
  }
}

@Composable
fun ImportKpiCard(
  label: String,
  count: Int,
  color: Color,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .clickable(onClick = onClick)
      .border(
        width = if (isSelected) 2.dp else 1.dp,
        color = if (isSelected) color else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
      ),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = count.toString(),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = color
      )
      Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun ImportStudentRow(
  draft: ParsedStudentDraft,
  onToggleSelection: () -> Unit,
  onEditClick: () -> Unit
) {
  val (statusText, statusBg, statusColor) = when (draft.status) {
    StudentImportStatus.NEW -> Triple("Yangi", PresentColor.copy(alpha = 0.12f), PresentColor)
    StudentImportStatus.EXISTING -> Triple("Mavjud", LateColor.copy(alpha = 0.12f), LateColor)
    StudentImportStatus.SUSPECTED_DUPLICATE -> Triple("Ehtimoliy dublikat", LateColor.copy(alpha = 0.12f), LateColor)
    StudentImportStatus.INVALID -> Triple("Xatolik", AbsentColor.copy(alpha = 0.12f), AbsentColor)
  }

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Checkbox(
        checked = draft.isSelectedForImport,
        onCheckedChange = { onToggleSelection() },
        colors = CheckboxDefaults.colors(checkedColor = UstozPrimary)
      )

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${draft.lastName} ${draft.firstName}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(statusBg)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(text = statusText, fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (draft.studentNumber.isNotBlank()) {
            Text(text = "ID: ${draft.studentNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          if (draft.birthDate.isNotBlank()) {
            Text(text = "Tug'ilgan: ${draft.birthDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        draft.statusNote?.let { note ->
          Text(
            text = note,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.error,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Default.Edit, contentDescription = "Tahrirlash", tint = UstozPrimary, modifier = Modifier.size(18.dp))
      }
    }
  }
}

@Composable
fun EditDraftStudentDialog(
  draft: ParsedStudentDraft,
  onDismiss: () -> Unit,
  onSave: (firstName: String, lastName: String, number: String) -> Unit
) {
  var firstName by remember { mutableStateOf(draft.firstName) }
  var lastName by remember { mutableStateOf(draft.lastName) }
  var number by remember { mutableStateOf(draft.studentNumber) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("O'quvchi ma'lumotlarini tahrirlash", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = lastName,
          onValueChange = { lastName = it },
          label = { Text("Familiya *") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = firstName,
          onValueChange = { firstName = it },
          label = { Text("Ism *") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = number,
          onValueChange = { number = it },
          label = { Text("O'quvchi raqami / ID") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(firstName, lastName, number) },
        enabled = firstName.isNotBlank() && lastName.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary)
      ) {
        Text("Saqlash")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Bekor qilish") }
    }
  )
}

@Composable
fun ImportResultSummaryDialog(
  result: StudentImportResultSummary,
  onClose: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onClose,
    icon = {
      Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = PresentColor,
        modifier = Modifier.size(44.dp)
      )
    },
    title = {
      Text(
        text = "Import muvaffaqiyatli yakunlandi!",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "Sinf: ${result.className}",
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))

        ResultDetailRow(label = "Tekshirilgan o'quvchilar:", value = "${result.totalChecked} ta", color = MaterialTheme.colorScheme.onSurface)
        ResultDetailRow(label = "Qo'shilgan o'quvchilar:", value = "${result.addedCount} ta", color = PresentColor)
        if (result.duplicateCount > 0) {
          ResultDetailRow(label = "Dublikat (o'tkazib yuborilgan):", value = "${result.duplicateCount} ta", color = LateColor)
        }
        if (result.errorCount > 0) {
          ResultDetailRow(label = "Xatolik bo'lgan yozuvlar:", value = "${result.errorCount} ta", color = AbsentColor)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onClose,
        colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("Tushundim")
      }
    }
  )
}

@Composable
private fun ResultDetailRow(label: String, value: String, color: Color) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
  }
}
