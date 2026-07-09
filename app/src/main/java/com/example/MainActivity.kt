package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.AlifVpnAppScreen
import com.example.ui.AlifVpnViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Obtain dynamic AlifVpnViewModel
    val viewModel = ViewModelProvider(this)[AlifVpnViewModel::class.java]
    
    setContent {
      AlifVpnAppScreen(viewModel = viewModel)
    }
  }
}
