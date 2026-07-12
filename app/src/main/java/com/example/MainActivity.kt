package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.ui.AlifVpnAppScreen
import com.example.ui.AlifVpnViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    try {
      // Obtain dynamic AlifVpnViewModel
      val viewModel = ViewModelProvider(this)[AlifVpnViewModel::class.java]
      
      setContent {
        AlifVpnAppScreen(viewModel = viewModel)
      }
    } catch (e: Throwable) {
      Log.e("MainActivity", "Startup Error", e)
      setContent {
        RenderErrorScreen(e)
      }
    }
  }

  @Composable
  private fun RenderErrorScreen(error: Throwable) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0F172A))
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Diagnostic & Recovery Console",
          color = Color(0xFFEF4444),
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "An unexpected crash was prevented on startup. Please share this screen or details with support to fix immediately.",
          color = Color.LightGray,
          fontSize = 14.sp,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
            .padding(16.dp)
        ) {
          Text(
            text = Log.getStackTraceString(error),
            color = Color(0xFF10B981),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
          )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
          onClick = {
            // Force recreation or retry database reset
            try {
              applicationContext.deleteDatabase("alif_vpn_database")
              android.os.Process.killProcess(android.os.Process.myPid())
            } catch (ex: Exception) {
              ex.printStackTrace()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
          Text("Wipe Local Cache & Restart App", color = Color.White)
        }
      }
    }
  }
}
