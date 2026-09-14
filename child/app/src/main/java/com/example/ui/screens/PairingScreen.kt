package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AirDroidCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    localIp: String,
    statusMessage: String?,
    onPairWithCode: (String) -> Unit,
    onBack: () -> Unit
) {
    var inputCode by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(DarkBackground)) {
        TopAppBar(
            title = { Text("Pair With Parent", color = TextPrimary, fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Key, null, tint = AirDroidCyan)
            Text("Real device pairing", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Enter the unique 8-character code shown by the Parent app. Game Centre will discover the Parent automatically on the same Wi-Fi network.", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = inputCode, onValueChange = { inputCode = it.uppercase().take(8) }, label = { Text("Unique Parent code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { onPairWithCode(inputCode.trim()) },
                enabled = inputCode.length == 8,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AirDroidCyan, contentColor = Color(0xFF00363D)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Find & Connect to Parent", fontWeight = FontWeight.Bold) }
            if (!statusMessage.isNullOrBlank()) Text(statusMessage, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}
