package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ClassEntity
import com.example.data.model.TeacherProfileEntity
import com.example.data.util.ExportFormat
import com.example.ui.attendance.LessonCard
import com.example.ui.attendance.LessonDetailDialog
import com.example.ui.attendance.TezkorDavomatScreen
import com.example.ui.classes.ClassCard
import com.example.ui.classes.ClassDetailDialog
import com.example.ui.classes.ClassesView
import com.example.ui.classes.CreateClassDialog
import com.example.ui.classes.EmptyClassesState
import com.example.ui.classes.ExportClassDialog
import com.example.ui.ktp.KTPScreen
import com.example.ui.main.MainAppUiState
import com.example.ui.main.MainAppViewModel
import com.example.ui.main.MainTab
import com.example.ui.profile.EditProfileDialog
import com.example.ui.profile.TeacherProfileDialog
import com.example.ui.schedule.CreateLessonDialog
import com.example.ui.schedule.CreateScheduleDialog
import com.example.ui.schedule.GenerateLessonsDialog
import com.example.ui.schedule.ScheduleListDialog
import com.example.ui.students.AddStudentDialog
import com.example.ui.students.StudentDetailDialog
import com.example.ui.students.StudentImportDialog
import com.example.ui.students.StudentsView
import com.example.ui.theme.LateColor
import com.example.ui.theme.PresentColor
import com.example.ui.theme.UstozGradientEnd
import com.example.ui.theme.UstozGradientStart
import com.example.ui.theme.UstozOnPrimaryContainer
import com.example.ui.theme.UstozOutline
import com.example.ui.theme.UstozPrimary
import com.example.ui.theme.UstozPrimaryContainer
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  profile: TeacherProfileEntity,
  onLockApp: () -> Unit,
  onLogout: () -> Unit,
  mainViewModel: MainAppViewModel = viewModel()
) {
  val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
  val activeProfile = uiState.profile ?: profile
  val snackbarHostState = remember { SnackbarHostState() }
  var showLogoutConfirm by remember { mutableStateOf(false) }

  LaunchedEffect(profile.userId) {
    mainViewModel.loadAllData()
  }

  LaunchedEffect(uiState.successMessage) {
    uiState.successMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      mainViewModel.clearMessages()
    }
  }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      mainViewModel.clearMessages()
    }
  }

  // Logout confirmation dialog
  if (showLogoutConfirm) {
    AlertDialog(
      onDismissRequest = { showLogoutConfirm = false },
      title = { Text("Tizimdan chiqish") },
      text = {
        Text("Haqiqatan ham hisobdan chiqmoqchimisiz? Faol sessiyangiz to'xtatiladi va xavfsiz tozalanadi.")
      },
      confirmButton = {
        Button(
          onClick = {
            showLogoutConfirm = false
            onLogout()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          shape = CircleShape,
          modifier = Modifier.testTag("dashboard_confirm_logout_button")
        ) {
          Text("Chiqish")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutConfirm = false }) {
          Text("Bekor qilish")
        }
      }
    )
  }

  // Dialogs
  if (uiState.showProfileDialog) {
    TeacherProfileDialog(
      profile = activeProfile,
      onDismiss = { mainViewModel.setShowProfileDialog(false) },
      onEditClick = { mainViewModel.setShowEditProfileDialog(true) }
    )
  }

  if (uiState.showEditProfileDialog) {
    EditProfileDialog(
      profile = activeProfile,
      onDismiss = { mainViewModel.setShowEditProfileDialog(false) },
      onSave = { first, last, phone, email, lang, school, subj, expYears ->
        mainViewModel.updateProfile(first, last, phone, email, lang, school, subj, expYears)
      }
    )
  }

  if (uiState.showAddClassDialog) {
    CreateClassDialog(
      onDismiss = { mainViewModel.setShowAddClassDialog(false) },
      onCreate = { grade, section, year ->
        mainViewModel.createClass(grade, section, year)
      }
    )
  }

  uiState.selectedClassForDetail?.let { classEntity ->
    val classStudents = uiState.students.filter { it.classId == classEntity.id }
    ClassDetailDialog(
      classEntity = classEntity,
      students = classStudents,
      onDismiss = { mainViewModel.selectClassForDetail(null) },
      onAddStudentClick = { classId ->
        mainViewModel.setShowAddStudentDialog(true, defaultClassId = classId)
      },
      onStudentClick = { student ->
        mainViewModel.selectStudentForDetail(student)
      },
      onDeleteClassClick = { classId ->
        mainViewModel.deleteClass(classId)
      },
      onImportStudentsClick = { classId ->
        mainViewModel.openStudentImportDialog(targetClassId = classId)
      },
      onExportStudentsClick = { classToExport ->
        mainViewModel.openExportClassDialog(classToExport)
      },
      onGenerateLessonsClick = { _ ->
        mainViewModel.setShowGenerateLessonsDialog(true)
      }
    )
  }

  if (uiState.showExportClassDialog && uiState.classToExport != null) {
    val exportClass = uiState.classToExport!!
    val count = uiState.students.count { it.classId == exportClass.id }
    val context = LocalContext.current
    ExportClassDialog(
      classEntity = exportClass,
      studentCount = count,
      onDismiss = { mainViewModel.closeExportClassDialog() },
      onExport = { format, onIntentReady ->
        mainViewModel.exportClassStudents(context, exportClass, format, onIntentReady)
      }
    )
  }

  if (uiState.showAddStudentDialog) {
    AddStudentDialog(
      classes = uiState.classes,
      defaultClassId = uiState.targetClassIdForNewStudent,
      onDismiss = { mainViewModel.setShowAddStudentDialog(false) },
      onAdd = { classId, first, last, num, bdate, gender, phone ->
        mainViewModel.createStudent(classId, first, last, num, bdate, gender, phone)
      }
    )
  }

  uiState.selectedStudentForDetail?.let { student ->
    val className = uiState.classes.find { it.classEntity.id == student.classId }?.classEntity?.name ?: "Sinf"
    StudentDetailDialog(
      student = student,
      className = className,
      onDismiss = { mainViewModel.selectStudentForDetail(null) },
      onDeleteClick = { studentId ->
        mainViewModel.deleteStudent(studentId)
      }
    )
  }

  // Bosqich 3: Lesson and Schedule Dialogs
  uiState.selectedLessonForDetail?.let { lessonDetails ->
    LessonDetailDialog(
      lessonWithDetails = lessonDetails,
      onDismiss = { mainViewModel.selectLessonForDetail(null) },
      onTakeAttendanceClick = { lessonId ->
        mainViewModel.openAttendance(lessonId)
      },
      onDeleteLesson = { lessonId ->
        mainViewModel.deleteLesson(lessonId)
      }
    )
  }

  if (uiState.showCreateLessonDialog) {
    CreateLessonDialog(
      classes = uiState.classes,
      defaultSubject = activeProfile.subject,
      onDismiss = { mainViewModel.setShowCreateLessonDialog(false) },
      onAdd = { classId, subject, date, startTime, endTime, room, topic ->
        mainViewModel.createLesson(classId, subject, date, startTime, endTime, room, topic)
      }
    )
  }

  if (uiState.showScheduleListDialog) {
    ScheduleListDialog(
      schedules = uiState.schedules,
      classes = uiState.classes,
      onDismiss = { mainViewModel.setShowScheduleListDialog(false) },
      onAddClick = { mainViewModel.setShowCreateScheduleDialog(true) },
      onDeleteSchedule = { scheduleId ->
        mainViewModel.deleteSchedule(scheduleId)
      },
      onUpdateSchedule = { scheduleId, classId, subject, dayOfWeek, startTime, endTime, room, lessonNumber ->
        mainViewModel.updateSchedule(scheduleId, classId, subject, dayOfWeek, startTime, endTime, room, lessonNumber)
      },
      onGenerateLessonsClick = {
        mainViewModel.setShowScheduleListDialog(false)
        mainViewModel.setShowGenerateLessonsDialog(true)
      }
    )
  }

  if (uiState.showCreateScheduleDialog) {
    CreateScheduleDialog(
      classes = uiState.classes,
      defaultSubject = activeProfile.subject,
      onDismiss = { mainViewModel.setShowCreateScheduleDialog(false) },
      onAdd = { classId, subject, dayOfWeek, startTime, endTime, room, lessonNumber ->
        mainViewModel.createSchedule(classId, subject, dayOfWeek, startTime, endTime, room, lessonNumber)
      },
      onAddMultiDay = { classId, subject, slots ->
        mainViewModel.createMultiDaySchedule(classId, subject, slots)
      }
    )
  }

  if (uiState.showImportStudentsDialog) {
    StudentImportDialog(
      uiState = uiState,
      viewModel = mainViewModel,
      onDismiss = { mainViewModel.closeStudentImportDialog() }
    )
  }

  if (uiState.showGenerateLessonsDialog) {
    GenerateLessonsDialog(
      uiState = uiState,
      viewModel = mainViewModel,
      onDismiss = { mainViewModel.setShowGenerateLessonsDialog(false) }
    )
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Ustoz AI",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = activeProfile.subject,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          // Profile icon button
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(UstozPrimaryContainer)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 20.dp),
                onClick = { mainViewModel.setShowProfileDialog(true) }
              )
              .testTag("dashboard_profile_button"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${activeProfile.firstName.take(1)}${activeProfile.lastName.take(1)}".uppercase(),
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = UstozPrimary
              )
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Lock button
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(UstozPrimaryContainer.copy(alpha = 0.5f))
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 20.dp),
                onClick = onLockApp
              )
              .testTag("dashboard_lock_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Ilovani qulflash",
              tint = UstozOnPrimaryContainer,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Logout button
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.errorContainer)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 20.dp),
                onClick = { showLogoutConfirm = true }
              )
              .testTag("dashboard_logout_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Tizimdan chiqish",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp
      ) {
        // 1. Bugun
        NavigationBarItem(
          selected = uiState.currentTab == MainTab.BUGUN,
          onClick = { mainViewModel.selectTab(MainTab.BUGUN) },
          icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Bugun") },
          label = { Text("Bugun", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = UstozPrimary,
            selectedTextColor = UstozPrimary,
            indicatorColor = UstozPrimaryContainer
          ),
          modifier = Modifier.testTag("nav_tab_today")
        )

        // 2. Davomat (Real badge with pending attendance count)
        NavigationBarItem(
          selected = uiState.currentTab == MainTab.DAVOMAT,
          onClick = { mainViewModel.selectTab(MainTab.DAVOMAT) },
          icon = {
            if (uiState.pendingAttendanceCount > 0) {
              BadgedBox(
                badge = {
                  Badge(
                    containerColor = LateColor,
                    contentColor = Color.White
                  ) {
                    Text("${uiState.pendingAttendanceCount}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  }
                }
              ) {
                Icon(Icons.Default.FactCheck, contentDescription = "Davomat")
              }
            } else {
              Icon(Icons.Default.FactCheck, contentDescription = "Davomat")
            }
          },
          label = { Text("Davomat", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = UstozPrimary,
            selectedTextColor = UstozPrimary,
            indicatorColor = UstozPrimaryContainer
          ),
          modifier = Modifier.testTag("nav_tab_attendance")
        )

        // 3. KTP (Kalendar-tematik reja)
        NavigationBarItem(
          selected = uiState.currentTab == MainTab.KTP,
          onClick = { mainViewModel.selectTab(MainTab.KTP) },
          icon = {
            val totalWarnings = uiState.ktpDocuments.sumOf { it.warningCount }
            if (totalWarnings > 0) {
              BadgedBox(
                badge = {
                  Badge(
                    containerColor = LateColor,
                    contentColor = Color.White
                  ) {
                    Text("$totalWarnings", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                  }
                }
              ) {
                Icon(Icons.Default.MenuBook, contentDescription = "KTP")
              }
            } else {
              Icon(Icons.Default.MenuBook, contentDescription = "KTP")
            }
          },
          label = { Text("KTP", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = UstozPrimary,
            selectedTextColor = UstozPrimary,
            indicatorColor = UstozPrimaryContainer
          ),
          modifier = Modifier.testTag("nav_tab_ktp")
        )

        // 4. O‘quvchilar
        NavigationBarItem(
          selected = uiState.currentTab == MainTab.OQUVCHILAR,
          onClick = { mainViewModel.selectTab(MainTab.OQUVCHILAR) },
          icon = { Icon(Icons.Default.Groups, contentDescription = "O‘quvchilar") },
          label = { Text("O‘quvchilar", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = UstozPrimary,
            selectedTextColor = UstozPrimary,
            indicatorColor = UstozPrimaryContainer
          ),
          modifier = Modifier.testTag("nav_tab_students")
        )

        // 5. Sinflar
        NavigationBarItem(
          selected = uiState.currentTab == MainTab.SINFLAR,
          onClick = { mainViewModel.selectTab(MainTab.SINFLAR) },
          icon = { Icon(Icons.Default.Class, contentDescription = "Sinflar") },
          label = { Text("Sinflar", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = UstozPrimary,
            selectedTextColor = UstozPrimary,
            indicatorColor = UstozPrimaryContainer
          ),
          modifier = Modifier.testTag("nav_tab_classes")
        )
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(paddingValues)
    ) {
      when (uiState.currentTab) {
        MainTab.BUGUN -> {
          TodayDashboardContent(
            profile = activeProfile,
            uiState = uiState,
            onAddClassClick = { mainViewModel.setShowAddClassDialog(true) },
            onAddStudentClick = { mainViewModel.setShowAddStudentDialog(true) },
            onAddLesson = { mainViewModel.setShowCreateLessonDialog(true) },
            onViewSchedule = { mainViewModel.setShowScheduleListDialog(true) },
            onLessonClick = { lesson -> mainViewModel.selectLessonForDetail(lesson) },
            onTakeAttendance = { lessonId -> mainViewModel.openAttendance(lessonId) },
            onClassClick = { classEntity -> mainViewModel.selectClassForDetail(classEntity) },
            onProfileClick = { mainViewModel.setShowProfileDialog(true) }
          )
        }

        MainTab.DAVOMAT -> {
          if (uiState.activeAttendanceLesson != null) {
            TezkorDavomatScreen(
              lessonWithDetails = uiState.activeAttendanceLesson!!,
              items = uiState.activeAttendanceItems,
              summary = uiState.attendanceSummary,
              hasUnsavedChanges = uiState.hasUnsavedAttendanceChanges,
              isSaving = uiState.isSavingAttendance,
              onBack = { mainViewModel.closeAttendance() },
              onMarkStatus = { studentId, status, lateMins, note ->
                mainViewModel.markStudentStatus(studentId, status, lateMins, note)
              },
              onMarkAllPresent = { mainViewModel.markAllPresent() },
              onSave = { mainViewModel.saveActiveAttendance() }
            )
          } else {
            AttendanceHubContent(
              uiState = uiState,
              onOpenAttendance = { lessonId -> mainViewModel.openAttendance(lessonId) },
              onAddLesson = { mainViewModel.setShowCreateLessonDialog(true) },
              onViewSchedule = { mainViewModel.setShowScheduleListDialog(true) }
            )
          }
        }

        MainTab.KTP -> {
          KTPScreen(
            uiState = uiState,
            viewModel = mainViewModel
          )
        }

        MainTab.OQUVCHILAR -> {
          StudentsView(
            students = uiState.students,
            classes = uiState.classes,
            searchQuery = uiState.searchQuery,
            selectedClassFilterId = uiState.selectedClassFilterId,
            onSearchQueryChanged = { mainViewModel.onSearchQueryChanged(it) },
            onClassFilterChanged = { mainViewModel.onClassFilterChanged(it) },
            onStudentClick = { mainViewModel.selectStudentForDetail(it) },
            onAddStudentClick = { mainViewModel.setShowAddStudentDialog(true) },
            onImportStudentsClick = { mainViewModel.openStudentImportDialog() }
          )
        }

        MainTab.AI_USTOZ -> {
          PlaceholderModuleContent(
            title = "AI Shaxsiy Pedagogik Metodist",
            subtitle = "Sun'iy intellekt orqali metodik yordamchi",
            description = "Dars ishlanmalari (Konspekt), test savollari generatsiyasi va pedagogik tavsiyalar sun'iy intellekt vositasida shakllantiriladi.",
            icon = Icons.Default.AutoAwesome,
            activeCountInfo = "Fan: ${activeProfile.subject} bo'yicha metodik bazalar tayyorlanmoqda."
          )
        }

        MainTab.SINFLAR -> {
          ClassesView(
            classes = uiState.classes,
            onClassClick = { mainViewModel.selectClassForDetail(it) },
            onAddClassClick = { mainViewModel.setShowAddClassDialog(true) }
          )
        }
      }
    }
  }
}

@Composable
fun TodayDashboardContent(
  profile: TeacherProfileEntity,
  uiState: MainAppUiState,
  onAddClassClick: () -> Unit,
  onAddStudentClick: () -> Unit,
  onAddLesson: () -> Unit,
  onViewSchedule: () -> Unit,
  onLessonClick: (com.example.data.model.LessonWithDetails) -> Unit,
  onTakeAttendance: (String) -> Unit,
  onClassClick: (ClassEntity) -> Unit,
  onProfileClick: () -> Unit
) {
  val scrollState = rememberScrollState()

  // Dynamic time-based greeting
  val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
  val greetingText = when (currentHour) {
    in 5..11 -> "Xayrli tong, ${profile.firstName} 👋"
    in 12..17 -> "Xayrli kun, ${profile.firstName} 👋"
    else -> "Xayrli kech, ${profile.firstName} 👋"
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // Greeting Hero Card with Stitch Gradient
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = Color.Transparent),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
      modifier = Modifier
        .fillMaxWidth()
        .background(
          brush = Brush.linearGradient(listOf(UstozGradientStart, UstozGradientEnd)),
          shape = RoundedCornerShape(26.dp)
        )
        .testTag("dashboard_greeting_card")
    ) {
      Column(modifier = Modifier.padding(22.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .background(Color.White.copy(alpha = 0.2f), CircleShape)
              .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
              .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${profile.firstName.take(1)}${profile.lastName.take(1)}".uppercase(),
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = greetingText,
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp
              ),
              color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = profile.subject,
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.85f)
            )
          }
        }

        if (profile.effectiveSchoolName.isNotBlank()) {
          Spacer(modifier = Modifier.height(14.dp))
          Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.18f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = profile.effectiveSchoolName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
              )
            }
          }
        }
      }
    }

    // REAL STATS Bento Grid (NO fake statistics!)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Classes count card
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("dashboard_classes_stat_card")
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .background(UstozPrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Class,
              contentDescription = null,
              tint = UstozPrimary,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "${uiState.classCount}",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Sinflar soni",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Students count card
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("dashboard_students_stat_card")
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .background(UstozPrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Groups,
              contentDescription = null,
              tint = UstozPrimary,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "${uiState.studentCount}",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "O'quvchilar soni",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Davomat Telemetry KPI Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (uiState.pendingAttendanceCount > 0) LateColor.copy(alpha = 0.08f) else PresentColor.copy(alpha = 0.08f)
      ),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (uiState.pendingAttendanceCount > 0) LateColor.copy(alpha = 0.25f) else PresentColor.copy(alpha = 0.25f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("dashboard_attendance_kpi_card")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .background(if (uiState.pendingAttendanceCount > 0) LateColor else PresentColor, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (uiState.pendingAttendanceCount > 0) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (uiState.pendingAttendanceCount > 0) {
              "${uiState.pendingAttendanceCount} ta darsda yo'qlama kutilmoqda"
            } else if (uiState.todayLessons.isNotEmpty()) {
              "Bugungi barcha davomatlar olindi"
            } else {
              "Bugun uchun dars jadvali"
            },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = if (uiState.todayLessons.isNotEmpty()) {
              "Bugungi jami darslar soni: ${uiState.todayLessons.size} ta"
            } else {
              "Bugun uchun dars rejalashtirilmagan"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Bugungi Darslar Section
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Bugungi Darslar",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onViewSchedule,
          shape = RoundedCornerShape(12.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Jadval", fontSize = 12.sp)
        }
        Button(
          onClick = onAddLesson,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Dars", fontSize = 12.sp)
        }
      }
    }

    if (uiState.todayLessons.isEmpty()) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp)
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Bugun uchun darslar belgilanmagan",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Dars jadvalidan foydalaning yoki yangi dars rejalashtiring.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = onAddLesson,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier.testTag("dashboard_empty_create_lesson_button")
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Dars rejalashtirish")
          }
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        uiState.todayLessons.forEach { lessonWithDetails ->
          LessonCard(
            lessonWithDetails = lessonWithDetails,
            onClick = { onLessonClick(lessonWithDetails) },
            onTakeAttendance = { onTakeAttendance(lessonWithDetails.lesson.id) }
          )
        }
      }
    }

    // Quick Actions
    Text(
      text = "Tezkor Amallar",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = onAddClassClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
        modifier = Modifier
          .weight(1f)
          .height(50.dp)
          .testTag("dashboard_quick_add_class")
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Sinf qo'shish")
      }

      OutlinedButton(
        onClick = onAddStudentClick,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, UstozPrimary),
        modifier = Modifier
          .weight(1f)
          .height(50.dp)
          .testTag("dashboard_quick_add_student")
      ) {
        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = UstozPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("O'quvchi", color = UstozPrimary)
      }
    }

    // Recent Classes Section
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Sinflaringiz",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
      if (uiState.classes.isNotEmpty()) {
        Text(
          text = "Jami ${uiState.classes.size} ta",
          style = MaterialTheme.typography.bodySmall,
          color = UstozPrimary,
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    if (uiState.classes.isEmpty()) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Hozircha sinflar qo'shilmagan",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Boshlash uchun birinchi sinfingizni yarating.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = onAddClassClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier.testTag("dashboard_empty_create_class_button")
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sinf yaratish")
          }
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        uiState.classes.take(3).forEach { item ->
          ClassCard(item = item, onClick = { onClassClick(item.classEntity) })
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
  }
}

@Composable
fun AttendanceHubContent(
  uiState: MainAppUiState,
  onOpenAttendance: (String) -> Unit,
  onAddLesson: () -> Unit,
  onViewSchedule: () -> Unit
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Davomat Tizimi",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "Darslar bo'yicha real yo'qlama",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(
          onClick = onViewSchedule,
          modifier = Modifier
            .size(40.dp)
            .background(UstozPrimaryContainer, CircleShape)
        ) {
          Icon(Icons.Default.Schedule, contentDescription = "Dars Jadvali", tint = UstozPrimary)
        }
        IconButton(
          onClick = onAddLesson,
          modifier = Modifier
            .size(40.dp)
            .background(UstozPrimary, CircleShape)
        ) {
          Icon(Icons.Default.Add, contentDescription = "Dars qo'shish", tint = Color.White)
        }
      }
    }

    // Status Banner
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (uiState.pendingAttendanceCount > 0) LateColor.copy(alpha = 0.08f) else PresentColor.copy(alpha = 0.08f)
      ),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (uiState.pendingAttendanceCount > 0) LateColor.copy(alpha = 0.25f) else PresentColor.copy(alpha = 0.25f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .background(if (uiState.pendingAttendanceCount > 0) LateColor else PresentColor, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (uiState.pendingAttendanceCount > 0) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (uiState.pendingAttendanceCount > 0) {
              "${uiState.pendingAttendanceCount} ta darsda yo'qlama kutilmoqda"
            } else if (uiState.todayLessons.isNotEmpty()) {
              "Barcha darslarda davomat to'liq olindi"
            } else {
              "Bugun uchun darslar belgilanmagan"
            },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Bugungi darslar: ${uiState.todayLessons.size} ta | Sinflar: ${uiState.classCount} ta",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Section title
    Text(
      text = "Bugungi Darslar Ro'yxati",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground
    )

    if (uiState.todayLessons.isEmpty()) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(44.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Bugun uchun darslar mavjud emas",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Dars jadvalidan foydalaning yoki yangi dars rejalashtiring.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(16.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = onAddLesson,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Dars qo'shish")
            }
            OutlinedButton(
              onClick = onViewSchedule,
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Dars jadvali")
            }
          }
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        uiState.todayLessons.forEach { lessonWithDetails ->
          LessonCard(
            lessonWithDetails = lessonWithDetails,
            onClick = { onOpenAttendance(lessonWithDetails.lesson.id) },
            onTakeAttendance = { onOpenAttendance(lessonWithDetails.lesson.id) }
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
fun PlaceholderModuleContent(
  title: String,
  subtitle: String,
  description: String,
  icon: ImageVector,
  activeCountInfo: String
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
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
        imageVector = icon,
        contentDescription = null,
        tint = UstozPrimary,
        modifier = Modifier.size(40.dp)
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Surface(
      shape = CircleShape,
      color = UstozPrimaryContainer
    ) {
      Text(
        text = "Tez kunda",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = UstozOnPrimaryContainer,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      color = UstozPrimary
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.12f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = activeCountInfo,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.padding(14.dp)
      )
    }
  }
}
