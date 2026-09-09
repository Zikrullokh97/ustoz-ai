package com.example.ui.ktp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.util.DocumentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KTPConfidence
import com.example.data.model.KTPDocumentEntity
import com.example.data.model.KTPEntryEntity
import com.example.data.model.KTPEntryStatus
import com.example.data.model.KTPStatus
import com.example.ui.main.MainAppUiState
import com.example.ui.main.MainAppViewModel
import com.example.ui.theme.AbsentColor
import com.example.ui.theme.LateColor
import com.example.ui.theme.PresentColor
import com.example.ui.theme.UstozPrimary
import com.example.ui.theme.UstozPrimaryContainer

@Composable
fun KTPScreen(
  uiState: MainAppUiState,
  viewModel: MainAppViewModel,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (uiState.selectedKTPDocument != null) {
      KTPDetailContent(
        document = uiState.selectedKTPDocument,
        entries = uiState.ktpEntries,
        uiState = uiState,
        viewModel = viewModel
      )
    } else {
      KTPDocumentListContent(
        uiState = uiState,
        viewModel = viewModel
      )
    }

    // Dialogs
    if (uiState.showUploadKTPDialog) {
      UploadKTPDialog(
        uiState = uiState,
        onDismiss = { viewModel.setShowUploadKTPDialog(false) },
        onUpload = { fileName, subject, year, content, classId ->
          viewModel.uploadAndAnalyzeKTP(fileName, subject, year, content, classId)
        }
      )
    }

    if (uiState.selectedKTPEntryForEdit != null) {
      EditKTPEntryDialog(
        entry = uiState.selectedKTPEntryForEdit,
        uiState = uiState,
        onDismiss = { viewModel.setKTPEntryForEdit(null) },
        onSave = { date, lessonNumber, topic, room, classId ->
          viewModel.updateKTPEntry(
            entryId = uiState.selectedKTPEntryForEdit.id,
            date = date,
            lessonNumber = lessonNumber,
            topic = topic,
            room = room,
            classId = classId
          )
        }
      )
    }
  }
}

@Composable
private fun KTPDocumentListContent(
  uiState: MainAppUiState,
  viewModel: MainAppViewModel
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("ktp_screen_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Title & Header (Stitch design language)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "KTP",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Kalendar-tematik reja",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Button(
              onClick = { viewModel.setShowUploadKTPDialog(true) },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = UstozPrimary
              ),
              modifier = Modifier.testTag("ktp_upload_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("KTP yuklash", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }

    // Section Header: Yuklangan hujjatlar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Yuklangan hujjatlar",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )

        Text(
          text = "${uiState.ktpDocuments.size} ta hujjat",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    if (uiState.isLoadingKTP) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = UstozPrimary)
        }
      }
    } else if (uiState.ktpDocuments.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.MenuBook,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Hozircha KTP hujjatlari yuklanmagan",
              fontSize = 15.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "KTP faylini yuklab, dars jadvalingiz bilan avtomatik moslashtiring.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = { viewModel.setShowUploadKTPDialog(true) },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary)
            ) {
              Text("+ KTP fayl yuklash")
            }
          }
        }
      }
    } else {
      items(uiState.ktpDocuments) { doc ->
        KTPDocumentCard(
          document = doc,
          onClick = { viewModel.selectKTPDocument(doc) },
          onDelete = { viewModel.deleteKTP(doc.id) }
        )
      }
    }
  }
}

@Composable
private fun KTPDocumentCard(
  document: KTPDocumentEntity,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  var showDeleteConfirm by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(),
        onClick = onClick
      )
      .testTag("ktp_doc_${document.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${document.subjectId} — ${document.academicYear}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = document.fileName,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        IconButton(
          onClick = { showDeleteConfirm = true },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            Icons.Default.Delete,
            contentDescription = "O'chirish",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Status Badge
        when (document.status) {
          KTPStatus.CONFIRMED -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PresentColor.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PresentColor,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "✓ Tasdiqlangan",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PresentColor
              )
            }
          }
          KTPStatus.REVIEW_REQUIRED -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(LateColor.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = LateColor,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "⚠ Tekshirish kerak (${document.warningCount})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = LateColor
              )
            }
          }
          else -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(UstozPrimaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = "Tahlil qilingan",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = UstozPrimary
              )
            }
          }
        }

        Text(
          text = "${document.lessonCount} ta dars rejalashtirilgan",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("KTP hujjatini o'chirish") },
      text = { Text("Ushbu KTP rejasini o'chirmoqchimisiz?") },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            onDelete()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("O'chirish")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) {
          Text("Bekor qilish")
        }
      }
    )
  }
}

@Composable
private fun KTPDetailContent(
  document: KTPDocumentEntity,
  entries: List<KTPEntryEntity>,
  uiState: MainAppUiState,
  viewModel: MainAppViewModel
) {
  var filterStatus by remember { mutableStateOf<String?>("ALL") }

  val filteredEntries = entries.filter { entry ->
    when (filterStatus) {
      "WARNINGS" -> entry.status != KTPEntryStatus.VALID && entry.status != KTPEntryStatus.CONFIRMED
      "CONFIRMED" -> entry.isConfirmed
      else -> true
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // Header
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { viewModel.closeKTPDocumentDetail() },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${document.subjectId} — ${document.academicYear}",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = document.fileName,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          if (document.status == KTPStatus.CONFIRMED) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PresentColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text("✓ Tasdiqlangan", fontSize = 11.sp, color = PresentColor, fontWeight = FontWeight.Bold)
            }
          }
        }

        if (document.warningCount > 0) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(LateColor.copy(alpha = 0.12f))
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = LateColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "${document.warningCount} ta bandda jadval yoki sana bilan mos kelmaslik aniqlandi. Quyida qizil/sariq belgilangan bandlarni tekshiring.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurface,
              lineHeight = 15.sp
            )
          }
        }

        // Filter chips
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          KTPFilterChip(
            label = "Barchasi (${entries.size})",
            selected = filterStatus == "ALL",
            onClick = { filterStatus = "ALL" }
          )
          if (document.warningCount > 0) {
            KTPFilterChip(
              label = "Xatoliklar (${document.warningCount})",
              selected = filterStatus == "WARNINGS",
              onClick = { filterStatus = "WARNINGS" }
            )
          }
          KTPFilterChip(
            label = "Tasdiqlangan",
            selected = filterStatus == "CONFIRMED",
            onClick = { filterStatus = "CONFIRMED" }
          )
        }
      }
    }

    // List of entries
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(filteredEntries) { entry ->
        KTPEntryCard(
          entry = entry,
          onEdit = { viewModel.setKTPEntryForEdit(entry) }
        )
      }
    }

    // Sticky Bottom Bar with Confirmation Action
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = { viewModel.deleteKTP(document.id) },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.weight(0.35f)
        ) {
          Text("O'chirish", fontSize = 13.sp)
        }

        Button(
          onClick = { viewModel.confirmKTP(document.id) },
          enabled = !uiState.isConfirmingKTP,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
          modifier = Modifier
            .weight(0.65f)
            .testTag("ktp_confirm_button")
        ) {
          if (uiState.isConfirmingKTP) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Yaratilmoqda...", fontSize = 13.sp)
          } else {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("✓ Tasdiqlash va Darslarni yaratish", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun KTPEntryCard(
  entry: KTPEntryEntity,
  onEdit: () -> Unit
) {
  val hasWarning = entry.status != KTPEntryStatus.VALID && entry.status != KTPEntryStatus.CONFIRMED

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = if (hasWarning) 1.5.dp else 1.dp,
        color = if (hasWarning) LateColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(14.dp)
      ),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (hasWarning) LateColor.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
    )
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Event, contentDescription = null, tint = UstozPrimary, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = entry.date,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "•  ${entry.classNameDisplay}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = UstozPrimary
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "•  ${entry.lessonNumber}-dars",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Edit, contentDescription = "Tahrirlash", tint = UstozPrimary, modifier = Modifier.size(18.dp))
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = entry.topic,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )

      if (entry.warningMessage != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = LateColor, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = entry.warningMessage,
            fontSize = 11.sp,
            color = LateColor,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          // Source badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = if (entry.manualOverride) "Qo'lda tahrirlangan" else "Manba: ${entry.source}",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Confidence badge
          val confColor = when (entry.confidence) {
            KTPConfidence.HIGH -> PresentColor
            KTPConfidence.MEDIUM -> UstozPrimary
            KTPConfidence.LOW -> LateColor
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(confColor.copy(alpha = 0.15f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "${entry.confidence} ishonch",
              fontSize = 10.sp,
              color = confColor,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Text(
          text = entry.room,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun KTPFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) UstozPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      fontSize = 12.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
      color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadKTPDialog(
  uiState: MainAppUiState,
  onDismiss: () -> Unit,
  onUpload: (fileName: String, subject: String, year: String, content: String, classId: String?) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var subject by remember { mutableStateOf("Matematika") }
  var academicYear by remember { mutableStateOf("2026/2027") }
  var fileName by remember { mutableStateOf("") }
  var fileSizeFormatted by remember { mutableStateOf<String?>(null) }
  var contentText by remember { mutableStateOf("") }

  var isReadingFile by remember { mutableStateOf(false) }
  var fileErrorMessage by remember { mutableStateOf<String?>(null) }

  var selectedClassId by remember {
    mutableStateOf(uiState.classes.firstOrNull()?.classEntity?.id ?: "")
  }
  var classExpanded by remember { mutableStateOf(false) }

  // Real Android File Picker Launcher
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri == null) return@rememberLauncherForActivityResult

    fileErrorMessage = null
    isReadingFile = true

    coroutineScope.launch {
      val (pickedName, sizeBytes) = withContext(Dispatchers.IO) {
        DocumentParser.readFileNameAndSize(context, uri)
      }

      val sizeStr = when {
        sizeBytes <= 0 -> ""
        sizeBytes < 1024 -> "$sizeBytes B"
        sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
        else -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
      }

      fileName = pickedName
      fileSizeFormatted = sizeStr

      // Auto-detect subject from file name if possible
      val lowerName = pickedName.lowercase()
      when {
        lowerName.contains("matematika") || lowerName.contains("algebra") || lowerName.contains("geometriya") -> subject = "Matematika"
        lowerName.contains("fizika") -> subject = "Fizika"
        lowerName.contains("kimyo") -> subject = "Kimyo"
        lowerName.contains("biologiya") -> subject = "Biologiya"
        lowerName.contains("informatika") -> subject = "Informatika"
        lowerName.contains("ona tili") || lowerName.contains("adabiyot") -> subject = "Ona tili"
        lowerName.contains("tarix") -> subject = "Tarix"
        lowerName.contains("ingliz") -> subject = "Ingliz tili"
      }

      val parseResult = withContext(Dispatchers.IO) {
        DocumentParser.parseDocumentText(context, uri, pickedName, allowPdf = true)
      }

      isReadingFile = false

      parseResult.fold(
        onSuccess = { extractedText ->
          contentText = extractedText
        },
        onFailure = { error ->
          fileErrorMessage = error.message ?: "Faylni o'qishda xatolik yuz berdi"
        }
      )
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = UstozPrimary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("KTP faylini yuklash va tahlil qilish", fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Android xotirasidan XLSX, CSV, DOCX, TXT yoki PDF formatidagi KTP faylini tanlang. Tizim mavzularni dars jadvalingiz bilan avtomatik moslashtiradi.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Class selector
        ExposedDropdownMenuBox(
          expanded = classExpanded,
          onExpandedChange = { classExpanded = !classExpanded }
        ) {
          val selectedClassName = uiState.classes.firstOrNull { it.classEntity.id == selectedClassId }?.classEntity?.name ?: "Sinfni tanlang"
          OutlinedTextField(
            value = selectedClassName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sinf *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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

        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Fan nomi *") },
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = academicYear,
          onValueChange = { academicYear = it },
          label = { Text("O'quv yili") },
          modifier = Modifier.fillMaxWidth()
        )

        // Real File Picker Card
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, UstozPrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            if (fileName.isBlank()) {
              Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = UstozPrimary,
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "KTP faylini tanlang",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = ".xlsx, .csv, .docx, .txt, .pdf",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(10.dp))
              Button(
                onClick = {
                  filePickerLauncher.launch(
                    arrayOf(
                      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                      "application/vnd.ms-excel",
                      "text/csv",
                      "text/plain",
                      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                      "application/pdf",
                      "*/*"
                    )
                  )
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary)
              ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Faylni tanlash", fontSize = 13.sp)
              }
            } else {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Icon(Icons.Default.Description, contentDescription = null, tint = UstozPrimary)
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = fileName,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    fileSizeFormatted?.let { size ->
                      Text(text = size, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }
                }

                TextButton(
                  onClick = {
                    filePickerLauncher.launch(
                      arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "text/csv",
                        "text/plain",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/pdf",
                        "*/*"
                      )
                    )
                  }
                ) {
                  Text("O'zgartirish", fontSize = 12.sp)
                }
              }
            }
          }
        }

        if (isReadingFile) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = UstozPrimary, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Fayl o'qilmoqda...", fontSize = 12.sp, color = UstozPrimary)
          }
        }

        fileErrorMessage?.let { err ->
          Text(text = err, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onUpload(fileName, subject, academicYear, contentText, selectedClassId.ifBlank { null })
        },
        enabled = contentText.isNotBlank() && subject.isNotBlank() && !isReadingFile,
        colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary)
      ) {
        Text("Tahlil qilish")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Bekor qilish")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditKTPEntryDialog(
  entry: KTPEntryEntity,
  uiState: MainAppUiState,
  onDismiss: () -> Unit,
  onSave: (date: String, lessonNumber: Int, topic: String, room: String, classId: String?) -> Unit
) {
  var date by remember { mutableStateOf(entry.date) }
  var lessonNumberStr by remember { mutableStateOf(entry.lessonNumber.toString()) }
  var topic by remember { mutableStateOf(entry.topic) }
  var room by remember { mutableStateOf(entry.room) }
  var selectedClassId by remember { mutableStateOf(entry.classId) }
  var classExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("KTP bandini tahrirlash", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Qo'lda tahrirlangan ma'lumotlar ustuvor bo'ladi (manualOverride = true).",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Class selector
        ExposedDropdownMenuBox(
          expanded = classExpanded,
          onExpandedChange = { classExpanded = !classExpanded }
        ) {
          val selectedClassName = uiState.classes.firstOrNull { it.classEntity.id == selectedClassId }?.classEntity?.name ?: entry.classNameDisplay
          OutlinedTextField(
            value = selectedClassName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sinf") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(
            expanded = classExpanded,
            onDismissRequest = { classExpanded = false }
          ) {
            uiState.classes.forEach { cls ->
              DropdownMenuItem(
                text = { Text(cls.classEntity.name) },
                onClick = {
                  selectedClassId = cls.classEntity.id
                  classExpanded = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = date,
          onValueChange = { date = it },
          label = { Text("Sana (YYYY-MM-DD)") },
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = lessonNumberStr,
          onValueChange = { lessonNumberStr = it },
          label = { Text("Dars raqami (1-8)") },
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = topic,
          onValueChange = { topic = it },
          label = { Text("Dars mavzusi") },
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = room,
          onValueChange = { room = it },
          label = { Text("Xona") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val num = lessonNumberStr.toIntOrNull() ?: 1
          onSave(date, num, topic, room, selectedClassId)
        },
        colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary)
      ) {
        Text("Saqlash")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Bekor qilish")
      }
    }
  )
}
