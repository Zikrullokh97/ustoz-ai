package com.example.ui.classes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import android.content.Intent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.ui.platform.LocalContext
import com.example.data.util.ExportFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ClassEntity
import com.example.data.model.ClassWithStudentCount
import com.example.data.model.StudentEntity
import com.example.data.repository.DayScheduleSlot
import com.example.ui.theme.UstozOnPrimaryContainer
import com.example.ui.theme.UstozOutline
import com.example.ui.theme.UstozPrimary
import com.example.ui.theme.UstozPrimaryContainer

@Composable
fun ClassesView(
  classes: List<ClassWithStudentCount>,
  onClassClick: (ClassEntity) -> Unit,
  onAddClassClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (classes.isEmpty()) {
      EmptyClassesState(onAddClassClick = onAddClassClick)
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Mening Sinflarim",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = "Jami ${classes.size} ta sinf",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Button(
              onClick = onAddClassClick,
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
              modifier = Modifier.testTag("classes_add_button_top")
            ) {
              Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Sinf qo'shish")
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        items(classes, key = { it.classEntity.id }) { item ->
          ClassCard(
            item = item,
            onClick = { onClassClick(item.classEntity) }
          )
        }

        item {
          Spacer(modifier = Modifier.height(70.dp))
        }
      }
    }
  }
}

@Composable
fun ClassCard(
  item: ClassWithStudentCount,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.12f)),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true),
        onClick = onClick
      )
      .testTag("class_card_${item.classEntity.name}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .background(UstozPrimaryContainer, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = item.classEntity.name,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = UstozPrimary,
            fontSize = 18.sp
          )
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${item.classEntity.grade}-sinf (${item.classEntity.section})",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CircleShape,
            color = UstozPrimaryContainer.copy(alpha = 0.6f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = UstozPrimary,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${item.studentCount} o'quvchi",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = UstozOnPrimaryContainer
              )
            }
          }

          Text(
            text = item.classEntity.academicYear,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
fun EmptyClassesState(onAddClassClick: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(80.dp)
        .background(UstozPrimaryContainer, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Class,
        contentDescription = null,
        tint = UstozPrimary,
        modifier = Modifier.size(40.dp)
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "Hozircha sinflar qo'shilmagan",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Boshlash uchun birinchi sinfingizni yarating. Keyin unga o'quvchilarni qo'shishingiz mumkin.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = onAddClassClick,
      shape = CircleShape,
      colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
      modifier = Modifier
        .height(48.dp)
        .testTag("empty_state_add_class_button")
    ) {
      Icon(imageVector = Icons.Default.Add, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Birinchi sinfni yaratish")
    }
  }
}

@Composable
fun CreateClassDialog(
  onDismiss: () -> Unit,
  defaultSubject: String = "Matematika",
  onCreate: (grade: Int, section: String, academicYear: String) -> Unit = { _, _, _ -> },
  onCreateWithSchedule: ((grade: Int, section: String, academicYear: String, subject: String, slots: List<DayScheduleSlot>) -> Unit)? = null
) {
  var selectedGrade by remember { mutableIntStateOf(7) }
  var section by remember { mutableStateOf("A") }
  var academicYear by remember { mutableStateOf("2024-2025") }
  var subject by remember { mutableStateOf(defaultSubject) }
  var room by remember { mutableStateOf("101-xona") }
  var attachSchedule by remember { mutableStateOf(true) }
  var selectedLessonNumber by remember { mutableIntStateOf(1) }
  var selectedDays by remember { mutableStateOf(setOf(1, 3, 5)) } // Default: Dushanba, Chorshanba, Juma

  val daysOfWeek = listOf(
    1 to "Dushanba",
    2 to "Seshanba",
    3 to "Chorshanba",
    4 to "Payshanba",
    5 to "Juma",
    6 to "Shanba"
  )

  val paraTimes = mapOf(
    1 to ("08:30" to "09:15"),
    2 to ("09:25" to "10:10"),
    3 to ("10:20" to "11:05"),
    4 to ("11:15" to "12:00"),
    5 to ("12:10" to "12:55"),
    6 to ("13:05" to "13:50")
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .padding(vertical = 16.dp)
        .testTag("create_class_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Yangi Sinf & Dars Jadvali",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Bekor qilish")
          }
        }

        // 1. Grade selection (1-11)
        Text(
          text = "1. Sinf raqamini tanlang: $selectedGrade-sinf",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items((1..11).toList()) { grade ->
            val isSelected = grade == selectedGrade
            Surface(
              shape = CircleShape,
              color = if (isSelected) UstozPrimary else UstozPrimaryContainer.copy(alpha = 0.5f),
              modifier = Modifier
                .clickable { selectedGrade = grade }
                .testTag("grade_chip_$grade")
            ) {
              Text(
                text = "$grade",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) Color.White else UstozOnPrimaryContainer
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = section,
            onValueChange = { if (it.length <= 3) section = it.uppercase() },
            label = { Text("Sinf harfi *") },
            placeholder = { Text("A") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("create_class_section_input"),
            shape = RoundedCornerShape(14.dp)
          )

          OutlinedTextField(
            value = academicYear,
            onValueChange = { academicYear = it },
            label = { Text("O'quv yili *") },
            placeholder = { Text("2024-2025") },
            singleLine = true,
            modifier = Modifier
              .weight(1.3f)
              .testTag("create_class_academic_year_input"),
            shape = RoundedCornerShape(14.dp)
          )
        }

        // Fan nomi
        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Fan nomi *") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_class_subject_input"),
          shape = RoundedCornerShape(14.dp)
        )

        // Quick Subject Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          val quickSubjects = listOf("Matematika", "Fizika", "Informatika", "Ingliz tili", "Kimyo", "Ona tili", "Tarix")
          items(quickSubjects) { subj ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (subject == subj) UstozPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              border = if (subject == subj) androidx.compose.foundation.BorderStroke(1.dp, UstozPrimary) else null,
              modifier = Modifier.clickable { subject = subj }
            ) {
              Text(
                text = subj,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (subject == subj) UstozPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        // 2. HAFTALIK JADVALNI BIRIKTIRISH
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.15f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = UstozPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Haftalik dars jadvali",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
              }
              Surface(
                shape = CircleShape,
                color = if (attachSchedule) UstozPrimary else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.clickable { attachSchedule = !attachSchedule }
              ) {
                Text(
                  text = if (attachSchedule) "Faol" else "O'chirilgan",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }

            if (attachSchedule) {
              Text(
                text = "Hafta kunlarini belgilang:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              // Day Selection Chips
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                daysOfWeek.forEach { (dayNum, dayName) ->
                  val isSelected = selectedDays.contains(dayNum)
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) UstozPrimary else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) UstozPrimary else UstozOutline.copy(alpha = 0.3f)),
                    modifier = Modifier
                      .weight(1f)
                      .clickable {
                        selectedDays = if (isSelected) selectedDays - dayNum else selectedDays + dayNum
                      }
                  ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                      Text(
                        text = dayName.take(2),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                }
              }

              // Para selection & room
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Dars parasi: $selectedLessonNumber-para",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..6).forEach { para ->
                      val isParaSelected = para == selectedLessonNumber
                      Surface(
                        shape = CircleShape,
                        color = if (isParaSelected) UstozPrimary else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isParaSelected) UstozPrimary else UstozOutline.copy(alpha = 0.3f)),
                        modifier = Modifier
                          .weight(1f)
                          .clickable { selectedLessonNumber = para }
                      ) {
                        Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                          Text(
                            text = "$para",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isParaSelected) Color.White else MaterialTheme.colorScheme.onSurface
                          )
                        }
                      }
                    }
                  }
                }
              }

              val timeRange = paraTimes[selectedLessonNumber] ?: ("08:30" to "09:15")
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.2f)),
                  modifier = Modifier.weight(1f)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = UstozPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "${timeRange.first} - ${timeRange.second}",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }

                OutlinedTextField(
                  value = room,
                  onValueChange = { room = it },
                  label = { Text("Xona") },
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp)
                )
              }
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = UstozPrimaryContainer.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Yaratiladigan sinf: $selectedGrade-${section.trim().uppercase()}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = UstozPrimary
            )
            if (attachSchedule && selectedDays.isNotEmpty()) {
              val daysText = daysOfWeek.filter { it.first in selectedDays }.joinToString(", ") { it.second }
              Text(
                text = "Haftalik darslar (${selectedDays.size} kun): $daysText • $selectedLessonNumber-para ($subject)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
          ) {
            Text("Bekor qilish")
          }

          Button(
            onClick = {
              val cleanSection = section.trim().uppercase()
              val cleanYear = academicYear.trim()
              val cleanSubject = subject.trim().ifBlank { "Matematika" }
              val timeRange = paraTimes[selectedLessonNumber] ?: ("08:30" to "09:15")

              if (attachSchedule && selectedDays.isNotEmpty() && onCreateWithSchedule != null) {
                val slots = selectedDays.sorted().map { day ->
                  DayScheduleSlot(
                    dayOfWeek = day,
                    lessonNumber = selectedLessonNumber,
                    startTime = timeRange.first,
                    endTime = timeRange.second,
                    room = room.trim().ifBlank { "Sinfxona" }
                  )
                }
                onCreateWithSchedule(selectedGrade, cleanSection, cleanYear, cleanSubject, slots)
              } else {
                onCreate(selectedGrade, cleanSection, cleanYear)
              }
            },
            enabled = section.isNotBlank(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier
              .weight(1f)
              .testTag("create_class_submit_button")
          ) {
            Text("Yaratish")
          }
        }
      }
    }
  }
}

@Composable
fun ClassDetailDialog(
  classEntity: ClassEntity,
  students: List<StudentEntity>,
  onDismiss: () -> Unit,
  onAddStudentClick: (classId: String) -> Unit,
  onStudentClick: (StudentEntity) -> Unit,
  onDeleteClassClick: (classId: String) -> Unit,
  onImportStudentsClick: (classId: String) -> Unit = {},
  onExportStudentsClick: (ClassEntity) -> Unit = {},
  onGenerateLessonsClick: (classId: String) -> Unit = {}
) {
  var showDeleteConfirm by remember { mutableStateOf(false) }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Sinfni o'chirish") },
      text = {
        Text("Haqiqatan ham ${classEntity.name} sinfini o'chirmoqchimisiz? Ushbu sinfga biriktirilgan barcha o'quvchilar ham o'chiriladi.")
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            onDeleteClassClick(classEntity.id)
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          shape = CircleShape,
          modifier = Modifier.testTag("confirm_delete_class_button")
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

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 20.dp)
        .testTag("class_detail_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .fillMaxWidth()
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
                .size(46.dp)
                .background(UstozPrimaryContainer, RoundedCornerShape(14.dp)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = classEntity.name,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = UstozPrimary
                )
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "${classEntity.grade}-sinf (${classEntity.section})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "O'quv yili: ${classEntity.academicYear}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actions Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "O'quvchilar (${students.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
              onClick = { onExportStudentsClick(classEntity) },
              shape = CircleShape,
              modifier = Modifier.testTag("class_detail_export_button")
            ) {
              Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Eksport", fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = { onImportStudentsClick(classEntity.id) },
              shape = CircleShape,
              modifier = Modifier.testTag("class_detail_import_file_button")
            ) {
              Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Fayldan", fontSize = 12.sp)
            }

            Button(
              onClick = { onAddStudentClick(classEntity.id) },
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
              modifier = Modifier.testTag("class_detail_add_student_button")
            ) {
              Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Qo'shish", fontSize = 12.sp)
            }

            IconButton(
              onClick = { showDeleteConfirm = true },
              modifier = Modifier.testTag("class_detail_delete_class_button")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Sinfni o'chirish",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (students.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Bu sinfda hali o'quvchilar yo'q.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Birinchi o'quvchini qo'shish uchun tugmani bosing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .weight(1f, fill = false)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(students, key = { it.id }) { student ->
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.1f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { onStudentClick(student) }
                  .testTag("student_item_${student.studentNumber}")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .background(UstozPrimaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "${student.firstName.take(1)}${student.lastName.take(1)}".uppercase(),
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = UstozPrimary
                      )
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = student.fullName,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                      text = "ID: ${student.studentNumber} • ${student.gender}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ExportClassDialog(
  classEntity: ClassEntity,
  studentCount: Int,
  onDismiss: () -> Unit,
  onExport: (ExportFormat, (Intent) -> Unit) -> Unit
) {
  val context = LocalContext.current
  var selectedFormat by remember { mutableStateOf(ExportFormat.XLSX) }
  var isExporting by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Share, contentDescription = null, tint = UstozPrimary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${classEntity.name} sinfini eksport qilish",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
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
          text = "Sinf o'quvchilari ro'yxatini Excel, CSV yoki TXT formatida saqlang va boshqa ilovalarga (Telegram, Mail, Drive) yuboring.",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Sinf: ${classEntity.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = "O'quvchilar soni: $studentCount ta", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "O'quv yili: ${classEntity.academicYear}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        Text(
          text = "Eksport formatini tanlang:",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )

        val formats = listOf(
          Triple(ExportFormat.XLSX, "Excel jadvali (.xlsx)", "Formatlangan jadval, rasmiy hisobot uchun"),
          Triple(ExportFormat.CSV, "CSV fayl (.csv)", "Boshqa tizimlar bilan integratsiya uchun"),
          Triple(ExportFormat.TXT, "Matn fayli (.txt)", "Oddiy ko'rish va chop etish uchun")
        )

        formats.forEach { (format, title, subtitle) ->
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (selectedFormat == format) UstozPrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
              width = if (selectedFormat == format) 2.dp else 1.dp,
              color = if (selectedFormat == format) UstozPrimary else UstozOutline.copy(alpha = 0.2f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedFormat = format }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              val icon = when (format) {
                ExportFormat.XLSX -> Icons.Default.TableChart
                ExportFormat.CSV -> Icons.Default.Description
                ExportFormat.TXT -> Icons.Default.TextFields
              }
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selectedFormat == format) UstozPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = title,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp,
                  color = if (selectedFormat == format) UstozPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = subtitle,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          isExporting = true
          onExport(selectedFormat) { shareIntent ->
            isExporting = false
            context.startActivity(Intent.createChooser(shareIntent, "${classEntity.name} ro'yxatini ulashish"))
            onDismiss()
          }
        },
        enabled = !isExporting,
        colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary)
      ) {
        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (isExporting) "Tayyorlanmoqda..." else "Eksport va ulashish")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isExporting) {
        Text("Bekor qilish")
      }
    }
  )
}
