package com.example.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.main.MainAppUiState
import com.example.ui.main.MainAppViewModel
import com.example.ui.theme.UstozPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateLessonsDialog(
  uiState: MainAppUiState,
  viewModel: MainAppViewModel,
  onDismiss: () -> Unit
) {
  var startDateStr by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
  var endDateStr by remember { mutableStateOf(LocalDate.now().plusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE)) }
  var selectedClassId by remember { mutableStateOf<String?>("") }
  var classExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Dars jadvalidan rejalashtirish", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Haftalik dars jadvalingiz asosida ko'rsatilgan muddatdagi real darslarni avtomatik yaratish.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Class selector (optional: all or specific class)
        ExposedDropdownMenuBox(
          expanded = classExpanded,
          onExpandedChange = { classExpanded = !classExpanded }
        ) {
          val currentClassName = if (selectedClassId.isNullOrBlank()) "Barcha sinflar" else {
            uiState.classes.firstOrNull { it.classEntity.id == selectedClassId }?.classEntity?.name ?: "Barcha sinflar"
          }
          OutlinedTextField(
            value = currentClassName,
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
            DropdownMenuItem(
              text = { Text("Barcha sinflar") },
              onClick = {
                selectedClassId = null
                classExpanded = false
              }
            )
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
          value = startDateStr,
          onValueChange = { startDateStr = it },
          label = { Text("Boshlanish sanasi (YYYY-MM-DD)") },
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = endDateStr,
          onValueChange = { endDateStr = it },
          label = { Text("Tugash sanasi (YYYY-MM-DD)") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val start = try { LocalDate.parse(startDateStr.trim()) } catch (_: Exception) { LocalDate.now() }
          val end = try { LocalDate.parse(endDateStr.trim()) } catch (_: Exception) { LocalDate.now().plusMonths(1) }
          viewModel.generateLessonsFromSchedule(start, end, selectedClassId)
        },
        colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary)
      ) {
        Text("Darslarni yaratish")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Bekor qilish")
      }
    }
  )
}
