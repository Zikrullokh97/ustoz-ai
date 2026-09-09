package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.UstozOnPrimaryContainer
import com.example.ui.theme.UstozOutline
import com.example.ui.theme.UstozPrimary
import com.example.ui.theme.UstozPrimaryContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(
  isLoading: Boolean,
  errorMessage: String?,
  onRegister: (
    firstName: String,
    lastName: String,
    emailOrPhone: String,
    password: String,
    language: String,
    subject: String,
    school: String
  ) -> Unit,
  onNavigateToLogin: () -> Unit,
  onNavigateBack: () -> Unit,
  onClearError: () -> Unit
) {
  var firstName by remember { mutableStateOf("") }
  var lastName by remember { mutableStateOf("") }
  var emailOrPhone by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var selectedLanguage by remember { mutableStateOf("O'zbekcha") }
  var subject by remember { mutableStateOf("") }
  var school by remember { mutableStateOf("") }

  val languages = listOf("O'zbekcha", "Русский", "English")

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Ro'yxatdan o'tish",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        },
        navigationIcon = {
          Box(
            modifier = Modifier
              .padding(start = 12.dp)
              .size(44.dp)
              .clip(CircleShape)
              .background(UstozPrimaryContainer)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 22.dp),
                onClick = onNavigateBack
              )
              .testTag("register_back_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Orqaga qaytish",
              tint = UstozOnPrimaryContainer,
              modifier = Modifier.size(22.dp)
            )
          }
        },
        actions = {
          Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 16.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(UstozPrimary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "UA",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    }
  ) { paddingValues ->
    val scrollState = rememberScrollState()

    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(paddingValues)
        .imePadding()
        .verticalScroll(scrollState)
        .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Yangi hisob yaratish",
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp
        ),
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Siz uchun shaxsiy User ID va xavfsiz Teacher Profile yaratiladi",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(20.dp))

      if (!errorMessage.isNullOrBlank()) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("register_error_banner")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.ErrorOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
              text = errorMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Name inputs row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = firstName,
          onValueChange = {
            firstName = it
            if (errorMessage != null) onClearError()
          },
          label = { Text("Ism") },
          leadingIcon = {
            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = UstozPrimary)
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = UstozPrimary,
            unfocusedBorderColor = UstozOutline.copy(alpha = 0.25f)
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("register_firstname_input")
        )
        OutlinedTextField(
          value = lastName,
          onValueChange = {
            lastName = it
            if (errorMessage != null) onClearError()
          },
          label = { Text("Familiya") },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = UstozPrimary,
            unfocusedBorderColor = UstozOutline.copy(alpha = 0.25f)
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("register_lastname_input")
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = emailOrPhone,
        onValueChange = {
          emailOrPhone = it
          if (errorMessage != null) onClearError()
        },
        label = { Text("Telefon yoki email") },
        placeholder = { Text("+998901234567 yoki ustoz@maktab.uz") },
        leadingIcon = {
          Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = UstozPrimary)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Email,
          imeAction = ImeAction.Next
        ),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = UstozPrimary,
          unfocusedBorderColor = UstozOutline.copy(alpha = 0.25f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("register_email_input")
      )

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = password,
        onValueChange = {
          password = it
          if (errorMessage != null) onClearError()
        },
        label = { Text("Parol") },
        placeholder = { Text("Kamida 6 ta belgi") },
        leadingIcon = {
          Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = UstozPrimary)
        },
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = if (passwordVisible) "Yashirish" else "Ko'rsatish",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Password,
          imeAction = ImeAction.Next
        ),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = UstozPrimary,
          unfocusedBorderColor = UstozOutline.copy(alpha = 0.25f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("register_password_input")
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Language selection
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Ilova tili",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          languages.forEach { lang ->
            FilterChip(
              selected = selectedLanguage == lang,
              onClick = { selectedLanguage = lang },
              label = { Text(lang) },
              shape = CircleShape,
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = UstozPrimaryContainer,
                selectedLabelColor = UstozOnPrimaryContainer
              )
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = subject,
        onValueChange = { subject = it },
        label = { Text("Dars beradigan fan") },
        placeholder = { Text("Masalan: Matematika, Fizika, Ingliz tili") },
        leadingIcon = {
          Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = UstozPrimary)
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = UstozPrimary,
          unfocusedBorderColor = UstozOutline.copy(alpha = 0.25f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("register_subject_input")
      )

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = school,
        onValueChange = { school = it },
        label = { Text("Maktab / Ta'lim muassasasi (ixtiyoriy)") },
        placeholder = { Text("Masalan: 45-umumta'lim maktabi") },
        leadingIcon = {
          Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = UstozPrimary)
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = UstozPrimary,
          unfocusedBorderColor = UstozOutline.copy(alpha = 0.25f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("register_school_input")
      )

      Spacer(modifier = Modifier.height(28.dp))

      Button(
        onClick = {
          onRegister(
            firstName,
            lastName,
            emailOrPhone,
            password,
            selectedLanguage,
            subject,
            school
          )
        },
        enabled = firstName.isNotBlank() && lastName.isNotBlank() &&
            emailOrPhone.isNotBlank() && password.length >= 6 && !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("register_submit_button"),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        )
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.5.dp
          )
        } else {
          Text(
            text = "Ro'yxatdan o'tish",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Hisobingiz bormi?",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
          onClick = onNavigateToLogin,
          shape = CircleShape,
          modifier = Modifier.testTag("register_go_to_login_button")
        ) {
          Text(
            text = "Kirish",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}
