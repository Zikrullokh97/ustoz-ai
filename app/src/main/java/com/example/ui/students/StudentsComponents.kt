package com.example.ui.students

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ClassEntity
import com.example.data.model.ClassWithStudentCount
import com.example.data.model.StudentEntity
import com.example.ui.theme.UstozOnPrimaryContainer
import com.example.ui.theme.UstozOutline
import com.example.ui.theme.UstozPrimary
import com.example.ui.theme.UstozPrimaryContainer

@Composable
fun StudentsView(
  students: List<StudentEntity>,
  classes: List<ClassWithStudentCount>,
  searchQuery: String,
  selectedClassFilterId: String?,
  onSearchQueryChanged: (String) -> Unit,
  onClassFilterChanged: (String?) -> Unit,
  onStudentClick: (StudentEntity) -> Unit,
  onAddStudentClick: () -> Unit,
  onImportStudentsClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val classMap = remember(classes) {
    classes.associate { it.classEntity.id to it.classEntity.name }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 16.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "O‘quvchilar",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "Jami ${students.size} nafar o'quvchi",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
          onClick = onImportStudentsClick,
          shape = CircleShape,
          modifier = Modifier.testTag("students_import_file_button_top")
        ) {
          Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Fayldan", fontSize = 12.sp)
        }

        Button(
          onClick = onAddStudentClick,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
          modifier = Modifier.testTag("students_add_button_top")
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Qo'shish", fontSize = 12.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Search bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChanged,
      placeholder = { Text("Ism, familiya yoki ID orqali qidirish...") },
      leadingIcon = {
        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = UstozPrimary)
      },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchQueryChanged("") }) {
            Icon(imageVector = Icons.Default.Clear, contentDescription = "Tozalash")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(18.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("students_search_input")
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Class Filter Chips
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      item {
        val isSelected = selectedClassFilterId == null
        Surface(
          shape = CircleShape,
          color = if (isSelected) UstozPrimary else UstozPrimaryContainer.copy(alpha = 0.5f),
          modifier = Modifier
            .clickable { onClassFilterChanged(null) }
            .testTag("filter_all_classes")
        ) {
          Text(
            text = "Barcha sinflar",
            style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.SemiBold,
              color = if (isSelected) Color.White else UstozOnPrimaryContainer
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
          )
        }
      }

      items(classes, key = { it.classEntity.id }) { item ->
        val isSelected = selectedClassFilterId == item.classEntity.id
        Surface(
          shape = CircleShape,
          color = if (isSelected) UstozPrimary else UstozPrimaryContainer.copy(alpha = 0.5f),
          modifier = Modifier
            .clickable { onClassFilterChanged(item.classEntity.id) }
            .testTag("filter_class_${item.classEntity.name}")
        ) {
          Text(
            text = item.classEntity.name,
            style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.SemiBold,
              color = if (isSelected) Color.White else UstozOnPrimaryContainer
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Students list
    if (students.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp)
        ) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .background(UstozPrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Groups,
              contentDescription = null,
              tint = UstozPrimary,
              modifier = Modifier.size(36.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = if (searchQuery.isNotBlank()) "Hech qanday o'quvchi topilmadi" else "Hozircha o'quvchilar qo'shilmagan",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = if (searchQuery.isNotBlank()) "Qidiruv so'zini o'zgartirib ko'ring" else "Sinfga yangi o'quvchi qo'shish uchun quyidagi tugmani bosing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          if (searchQuery.isBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
              onClick = onAddStudentClick,
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
              modifier = Modifier.testTag("students_empty_add_button")
            ) {
              Icon(imageVector = Icons.Default.Add, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("O'quvchi qo'shish")
            }
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(students, key = { it.id }) { student ->
          val className = classMap[student.classId] ?: "Sinf"
          StudentCard(
            student = student,
            className = className,
            onClick = { onStudentClick(student) }
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
fun StudentCard(
  student: StudentEntity,
  className: String,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.12f)),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true),
        onClick = onClick
      )
      .testTag("student_card_${student.studentNumber}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(UstozPrimaryContainer, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "${student.firstName.take(1)}${student.lastName.take(1)}".uppercase(),
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = UstozPrimary
          )
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = student.fullName,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(3.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CircleShape,
            color = UstozPrimaryContainer.copy(alpha = 0.6f)
          ) {
            Text(
              text = className,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = UstozOnPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }

          Text(
            text = "ID: ${student.studentNumber}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Text(
            text = "• ${student.gender}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentDialog(
  classes: List<ClassWithStudentCount>,
  defaultClassId: String? = null,
  onDismiss: () -> Unit,
  onAdd: (classId: String, firstName: String, lastName: String, studentNumber: String, birthDate: String, gender: String, phone: String) -> Unit
) {
  var selectedClassId by remember {
    mutableStateOf(defaultClassId ?: classes.firstOrNull()?.classEntity?.id ?: "")
  }
  var firstName by remember { mutableStateOf("") }
  var lastName by remember { mutableStateOf("") }
  var studentNumber by remember {
    mutableStateOf("ST-" + (1000 + (Math.random() * 9000).toInt()))
  }
  var birthDate by remember { mutableStateOf("2012-05-15") }
  var gender by remember { mutableStateOf("Erkak") }
  var phone by remember { mutableStateOf("") }

  var isDropdownExpanded by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(vertical = 20.dp)
        .testTag("add_student_dialog")
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
          Text(
            text = "Yangi o'quvchi qo'shish",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        if (classes.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer
          ) {
            Text(
              text = "O'quvchi qo'shish uchun avval kamida bitta sinf yaratishingiz kerak.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(12.dp)
            )
          }
        } else {
          // Class picker
          Text(
            text = "Biriktiriladigan sinf *",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
          )

          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(classes) { item ->
              val isSelected = item.classEntity.id == selectedClassId
              Surface(
                shape = CircleShape,
                color = if (isSelected) UstozPrimary else UstozPrimaryContainer.copy(alpha = 0.5f),
                modifier = Modifier
                  .clickable { selectedClassId = item.classEntity.id }
                  .testTag("select_class_chip_${item.classEntity.name}")
              ) {
                Text(
                  text = item.classEntity.name,
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else UstozOnPrimaryContainer
                  ),
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = firstName,
          onValueChange = { firstName = it },
          label = { Text("Ism *") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_student_first_name_input"),
          shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
          value = lastName,
          onValueChange = { lastName = it },
          label = { Text("Familiya *") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_student_last_name_input"),
          shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
          value = studentNumber,
          onValueChange = { studentNumber = it },
          label = { Text("O'quvchi raqami (Student ID) *") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_student_number_input"),
          shape = RoundedCornerShape(14.dp)
        )

        // Gender selector
        Text(
          text = "Jinsi *",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          listOf("Erkak", "Ayol").forEach { g ->
            val isSelected = gender == g
            Surface(
              shape = CircleShape,
              color = if (isSelected) UstozPrimary else UstozPrimaryContainer.copy(alpha = 0.4f),
              modifier = Modifier
                .clickable { gender = g }
                .testTag("gender_chip_$g")
            ) {
              Text(
                text = g,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = if (isSelected) Color.White else UstozOnPrimaryContainer
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
              )
            }
          }
        }

        OutlinedTextField(
          value = birthDate,
          onValueChange = { birthDate = it },
          label = { Text("Tug'ilgan sana (YYYY-MM-DD)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_student_birth_date_input"),
          shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Ota-ona telefon raqami (ixtiyoriy)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_student_phone_input"),
          shape = RoundedCornerShape(14.dp)
        )

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
              if (selectedClassId.isNotBlank() && firstName.isNotBlank() && lastName.isNotBlank()) {
                onAdd(
                  selectedClassId,
                  firstName.trim(),
                  lastName.trim(),
                  studentNumber.trim(),
                  birthDate.trim(),
                  gender.trim(),
                  phone.trim()
                )
              }
            },
            enabled = selectedClassId.isNotBlank() && firstName.isNotBlank() && lastName.isNotBlank(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier
              .weight(1f)
              .testTag("add_student_submit_button")
          ) {
            Text("Qo'shish")
          }
        }
      }
    }
  }
}

@Composable
fun StudentDetailDialog(
  student: StudentEntity,
  className: String,
  onDismiss: () -> Unit,
  onDeleteClick: (studentId: String) -> Unit
) {
  var showDeleteConfirm by remember { mutableStateOf(false) }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("O'quvchini o'chirish") },
      text = {
        Text("Haqiqatan ham ${student.fullName} o'quvchisini ro'yxatdan o'chirmoqchimisiz?")
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            onDeleteClick(student.id)
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          shape = CircleShape,
          modifier = Modifier.testTag("confirm_delete_student_button")
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
        .fillMaxWidth(0.92f)
        .padding(vertical = 20.dp)
        .testTag("student_detail_dialog")
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
          Text(
            text = "O'quvchi ma'lumoti",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        // Avatar Card
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = UstozPrimaryContainer.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(56.dp)
                .background(UstozPrimary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${student.firstName.take(1)}${student.lastName.take(1)}".uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = student.fullName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "ID: ${student.studentNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Details
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.1f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            InfoLine(label = "Sinf", value = className)
            InfoLine(label = "Jinsi", value = student.gender)
            InfoLine(label = "Tug'ilgan sana", value = student.birthDate.ifBlank { "Kiritilmagan" })
            InfoLine(label = "Telefon", value = student.phone.ifBlank { "Kiritilmagan" })
            InfoLine(label = "O'quvchi unikal ID", value = student.id)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = CircleShape,
            modifier = Modifier.testTag("delete_student_button")
          ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("O'quvchini o'chirish")
          }

          TextButton(onClick = onDismiss) {
            Text("Yopish")
          }
        }
      }
    }
  }
}

@Composable
private fun InfoLine(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
