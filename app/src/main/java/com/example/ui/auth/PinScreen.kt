package com.example.ui.auth

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.UstozDotUnfilled
import com.example.ui.theme.UstozGradientEnd
import com.example.ui.theme.UstozGradientStart
import com.example.ui.theme.UstozOnPrimaryContainer
import com.example.ui.theme.UstozOutline
import com.example.ui.theme.UstozPrimary
import com.example.ui.theme.UstozPrimaryContainer

@Composable
fun PinScreen(
  isSetupMode: Boolean,
  teacherName: String = "",
  isLoading: Boolean = false,
  errorMessage: String? = null,
  onPinCompleted: (pin: String) -> Unit,
  onSwitchAccount: () -> Unit = {},
  onClearError: () -> Unit = {}
) {
  var enteredPin by remember { mutableStateOf("") }
  var firstStepPin by remember { mutableStateOf<String?>(null) }
  var localError by remember { mutableStateOf<String?>(null) }

  val activeError = errorMessage ?: localError

  LaunchedEffect(enteredPin) {
    if (enteredPin.length == 4) {
      if (isSetupMode) {
        if (firstStepPin == null) {
          firstStepPin = enteredPin
          enteredPin = ""
          localError = null
        } else {
          if (enteredPin == firstStepPin) {
            onPinCompleted(enteredPin)
          } else {
            localError = "PIN kodlar mos kelmadi. Qaytadan kiriting."
            firstStepPin = null
            enteredPin = ""
          }
        }
      } else {
        onPinCompleted(enteredPin)
        enteredPin = ""
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Top Content Area
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Bar: Back button & Ustoz AI Brand Badge
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (!isSetupMode) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(UstozPrimaryContainer)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 24.dp),
                onClick = onSwitchAccount
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Chiqish",
              tint = UstozOnPrimaryContainer,
              modifier = Modifier.size(24.dp)
            )
          }
        } else {
          Spacer(modifier = Modifier.size(48.dp))
        }

        // Ustoz AI Brand Emblem (from Immersive UI spec)
        Column(horizontalAlignment = Alignment.End) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .shadow(elevation = 2.dp, shape = CircleShape)
              .background(UstozPrimary, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "UA",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
          Text(
            text = "USTOZ AI",
            color = UstozPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 3.dp)
          )
        }
      }

      // Center Hero: Tilted gradient container with security icon and floating badge
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .padding(top = 10.dp, bottom = 20.dp),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .rotate(3f)
              .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
              .background(
                brush = Brush.linearGradient(
                  colors = listOf(UstozGradientStart, UstozGradientEnd)
                ),
                shape = RoundedCornerShape(24.dp)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isSetupMode) Icons.Default.Pin else Icons.Default.Lock,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier
                .size(40.dp)
                .rotate(-3f)
            )
          }

          // Floating fingerprint badge
          Box(
            modifier = Modifier
              .size(32.dp)
              .align(Alignment.BottomEnd)
              .shadow(elevation = 4.dp, shape = CircleShape)
              .background(Color.White, CircleShape)
              .border(1.dp, UstozOutline.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Fingerprint,
              contentDescription = null,
              tint = UstozPrimary,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        val title = when {
          isSetupMode && firstStepPin == null -> "PIN kod yarating"
          isSetupMode && firstStepPin != null -> "PIN kodni tasdiqlang"
          teacherName.isNotBlank() -> "Xush kelibsiz"
          else -> "Xush kelibsiz"
        }

        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
          ),
          color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        val subtitle = when {
          isSetupMode && firstStepPin == null -> "Ilovaga tezkor va xavfsiz kirish uchun 4 xonali PIN kod o'rnating"
          isSetupMode && firstStepPin != null -> "Xatolik bo'lmasligi uchun PIN kodni yana bir bor kiriting"
          teacherName.isNotBlank() -> "$teacherName, ilovaga kirish uchun PIN kodni kiriting"
          else -> "Ilovaga kirish uchun PIN kodni kiriting"
        }

        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.widthIn(max = 280.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 4 Dots Indicator (Immersive UI specification: filled purple, unfilled soft tinted gray)
        Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 6.dp)
        ) {
          for (i in 0 until 4) {
            val isFilled = i < enteredPin.length
            Box(
              modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(
                  if (isFilled) UstozPrimary else UstozDotUnfilled
                )
                .testTag("pin_dot_$i")
            )
          }
        }

        if (isLoading) {
          Spacer(modifier = Modifier.height(10.dp))
          CircularProgressIndicator(
            color = UstozPrimary,
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp
          )
        } else if (!activeError.isNullOrBlank()) {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 12.dp)
              .testTag("pin_error_banner")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.size(8.dp))
              Text(
                text = activeError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }

      // Immersive Keypad
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 320.dp)
          .align(Alignment.CenterHorizontally),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        val rows = listOf(
          listOf("1", "2", "3"),
          listOf("4", "5", "6"),
          listOf("7", "8", "9"),
          listOf("fp", "0", "back")
        )

        for (row in rows) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            for (key in row) {
              when (key) {
                "fp" -> {
                  // Decorative / Biometric affordance button as in Immersive UI
                  Box(
                    modifier = Modifier
                      .size(72.dp)
                      .clip(CircleShape)
                      .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = UstozPrimaryContainer, radius = 36.dp),
                        onClick = { /* Biometric placeholder */ }
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Fingerprint,
                      contentDescription = "Biometriya",
                      tint = UstozPrimary,
                      modifier = Modifier.size(30.dp)
                    )
                  }
                }
                "back" -> {
                  Box(
                    modifier = Modifier
                      .size(72.dp)
                      .clip(CircleShape)
                      .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = UstozPrimaryContainer, radius = 36.dp)
                      ) {
                        if (enteredPin.isNotEmpty()) {
                          enteredPin = enteredPin.dropLast(1)
                          localError = null
                          if (errorMessage != null) onClearError()
                        }
                      }
                      .testTag("pin_backspace_button"),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Backspace,
                      contentDescription = "O'chirish",
                      tint = UstozPrimary,
                      modifier = Modifier.size(28.dp)
                    )
                  }
                }
                else -> {
                  Box(
                    modifier = Modifier
                      .size(72.dp)
                      .clip(CircleShape)
                      .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = UstozPrimaryContainer, radius = 36.dp)
                      ) {
                        if (enteredPin.length < 4) {
                          enteredPin += key
                          localError = null
                          if (errorMessage != null) onClearError()
                        }
                      }
                      .testTag("pin_key_$key"),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = key,
                      style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium
                      ),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Bottom Footer Action (From Immersive UI layout)
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = Color.White,
      shadowElevation = 2.dp,
      border = androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = UstozOutline.copy(alpha = 0.10f)
      )
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
      ) {
        if (!isSetupMode) {
          TextButton(
            onClick = onSwitchAccount,
            shape = CircleShape,
            modifier = Modifier.testTag("pin_logout_button")
          ) {
            Text(
              text = "Boshqa hisob bilan kirish",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              ),
              color = UstozPrimary
            )
          }
        } else {
          Text(
            text = "Ustoz AI Shaxsiy Xavfsizlik Tizimi",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
