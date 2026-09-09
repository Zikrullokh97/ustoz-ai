package com.example.ui.attendance

import android.content.Context
import android.nfc.NfcAdapter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AttendanceAuditEntity
import com.example.data.model.AttendanceDetectionState
import com.example.data.model.AttendanceMethod
import com.example.data.model.AttendanceStatus
import com.example.data.model.CameraEntity
import com.example.data.model.StudentCardEntity
import com.example.data.model.StudentEntity
import com.example.data.model.StudentFaceEnrollmentEntity
import com.example.data.repository.AttendanceScanResult
import com.example.ui.theme.AbsentColor
import com.example.ui.theme.ExcusedColor
import com.example.ui.theme.LateColor
import com.example.ui.theme.PresentColor
import com.example.ui.theme.UstozOnPrimaryContainer
import com.example.ui.theme.UstozOutline
import com.example.ui.theme.UstozPrimary
import com.example.ui.theme.UstozPrimaryContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ==========================================
// 1. ATTENDANCE METHOD TABS SELECTOR
// ==========================================

@Composable
fun AttendanceMethodSelector(
  selectedMethod: AttendanceMethod,
  onSelectMethod: (AttendanceMethod) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.15f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        text = "Davomat olish usuli",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        MethodTabItem(
          label = "Qo'lda",
          icon = Icons.Default.TouchApp,
          isSelected = selectedMethod == AttendanceMethod.MANUAL,
          onClick = { onSelectMethod(AttendanceMethod.MANUAL) },
          modifier = Modifier.weight(1f),
          testTag = "tab_method_manual"
        )
        MethodTabItem(
          label = "Face ID",
          icon = Icons.Default.Face,
          isSelected = selectedMethod == AttendanceMethod.FACE,
          onClick = { onSelectMethod(AttendanceMethod.FACE) },
          modifier = Modifier.weight(1f),
          testTag = "tab_method_face"
        )
        MethodTabItem(
          label = "QR Kod",
          icon = Icons.Default.QrCodeScanner,
          isSelected = selectedMethod == AttendanceMethod.QR,
          onClick = { onSelectMethod(AttendanceMethod.QR) },
          modifier = Modifier.weight(1f),
          testTag = "tab_method_qr"
        )
        MethodTabItem(
          label = "NFC Karta",
          icon = Icons.Default.CreditCard,
          isSelected = selectedMethod == AttendanceMethod.NFC,
          onClick = { onSelectMethod(AttendanceMethod.NFC) },
          modifier = Modifier.weight(1f),
          testTag = "tab_method_nfc"
        )
      }
    }
  }
}

@Composable
private fun MethodTabItem(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (isSelected) UstozPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

// ==========================================
// 2. FACE ID / CAMERA AI SCANNER PANEL
// ==========================================

@Composable
fun FaceIdAttendancePanel(
  cameras: List<CameraEntity>,
  selectedCamera: CameraEntity?,
  onSelectCamera: (CameraEntity) -> Unit,
  onManageCamerasClick: () -> Unit,
  detectedCount: Int,
  uncertainCount: Int,
  notDetectedCount: Int,
  detectedCandidates: List<AttendanceScanResult>,
  onConfirmCandidates: () -> Unit,
  onSimulateDetection: () -> Unit,
  isScanning: Boolean = false,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "face_scan")
  val scanAnim by infiniteTransition.animateFloat(
    initialValue = 0.1f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scan_line"
  )

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, UstozPrimary.copy(alpha = 0.3f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header with active camera info
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .background(if (selectedCamera != null && selectedCamera.status == "ACTIVE") PresentColor else LateColor, CircleShape)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = selectedCamera?.name ?: "Kamera tanlanmagan",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
        }

        OutlinedButton(
          onClick = onManageCamerasClick,
          shape = CircleShape,
          modifier = Modifier.height(32.dp).testTag("btn_manage_cameras")
        ) {
          Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Kameralar", fontSize = 11.sp)
        }
      }

      if (selectedCamera != null) {
        Text(
          text = "${selectedCamera.deviceId} • ${selectedCamera.location}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = 18.dp, bottom = 10.dp)
        )
      } else {
        Spacer(modifier = Modifier.height(8.dp))
      }

      // Live Viewfinder / Face Box Visualizer
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(170.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          // Bounding Box 1
          drawRoundRect(
            color = PresentColor.copy(alpha = 0.85f),
            topLeft = Offset(size.width * 0.22f, size.height * 0.22f),
            size = Size(size.width * 0.26f, size.height * 0.55f),
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(width = 3.dp.toPx())
          )
          // Bounding Box 2
          drawRoundRect(
            color = if (uncertainCount > 0) LateColor.copy(alpha = 0.85f) else PresentColor.copy(alpha = 0.85f),
            topLeft = Offset(size.width * 0.55f, size.height * 0.25f),
            size = Size(size.width * 0.25f, size.height * 0.52f),
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(width = 3.dp.toPx())
          )
          // Scanning Line
          val lineY = size.height * scanAnim
          drawLine(
            color = UstozPrimary.copy(alpha = 0.7f),
            start = Offset(0f, lineY),
            end = Offset(size.width, lineY),
            strokeWidth = 2.dp.toPx()
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .background(Color.Black.copy(alpha = 0.6f), CircleShape)
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.Face, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Face AI skanerlash faol",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          Spacer(modifier = Modifier.height(70.dp))

          Text(
            text = "Kameraga qaragan o'quvchilar avtomatik aniqlanadi",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 11.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Counters: Aniq, Noaniq, Aniqlanmadi
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        DetectionCountBadge(
          label = "Aniq (Kelgan)",
          count = detectedCount,
          color = PresentColor,
          bgColor = PresentColor.copy(alpha = 0.12f),
          modifier = Modifier.weight(1f)
        )
        DetectionCountBadge(
          label = "Noaniq (Tekshirish)",
          count = uncertainCount,
          color = LateColor,
          bgColor = LateColor.copy(alpha = 0.12f),
          modifier = Modifier.weight(1f)
        )
        DetectionCountBadge(
          label = "Aniqlanmadi",
          count = notDetectedCount,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Important Rule Banner
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Security, contentDescription = null, tint = UstozPrimary, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Kamera ko'rmagan o'quvchilar avtomatik ravishda 'Yo'q' deb hisoblanmaydi.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onSimulateDetection,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).testTag("btn_face_scan_simulate")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Aniqlash (Scan)", fontSize = 12.sp)
        }

        Button(
          onClick = onConfirmCandidates,
          enabled = detectedCandidates.isNotEmpty(),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
          modifier = Modifier.weight(1.3f).testTag("btn_confirm_face_candidates")
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Natijalarni kiritish (${detectedCandidates.size})", fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun DetectionCountBadge(
  label: String,
  count: Int,
  color: Color,
  bgColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(bgColor)
      .padding(vertical = 6.dp, horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "$count",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color)
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

// ==========================================
// 3. QR CODE SCANNER PANEL
// ==========================================

@Composable
fun QrAttendancePanel(
  onScanToken: (String) -> Unit,
  lastScannedResult: AttendanceScanResult?,
  modifier: Modifier = Modifier
) {
  var manualInputCode by remember { mutableStateOf("") }
  val infiniteTransition = rememberInfiniteTransition(label = "qr_scan")
  val scanAnim by infiniteTransition.animateFloat(
    initialValue = 0.1f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "qr_line"
  )

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, UstozPrimary.copy(alpha = 0.3f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = UstozPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "QR Kod Skaneri",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
        }
        Surface(
          shape = CircleShape,
          color = PresentColor.copy(alpha = 0.15f)
        ) {
          Text(
            text = "Faol",
            color = PresentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // QR Viewfinder Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val frameSize = size.height * 0.65f
          val left = (size.width - frameSize) / 2
          val top = (size.height - frameSize) / 2

          // Target frame
          drawRoundRect(
            color = UstozPrimary,
            topLeft = Offset(left, top),
            size = Size(frameSize, frameSize),
            cornerRadius = CornerRadius(14f, 14f),
            style = Stroke(width = 3.dp.toPx())
          )

          // Laser line inside target
          val laserY = top + (frameSize * scanAnim)
          drawLine(
            color = PresentColor,
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(left + frameSize - 8.dp.toPx(), laserY),
            strokeWidth = 3.dp.toPx()
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(12.dp)
        ) {
          Text(
            text = "O'quvchi QR kodini kameraga tuting",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Dars boshlanganidan 5 daqiqadan so'ng 'Kechikkan' deb belgilanadi",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
          )
        }
      }

      if (lastScannedResult != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (lastScannedResult.status == AttendanceStatus.PRESENT) PresentColor.copy(alpha = 0.15f) else LateColor.copy(alpha = 0.15f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (lastScannedResult.status == AttendanceStatus.PRESENT) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
              contentDescription = null,
              tint = if (lastScannedResult.status == AttendanceStatus.PRESENT) PresentColor else LateColor,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = lastScannedResult.studentName,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
              Text(
                text = lastScannedResult.message,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Fast Scanner Simulation / Code Input
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = manualInputCode,
          onValueChange = { manualInputCode = it },
          placeholder = { Text("QR token yoki o'quvchi ID", fontSize = 12.sp) },
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("input_qr_code_manual"),
          shape = RoundedCornerShape(12.dp)
        )

        Button(
          onClick = {
            if (manualInputCode.isNotBlank()) {
              onScanToken(manualInputCode.trim())
              manualInputCode = ""
            }
          },
          enabled = manualInputCode.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
          modifier = Modifier.testTag("btn_submit_qr_manual")
        ) {
          Text("Skanerlash")
        }
      }
    }
  }
}

// ==========================================
// 4. NFC / RFID CARD SCANNER PANEL
// ==========================================

@Composable
fun NfcAttendancePanel(
  onCardTapped: (cardUid: String) -> Unit,
  lastScannedResult: AttendanceScanResult?,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val nfcAdapter = remember {
    try {
      NfcAdapter.getDefaultAdapter(context)
    } catch (_: Exception) {
      null
    }
  }

  val isNfcSupported = nfcAdapter != null
  val isNfcEnabled = nfcAdapter?.isEnabled == true
  var testCardUid by remember { mutableStateOf("") }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, UstozPrimary.copy(alpha = 0.3f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CreditCard, contentDescription = null, tint = UstozPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "NFC / RFID Karta O'quvchi",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
        }

        Surface(
          shape = CircleShape,
          color = if (isNfcEnabled) PresentColor.copy(alpha = 0.15f) else LateColor.copy(alpha = 0.15f)
        ) {
          Text(
            text = if (!isNfcSupported) "Qo'llab-quvvatlamaydi" else if (isNfcEnabled) "Tayyor" else "O'chirilgan",
            color = if (isNfcEnabled) PresentColor else LateColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Hardware status message
      if (!isNfcSupported) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = LateColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "NFC bu qurilmada mavjud emas (qurilmada NFC chipi yo'q). Karta UID kiritish orqali test qilishingiz mumkin.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      } else if (!isNfcEnabled) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = LateColor.copy(alpha = 0.15f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = LateColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "NFC o'chirilgan. Qurilma sozlamalaridan NFC-ni yoqing.",
              style = MaterialTheme.typography.labelSmall,
              color = LateColor
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      // NFC Card Tap Area
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(150.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(UstozPrimaryContainer.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier
              .size(54.dp)
              .background(UstozPrimary, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Nfc, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Kartani telefon orqasiga tekkizing",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = UstozPrimary
          )
          Text(
            text = "MIFARE, RFID yoki NFC student kartalari",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (lastScannedResult != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (lastScannedResult.status == AttendanceStatus.PRESENT) PresentColor.copy(alpha = 0.15f) else LateColor.copy(alpha = 0.15f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (lastScannedResult.status == AttendanceStatus.PRESENT) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
              contentDescription = null,
              tint = if (lastScannedResult.status == AttendanceStatus.PRESENT) PresentColor else LateColor,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = lastScannedResult.studentName,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
              Text(
                text = lastScannedResult.message,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Card UID Input (for testing or manual card reader entry)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = testCardUid,
          onValueChange = { testCardUid = it.uppercase() },
          placeholder = { Text("Karta UID (masalan: 04A25F3B)", fontSize = 12.sp) },
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("input_card_uid_manual"),
          shape = RoundedCornerShape(12.dp)
        )

        Button(
          onClick = {
            if (testCardUid.isNotBlank()) {
              onCardTapped(testCardUid.trim())
              testCardUid = ""
            }
          },
          enabled = testCardUid.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
          modifier = Modifier.testTag("btn_submit_card_uid_manual")
        ) {
          Text("Tekkizish")
        }
      }
    }
  }
}

// ==========================================
// 5. ATTENDANCE CONFIRMATION DIALOG (TEACHER VERIFICATION)
// ==========================================

@Composable
fun AttendanceConfirmationDialog(
  detectedCount: Int,
  uncertainCount: Int,
  notDetectedCount: Int,
  onDismiss: () -> Unit,
  onConfirmSave: (markRemainingAsAbsent: Boolean) -> Unit
) {
  var markRemainingAsAbsent by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(vertical = 20.dp)
        .testTag("dialog_attendance_confirmation")
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Davomatni Tasdiqlash",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        Text(
          text = "Avtomatik davomat natijalarini o'qituvchi sifatida tasdiqlang va yakuniy davomat jurnaliga saqlang.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Bento Summary
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.15f))
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Aniq aniqlangan (Kelgan):", fontSize = 13.sp)
              Text("$detectedCount nafar", fontWeight = FontWeight.Bold, color = PresentColor)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Noaniq (Ko'rib chiqilgan):", fontSize = 13.sp)
              Text("$uncertainCount nafar", fontWeight = FontWeight.Bold, color = LateColor)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Aniqlanmagan o'quvchilar:", fontSize = 13.sp)
              Text("$notDetectedCount nafar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
          }
        }

        // Rule disclaimer
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = UstozPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Ustoz AI qoidasi: Kamera/skaner ko'rmagan o'quvchilar avtomatik 'Yo'q' deb hisoblanmaydi. Qaror faqat o'qituvchi tomonidan tasdiqlanadi.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Option to mark remaining as absent
        if (notDetectedCount > 0) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { markRemainingAsAbsent = !markRemainingAsAbsent },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = markRemainingAsAbsent,
              onCheckedChange = { markRemainingAsAbsent = it },
              colors = CheckboxDefaults.colors(checkedColor = UstozPrimary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Aniqlanmagan $notDetectedCount nafar o'quvchini 'Darsda yo'q' (Absent) deb belgilash",
              style = MaterialTheme.typography.bodySmall
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Bekor qilish")
          }
          Button(
            onClick = { onConfirmSave(markRemainingAsAbsent) },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier.weight(1f).testTag("btn_final_confirm_attendance_save")
          ) {
            Text("Tasdiqlash va Saqlash")
          }
        }
      }
    }
  }
}

// ==========================================
// 6. STUDENT QR CODE DIALOG
// ==========================================

@Composable
fun StudentQrCardDialog(
  student: StudentEntity,
  className: String,
  onDismiss: () -> Unit
) {
  val clipboardManager = LocalClipboardManager.current
  val qrToken = "USTOZ_ATTENDANCE:${student.id}:${abs((student.id + student.fullName).hashCode())}"
  var isCopied by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxWidth(0.88f)
        .padding(vertical = 20.dp)
        .testTag("dialog_student_qr_card")
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "O'quvchi QR Kartasi",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        // Student Card Layout
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Header
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "USTOZ AI",
                color = UstozPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
              )
              Surface(
                shape = CircleShape,
                color = UstozPrimaryContainer
              ) {
                Text(
                  text = className,
                  color = UstozPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic QR Matrix Canvas Generator
            Box(
              modifier = Modifier
                .size(190.dp)
                .background(Color.White)
                .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(10.dp),
              contentAlignment = Alignment.Center
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val moduleCount = 21
                val cellSize = size.width / moduleCount
                val hash = qrToken.hashCode()

                for (r in 0 until moduleCount) {
                  for (c in 0 until moduleCount) {
                    // Draw Finder Patterns in corners
                    val isTopLeft = r < 7 && c < 7
                    val isTopRight = r < 7 && c >= moduleCount - 7
                    val isBottomLeft = r >= moduleCount - 7 && c < 7

                    val isBlack = if (isTopLeft || isTopRight || isBottomLeft) {
                      val localR = if (r >= moduleCount - 7) r - (moduleCount - 7) else r
                      val localC = if (c >= moduleCount - 7) c - (moduleCount - 7) else c
                      (localR == 0 || localR == 6 || localC == 0 || localC == 6) ||
                        (localR in 2..4 && localC in 2..4)
                    } else {
                      // Deterministic data pattern based on token hash
                      ((r * 31 + c * 17 + hash) % 3 == 0) || ((r + c) % 4 == 0)
                    }

                    if (isBlack) {
                      drawRect(
                        color = Color.Black,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize, cellSize)
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = student.fullName,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.Black
            )
            Text(
              text = "O'quvchi ID: ${student.id}",
              style = MaterialTheme.typography.labelSmall,
              color = Color.Gray
            )
          }
        }

        Text(
          text = "Ushbu QR kod dars boshida skaner qilinib, davomat olinadi.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(qrToken))
              isCopied = true
            },
            shape = CircleShape,
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isCopied) "Nusxalandi!" else "Tokenni olish")
          }
          Button(
            onClick = onDismiss,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier.weight(1f)
          ) {
            Text("Yopish")
          }
        }
      }
    }
  }
}

// ==========================================
// 7. STUDENT CARD MANAGEMENT DIALOG
// ==========================================

@Composable
fun StudentCardPairDialog(
  student: StudentEntity,
  currentCard: StudentCardEntity?,
  onDismiss: () -> Unit,
  onPairCard: (cardUid: String) -> Unit,
  onDeactivateCard: (cardId: String) -> Unit
) {
  var cardUidInput by remember { mutableStateOf("") }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxWidth(0.9f)
        .padding(vertical = 20.dp)
        .testTag("dialog_student_card_pair")
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "NFC Karta Biriktirish",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        Text(
          text = "O'quvchi: ${student.fullName}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = UstozPrimary
        )

        if (currentCard != null) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = PresentColor.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Faol Karta UID:",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = currentCard.cardUid,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = PresentColor
                )
              }
              IconButton(
                onClick = { onDeactivateCard(currentCard.id) },
                modifier = Modifier.testTag("btn_deactivate_card")
              ) {
                Icon(Icons.Default.Delete, contentDescription = "O'chirish", tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        } else {
          Text(
            text = "Hozircha o'quvchiga NFC karta biriktirilmagan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        OutlinedTextField(
          value = cardUidInput,
          onValueChange = { cardUidInput = it.uppercase() },
          label = { Text("Karta UID (masalan: 04E5A8B2)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_new_card_uid"),
          shape = RoundedCornerShape(12.dp)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Bekor qilish")
          }
          Button(
            onClick = {
              if (cardUidInput.isNotBlank()) {
                onPairCard(cardUidInput.trim())
              }
            },
            enabled = cardUidInput.isNotBlank(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier.weight(1f).testTag("btn_save_card_uid")
          ) {
            Text("Saqlash")
          }
        }
      }
    }
  }
}

// ==========================================
// 8. STUDENT FACE ENROLLMENT DIALOG
// ==========================================

@Composable
fun StudentFaceEnrollDialog(
  student: StudentEntity,
  currentEnrollment: StudentFaceEnrollmentEntity?,
  onDismiss: () -> Unit,
  onEnroll: () -> Unit,
  onDeleteEnrollment: () -> Unit
) {
  var consentGiven by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(vertical = 20.dp)
        .testTag("dialog_student_face_enroll")
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Face ID Ro'yxatdan O'tkazish",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        Text(
          text = "O'quvchi: ${student.fullName}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = UstozPrimary
        )

        if (currentEnrollment != null) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = PresentColor.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Face ID Holati: Biriktirilgan",
                  fontWeight = FontWeight.Bold,
                  color = PresentColor
                )
                Text(
                  text = "Shablon: ${currentEnrollment.faceTemplateHash.take(16)}...",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              IconButton(
                onClick = onDeleteEnrollment,
                modifier = Modifier.testTag("btn_delete_face_enrollment")
              ) {
                Icon(Icons.Default.Delete, contentDescription = "O'chirish", tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        } else {
          // Biometric Consent Section (Privacy Mandate)
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = UstozPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Biometrik Maxfiylik va Xavfsizlik",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "O'quvchining yuz surati serverda saqlanmaydi. Faqat xavfsiz matematik shablon (embedding hash) shakllantiriladi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { consentGiven = !consentGiven },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = consentGiven,
              onCheckedChange = { consentGiven = it },
              colors = CheckboxDefaults.colors(checkedColor = UstozPrimary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "O'quvchi va ota-onaning biometrik ma'lumotlarni qayta ishlashga roziligi olingan *",
              style = MaterialTheme.typography.bodySmall
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Bekor qilish")
          }
          Button(
            onClick = onEnroll,
            enabled = currentEnrollment == null && consentGiven,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier.weight(1f).testTag("btn_confirm_face_enroll")
          ) {
            Text("Yuzni Ro'yxatlash")
          }
        }
      }
    }
  }
}

// ==========================================
// 9. CAMERA MANAGEMENT DIALOG
// ==========================================

@Composable
fun CameraManagementDialog(
  cameras: List<CameraEntity>,
  onDismiss: () -> Unit,
  onAddCamera: (name: String, location: String, deviceId: String, secretToken: String) -> Unit,
  onDeleteCamera: (String) -> Unit,
  onToggleCamera: (String, Boolean) -> Unit,
  onTestConnection: (String) -> Unit
) {
  var showAddDialog by remember { mutableStateOf(false) }

  if (showAddDialog) {
    AddCameraDialog(
      onDismiss = { showAddDialog = false },
      onSave = { name, loc, devId, token ->
        onAddCamera(name, loc, devId, token)
        showAddDialog = false
      }
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .padding(vertical = 20.dp)
        .testTag("dialog_camera_management")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Videocam, contentDescription = null, tint = UstozPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Kamera Sozlamalari",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Ro'yxatdan o'tgan kameralar (${cameras.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Button(
            onClick = { showAddDialog = true },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier.height(36.dp).testTag("btn_open_add_camera")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Yangi kamera", fontSize = 12.sp)
          }
        }

        if (cameras.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(44.dp))
              Spacer(modifier = Modifier.height(10.dp))
              Text("Hozircha kameralar biriktirilmagan", style = MaterialTheme.typography.bodyMedium)
              Text("Yangi kamera qo'shish tugmasidan foydalaning.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(cameras, key = { it.id }) { cam ->
              CameraItemCard(
                camera = cam,
                onDelete = { onDeleteCamera(cam.id) },
                onToggle = { active -> onToggleCamera(cam.id, active) },
                onTest = { onTestConnection(cam.id) }
              )
            }
          }
        }

        Button(
          onClick = onDismiss,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Tayyor")
        }
      }
    }
  }
}

@Composable
private fun CameraItemCard(
  camera: CameraEntity,
  onDelete: () -> Unit,
  onToggle: (Boolean) -> Unit,
  onTest: () -> Unit
) {
  val isActive = camera.status == "ACTIVE"

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.12f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .background(if (isActive) PresentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.Videocam,
          contentDescription = null,
          tint = if (isActive) PresentColor else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = camera.name,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = "${camera.deviceId} • ${camera.location}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
          checked = isActive,
          onCheckedChange = onToggle,
          colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PresentColor)
        )

        IconButton(onClick = onDelete) {
          Icon(Icons.Default.Delete, contentDescription = "O'chirish", tint = MaterialTheme.colorScheme.error)
        }
      }
    }
  }
}

@Composable
fun AddCameraDialog(
  onDismiss: () -> Unit,
  onSave: (name: String, location: String, deviceId: String, secretToken: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }
  var deviceId by remember { mutableStateOf("") }
  var secretToken by remember { mutableStateOf("") }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxWidth(0.9f)
        .padding(vertical = 20.dp)
        .testTag("dialog_add_camera")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Yangi Kamera Qo'shish",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Kamera nomi (masalan: 7-A kirish kamerasi) *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_camera_name"),
          shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text("Joylashuv (masalan: 204-xona)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_camera_location"),
          shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
          value = deviceId,
          onValueChange = { deviceId = it },
          label = { Text("Qurilma ID (Device ID) *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_camera_device_id"),
          shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
          value = secretToken,
          onValueChange = { secretToken = it },
          label = { Text("Maxfiy Token (Secret Key) *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_camera_secret_token"),
          shape = RoundedCornerShape(12.dp)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Bekor qilish")
          }
          Button(
            onClick = { onSave(name, location, deviceId, secretToken) },
            enabled = name.isNotBlank() && deviceId.isNotBlank() && secretToken.isNotBlank(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
            modifier = Modifier.weight(1f).testTag("btn_save_camera")
          ) {
            Text("Saqlash")
          }
        }
      }
    }
  }
}

// ==========================================
// 10. ATTENDANCE AUDIT LOGS DIALOG
// ==========================================

@Composable
fun AttendanceAuditDialog(
  audits: List<AttendanceAuditEntity>,
  onDismiss: () -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("HH:mm • dd.MM.yyyy", Locale.getDefault()) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .padding(vertical = 20.dp)
        .testTag("dialog_attendance_audits")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, tint = UstozPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Davomat Audit Jurnali",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Yopish")
          }
        }

        Text(
          text = "Avtomatik va qo'lda bajarilgan barcha davomat operatsiyalari tarixi.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (audits.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("Hozircha audit yozuvlari mavjud emas", style = MaterialTheme.typography.bodyMedium)
          }
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(audits, key = { it.id }) { audit ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, UstozOutline.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = audit.action,
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = UstozPrimary
                    )
                    Text(
                      text = dateFormat.format(Date(audit.timestamp)),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = audit.details,
                    style = MaterialTheme.typography.bodySmall
                  )
                }
              }
            }
          }
        }

        Button(
          onClick = onDismiss,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = UstozPrimary),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Yopish")
        }
      }
    }
  }
}
