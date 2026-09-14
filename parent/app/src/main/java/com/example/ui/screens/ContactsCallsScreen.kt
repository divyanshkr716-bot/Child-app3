package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallLog
import com.example.data.model.CallType
import com.example.data.model.Contact
import com.example.ui.theme.*

@Composable
fun ContactsCallsScreen(
  contacts: List<Contact>,
  callLogs: List<CallLog>,
  onAddContact: (name: String, phone: String, email: String, label: String) -> Unit,
  onDeleteContact: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf(0) } // 0: Contacts, 1: Call Logs
  var searchQuery by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }

  var newName by remember { mutableStateOf("") }
  var newPhone by remember { mutableStateOf("") }
  var newEmail by remember { mutableStateOf("") }
  var newLabel by remember { mutableStateOf("Family") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header & Tabs
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = Color.Transparent,
          contentColor = AirDroidGreen,
          modifier = Modifier.width(320.dp)
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Contacts (${contacts.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Call Logs (${callLogs.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
          )
        }

        if (selectedTab == 0) {
          Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("add_contact_button")
          ) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Contact", fontSize = 12.sp)
          }
        }
      }
    }

    if (selectedTab == 0) {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search child device address book...", fontSize = 12.sp) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = AirDroidGreen) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().testTag("contacts_search_input")
      )

      val filtered = contacts.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
      }

      // Contacts List
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(filtered) { contact ->
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AirDroidGreen.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = contact.name.first().toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AirDroidGreen
                  )
                }

                Column {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                      text = contact.name,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = MaterialTheme.colorScheme.surface
                    ) {
                      Text(
                        text = contact.label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Text(contact.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  if (contact.email.isNotEmpty()) {
                    Text(contact.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }

              IconButton(
                onClick = { onDeleteContact(contact.id) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Filled.DeleteOutline,
                  contentDescription = "Delete Contact",
                  tint = StatusOffline,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    } else {
      // Call Logs List (Missed, Incoming, Outgoing)
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(callLogs) { log ->
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                      when (log.callType) {
                        CallType.MISSED -> Color(0xFFEF4444).copy(alpha = 0.18f)
                        CallType.INCOMING -> AirDroidGreen.copy(alpha = 0.18f)
                        CallType.OUTGOING -> AirDroidCyan.copy(alpha = 0.18f)
                      }
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = when (log.callType) {
                      CallType.MISSED -> Icons.Filled.PhoneMissed
                      CallType.INCOMING -> Icons.Filled.PhoneCallback
                      CallType.OUTGOING -> Icons.Filled.PhoneForwarded
                    },
                    contentDescription = null,
                    tint = when (log.callType) {
                      CallType.MISSED -> Color(0xFFEF4444)
                      CallType.INCOMING -> AirDroidGreen
                      CallType.OUTGOING -> AirDroidCyan
                    },
                    modifier = Modifier.size(20.dp)
                  )
                }

                Column {
                  Text(
                    text = log.contactName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "${log.phoneNumber} • ${log.time}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = when (log.callType) {
                    CallType.MISSED -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    CallType.INCOMING -> AirDroidGreen.copy(alpha = 0.15f)
                    CallType.OUTGOING -> AirDroidCyan.copy(alpha = 0.15f)
                  }
                ) {
                  Text(
                    text = log.callType.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (log.callType) {
                      CallType.MISSED -> Color(0xFFEF4444)
                      CallType.INCOMING -> AirDroidGreen
                      CallType.OUTGOING -> AirDroidCyan
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text("${log.durationSeconds}s", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }
  }

  // Add Contact Dialog
  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Add New Contact to Mobile") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Full Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newPhone,
            onValueChange = { newPhone = it },
            label = { Text("Phone Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newEmail,
            onValueChange = { newEmail = it },
            label = { Text("Email (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newName.isNotBlank() && newPhone.isNotBlank()) {
              onAddContact(newName, newPhone, newEmail, newLabel)
              newName = ""
              newPhone = ""
              newEmail = ""
              showAddDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen)
        ) {
          Text("Save to Phone")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
      }
    )
  }
}
