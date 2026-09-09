package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TeacherProfileEntity
import com.example.ui.theme.UstozGradientEnd
import com.example.ui.theme.UstozGradientStart
import com.example.ui.theme.UstozOnPrimaryContainer
import com.example.ui.theme.UstozOutline
import com.example.ui.theme.UstozPrimary
import com.example.ui.theme.UstozPrimaryContainer

@Composable
fun TeacherProfileDialog(
  profile: TeacherProfileEntity,
  onDismiss: () -> Unit,
  onEditClick: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(vertical = 24.dp)
        .testTag("teacher_profile_dialog")
    ) {
      val scrollState = rememberScrollState()
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(scrollState)
      ) {
        // Top bar with close button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "O'qituvchi Profili",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("profile_dialog_close_button")
          ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Avatar Header Card
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = Color.Transparent),
          modifier = Modifier
            .fillMaxWidth()
            .background(
              brush = Brush.linearGradient(listOf(UstozGradientStart, UstozGradientEnd)),
              shape = RoundedCornerShape(22.dp)
            )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .background(Color.White.copy(alpha = 0.25f), CircleShape)
                .border(2.5.dp, Color.White, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${profile.firstName.take(1)}${profile.lastName.take(1)}".uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = profile.fullName,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
              ),
              color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = profile.subject,
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.9f)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Details Section
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.12f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            ProfileInfoRow(
              icon = Icons.Default.School,
              label = "Ta'lim muassasasi",
              value = profile.effectiveSchoolName.ifBlank { "Kiritilmagan" }
            )
            ProfileInfoRow(
              icon = Icons.Default.Work,
              label = "Ish tajribasi",
              value = if (profile.experienceYears > 0) "${profile.experienceYears} yil" else profile.experience.ifBlank { "Kiritilmagan" }
            )
            ProfileInfoRow(
              icon = Icons.Default.Phone,
              label = "Telefon raqam",
              value = profile.effectivePhone.ifBlank { "Kiritilmagan" }
            )
            ProfileInfoRow(
              icon = Icons.Default.Email,
              label = "Elektron pochta",
              value = profile.effectiveEmail.ifBlank { "Kiritilmagan" }
            )
            ProfileInfoRow(
              icon = Icons.Default.Language,
              label = "Ilova tili",
              value = profile.language
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Security Identity Card
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = UstozPrimaryContainer.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = UstozPrimary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Xavfsiz Identifikator",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = UstozPrimary
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "User ID: ${profile.userId}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Profile ID: ${profile.profileId}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Edit Profile Button
        Button(
          onClick = {
            onDismiss()
            onEditClick()
          },
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("profile_edit_button")
        ) {
          Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Profilni tahrirlash", fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
private fun ProfileInfoRow(
  icon: ImageVector,
  label: String,
  value: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .background(UstozPrimaryContainer, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = UstozPrimary,
        modifier = Modifier.size(18.dp)
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
fun EditProfileDialog(
  profile: TeacherProfileEntity,
  onDismiss: () -> Unit,
  onSave: (firstName: String, lastName: String, phone: String, email: String, language: String, schoolName: String, subject: String, experienceYears: Int) -> Unit
) {
  var firstName by remember { mutableStateOf(profile.firstName) }
  var lastName by remember { mutableStateOf(profile.lastName) }
  var phone by remember { mutableStateOf(profile.effectivePhone) }
  var email by remember { mutableStateOf(profile.effectiveEmail) }
  var schoolName by remember { mutableStateOf(profile.effectiveSchoolName) }
  var subject by remember { mutableStateOf(profile.subject) }
  var experienceYearsStr by remember {
    mutableStateOf(if (profile.experienceYears > 0) profile.experienceYears.toString() else "")
  }
  var language by remember { mutableStateOf(profile.language) }

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
        .testTag("edit_profile_dialog")
    ) {
      val scrollState = rememberScrollState()
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Profilni tahrirlash",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Bekor qilish")
          }
        }

        OutlinedTextField(
          value = firstName,
          onValueChange = { firstName = it },
          label = { Text("Ism *") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_first_name_input"),
          shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
          value = lastName,
          onValueChange = { lastName = it },
          label = { Text("Familiya *") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_last_name_input"),
          shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Dars beradigan fan *") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_subject_input"),
          shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
          value = schoolName,
          onValueChange = { schoolName = it },
          label = { Text("Maktab / Ta'lim muassasasi") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_school_input"),
          shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
          value = experienceYearsStr,
          onValueChange = { if (it.all { c -> c.isDigit() }) experienceYearsStr = it },
          label = { Text("Tajriba (yillar)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_experience_input"),
          shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Telefon raqam") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_phone_input"),
          shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Elektron pochta") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_email_input"),
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
              val expYears = experienceYearsStr.toIntOrNull() ?: 0
              onSave(
                firstName.trim(),
                lastName.trim(),
                phone.trim(),
                email.trim(),
                language,
                schoolName.trim(),
                subject.trim(),
                expYears
              )
            },
            enabled = firstName.isNotBlank() && lastName.isNotBlank() && subject.isNotBlank(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier
              .weight(1f)
              .testTag("edit_profile_save_button")
          ) {
            Text("Saqlash")
          }
        }
      }
    }
  }
}
