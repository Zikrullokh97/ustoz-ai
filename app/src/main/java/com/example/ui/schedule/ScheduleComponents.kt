package com.example.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.model.ClassWithStudentCount
import com.example.data.model.ScheduleWithClass
import com.example.data.repository.DayScheduleSlot
import com.example.ui.theme.UstozOutline
import com.example.ui.theme.UstozPrimary
import com.example.ui.theme.UstozPrimaryContainer

val DAYS_OF_WEEK = listOf(
  1 to "Dushanba",
  2 to "Seshanba",
  3 to "Chorshanba",
  4 to "Payshanba",
  5 to "Juma",
  6 to "Shanba"
)

// Standard bell schedule
fun getStandardTimeForLesson(lessonNum: Int): Pair<String, String> {
  return when (lessonNum) {
    1 -> "08:30" to "09:15"
    2 -> "09:20" to "10:05"
    3 -> "10:15" to "11:00"
    4 -> "11:10" to "11:55"
    5 -> "12:05" to "12:50"
    6 -> "13:00" to "13:45"
    7 -> "13:50" to "14:35"
    else -> "14:40" to "15:25"
  }
}

data class DayConfigState(
  val dayOfWeek: Int,
  var lessonNumber: Int = 1,
  var startTime: String = "08:30",
  var endTime: String = "09:15",
  var room: String = "Sinfxona"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScheduleDialog(
  classes: List<ClassWithStudentCount>,
  defaultSubject: String,
  onDismiss: () -> Unit,
  onAdd: ((classId: String, subject: String, dayOfWeek: Int, startTime: String, endTime: String, room: String, lessonNumber: Int) -> Unit)? = null,
  onAddMultiDay: ((classId: String, subject: String, slots: List<DayScheduleSlot>) -> Unit)? = null
) {
  var selectedClassId by remember { mutableStateOf(classes.firstOrNull()?.classEntity?.id ?: "") }
  var subject by remember { mutableStateOf(defaultSubject) }
  var globalRoom by remember { mutableStateOf("24-xona") }
  var classDropdownExpanded by remember { mutableStateOf(false) }
  var errorText by remember { mutableStateOf<String?>(null) }

  // Multi-day selection tracking
  val selectedDays = remember { mutableStateMapOf<Int, DayConfigState>() }

  // Initialize with Dushanba by default
  remember {
    val times = getStandardTimeForLesson(1)
    selectedDays[1] = DayConfigState(
      dayOfWeek = 1,
      lessonNumber = 1,
      startTime = times.first,
      endTime = times.second,
      room = globalRoom
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 16.dp)
        .testTag("create_schedule_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(22.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Dars Jadvaliga Qo'shish",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Bir vaqtning o'zida bir nechta kunni tanlang",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        if (classes.isEmpty()) {
          Text(
            text = "Jadval yaratishdan oldin kamida bitta sinf qo'shishingiz kerak.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
          )
          Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Tushunarli")
          }
          return@Column
        }

        // Class Selector
        ExposedDropdownMenuBox(
          expanded = classDropdownExpanded,
          onExpandedChange = { classDropdownExpanded = it }
        ) {
          val selectedClassName = classes.find { it.classEntity.id == selectedClassId }?.classEntity?.name ?: "Sinfni tanlang"
          OutlinedTextField(
            value = selectedClassName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sinf *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(
            expanded = classDropdownExpanded,
            onDismissRequest = { classDropdownExpanded = false }
          ) {
            classes.forEach { c ->
              DropdownMenuItem(
                text = { Text("${c.classEntity.name} (${c.studentCount} o'quvchi)") },
                onClick = {
                  selectedClassId = c.classEntity.id
                  classDropdownExpanded = false
                }
              )
            }
          }
        }

        // Subject Input & Room
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Fan nomi *") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1.3f)
          )
          OutlinedTextField(
            value = globalRoom,
            onValueChange = { newRoom ->
              globalRoom = newRoom
              selectedDays.values.forEach { it.room = newRoom }
            },
            label = { Text("Xona") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(0.9f)
          )
        }

        // Multi-Day Selector Chips
        Text(
          text = "Hafta kunlarini tanlang (${selectedDays.size} ta kun belgilandi):",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          DAYS_OF_WEEK.forEach { (dayNum, dayName) ->
            val isChecked = selectedDays.containsKey(dayNum)
            FilterChip(
              selected = isChecked,
              onClick = {
                if (isChecked) {
                  if (selectedDays.size > 1) {
                    selectedDays.remove(dayNum)
                  }
                } else {
                  val times = getStandardTimeForLesson(1)
                  selectedDays[dayNum] = DayConfigState(
                    dayOfWeek = dayNum,
                    lessonNumber = 1,
                    startTime = times.first,
                    endTime = times.second,
                    room = globalRoom
                  )
                }
              },
              label = { Text(dayName.take(3), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = UstozPrimary,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.weight(1f)
            )
          }
        }

        // Config slots for each selected day
        Text(
          text = "Tanlangan kunlar bo'yicha dars vaqtlari:",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        selectedDays.keys.sorted().forEach { dayNum ->
          val dayName = DAYS_OF_WEEK.find { it.first == dayNum }?.second ?: "Kun"
          val config = selectedDays[dayNum] ?: return@forEach

          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .background(UstozPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = dayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (selectedDays.size > 1) {
                  IconButton(
                    onClick = { selectedDays.remove(dayNum) },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(Icons.Default.Close, contentDescription = "Olib tashlash", modifier = Modifier.size(16.dp))
                  }
                }
              }

              // Lesson Number & Times Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Lesson Number selector
                Column(modifier = Modifier.weight(1f)) {
                  Text("Dars raqami", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { num ->
                      Box(
                        modifier = Modifier
                          .size(28.dp)
                          .clip(CircleShape)
                          .background(if (config.lessonNumber == num) UstozPrimary else MaterialTheme.colorScheme.surface)
                          .clickable {
                            config.lessonNumber = num
                            val standard = getStandardTimeForLesson(num)
                            config.startTime = standard.first
                            config.endTime = standard.second
                            // Trigger recomposition
                            selectedDays[dayNum] = config.copy()
                          },
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = num.toString(),
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = if (config.lessonNumber == num) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                      }
                    }
                  }
                }

                // Start & End Time
                OutlinedTextField(
                  value = config.startTime,
                  onValueChange = {
                    config.startTime = it
                    selectedDays[dayNum] = config.copy()
                  },
                  label = { Text("Boshlanish", fontSize = 10.sp) },
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(0.9f)
                )

                OutlinedTextField(
                  value = config.endTime,
                  onValueChange = {
                    config.endTime = it
                    selectedDays[dayNum] = config.copy()
                  },
                  label = { Text("Tugash", fontSize = 10.sp) },
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(0.9f)
                )
              }
            }
          }
        }

        errorText?.let {
          Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
            Text("Bekor qilish")
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              if (selectedClassId.isBlank()) {
                errorText = "Iltimos, sinfni tanlang"
                return@Button
              }
              if (subject.isBlank()) {
                errorText = "Fan nomini kiriting"
                return@Button
              }
              if (selectedDays.isEmpty()) {
                errorText = "Kamida bitta hafta kunini tanlang"
                return@Button
              }

              val slots = selectedDays.values.map {
                DayScheduleSlot(
                  dayOfWeek = it.dayOfWeek,
                  lessonNumber = it.lessonNumber,
                  startTime = it.startTime.trim(),
                  endTime = it.endTime.trim(),
                  room = it.room.trim().ifBlank { globalRoom.trim() }
                )
              }

              if (onAddMultiDay != null) {
                onAddMultiDay(selectedClassId, subject.trim(), slots)
              } else if (onAdd != null) {
                val first = slots.first()
                onAdd(selectedClassId, subject.trim(), first.dayOfWeek, first.startTime, first.endTime, first.room, first.lessonNumber)
              }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier.testTag("submit_create_schedule_button")
          ) {
            Text("${selectedDays.size} ta dars jadvalini saqlash")
          }
        }
      }
    }
  }
}

@Composable
fun ScheduleListDialog(
  schedules: List<ScheduleWithClass>,
  classes: List<ClassWithStudentCount>,
  onDismiss: () -> Unit,
  onAddClick: () -> Unit,
  onDeleteSchedule: (String) -> Unit,
  onUpdateSchedule: ((scheduleId: String, classId: String, subject: String, dayOfWeek: Int, startTime: String, endTime: String, room: String, lessonNumber: Int) -> Unit)? = null,
  onGenerateLessonsClick: () -> Unit = {}
) {
  var selectedDayTab by remember { mutableIntStateOf(1) }
  var editingSchedule by remember { mutableStateOf<ScheduleWithClass?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .height(580.dp)
        .padding(4.dp)
        .testTag("schedule_list_dialog")
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = UstozPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Dars Jadvali",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Days Tab Row
        ScrollableTabRow(
          selectedTabIndex = selectedDayTab - 1,
          edgePadding = 0.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          DAYS_OF_WEEK.forEach { (num, name) ->
            val countForDay = schedules.count { it.schedule.dayOfWeek == num }
            Tab(
              selected = selectedDayTab == num,
              onClick = { selectedDayTab = num },
              text = {
                Text(
                  text = "${name.take(3)} ($countForDay)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val daySchedules = schedules.filter { it.schedule.dayOfWeek == selectedDayTab }
          .sortedBy { it.schedule.lessonNumber }

        if (daySchedules.isEmpty()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Bu kunga darslar kiritilmagan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedButton(
                onClick = onAddClick,
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dars qo'shish", fontSize = 12.sp)
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(daySchedules, key = { it.schedule.id }) { item ->
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                      modifier = Modifier
                        .size(36.dp)
                        .background(UstozPrimaryContainer, CircleShape),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = "${item.schedule.lessonNumber}",
                        fontWeight = FontWeight.Bold,
                        color = UstozPrimary,
                        fontSize = 14.sp
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = "${item.className} • ${item.schedule.subjectId}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = "${item.schedule.startTime} – ${item.schedule.endTime}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = item.schedule.room,
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onUpdateSchedule != null) {
                      IconButton(
                        onClick = { editingSchedule = item },
                        modifier = Modifier.size(32.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Edit,
                          contentDescription = "Tahrirlash",
                          tint = UstozPrimary,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }

                    IconButton(
                      onClick = { onDeleteSchedule(item.schedule.id) },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "O'chirish",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onGenerateLessonsClick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("schedule_generate_lessons_button")
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Rejalash", fontSize = 13.sp)
          }

          Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier
              .weight(1.3f)
              .height(48.dp)
              .testTag("schedule_add_button")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Dars qo'shish", fontSize = 13.sp)
          }
        }
      }
    }
  }

  // Edit Schedule Dialog
  editingSchedule?.let { item ->
    EditScheduleDialog(
      scheduleWithClass = item,
      classes = classes,
      onDismiss = { editingSchedule = null },
      onSave = { scheduleId, classId, subject, dayOfWeek, startTime, endTime, room, lessonNumber ->
        onUpdateSchedule?.invoke(scheduleId, classId, subject, dayOfWeek, startTime, endTime, room, lessonNumber)
        editingSchedule = null
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleDialog(
  scheduleWithClass: ScheduleWithClass,
  classes: List<ClassWithStudentCount>,
  onDismiss: () -> Unit,
  onSave: (scheduleId: String, classId: String, subject: String, dayOfWeek: Int, startTime: String, endTime: String, room: String, lessonNumber: Int) -> Unit
) {
  val schedule = scheduleWithClass.schedule
  var selectedClassId by remember { mutableStateOf(schedule.classId) }
  var subject by remember { mutableStateOf(schedule.subjectId) }
  var selectedDay by remember { mutableIntStateOf(schedule.dayOfWeek) }
  var startTime by remember { mutableStateOf(schedule.startTime) }
  var endTime by remember { mutableStateOf(schedule.endTime) }
  var room by remember { mutableStateOf(schedule.room) }
  var lessonNumber by remember { mutableIntStateOf(schedule.lessonNumber) }

  var classDropdownExpanded by remember { mutableStateOf(false) }
  var dayDropdownExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Dars jadvalini tahrirlash",
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Class Selector
        ExposedDropdownMenuBox(
          expanded = classDropdownExpanded,
          onExpandedChange = { classDropdownExpanded = it }
        ) {
          val selectedClassName = classes.find { it.classEntity.id == selectedClassId }?.classEntity?.name ?: scheduleWithClass.className
          OutlinedTextField(
            value = selectedClassName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sinf") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(
            expanded = classDropdownExpanded,
            onDismissRequest = { classDropdownExpanded = false }
          ) {
            classes.forEach { c ->
              DropdownMenuItem(
                text = { Text(c.classEntity.name) },
                onClick = {
                  selectedClassId = c.classEntity.id
                  classDropdownExpanded = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Fan") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        // Day of Week
        ExposedDropdownMenuBox(
          expanded = dayDropdownExpanded,
          onExpandedChange = { dayDropdownExpanded = it }
        ) {
          val selectedDayName = DAYS_OF_WEEK.find { it.first == selectedDay }?.second ?: "Dushanba"
          OutlinedTextField(
            value = selectedDayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Hafta kuni") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(
            expanded = dayDropdownExpanded,
            onDismissRequest = { dayDropdownExpanded = false }
          ) {
            DAYS_OF_WEEK.forEach { (num, name) ->
              DropdownMenuItem(
                text = { Text(name) },
                onClick = {
                  selectedDay = num
                  dayDropdownExpanded = false
                }
              )
            }
          }
        }

        // Lesson Number and Room
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = lessonNumber.toString(),
            onValueChange = {
              val n = it.toIntOrNull() ?: 1
              lessonNumber = n
              val times = getStandardTimeForLesson(n)
              startTime = times.first
              endTime = times.second
            },
            label = { Text("Dars №") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(0.8f)
          )
          OutlinedTextField(
            value = room,
            onValueChange = { room = it },
            label = { Text("Xona") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1.2f)
          )
        }

        // Times
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text("Boshlanish") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = { Text("Tugash") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(schedule.id, selectedClassId, subject.trim(), selectedDay, startTime.trim(), endTime.trim(), room.trim(), lessonNumber)
        },
        enabled = subject.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLessonDialog(
  classes: List<ClassWithStudentCount>,
  defaultSubject: String,
  onDismiss: () -> Unit,
  onAdd: (classId: String, subject: String, date: String, startTime: String, endTime: String, room: String, topic: String) -> Unit
) {
  val todayStr = remember { java.time.LocalDate.now().toString() }
  var selectedClassId by remember { mutableStateOf(classes.firstOrNull()?.classEntity?.id ?: "") }
  var subject by remember { mutableStateOf(defaultSubject) }
  var date by remember { mutableStateOf(todayStr) }
  var startTime by remember { mutableStateOf("09:00") }
  var endTime by remember { mutableStateOf("09:45") }
  var room by remember { mutableStateOf("24-xona") }
  var topic by remember { mutableStateOf("") }
  var classDropdownExpanded by remember { mutableStateOf(false) }
  var errorText by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .testTag("create_lesson_dialog")
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Yangi Dars Qo'shish",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        if (classes.isEmpty()) {
          Text(
            text = "Dars yaratishdan oldin kamida bitta sinf qo'shishingiz kerak.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
          )
          Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Tushunarli")
          }
          return@Column
        }

        // Class Selector
        ExposedDropdownMenuBox(
          expanded = classDropdownExpanded,
          onExpandedChange = { classDropdownExpanded = it }
        ) {
          val selectedClassName = classes.find { it.classEntity.id == selectedClassId }?.classEntity?.name ?: "Sinfni tanlang"
          OutlinedTextField(
            value = selectedClassName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sinf") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(
            expanded = classDropdownExpanded,
            onDismissRequest = { classDropdownExpanded = false }
          ) {
            classes.forEach { c ->
              DropdownMenuItem(
                text = { Text(c.classEntity.name) },
                onClick = {
                  selectedClassId = c.classEntity.id
                  classDropdownExpanded = false
                }
              )
            }
          }
        }

        // Subject & Topic
        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Fan") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = topic,
          onValueChange = { topic = it },
          label = { Text("Dars mavzusi (masalan: Kasrlar)") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        // Date & Room
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Sana (YYYY-MM-DD)") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = room,
            onValueChange = { room = it },
            label = { Text("Xona") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )
        }

        // Start Time & End Time
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text("Boshlanish (09:00)") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = { Text("Tugash (09:45)") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )
        }

        errorText?.let {
          Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
            Text("Bekor qilish")
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              if (selectedClassId.isBlank()) {
                errorText = "Iltimos, sinfni tanlang"
                return@Button
              }
              if (subject.isBlank()) {
                errorText = "Fan nomini kiriting"
                return@Button
              }
              if (topic.isBlank()) {
                errorText = "Dars mavzusini kiriting"
                return@Button
              }
              if (startTime >= endTime) {
                errorText = "Boshlanish vaqti tugash vaqtidan oldin bo'lishi kerak"
                return@Button
              }
              onAdd(selectedClassId, subject, date, startTime, endTime, room, topic)
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier.testTag("submit_create_lesson_button")
          ) {
            Text("Darsni saqlash")
          }
        }
      }
    }
  }
}
