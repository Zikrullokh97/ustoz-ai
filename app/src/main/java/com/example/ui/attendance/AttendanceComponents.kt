package com.example.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceSummary
import com.example.data.model.LessonEntity
import com.example.data.model.LessonStatus
import com.example.data.model.LessonWithDetails
import com.example.data.model.StudentAttendanceItem
import com.example.ui.theme.AbsentColor
import com.example.ui.theme.ExcusedColor
import com.example.ui.theme.LateColor
import com.example.ui.theme.PresentColor
import com.example.ui.theme.UstozOutline
import com.example.ui.theme.UstozPrimary
import com.example.ui.theme.UstozPrimaryContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TezkorDavomatScreen(
  lessonWithDetails: LessonWithDetails,
  items: List<StudentAttendanceItem>,
  summary: AttendanceSummary,
  hasUnsavedChanges: Boolean,
  isSaving: Boolean,
  onBack: () -> Unit,
  onMarkStatus: (studentId: String, status: AttendanceStatus, lateMinutes: Int?, note: String?) -> Unit,
  onMarkAllPresent: () -> Unit,
  onSave: () -> Unit
) {
  var studentForNoteDialog by remember { mutableStateOf<StudentAttendanceItem?>(null) }
  var studentForLateDialog by remember { mutableStateOf<StudentAttendanceItem?>(null) }

  // Note dialog for Absent or Excused
  studentForNoteDialog?.let { currentItem ->
    AttendanceNoteDialog(
      studentName = currentItem.student.fullName,
      initialNote = currentItem.note ?: "",
      onDismiss = { studentForNoteDialog = null },
      onConfirm = { note ->
        val status = currentItem.status ?: AttendanceStatus.EXCUSED
        onMarkStatus(currentItem.student.id, status, null, note)
        studentForNoteDialog = null
      }
    )
  }

  // Late minutes dialog
  studentForLateDialog?.let { currentItem ->
    LateMinutesDialog(
      studentName = currentItem.student.fullName,
      currentMinutes = currentItem.lateMinutes ?: 5,
      onDismiss = { studentForLateDialog = null },
      onConfirm = { mins ->
        onMarkStatus(currentItem.student.id, AttendanceStatus.LATE, mins, null)
        studentForLateDialog = null
      }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Tezkor Davomat",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "${lessonWithDetails.className} • ${lessonWithDetails.lesson.subjectId} • ${lessonWithDetails.lesson.startTime}–${lessonWithDetails.lesson.endTime}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("attendance_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
          }
        },
        actions = {
          if (hasUnsavedChanges) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = LateColor.copy(alpha = 0.15f),
              modifier = Modifier.padding(end = 12.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = LateColor, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Saqlanmagan", color = LateColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    bottomBar = {
      // Sticky Save Button
      Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
          Button(
            onClick = onSave,
            enabled = !isSaving && items.isNotEmpty(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("save_attendance_button")
          ) {
            if (isSaving) {
              CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text("Saqlanmoqda...")
            } else {
              Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Davomatni Tasdiqlash va Saqlash",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
            }
          }
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        // Lesson Info Banner
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = UstozPrimaryContainer.copy(alpha = 0.35f)),
          border = androidx.compose.foundation.BorderStroke(1.dp, UstozPrimary.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .background(UstozPrimary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${lessonWithDetails.className} • ${lessonWithDetails.lesson.topic}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Xona: ${lessonWithDetails.lesson.room} • Sana: ${lessonWithDetails.lesson.date}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // REAL-TIME BENTO COUNTERS
      item {
        AttendanceBentoCounters(summary = summary)
      }

      // Quick Mark All Present Button
      item {
        Button(
          onClick = onMarkAllPresent,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = PresentColor.copy(alpha = 0.12f),
            contentColor = PresentColor
          ),
          border = androidx.compose.foundation.BorderStroke(1.dp, PresentColor.copy(alpha = 0.3f)),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("mark_all_present_button")
        ) {
          Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Barchasini 'Kelgan' deb belgilash (${summary.totalStudents}/${summary.totalStudents})",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "O'quvchilar ro'yxati",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "${items.size} nafar",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (items.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
              Spacer(modifier = Modifier.height(8.dp))
              Text("Bu sinfda hali o'quvchilar yo'q", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      } else {
        items(items, key = { it.student.id }) { item ->
          StudentAttendanceCard(
            item = item,
            onSelectStatus = { status ->
              when (status) {
                AttendanceStatus.PRESENT -> onMarkStatus(item.student.id, AttendanceStatus.PRESENT, null, null)
                AttendanceStatus.LATE -> studentForLateDialog = item
                AttendanceStatus.ABSENT -> {
                  onMarkStatus(item.student.id, AttendanceStatus.ABSENT, null, item.note)
                  if (item.note == null) studentForNoteDialog = item
                }
                AttendanceStatus.EXCUSED -> {
                  onMarkStatus(item.student.id, AttendanceStatus.EXCUSED, null, item.note)
                  if (item.note == null) studentForNoteDialog = item
                }
              }
            },
            onEditNote = { studentForNoteDialog = item },
            onEditLateMinutes = { studentForLateDialog = item }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
fun AttendanceBentoCounters(summary: AttendanceSummary) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.12f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Davomat Hisoboti",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "${summary.presentCount + summary.lateCount + summary.absentCount + summary.excusedCount} / ${summary.totalStudents} belgilandi",
          style = MaterialTheme.typography.labelSmall,
          color = UstozPrimary,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Jami
        BentoCountItem(
          label = "Jami",
          count = summary.totalStudents,
          color = MaterialTheme.colorScheme.onSurface,
          bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.weight(1f)
        )
        // Kelgan
        BentoCountItem(
          label = "Kelgan",
          count = summary.presentCount,
          color = PresentColor,
          bgColor = PresentColor.copy(alpha = 0.12f),
          modifier = Modifier.weight(1f)
        )
        // Kech
        BentoCountItem(
          label = "Kech",
          count = summary.lateCount,
          color = LateColor,
          bgColor = LateColor.copy(alpha = 0.12f),
          modifier = Modifier.weight(1f)
        )
        // Yo'q
        BentoCountItem(
          label = "Yo'q",
          count = summary.absentCount,
          color = AbsentColor,
          bgColor = AbsentColor.copy(alpha = 0.12f),
          modifier = Modifier.weight(1f)
        )
        // Sabab
        BentoCountItem(
          label = "Sabab",
          count = summary.excusedCount,
          color = ExcusedColor,
          bgColor = ExcusedColor.copy(alpha = 0.12f),
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
fun BentoCountItem(
  label: String,
  count: Int,
  color: Color,
  bgColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(bgColor)
      .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "$count",
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        color = color
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = color.copy(alpha = 0.85f)
      )
    }
  }
}

@Composable
fun StudentAttendanceCard(
  item: StudentAttendanceItem,
  onSelectStatus: (AttendanceStatus) -> Unit,
  onEditNote: () -> Unit,
  onEditLateMinutes: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.12f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(UstozPrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${item.student.firstName.take(1)}${item.student.lastName.take(1)}".uppercase(),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = UstozPrimary)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = item.student.fullName,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "№ ${item.student.studentNumber}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Details chip if late or has note
        if (item.status == AttendanceStatus.LATE && item.lateMinutes != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = LateColor.copy(alpha = 0.15f),
            modifier = Modifier.clickable { onEditLateMinutes() }
          ) {
            Text(
              text = "+${item.lateMinutes} daq",
              color = LateColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        } else if (!item.note.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = ExcusedColor.copy(alpha = 0.15f),
            modifier = Modifier.clickable { onEditNote() }
          ) {
            Text(
              text = item.note.take(12),
              color = ExcusedColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 4 Interactive Status Buttons: Kelgan, Kech, Yo'q, Sabab
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        StatusButton(
          text = "Kelgan",
          isSelected = item.status == AttendanceStatus.PRESENT,
          activeColor = PresentColor,
          modifier = Modifier.weight(1f),
          onClick = { onSelectStatus(AttendanceStatus.PRESENT) }
        )
        StatusButton(
          text = "Kech",
          isSelected = item.status == AttendanceStatus.LATE,
          activeColor = LateColor,
          modifier = Modifier.weight(1f),
          onClick = { onSelectStatus(AttendanceStatus.LATE) }
        )
        StatusButton(
          text = "Yo'q",
          isSelected = item.status == AttendanceStatus.ABSENT,
          activeColor = AbsentColor,
          modifier = Modifier.weight(1f),
          onClick = { onSelectStatus(AttendanceStatus.ABSENT) }
        )
        StatusButton(
          text = "Sabab",
          isSelected = item.status == AttendanceStatus.EXCUSED,
          activeColor = ExcusedColor,
          modifier = Modifier.weight(1f),
          onClick = { onSelectStatus(AttendanceStatus.EXCUSED) }
        )
      }
    }
  }
}

@Composable
fun StatusButton(
  text: String,
  isSelected: Boolean,
  activeColor: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .height(34.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) activeColor else activeColor.copy(alpha = 0.08f))
      .clickable(onClick = onClick)
      .border(
        width = 1.dp,
        color = if (isSelected) activeColor else activeColor.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
      ),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      color = if (isSelected) Color.White else activeColor,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
      fontSize = 11.sp
    )
  }
}

@Composable
fun LateMinutesDialog(
  studentName: String,
  currentMinutes: Int,
  onDismiss: () -> Unit,
  onConfirm: (Int) -> Unit
) {
  var selectedMinutes by remember { mutableStateOf(currentMinutes) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.padding(12.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Kech qolish vaqti",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = "$studentName necha daqiqa kech qoldi?",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(5, 10, 15, 20).forEach { mins ->
            Box(
              modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selectedMinutes == mins) LateColor else LateColor.copy(alpha = 0.1f))
                .clickable { selectedMinutes = mins },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "$mins daq",
                color = if (selectedMinutes == mins) Color.White else LateColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
            Text("Bekor qilish")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onConfirm(selectedMinutes) },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LateColor)
          ) {
            Text("Tasdiqlash")
          }
        }
      }
    }
  }
}

@Composable
fun AttendanceNoteDialog(
  studentName: String,
  initialNote: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var noteText by remember { mutableStateOf(initialNote) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.padding(12.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Sabab / Izoh kiritish",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = "$studentName uchun izoh:",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Quick suggestions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("Oilaviy sabab", "Kasallik", "Rasmiy ruxsat").forEach { suggestion ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.clickable { noteText = suggestion }
            ) {
              Text(
                text = suggestion,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Izoh matni") },
          placeholder = { Text("Sababini yozing...") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
            Text("Bekor qilish")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onConfirm(noteText.trim()) },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary)
          ) {
            Text("Saqlash")
          }
        }
      }
    }
  }
}

@Composable
fun LessonDetailDialog(
  lessonWithDetails: LessonWithDetails,
  onDismiss: () -> Unit,
  onTakeAttendanceClick: (String) -> Unit,
  onDeleteLesson: (String) -> Unit
) {
  var showDeleteConfirm by remember { mutableStateOf(false) }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Darsni o'chirish") },
      text = { Text("Ushbu dars va unga tegishli davomat ma'lumotlarini o'chirmoqchimisiz?") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteLesson(lessonWithDetails.lesson.id)
            showDeleteConfirm = false
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("O'chirish")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showDeleteConfirm = false }) {
          Text("Bekor qilish")
        }
      }
    )
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp)
        .testTag("lesson_detail_dialog")
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .background(UstozPrimaryContainer, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Schedule, contentDescription = null, tint = UstozPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "${lessonWithDetails.className} • ${lessonWithDetails.lesson.subjectId}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = lessonWithDetails.lesson.topic,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        // Details grid
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailRow(label = "Sana", value = lessonWithDetails.lesson.date)
            DetailRow(label = "Vaqt", value = "${lessonWithDetails.lesson.startTime} – ${lessonWithDetails.lesson.endTime}")
            DetailRow(label = "Xona", value = lessonWithDetails.lesson.room)
            DetailRow(label = "O'quvchilar soni", value = "${lessonWithDetails.studentCount} nafar")
            DetailRow(
              label = "Davomat holati",
              value = if (lessonWithDetails.attendanceTaken) {
                "${lessonWithDetails.presentCount} kelgan, ${lessonWithDetails.lateCount} kech, ${lessonWithDetails.absentCount} yo'q"
              } else {
                "Kutilmoqda (olinmagan)"
              },
              highlight = !lessonWithDetails.attendanceTaken
            )
          }
        }

        // Action Buttons
        Button(
          onClick = {
            onDismiss()
            onTakeAttendanceClick(lessonWithDetails.lesson.id)
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("open_attendance_from_detail_button")
        ) {
          Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (lessonWithDetails.attendanceTaken) "Davomatni ko'rish / tahrirlash" else "Davomat olish",
            fontWeight = FontWeight.Bold
          )
        }

        OutlinedButton(
          onClick = { showDeleteConfirm = true },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Darsni o'chirish")
        }
      }
    }
  }
}

@Composable
fun DetailRow(label: String, value: String, highlight: Boolean = false) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
      color = if (highlight) LateColor else MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
fun LessonCard(
  lessonWithDetails: LessonWithDetails,
  onClick: () -> Unit,
  onTakeAttendance: () -> Unit
) {
  val lesson = lessonWithDetails.lesson
  val isCurrent = lesson.status == LessonStatus.CURRENT

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isCurrent) UstozPrimaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = if (isCurrent) 1.5.dp else 1.dp,
      color = if (isCurrent) UstozPrimary else UstozOutline.copy(alpha = 0.12f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 3.dp else 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("lesson_card_${lesson.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isCurrent) UstozPrimary else MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = "${lesson.startTime} – ${lesson.endTime}",
              color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          if (isCurrent) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = PresentColor.copy(alpha = 0.15f)
            ) {
              Text(
                text = "Hozirgi dars",
                color = PresentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        // Attendance Status Badge
        if (lessonWithDetails.attendanceTaken) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = PresentColor.copy(alpha = 0.12f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PresentColor, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${lessonWithDetails.presentCount}/${lessonWithDetails.studentCount}",
                color = PresentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = LateColor.copy(alpha = 0.15f)
          ) {
            Text(
              text = "Kutilmoqda",
              color = LateColor,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${lessonWithDetails.className} • ${lesson.subjectId}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${lesson.topic} • ${lesson.room}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        IconButton(
          onClick = onTakeAttendance,
          modifier = Modifier
            .size(36.dp)
            .background(UstozPrimaryContainer, CircleShape)
            .testTag("lesson_quick_attendance_button_${lesson.id}")
        ) {
          Icon(
            imageVector = Icons.Default.FactCheck,
            contentDescription = "Davomat olish",
            tint = UstozPrimary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
