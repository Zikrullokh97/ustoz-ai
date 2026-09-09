package com.example.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UstozDatabase
import com.example.data.model.TeacherProfileEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthScreen {
  object Splash : AuthScreen
  object Welcome : AuthScreen
  object Login : AuthScreen
  object Register : AuthScreen
  data class PinSetup(val userId: String) : AuthScreen
  data class PinUnlock(val userId: String, val teacherName: String) : AuthScreen
  data class Dashboard(val profile: TeacherProfileEntity) : AuthScreen
}

data class AuthUiState(
  val currentScreen: AuthScreen = AuthScreen.Splash,
  val activeProfile: TeacherProfileEntity? = null,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val infoMessage: String? = null
)

class AuthViewModel @JvmOverloads constructor(
  application: Application,
  private val repository: AuthRepository = AuthRepository(
    userDao = UstozDatabase.getDatabase(application).userDao(),
    teacherProfileDao = UstozDatabase.getDatabase(application).teacherProfileDao(),
    sessionDao = UstozDatabase.getDatabase(application).sessionDao()
  )
) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  init {
    checkSession()
  }

  fun checkSession() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      when (val status = repository.checkSession()) {
        is SessionStatus.NoSession -> {
          _uiState.update {
            it.copy(
              currentScreen = AuthScreen.Welcome,
              activeProfile = null,
              isLoading = false
            )
          }
        }
        is SessionStatus.NeedsPinSetup -> {
          _uiState.update {
            it.copy(
              currentScreen = AuthScreen.PinSetup(status.userId),
              activeProfile = null,
              isLoading = false
            )
          }
        }
        is SessionStatus.Locked -> {
          _uiState.update {
            it.copy(
              currentScreen = AuthScreen.PinUnlock(status.userId, status.teacherName),
              activeProfile = null,
              isLoading = false
            )
          }
        }
        is SessionStatus.Authenticated -> {
          _uiState.update {
            it.copy(
              currentScreen = AuthScreen.Dashboard(status.profile),
              activeProfile = status.profile,
              isLoading = false
            )
          }
        }
      }
    }
  }

  fun navigateTo(screen: AuthScreen) {
    _uiState.update { it.copy(currentScreen = screen, errorMessage = null) }
  }

  fun login(emailOrPhone: String, password: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val result = repository.login(emailOrPhone, password)
      result.fold(
        onSuccess = { status ->
          when (status) {
            is SessionStatus.NeedsPinSetup -> {
              _uiState.update {
                it.copy(
                  currentScreen = AuthScreen.PinSetup(status.userId),
                  isLoading = false,
                  errorMessage = null
                )
              }
            }
            is SessionStatus.Locked -> {
              _uiState.update {
                it.copy(
                  currentScreen = AuthScreen.PinUnlock(status.userId, status.teacherName),
                  isLoading = false,
                  errorMessage = null
                )
              }
            }
            is SessionStatus.Authenticated -> {
              _uiState.update {
                it.copy(
                  currentScreen = AuthScreen.Dashboard(status.profile),
                  activeProfile = status.profile,
                  isLoading = false,
                  errorMessage = null
                )
              }
            }
            is SessionStatus.NoSession -> {
              _uiState.update { it.copy(isLoading = false, errorMessage = "Sessiya xatosi") }
            }
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Kirishda xatolik yuz berdi"
            )
          }
        }
      )
    }
  }

  fun register(
    firstName: String,
    lastName: String,
    emailOrPhone: String,
    password: String,
    language: String,
    subject: String,
    school: String
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val result = repository.register(
        firstName = firstName,
        lastName = lastName,
        emailOrPhone = emailOrPhone,
        password = password,
        language = language,
        subject = subject,
        school = school
      )
      result.fold(
        onSuccess = { profile ->
          // Successfully registered -> prompt PIN setup
          _uiState.update {
            it.copy(
              currentScreen = AuthScreen.PinSetup(profile.userId),
              isLoading = false,
              errorMessage = null
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Ro'yxatdan o'tishda xatolik yuz berdi"
            )
          }
        }
      )
    }
  }

  fun setupPin(pin: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val result = repository.setupPin(pin)
      result.fold(
        onSuccess = {
          val profile = repository.getCurrentTeacherProfile()
          if (profile != null) {
            _uiState.update {
              it.copy(
                currentScreen = AuthScreen.Dashboard(profile),
                activeProfile = profile,
                isLoading = false,
                errorMessage = null
              )
            }
          } else {
            _uiState.update {
              it.copy(
                currentScreen = AuthScreen.Welcome,
                isLoading = false,
                errorMessage = "Profil ma'lumotlari topilmadi"
              )
            }
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "PIN kodni saqlashda xatolik"
            )
          }
        }
      )
    }
  }

  fun unlockWithPin(pin: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val result = repository.unlockWithPin(pin)
      result.fold(
        onSuccess = { profile ->
          _uiState.update {
            it.copy(
              currentScreen = AuthScreen.Dashboard(profile),
              activeProfile = profile,
              isLoading = false,
              errorMessage = null
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "PIN kod noto'g'ri"
            )
          }
        }
      )
    }
  }

  fun lockApp() {
    viewModelScope.launch {
      repository.lockApp()
      val profile = _uiState.value.activeProfile
      val teacherName = profile?.let { "${it.firstName} ${it.lastName}".trim() } ?: "O'qituvchi"
      val userId = profile?.userId ?: ""
      _uiState.update {
        it.copy(
          currentScreen = AuthScreen.PinUnlock(userId, teacherName),
          activeProfile = null,
          errorMessage = null
        )
      }
    }
  }

  fun logout() {
    viewModelScope.launch {
      repository.logout()
      _uiState.update {
        AuthUiState(
          currentScreen = AuthScreen.Welcome,
          activeProfile = null,
          errorMessage = null
        )
      }
    }
  }

  fun clearErrorMessage() {
    _uiState.update { it.copy(errorMessage = null) }
  }
}
