package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.PinScreen
import com.example.ui.auth.RegisterScreen
import com.example.ui.auth.WelcomeScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: AuthViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        UstozApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun UstozApp(viewModel: AuthViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    when (val screen = uiState.currentScreen) {
      is AuthScreen.Splash -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
            strokeWidth = 3.dp
          )
        }
      }

      is AuthScreen.Welcome -> {
        WelcomeScreen(
          onNavigateToLogin = { viewModel.navigateTo(AuthScreen.Login) },
          onNavigateToRegister = { viewModel.navigateTo(AuthScreen.Register) }
        )
      }

      is AuthScreen.Login -> {
        LoginScreen(
          isLoading = uiState.isLoading,
          errorMessage = uiState.errorMessage,
          onLogin = { email, password -> viewModel.login(email, password) },
          onNavigateToRegister = { viewModel.navigateTo(AuthScreen.Register) },
          onNavigateBack = { viewModel.navigateTo(AuthScreen.Welcome) },
          onClearError = { viewModel.clearErrorMessage() }
        )
      }

      is AuthScreen.Register -> {
        RegisterScreen(
          isLoading = uiState.isLoading,
          errorMessage = uiState.errorMessage,
          onRegister = { first, last, email, pass, lang, subj, school ->
            viewModel.register(first, last, email, pass, lang, subj, school)
          },
          onNavigateToLogin = { viewModel.navigateTo(AuthScreen.Login) },
          onNavigateBack = { viewModel.navigateTo(AuthScreen.Welcome) },
          onClearError = { viewModel.clearErrorMessage() }
        )
      }

      is AuthScreen.PinSetup -> {
        PinScreen(
          isSetupMode = true,
          isLoading = uiState.isLoading,
          errorMessage = uiState.errorMessage,
          onPinCompleted = { pin -> viewModel.setupPin(pin) },
          onClearError = { viewModel.clearErrorMessage() }
        )
      }

      is AuthScreen.PinUnlock -> {
        PinScreen(
          isSetupMode = false,
          teacherName = screen.teacherName,
          isLoading = uiState.isLoading,
          errorMessage = uiState.errorMessage,
          onPinCompleted = { pin -> viewModel.unlockWithPin(pin) },
          onSwitchAccount = { viewModel.logout() },
          onClearError = { viewModel.clearErrorMessage() }
        )
      }

      is AuthScreen.Dashboard -> {
        DashboardScreen(
          profile = screen.profile,
          onLockApp = { viewModel.lockApp() },
          onLogout = { viewModel.logout() }
        )
      }
    }
  }
}
