package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SupportScreen
import com.example.ui.SupportViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      val field = android.database.CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
      field.isAccessible = true
      field.set(null, 100 * 1024 * 1024) // 100 MB buffer for large SQLite queries
    } catch (_: Throwable) {}
    enableEdgeToEdge()

    val permissionsToRequest = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
    }
    if (permissionsToRequest.isNotEmpty()) {
      ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1001)
    }

    val isMasterPower = try {
      getSharedPreferences("support_app_prefs", android.content.Context.MODE_PRIVATE)
        .getBoolean("master_power_enabled", true)
    } catch (_: Exception) { true }

    if (isMasterPower) {
      try {
        com.example.sip.VerimorSipService.startService(this)
        com.example.sip.VerimorSipService.requestIgnoreBatteryOptimizations(this)
      } catch (_: Exception) {}
    }

    setContent {
      MyApplicationTheme {
        val viewModel: SupportViewModel = viewModel()
        LaunchedEffect(Unit) {
          viewModel.checkAndExecuteAutomaticWeeklySqlSync(this@MainActivity)
        }
        SupportScreen(viewModel = viewModel)
      }
    }
  }
}

