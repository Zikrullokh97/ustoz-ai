package com.example.ui.main

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UstozDatabase
import com.example.data.model.AttendanceAuditEntity
import com.example.data.model.AttendanceDetectionState
import com.example.data.model.AttendanceMethod
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceSummary
import com.example.data.model.BulkAttendanceRecord
import com.example.data.model.CameraEntity
import com.example.data.model.ClassEntity
import com.example.data.model.ClassWithStudentCount
import com.example.data.model.FaceDetectionCandidate
import com.example.data.model.KTPDocumentEntity
import com.example.data.model.KTPEntryEntity
import com.example.data.model.LessonEntity
import com.example.data.model.LessonWithDetails
import com.example.data.model.ParsedStudentDraft
import com.example.data.model.ScheduleEntity
import com.example.data.model.ScheduleWithClass
import com.example.data.model.StudentAttendanceItem
import com.example.data.model.StudentCardEntity
import com.example.data.model.StudentEntity
import com.example.data.model.StudentFaceEnrollmentEntity
import com.example.data.model.StudentImportPreview
import com.example.data.model.TeacherProfileEntity
import com.example.data.repository.AttendanceScanResult
import com.example.data.repository.AuthRepository
import com.example.data.repository.AutomatedAttendanceRepository
import com.example.data.repository.DayScheduleSlot
import com.example.data.repository.KTPRepository
import com.example.data.repository.LessonRepository
import com.example.data.repository.StudentFileParser
import com.example.data.repository.TeacherRepository
import com.example.data.util.ClassExporter
import com.example.data.util.ExportFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

enum class MainTab {
  BUGUN,
  DAVOMAT,
  KTP,
  OQUVCHILAR,
  AI_USTOZ,
  SINFLAR
}

data class StudentImportResultSummary(
  val totalChecked: Int,
  val addedCount: Int,
  val duplicateCount: Int,
  val errorCount: Int,
  val className: String
)

data class MainAppUiState(
  val currentTab: MainTab = MainTab.BUGUN,
  val profile: TeacherProfileEntity? = null,
  val classes: List<ClassWithStudentCount> = emptyList(),
  val students: List<StudentEntity> = emptyList(),
  val classCount: Int = 0,
  val studentCount: Int = 0,
  val searchQuery: String = "",
  val selectedClassFilterId: String? = null,
  val selectedClassForDetail: ClassEntity? = null,
  val selectedStudentForDetail: StudentEntity? = null,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val showAddClassDialog: Boolean = false,
  val showAddStudentDialog: Boolean = false,
  val showProfileDialog: Boolean = false,
  val showEditProfileDialog: Boolean = false,
  val targetClassIdForNewStudent: String? = null,

  // Bosqich 3: Schedule, Lesson, Davomat states
  val todayLessons: List<LessonWithDetails> = emptyList(),
  val pendingAttendanceCount: Int = 0,
  val schedules: List<ScheduleWithClass> = emptyList(),
  val selectedLessonForDetail: LessonWithDetails? = null,
  val activeAttendanceLesson: LessonWithDetails? = null,
  val activeAttendanceItems: List<StudentAttendanceItem> = emptyList(),
  val attendanceSummary: AttendanceSummary = AttendanceSummary(0, 0, 0, 0, 0),
  val hasUnsavedAttendanceChanges: Boolean = false,
  val isSavingAttendance: Boolean = false,
  val showCreateLessonDialog: Boolean = false,
  val showCreateScheduleDialog: Boolean = false,
  val showScheduleListDialog: Boolean = false,
  val classLessonsForDetail: List<LessonWithDetails> = emptyList(),

  // Bosqich 4: KTP (Kalendar-tematik rejalashtirish)
  val ktpDocuments: List<KTPDocumentEntity> = emptyList(),
  val selectedKTPDocument: KTPDocumentEntity? = null,
  val ktpEntries: List<KTPEntryEntity> = emptyList(),
  val isLoadingKTP: Boolean = false,
  val isConfirmingKTP: Boolean = false,
  val showUploadKTPDialog: Boolean = false,
  val selectedKTPEntryForEdit: KTPEntryEntity? = null,
  val ktpFilterSubject: String? = null,
  val ktpFilterClassId: String? = null,

  // Bosqich 4: Student File Import
  val showImportStudentsDialog: Boolean = false,
  val importStudentsPreview: StudentImportPreview? = null,
  val importTargetClassId: String? = null,
  val isImportingStudents: Boolean = false,
  val lastImportResult: StudentImportResultSummary? = null,
  val showGenerateLessonsDialog: Boolean = false,

  // Bosqich 4: Class Export
  val showExportClassDialog: Boolean = false,
  val classToExport: ClassEntity? = null,
  val isExportingClass: Boolean = false,

  // Bosqich 5: Automated Attendance states
  val selectedAttendanceMethod: AttendanceMethod = AttendanceMethod.MANUAL,
  val cameras: List<CameraEntity> = emptyList(),
  val selectedCamera: CameraEntity? = null,
  val detectedCandidates: List<AttendanceScanResult> = emptyList(),
  val lastScannedResult: AttendanceScanResult? = null,
  val showAttendanceConfirmDialog: Boolean = false,
  val showCameraManagementDialog: Boolean = false,
  val showAttendanceAuditDialog: Boolean = false,
  val attendanceAudits: List<AttendanceAuditEntity> = emptyList(),
  val selectedStudentCard: StudentCardEntity? = null,
  val selectedStudentFaceEnrollment: StudentFaceEnrollmentEntity? = null,
  val showStudentQrDialog: Boolean = false,
  val showStudentCardDialog: Boolean = false,
  val showStudentFaceDialog: Boolean = false,
  val studentForAutomationModal: StudentEntity? = null
)

class MainAppViewModel @JvmOverloads constructor(
  application: Application,
  private val teacherRepository: TeacherRepository = TeacherRepository(
    sessionDao = UstozDatabase.getDatabase(application).sessionDao(),
    teacherProfileDao = UstozDatabase.getDatabase(application).teacherProfileDao(),
    classDao = UstozDatabase.getDatabase(application).classDao(),
    studentDao = UstozDatabase.getDatabase(application).studentDao(),
    authRepository = AuthRepository(
      userDao = UstozDatabase.getDatabase(application).userDao(),
      teacherProfileDao = UstozDatabase.getDatabase(application).teacherProfileDao(),
      sessionDao = UstozDatabase.getDatabase(application).sessionDao()
    ),
    database = UstozDatabase.getDatabase(application)
  ),
  private val lessonRepository: LessonRepository = LessonRepository(
    database = UstozDatabase.getDatabase(application),
    scheduleDao = UstozDatabase.getDatabase(application).scheduleDao(),
    lessonDao = UstozDatabase.getDatabase(application).lessonDao(),
    attendanceDao = UstozDatabase.getDatabase(application).attendanceDao(),
    classDao = UstozDatabase.getDatabase(application).classDao(),
    studentDao = UstozDatabase.getDatabase(application).studentDao(),
    sessionDao = UstozDatabase.getDatabase(application).sessionDao(),
    teacherProfileDao = UstozDatabase.getDatabase(application).teacherProfileDao(),
    authRepository = AuthRepository(
      userDao = UstozDatabase.getDatabase(application).userDao(),
      teacherProfileDao = UstozDatabase.getDatabase(application).teacherProfileDao(),
      sessionDao = UstozDatabase.getDatabase(application).sessionDao()
    )
  ),
  private val ktpRepository: KTPRepository = KTPRepository(
    database = UstozDatabase.getDatabase(application),
    ktpDao = UstozDatabase.getDatabase(application).ktpDao(),
    scheduleDao = UstozDatabase.getDatabase(application).scheduleDao(),
    lessonDao = UstozDatabase.getDatabase(application).lessonDao(),
    classDao = UstozDatabase.getDatabase(application).classDao(),
    sessionDao = UstozDatabase.getDatabase(application).sessionDao(),
    authRepository = AuthRepository(
      userDao = UstozDatabase.getDatabase(application).userDao(),
      teacherProfileDao = UstozDatabase.getDatabase(application).teacherProfileDao(),
      sessionDao = UstozDatabase.getDatabase(application).sessionDao()
    )
  ),
  private val automatedAttendanceRepository: AutomatedAttendanceRepository = AutomatedAttendanceRepository(
    database = UstozDatabase.getDatabase(application),
    automatedAttendanceDao = UstozDatabase.getDatabase(application).automatedAttendanceDao(),
    attendanceDao = UstozDatabase.getDatabase(application).attendanceDao(),
    studentDao = UstozDatabase.getDatabase(application).studentDao(),
    lessonDao = UstozDatabase.getDatabase(application).lessonDao(),
    sessionDao = UstozDatabase.getDatabase(application).sessionDao(),
    authRepository = AuthRepository(
      userDao = UstozDatabase.getDatabase(application).userDao(),
      teacherProfileDao = UstozDatabase.getDatabase(application).teacherProfileDao(),
      sessionDao = UstozDatabase.getDatabase(application).sessionDao()
    )
  )
) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(MainAppUiState())
  val uiState: StateFlow<MainAppUiState> = _uiState.asStateFlow()

  init {
    loadAllData()
  }

  fun selectTab(tab: MainTab) {
    _uiState.update { it.copy(currentTab = tab, errorMessage = null) }
    if (tab == MainTab.KTP) {
      loadKTPDocuments()
    }
  }

  fun loadAllData() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      loadProfileInternal()
      loadStatsInternal()
      loadClassesInternal()
      loadStudentsInternal()
      loadTodayLessonsInternal()
      loadSchedulesInternal()
      loadPendingAttendanceCountInternal()
      loadCameras()
      _uiState.update { it.copy(isLoading = false) }
    }
  }

  private suspend fun loadTodayLessonsInternal() {
    lessonRepository.getTodayLessons().fold(
      onSuccess = { list ->
        _uiState.update { it.copy(todayLessons = list) }
      },
      onFailure = { error ->
        _uiState.update { it.copy(errorMessage = error.message) }
      }
    )
  }

  private suspend fun loadSchedulesInternal() {
    lessonRepository.getSchedules().fold(
      onSuccess = { list ->
        _uiState.update { it.copy(schedules = list) }
      },
      onFailure = { error ->
        _uiState.update { it.copy(errorMessage = error.message) }
      }
    )
  }

  private suspend fun loadPendingAttendanceCountInternal() {
    lessonRepository.getPendingAttendanceCount().fold(
      onSuccess = { count ->
        _uiState.update { it.copy(pendingAttendanceCount = count) }
      },
      onFailure = {}
    )
  }

  private suspend fun loadProfileInternal() {
    teacherRepository.getProfile().fold(
      onSuccess = { prof ->
        _uiState.update { it.copy(profile = prof) }
      },
      onFailure = { error ->
        _uiState.update { it.copy(errorMessage = error.message) }
      }
    )
  }

  private suspend fun loadStatsInternal() {
    teacherRepository.getDashboardStats().fold(
      onSuccess = { (classes, students) ->
        _uiState.update { it.copy(classCount = classes, studentCount = students) }
      },
      onFailure = {}
    )
  }

  private suspend fun loadClassesInternal() {
    teacherRepository.getClasses().fold(
      onSuccess = { list ->
        _uiState.update { it.copy(classes = list) }
      },
      onFailure = { error ->
        _uiState.update { it.copy(errorMessage = error.message) }
      }
    )
  }

  private suspend fun loadStudentsInternal() {
    val state = _uiState.value
    teacherRepository.getStudents(
      classId = state.selectedClassFilterId,
      searchQuery = state.searchQuery
    ).fold(
      onSuccess = { list ->
        _uiState.update { it.copy(students = list) }
      },
      onFailure = { error ->
        _uiState.update { it.copy(errorMessage = error.message) }
      }
    )
  }

  // ==========================================
  // PROFILE ACTIONS
  // ==========================================

  fun updateProfile(
    firstName: String,
    lastName: String,
    phone: String,
    email: String,
    language: String,
    schoolName: String,
    subject: String,
    experienceYears: Int
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      teacherRepository.updateProfile(
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        email = email,
        language = language,
        schoolName = schoolName,
        subject = subject,
        experienceYears = experienceYears
      ).fold(
        onSuccess = { updated ->
          _uiState.update {
            it.copy(
              profile = updated,
              isLoading = false,
              showEditProfileDialog = false,
              successMessage = "Profil muvaffaqiyatli yangilandi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Profilni yangilashda xatolik"
            )
          }
        }
      )
    }
  }

  // ==========================================
  // CLASS ACTIONS
  // ==========================================

  fun createClass(grade: Int, section: String, academicYear: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      teacherRepository.createClass(grade, section, academicYear).fold(
        onSuccess = {
          loadClassesInternal()
          loadStatsInternal()
          _uiState.update {
            it.copy(
              isLoading = false,
              showAddClassDialog = false,
              successMessage = "Sinf muvaffaqiyatli yaratildi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Sinf yaratishda xatolik"
            )
          }
        }
      )
    }
  }

  fun createClassWithSchedule(
    grade: Int,
    section: String,
    academicYear: String,
    subject: String,
    slots: List<DayScheduleSlot>
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      if (slots.isEmpty()) {
        teacherRepository.createClass(grade, section, academicYear).fold(
          onSuccess = {
            loadClassesInternal()
            loadStatsInternal()
            _uiState.update {
              it.copy(
                isLoading = false,
                showAddClassDialog = false,
                successMessage = "Sinf muvaffaqiyatli yaratildi"
              )
            }
          },
          onFailure = { error ->
            _uiState.update {
              it.copy(
                isLoading = false,
                errorMessage = error.message ?: "Sinf yaratishda xatolik"
              )
            }
          }
        )
      } else {
        teacherRepository.createClassWithSchedule(grade, section, academicYear, subject, slots).fold(
          onSuccess = { (newClass, schedules) ->
            loadClassesInternal()
            loadStatsInternal()
            loadSchedulesInternal()
            loadTodayLessonsInternal()
            _uiState.update {
              it.copy(
                isLoading = false,
                showAddClassDialog = false,
                successMessage = "${newClass.name} sinfi va ${schedules.size} ta dars jadvali yaratildi"
              )
            }
          },
          onFailure = { error ->
            _uiState.update {
              it.copy(
                isLoading = false,
                errorMessage = error.message ?: "Sinf va jadval yaratishda xatolik"
              )
            }
          }
        )
      }
    }
  }

  fun deleteClass(classId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      teacherRepository.deleteClass(classId).fold(
        onSuccess = {
          if (_uiState.value.selectedClassForDetail?.id == classId) {
            _uiState.update { it.copy(selectedClassForDetail = null) }
          }
          if (_uiState.value.selectedClassFilterId == classId) {
            _uiState.update { it.copy(selectedClassFilterId = null) }
          }
          loadClassesInternal()
          loadStudentsInternal()
          loadStatsInternal()
          _uiState.update {
            it.copy(
              isLoading = false,
              successMessage = "Sinf va uning o'quvchilari o'chirildi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Sinfni o'chirishda xatolik"
            )
          }
        }
      )
    }
  }

  fun selectClassForDetail(classEntity: ClassEntity?) {
    _uiState.update { it.copy(selectedClassForDetail = classEntity) }
    if (classEntity != null) {
      viewModelScope.launch {
        loadStudentsInternal()
      }
    }
  }

  // ==========================================
  // STUDENT ACTIONS
  // ==========================================

  fun createStudent(
    classId: String,
    firstName: String,
    lastName: String,
    studentNumber: String,
    birthDate: String,
    gender: String,
    phone: String = ""
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      teacherRepository.createStudent(
        classId = classId,
        firstName = firstName,
        lastName = lastName,
        studentNumber = studentNumber,
        birthDate = birthDate,
        gender = gender,
        phone = phone
      ).fold(
        onSuccess = {
          loadStudentsInternal()
          loadClassesInternal()
          loadStatsInternal()
          _uiState.update {
            it.copy(
              isLoading = false,
              showAddStudentDialog = false,
              targetClassIdForNewStudent = null,
              successMessage = "O'quvchi muvaffaqiyatli qo'shildi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "O'quvchi qo'shishda xatolik"
            )
          }
        }
      )
    }
  }

  fun updateStudent(
    studentId: String,
    firstName: String,
    lastName: String,
    studentNumber: String,
    birthDate: String,
    gender: String,
    phone: String = "",
    classId: String? = null
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      teacherRepository.updateStudent(
        studentId = studentId,
        firstName = firstName,
        lastName = lastName,
        studentNumber = studentNumber,
        birthDate = birthDate,
        gender = gender,
        phone = phone,
        classId = classId
      ).fold(
        onSuccess = { updated ->
          loadStudentsInternal()
          loadClassesInternal()
          _uiState.update {
            it.copy(
              selectedStudentForDetail = updated,
              isLoading = false,
              successMessage = "O'quvchi ma'lumotlari yangilandi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "O'quvchi ma'lumotlarini yangilashda xatolik"
            )
          }
        }
      )
    }
  }

  fun deleteStudent(studentId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      teacherRepository.deleteStudent(studentId).fold(
        onSuccess = {
          _uiState.update { it.copy(selectedStudentForDetail = null) }
          loadStudentsInternal()
          loadClassesInternal()
          loadStatsInternal()
          _uiState.update {
            it.copy(
              isLoading = false,
              successMessage = "O'quvchi o'chirildi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "O'quvchini o'chirishda xatolik"
            )
          }
        }
      )
    }
  }

  fun selectStudentForDetail(student: StudentEntity?) {
    _uiState.update { it.copy(selectedStudentForDetail = student) }
  }

  fun onSearchQueryChanged(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    viewModelScope.launch {
      loadStudentsInternal()
    }
  }

  fun onClassFilterChanged(classId: String?) {
    _uiState.update { it.copy(selectedClassFilterId = classId) }
    viewModelScope.launch {
      loadStudentsInternal()
    }
  }

  // Dialog Visibility Setters
  fun setShowAddClassDialog(show: Boolean) {
    _uiState.update { it.copy(showAddClassDialog = show, errorMessage = null) }
  }

  fun setShowAddStudentDialog(show: Boolean, defaultClassId: String? = null) {
    _uiState.update {
      it.copy(
        showAddStudentDialog = show,
        targetClassIdForNewStudent = defaultClassId,
        errorMessage = null
      )
    }
  }

  fun setShowProfileDialog(show: Boolean) {
    _uiState.update { it.copy(showProfileDialog = show) }
  }

  fun setShowEditProfileDialog(show: Boolean) {
    _uiState.update { it.copy(showEditProfileDialog = show) }
  }

  fun clearMessages() {
    _uiState.update { it.copy(errorMessage = null, successMessage = null) }
  }

  // ==========================================
  // BOSQICH 3: SCHEDULE ACTIONS
  // ==========================================

  fun loadSchedules() {
    viewModelScope.launch {
      loadSchedulesInternal()
    }
  }

  fun createSchedule(
    classId: String,
    subject: String,
    dayOfWeek: Int,
    startTime: String,
    endTime: String,
    room: String,
    lessonNumber: Int
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      lessonRepository.createSchedule(
        classId = classId,
        subjectId = subject,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        room = room,
        lessonNumber = lessonNumber
      ).fold(
        onSuccess = {
          loadSchedulesInternal()
          _uiState.update {
            it.copy(
              isLoading = false,
              showCreateScheduleDialog = false,
              successMessage = "Dars jadvali muvaffaqiyatli qo'shildi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Jadval yaratishda xatolik"
            )
          }
        }
      )
    }
  }

  fun deleteSchedule(scheduleId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      lessonRepository.deleteSchedule(scheduleId).fold(
        onSuccess = {
          loadSchedulesInternal()
          _uiState.update {
            it.copy(
              isLoading = false,
              successMessage = "Jadval o'chirildi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Jadvalni o'chirishda xatolik"
            )
          }
        }
      )
    }
  }

  fun createMultiDaySchedule(
    classId: String,
    subject: String,
    slots: List<DayScheduleSlot>
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      lessonRepository.createMultiDaySchedule(classId, subject, slots).fold(
        onSuccess = { list ->
          loadSchedulesInternal()
          _uiState.update {
            it.copy(
              isLoading = false,
              showCreateScheduleDialog = false,
              successMessage = "${list.size} ta dars jadvali muvaffaqiyatli saqlandi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Jadval yaratishda xatolik"
            )
          }
        }
      )
    }
  }

  fun updateSchedule(
    scheduleId: String,
    classId: String,
    subject: String,
    dayOfWeek: Int,
    startTime: String,
    endTime: String,
    room: String,
    lessonNumber: Int
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      lessonRepository.updateSchedule(
        scheduleId = scheduleId,
        classId = classId,
        subjectId = subject,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        room = room,
        lessonNumber = lessonNumber
      ).fold(
        onSuccess = {
          loadSchedulesInternal()
          _uiState.update {
            it.copy(
              isLoading = false,
              successMessage = "Dars jadvali muvaffaqiyatli yangilandi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Jadvalni yangilashda xatolik"
            )
          }
        }
      )
    }
  }

  // ==========================================
  // BOSQICH 3: LESSON ACTIONS
  // ==========================================

  fun loadTodayLessons() {
    viewModelScope.launch {
      loadTodayLessonsInternal()
      loadPendingAttendanceCountInternal()
    }
  }

  fun createLesson(
    classId: String,
    subject: String,
    date: String,
    startTime: String,
    endTime: String,
    room: String,
    topic: String
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      lessonRepository.createLesson(
        classId = classId,
        subjectId = subject,
        date = date,
        startTime = startTime,
        endTime = endTime,
        room = room,
        topic = topic
      ).fold(
        onSuccess = {
          loadTodayLessonsInternal()
          loadPendingAttendanceCountInternal()
          _uiState.update {
            it.copy(
              isLoading = false,
              showCreateLessonDialog = false,
              successMessage = "Dars muvaffaqiyatli rejalashtirildi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Dars yaratishda xatolik"
            )
          }
        }
      )
    }
  }

  fun deleteLesson(lessonId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      lessonRepository.deleteLesson(lessonId).fold(
        onSuccess = {
          if (_uiState.value.selectedLessonForDetail?.lesson?.id == lessonId) {
            _uiState.update { it.copy(selectedLessonForDetail = null) }
          }
          if (_uiState.value.activeAttendanceLesson?.lesson?.id == lessonId) {
            _uiState.update { it.copy(activeAttendanceLesson = null, activeAttendanceItems = emptyList()) }
          }
          loadTodayLessonsInternal()
          loadPendingAttendanceCountInternal()
          _uiState.update {
            it.copy(
              isLoading = false,
              successMessage = "Dars o'chirildi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Darsni o'chirishda xatolik"
            )
          }
        }
      )
    }
  }

  fun selectLessonForDetail(lesson: LessonWithDetails?) {
    _uiState.update { it.copy(selectedLessonForDetail = lesson) }
  }

  fun loadClassLessons(classId: String) {
    viewModelScope.launch {
      lessonRepository.getLessonsByClass(classId).fold(
        onSuccess = { list ->
          _uiState.update { it.copy(classLessonsForDetail = list) }
        },
        onFailure = {}
      )
    }
  }

  // ==========================================
  // BOSQICH 3: DAVOMAT ACTIONS (TEZKOR DAVOMAT)
  // ==========================================

  fun openAttendance(lessonId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      lessonRepository.getLessonAttendance(lessonId).fold(
        onSuccess = { (details, items) ->
          val summary = calculateAttendanceSummary(items)
          _uiState.update {
            it.copy(
              isLoading = false,
              currentTab = MainTab.DAVOMAT,
              activeAttendanceLesson = details,
              activeAttendanceItems = items,
              attendanceSummary = summary,
              hasUnsavedAttendanceChanges = false,
              selectedLessonForDetail = null
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Davomat ma'lumotlarini yuklashda xatolik"
            )
          }
        }
      )
    }
  }

  fun closeAttendance() {
    _uiState.update {
      it.copy(
        activeAttendanceLesson = null,
        activeAttendanceItems = emptyList(),
        hasUnsavedAttendanceChanges = false
      )
    }
  }

  fun markStudentStatus(
    studentId: String,
    status: AttendanceStatus,
    lateMinutes: Int? = null,
    note: String? = null
  ) {
    val currentItems = _uiState.value.activeAttendanceItems
    val updatedItems = currentItems.map { item ->
      if (item.student.id == studentId) {
        val cleanLate = if (status == AttendanceStatus.LATE) (lateMinutes ?: item.lateMinutes ?: 5) else null
        val cleanNote = if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.EXCUSED) {
          (note ?: item.note)
        } else null
        item.copy(status = status, lateMinutes = cleanLate, note = cleanNote)
      } else {
        item
      }
    }
    val summary = calculateAttendanceSummary(updatedItems)
    _uiState.update {
      it.copy(
        activeAttendanceItems = updatedItems,
        attendanceSummary = summary,
        hasUnsavedAttendanceChanges = true
      )
    }
  }

  fun markAllPresent() {
    val currentItems = _uiState.value.activeAttendanceItems
    val updatedItems = currentItems.map { item ->
      item.copy(status = AttendanceStatus.PRESENT, lateMinutes = null, note = null)
    }
    val summary = calculateAttendanceSummary(updatedItems)
    _uiState.update {
      it.copy(
        activeAttendanceItems = updatedItems,
        attendanceSummary = summary,
        hasUnsavedAttendanceChanges = true
      )
    }
  }

  fun saveActiveAttendance() {
    val activeLesson = _uiState.value.activeAttendanceLesson ?: return
    val items = _uiState.value.activeAttendanceItems
    if (items.isEmpty()) return

    // Ensure all students have a marked status, or default remaining to PRESENT
    val records = items.map { item ->
      val status = item.status ?: AttendanceStatus.PRESENT
      BulkAttendanceRecord(
        studentId = item.student.id,
        status = status,
        lateMinutes = if (status == AttendanceStatus.LATE) (item.lateMinutes ?: 5) else null,
        note = if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.EXCUSED) item.note else null
      )
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSavingAttendance = true, errorMessage = null) }
      lessonRepository.saveBulkAttendance(activeLesson.lesson.id, records).fold(
        onSuccess = { updatedDetails ->
          loadTodayLessonsInternal()
          loadPendingAttendanceCountInternal()
          val updatedItems = items.map { item ->
            val rec = records.find { it.studentId == item.student.id }
            item.copy(
              status = rec?.status ?: AttendanceStatus.PRESENT,
              lateMinutes = rec?.lateMinutes,
              note = rec?.note
            )
          }
          val summary = calculateAttendanceSummary(updatedItems)
          _uiState.update {
            it.copy(
              isSavingAttendance = false,
              activeAttendanceLesson = updatedDetails,
              activeAttendanceItems = updatedItems,
              attendanceSummary = summary,
              hasUnsavedAttendanceChanges = false,
              successMessage = "Davomat muvaffaqiyatli saqlandi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isSavingAttendance = false,
              errorMessage = error.message ?: "Davomatni saqlashda xatolik yuz berdi"
            )
          }
        }
      )
    }
  }

  private fun calculateAttendanceSummary(items: List<StudentAttendanceItem>): AttendanceSummary {
    val total = items.size
    val present = items.count { it.status == AttendanceStatus.PRESENT }
    val late = items.count { it.status == AttendanceStatus.LATE }
    val absent = items.count { it.status == AttendanceStatus.ABSENT }
    val excused = items.count { it.status == AttendanceStatus.EXCUSED }
    return AttendanceSummary(
      totalStudents = total,
      presentCount = present,
      lateCount = late,
      absentCount = absent,
      excusedCount = excused
    )
  }

  // ==========================================
  // BOSQICH 5: AVTOMATIK DAVOMAT (FACE ID, QR, NFC)
  // ==========================================

  fun selectAttendanceMethod(method: AttendanceMethod) {
    _uiState.update { it.copy(selectedAttendanceMethod = method, errorMessage = null) }
    if (method == AttendanceMethod.FACE) {
      loadCameras()
    }
  }

  fun loadCameras() {
    viewModelScope.launch {
      automatedAttendanceRepository.getCameras().fold(
        onSuccess = { list ->
          _uiState.update { state ->
            val active = list.firstOrNull { it.status == "ACTIVE" } ?: list.firstOrNull()
            state.copy(cameras = list, selectedCamera = active)
          }
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message) }
        }
      )
    }
  }

  fun addCamera(name: String, location: String, deviceId: String, secretToken: String) {
    val activeClassId = _uiState.value.activeAttendanceLesson?.lesson?.classId
    viewModelScope.launch {
      automatedAttendanceRepository.registerCamera(
        name = name,
        location = location,
        deviceId = deviceId,
        secretToken = secretToken,
        classId = activeClassId
      ).fold(
        onSuccess = {
          loadCameras()
          _uiState.update { it.copy(successMessage = "Kamera muvaffaqiyatli qo'shildi") }
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message ?: "Kamera qo'shishda xatolik") }
        }
      )
    }
  }

  fun toggleCamera(cameraId: String, active: Boolean) {
    viewModelScope.launch {
      automatedAttendanceRepository.updateCameraStatus(cameraId, if (active) "ACTIVE" else "INACTIVE").fold(
        onSuccess = { loadCameras() },
        onFailure = { error -> _uiState.update { it.copy(errorMessage = error.message) } }
      )
    }
  }

  fun deleteCamera(cameraId: String) {
    viewModelScope.launch {
      automatedAttendanceRepository.deleteCamera(cameraId).fold(
        onSuccess = {
          loadCameras()
          _uiState.update { it.copy(successMessage = "Kamera o'chirildi") }
        },
        onFailure = { error -> _uiState.update { it.copy(errorMessage = error.message) } }
      )
    }
  }

  fun testCameraConnection(cameraId: String) {
    viewModelScope.launch {
      automatedAttendanceRepository.updateHeartbeat(cameraId).fold(
        onSuccess = {
          loadCameras()
          _uiState.update { it.copy(successMessage = "Kamera aloqasi tekshirildi (Faol)") }
        },
        onFailure = { error -> _uiState.update { it.copy(errorMessage = error.message) } }
      )
    }
  }

  fun scanQrToken(token: String) {
    val lessonId = _uiState.value.activeAttendanceLesson?.lesson?.id ?: return
    viewModelScope.launch {
      automatedAttendanceRepository.processQrCode(lessonId, token).fold(
        onSuccess = { result ->
          _uiState.update { it.copy(lastScannedResult = result) }
          applyScanResultToActiveList(result, "QR")
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message ?: "QR kodni skanerlashda xatolik") }
        }
      )
    }
  }

  fun tapNfcCard(cardUid: String) {
    val lessonId = _uiState.value.activeAttendanceLesson?.lesson?.id ?: return
    viewModelScope.launch {
      automatedAttendanceRepository.processCardTap(lessonId, cardUid).fold(
        onSuccess = { result ->
          _uiState.update { it.copy(lastScannedResult = result) }
          applyScanResultToActiveList(result, "NFC")
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message ?: "NFC kartani tekshirishda xatolik") }
        }
      )
    }
  }

  private fun applyScanResultToActiveList(result: AttendanceScanResult, source: String) {
    val currentItems = _uiState.value.activeAttendanceItems
    val updated = currentItems.map { item ->
      if (item.student.id == result.studentId) {
        item.copy(
          status = result.status,
          lateMinutes = result.lateMinutes,
          source = source,
          detectionState = result.detectionState,
          confidence = result.confidence,
          manualOverride = false,
          originalSource = source
        )
      } else {
        item
      }
    }
    val summary = calculateAttendanceSummary(updated)
    _uiState.update {
      it.copy(
        activeAttendanceItems = updated,
        attendanceSummary = summary,
        hasUnsavedAttendanceChanges = true,
        successMessage = "${result.studentName}: ${result.message}"
      )
    }
  }

  fun simulateFaceDetection() {
    val lessonId = _uiState.value.activeAttendanceLesson?.lesson?.id ?: return
    val currentItems = _uiState.value.activeAttendanceItems
    if (currentItems.isEmpty()) return

    viewModelScope.launch {
      val candidates = currentItems.mapIndexed { index, item ->
        val confidence = when {
          index % 5 == 0 -> 0.65f
          else -> 0.94f
        }
        FaceDetectionCandidate(
          studentId = item.student.id,
          confidence = confidence,
          timestamp = System.currentTimeMillis()
        )
      }.shuffled().take((currentItems.size * 0.85).toInt().coerceAtLeast(1))

      automatedAttendanceRepository.processFaceDetections(lessonId, candidates).fold(
        onSuccess = { results ->
          _uiState.update {
            it.copy(
              detectedCandidates = results,
              successMessage = "Face AI: ${results.size} nafar yuz aniqlandi. Natijalarni kiritish tugmasini bosing."
            )
          }
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message ?: "Face AI skanerlashda xatolik") }
        }
      )
    }
  }

  fun applyDetectedCandidates() {
    val candidates = _uiState.value.detectedCandidates
    if (candidates.isEmpty()) return

    val candidateMap = candidates.associateBy { it.studentId }
    val currentItems = _uiState.value.activeAttendanceItems

    val updated = currentItems.map { item ->
      val detected = candidateMap[item.student.id]
      if (detected != null) {
        item.copy(
          status = detected.status,
          lateMinutes = detected.lateMinutes,
          source = "FACE",
          detectionState = detected.detectionState,
          confidence = detected.confidence,
          manualOverride = false,
          originalSource = "FACE"
        )
      } else {
        item
      }
    }

    val summary = calculateAttendanceSummary(updated)
    _uiState.update {
      it.copy(
        activeAttendanceItems = updated,
        attendanceSummary = summary,
        hasUnsavedAttendanceChanges = true,
        detectedCandidates = emptyList(),
        successMessage = "Face AI natijalari kiritildi. O'qituvchi sifatida tasdiqlang."
      )
    }
  }

  fun openAttendanceConfirmDialog() {
    _uiState.update { it.copy(showAttendanceConfirmDialog = true) }
  }

  fun closeAttendanceConfirmDialog() {
    _uiState.update { it.copy(showAttendanceConfirmDialog = false) }
  }

  fun confirmAndSaveAttendance(markRemainingAsAbsent: Boolean) {
    val currentItems = _uiState.value.activeAttendanceItems
    val updated = if (markRemainingAsAbsent) {
      currentItems.map { item ->
        if (item.status == null || item.detectionState == AttendanceDetectionState.NOT_DETECTED) {
          item.copy(status = AttendanceStatus.ABSENT, source = "MANUAL")
        } else {
          item
        }
      }
    } else {
      currentItems
    }

    _uiState.update {
      it.copy(
        activeAttendanceItems = updated,
        attendanceSummary = calculateAttendanceSummary(updated),
        showAttendanceConfirmDialog = false
      )
    }
    saveActiveAttendance()
  }

  // Student Automation Actions (QR, Card, Face Enrollment)
  fun openStudentAutomationQr(student: StudentEntity) {
    _uiState.update {
      it.copy(
        studentForAutomationModal = student,
        showStudentQrDialog = true
      )
    }
  }

  fun openStudentAutomationCard(student: StudentEntity) {
    viewModelScope.launch {
      automatedAttendanceRepository.getCardByStudent(student.id).fold(
        onSuccess = { card ->
          _uiState.update {
            it.copy(
              studentForAutomationModal = student,
              selectedStudentCard = card,
              showStudentCardDialog = true
            )
          }
        },
        onFailure = {
          _uiState.update {
            it.copy(
              studentForAutomationModal = student,
              selectedStudentCard = null,
              showStudentCardDialog = true
            )
          }
        }
      )
    }
  }

  fun openStudentAutomationFace(student: StudentEntity) {
    viewModelScope.launch {
      automatedAttendanceRepository.getFaceEnrollment(student.id).fold(
        onSuccess = { face ->
          _uiState.update {
            it.copy(
              studentForAutomationModal = student,
              selectedStudentFaceEnrollment = face,
              showStudentFaceDialog = true
            )
          }
        },
        onFailure = {
          _uiState.update {
            it.copy(
              studentForAutomationModal = student,
              selectedStudentFaceEnrollment = null,
              showStudentFaceDialog = true
            )
          }
        }
      )
    }
  }

  fun pairStudentCard(cardUid: String) {
    val student = _uiState.value.studentForAutomationModal ?: return
    viewModelScope.launch {
      automatedAttendanceRepository.registerCard(student.id, cardUid).fold(
        onSuccess = { card ->
          _uiState.update {
            it.copy(
              selectedStudentCard = card,
              successMessage = "Karta muvaffaqiyatli biriktirildi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message ?: "Karta biriktirishda xatolik") }
        }
      )
    }
  }

  fun deactivateStudentCard(cardId: String) {
    viewModelScope.launch {
      automatedAttendanceRepository.deactivateCard(cardId).fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              selectedStudentCard = null,
              successMessage = "Karta bekor qilindi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message ?: "Kartani o'chirishda xatolik") }
        }
      )
    }
  }

  fun enrollStudentFace() {
    val student = _uiState.value.studentForAutomationModal ?: return
    viewModelScope.launch {
      val mockEmbedding = java.util.UUID.randomUUID().toString().replace("-", "") +
        java.util.UUID.randomUUID().toString().replace("-", "")
      automatedAttendanceRepository.enrollFace(student.id, mockEmbedding).fold(
        onSuccess = { face ->
          _uiState.update {
            it.copy(
              selectedStudentFaceEnrollment = face,
              successMessage = "Yuz biometrik shabloni saqlandi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message ?: "Face ID saqlashda xatolik") }
        }
      )
    }
  }

  fun deleteStudentFace() {
    val student = _uiState.value.studentForAutomationModal ?: return
    viewModelScope.launch {
      automatedAttendanceRepository.deleteFaceEnrollment(student.id).fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              selectedStudentFaceEnrollment = null,
              successMessage = "Face ID biometrik shabloni o'chirildi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message ?: "Face ID o'chirishda xatolik") }
        }
      )
    }
  }

  fun openCameraManagementDialog() {
    loadCameras()
    _uiState.update { it.copy(showCameraManagementDialog = true) }
  }

  fun closeCameraManagementDialog() {
    _uiState.update { it.copy(showCameraManagementDialog = false) }
  }

  fun openAttendanceAuditsDialog() {
    viewModelScope.launch {
      automatedAttendanceRepository.getAuditLogs().fold(
        onSuccess = { list ->
          _uiState.update { it.copy(attendanceAudits = list, showAttendanceAuditDialog = true) }
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message) }
        }
      )
    }
  }

  fun closeAttendanceAuditsDialog() {
    _uiState.update { it.copy(showAttendanceAuditDialog = false) }
  }

  fun closeStudentAutomationModal() {
    _uiState.update {
      it.copy(
        showStudentQrDialog = false,
        showStudentCardDialog = false,
        showStudentFaceDialog = false,
        studentForAutomationModal = null
      )
    }
  }

  // Dialog setters
  fun setShowCreateLessonDialog(show: Boolean) {
    _uiState.update { it.copy(showCreateLessonDialog = show) }
  }

  fun setShowCreateScheduleDialog(show: Boolean) {
    _uiState.update { it.copy(showCreateScheduleDialog = show) }
  }

  fun setShowScheduleListDialog(show: Boolean) {
    _uiState.update { it.copy(showScheduleListDialog = show) }
  }

  fun setShowGenerateLessonsDialog(show: Boolean) {
    _uiState.update { it.copy(showGenerateLessonsDialog = show) }
  }

  // ==========================================
  // BOSQICH 4: KTP MANAGEMENT
  // ==========================================

  fun loadKTPDocuments() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingKTP = true) }
      ktpRepository.getDocuments().fold(
        onSuccess = { docs ->
          _uiState.update { it.copy(ktpDocuments = docs, isLoadingKTP = false) }
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message, isLoadingKTP = false) }
        }
      )
    }
  }

  fun selectKTPDocument(doc: KTPDocumentEntity) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingKTP = true, selectedKTPDocument = doc) }
      ktpRepository.getDocumentWithEntries(doc.id).fold(
        onSuccess = { docWithEntries ->
          _uiState.update {
            it.copy(
              selectedKTPDocument = docWithEntries.document,
              ktpEntries = docWithEntries.entries,
              isLoadingKTP = false
            )
          }
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message, isLoadingKTP = false) }
        }
      )
    }
  }

  fun closeKTPDocumentDetail() {
    _uiState.update { it.copy(selectedKTPDocument = null, ktpEntries = emptyList()) }
  }

  fun setShowUploadKTPDialog(show: Boolean) {
    _uiState.update { it.copy(showUploadKTPDialog = show) }
  }

  fun uploadAndAnalyzeKTP(
    fileName: String,
    subjectId: String,
    academicYear: String,
    content: String,
    explicitClassId: String? = null
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingKTP = true, errorMessage = null) }
      ktpRepository.uploadAndAnalyzeKTP(
        fileName = fileName,
        subjectId = subjectId,
        academicYear = academicYear,
        content = content,
        explicitClassId = explicitClassId
      ).fold(
        onSuccess = { docWithEntries ->
          loadKTPDocuments()
          _uiState.update {
            it.copy(
              isLoadingKTP = false,
              showUploadKTPDialog = false,
              selectedKTPDocument = docWithEntries.document,
              ktpEntries = docWithEntries.entries,
              successMessage = "KTP muvaffaqiyatli tahlil qilindi. Iltimos tekshiring."
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoadingKTP = false,
              errorMessage = error.message ?: "KTP tahlilida xatolik yuz berdi"
            )
          }
        }
      )
    }
  }

  fun setKTPEntryForEdit(entry: KTPEntryEntity?) {
    _uiState.update { it.copy(selectedKTPEntryForEdit = entry) }
  }

  fun updateKTPEntry(
    entryId: String,
    date: String,
    lessonNumber: Int,
    topic: String,
    room: String,
    classId: String? = null
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingKTP = true, errorMessage = null) }
      ktpRepository.updateKTPEntry(
        entryId = entryId,
        date = date,
        lessonNumber = lessonNumber,
        topic = topic,
        room = room,
        classId = classId
      ).fold(
        onSuccess = { updated ->
          val currentList = _uiState.value.ktpEntries.map { if (it.id == updated.id) updated else it }
          _uiState.update {
            it.copy(
              ktpEntries = currentList,
              selectedKTPEntryForEdit = null,
              isLoadingKTP = false,
              successMessage = "KTP bandi yangilandi (qo'lda tahrirlangan)"
            )
          }
          // Refresh parent document
          _uiState.value.selectedKTPDocument?.let { selectKTPDocument(it) }
        },
        onFailure = { error ->
          _uiState.update { it.copy(errorMessage = error.message, isLoadingKTP = false) }
        }
      )
    }
  }

  fun confirmKTP(documentId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isConfirmingKTP = true, errorMessage = null) }
      ktpRepository.confirmAndGenerateLessons(documentId).fold(
        onSuccess = { count ->
          loadKTPDocuments()
          loadTodayLessonsInternal()
          _uiState.update {
            it.copy(
              isConfirmingKTP = false,
              successMessage = "KTP tasdiqlandi! $count ta yangi real dars jadvalga kiritildi."
            )
          }
          // Refresh selected doc
          _uiState.value.selectedKTPDocument?.let { selectKTPDocument(it) }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isConfirmingKTP = false,
              errorMessage = error.message ?: "KTP tasdiqlashda xatolik yuz berdi"
            )
          }
        }
      )
    }
  }

  fun deleteKTP(documentId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingKTP = true, errorMessage = null) }
      ktpRepository.deleteDocument(documentId).fold(
        onSuccess = {
          loadKTPDocuments()
          _uiState.update {
            it.copy(
              isLoadingKTP = false,
              selectedKTPDocument = null,
              ktpEntries = emptyList(),
              successMessage = "KTP muvaffaqiyatli o'chirildi"
            )
          }
        },
        onFailure = { error ->
          _uiState.update { it.copy(isLoadingKTP = false, errorMessage = error.message) }
        }
      )
    }
  }

  fun setKTPFilters(subject: String?, classId: String?) {
    _uiState.update { it.copy(ktpFilterSubject = subject, ktpFilterClassId = classId) }
  }

  // ==========================================
  // BOSQICH 4: STUDENT FILE IMPORT
  // ==========================================

  fun openStudentImportDialog(targetClassId: String? = null) {
    _uiState.update {
      it.copy(
        showImportStudentsDialog = true,
        importTargetClassId = targetClassId,
        importStudentsPreview = null,
        errorMessage = null
      )
    }
  }

  fun closeStudentImportDialog() {
    _uiState.update {
      it.copy(
        showImportStudentsDialog = false,
        importStudentsPreview = null,
        importTargetClassId = null
      )
    }
  }

  fun parseStudentImportFile(classId: String, fileName: String, content: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }

      val classStudents = teacherRepository.getStudents(classId = classId).getOrDefault(emptyList())
      val allStudents = teacherRepository.getStudents().getOrDefault(emptyList())

      StudentFileParser.parseStudents(
        content = content,
        fileName = fileName,
        existingStudentsInClass = classStudents,
        allTeacherStudents = allStudents
      ).fold(
        onSuccess = { preview ->
          _uiState.update {
            it.copy(
              isLoading = false,
              importStudentsPreview = preview,
              importTargetClassId = classId
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Faylni o'qishda xatolik yuz berdi"
            )
          }
        }
      )
    }
  }

  fun toggleStudentImportSelection(tempId: String) {
    val current = _uiState.value.importStudentsPreview ?: return
    val updatedDrafts = current.drafts.map { draft ->
      if (draft.tempId == tempId) draft.copy(isSelectedForImport = !draft.isSelectedForImport) else draft
    }
    val updatedPreview = current.copy(
      drafts = updatedDrafts,
      newCount = updatedDrafts.count { it.status == com.example.data.model.StudentImportStatus.NEW }
    )
    _uiState.update { it.copy(importStudentsPreview = updatedPreview) }
  }

  fun updateDraftStudent(tempId: String, firstName: String, lastName: String, number: String) {
    val current = _uiState.value.importStudentsPreview ?: return
    val updatedDrafts = current.drafts.map { draft ->
      if (draft.tempId == tempId) {
        draft.copy(
          firstName = firstName.trim(),
          lastName = lastName.trim(),
          studentNumber = number.trim()
        )
      } else draft
    }
    _uiState.update { it.copy(importStudentsPreview = current.copy(drafts = updatedDrafts)) }
  }

  fun confirmStudentImport(classId: String) {
    val preview = _uiState.value.importStudentsPreview ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isImportingStudents = true, errorMessage = null) }

      val totalChecked = preview.drafts.size
      val duplicateCount = preview.drafts.count {
        it.status == com.example.data.model.StudentImportStatus.EXISTING ||
            it.status == com.example.data.model.StudentImportStatus.SUSPECTED_DUPLICATE
      }
      val errorCount = preview.drafts.count { it.status == com.example.data.model.StudentImportStatus.INVALID }
      val targetClassName = _uiState.value.classes.find { it.classEntity.id == classId }?.classEntity?.name ?: "Sinf"

      teacherRepository.importStudents(classId, preview.drafts).fold(
        onSuccess = { count ->
          loadStudentsInternal()
          loadClassesInternal()
          loadStatsInternal()
          val resultSummary = StudentImportResultSummary(
            totalChecked = totalChecked,
            addedCount = count,
            duplicateCount = duplicateCount,
            errorCount = errorCount,
            className = targetClassName
          )
          _uiState.update {
            it.copy(
              isImportingStudents = false,
              showImportStudentsDialog = false,
              importStudentsPreview = null,
              lastImportResult = resultSummary,
              successMessage = "$count ta o'quvchi muvaffaqiyatli import qilindi!"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isImportingStudents = false,
              errorMessage = error.message ?: "O'quvchilarni import qilishda xatolik yuz berdi"
            )
          }
        }
      )
    }
  }

  fun clearLastImportResult() {
    _uiState.update { it.copy(lastImportResult = null) }
  }

  // ==========================================
  // BOSQICH 4: CLASS EXPORT ACTIONS
  // ==========================================

  fun openExportClassDialog(classEntity: ClassEntity) {
    _uiState.update { it.copy(showExportClassDialog = true, classToExport = classEntity) }
  }

  fun closeExportClassDialog() {
    _uiState.update { it.copy(showExportClassDialog = false, classToExport = null) }
  }

  fun exportClassStudents(
    context: Context,
    classEntity: ClassEntity,
    format: ExportFormat,
    onShareIntentReady: (Intent) -> Unit
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isExportingClass = true, errorMessage = null) }
      val teacherId = teacherRepository.getCurrentTeacherId().getOrNull()
        ?: _uiState.value.profile?.userId
        ?: ""
      teacherRepository.getStudents(classId = classEntity.id).fold(
        onSuccess = { students ->
          ClassExporter.exportClassStudents(
            context = context,
            classEntity = classEntity,
            students = students,
            format = format,
            currentTeacherId = teacherId
          ).fold(
            onSuccess = { file ->
              val shareIntent = ClassExporter.createShareIntent(context, file, format.mimeType)
              _uiState.update {
                it.copy(
                  isExportingClass = false,
                  showExportClassDialog = false,
                  classToExport = null,
                  successMessage = "${classEntity.name} o'quvchilari muvaffaqiyatli eksport qilindi (${file.name})"
                )
              }
              onShareIntentReady(shareIntent)
            },
            onFailure = { error ->
              _uiState.update {
                it.copy(
                  isExportingClass = false,
                  errorMessage = "Eksport qilishda xatolik: ${error.message}"
                )
              }
            }
          )
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isExportingClass = false,
              errorMessage = error.message ?: "O'quvchilar ro'yxatini yuklab bo'lmadi"
            )
          }
        }
      )
    }
  }

  // ==========================================
  // BOSQICH 4: SCHEDULE-BASED LESSON GENERATION
  // ==========================================

  fun generateLessonsFromSchedule(startDate: LocalDate, endDate: LocalDate, classId: String? = null) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      lessonRepository.generateLessonsFromSchedule(startDate, endDate, classId).fold(
        onSuccess = { count ->
          loadTodayLessonsInternal()
          _uiState.update {
            it.copy(
              isLoading = false,
              showGenerateLessonsDialog = false,
              successMessage = "$count ta dars jadval asosida muvaffaqiyatli rejalashtirildi!"
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Darslarni rejalashtirishda xatolik yuz berdi"
            )
          }
        }
      )
    }
  }
}
