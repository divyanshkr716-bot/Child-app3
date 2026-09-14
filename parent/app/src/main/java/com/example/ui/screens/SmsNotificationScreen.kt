package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RemoteNotification
import com.example.data.model.SmsMessage
import com.example.ui.theme.*

@Composable
fun SmsNotificationScreen(
  notifications: List<RemoteNotification>,
  smsMessages: List<SmsMessage>,
  selectedThreadId: String,
  smsDraft: String,
  onSelectThread: (String) -> Unit,
  onSmsDraftChange: (String) -> Unit,
  onSendSms: (String) -> Unit,
  onReplyNotification: (String, String) -> Unit,
  onDismissNotification: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf(0) } // 0: SMS Manager, 1: Notification Center
  var inlineReplyMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

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
          modifier = Modifier.width(340.dp)
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("SMS Manager (PC Inbox)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Notifications", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (notifications.isNotEmpty()) {
                  Surface(shape = CircleShape, color = AirDroidCyan) {
                    Text(
                      "${notifications.size}",
                      fontSize = 10.sp,
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                  }
                }
              }
            }
          )
        }

        Surface(
          shape = RoundedCornerShape(20.dp),
          color = AirDroidGreen.copy(alpha = 0.15f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(Icons.Filled.CellTower, contentDescription = null, tint = AirDroidGreen, modifier = Modifier.size(14.dp))
            Text("Child Cellular Link: Online", fontSize = 11.sp, color = AirDroidGreen, fontWeight = FontWeight.Medium)
          }
        }
      }
    }

    if (selectedTab == 0) {
      // SMS Manager (Thread-based SMS view split into threads list and active conversation)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Left: Threads list
        val threads = listOf(
          Triple("thread-mom", "Mom", "+1 (555) 234-5678"),
          Triple("thread-dad", "Dad", "+1 (555) 345-6789"),
          Triple("thread-tutor", "Math Tutor David", "+1 (555) 456-7890")
        )

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "SMS CONVERSATIONS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              items(threads) { (threadId, name, phone) ->
                val isSelected = selectedThreadId == threadId
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (isSelected) AirDroidGreen.copy(alpha = 0.18f) else Color.Transparent,
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectThread(threadId) }
                    .testTag("sms_thread_$threadId")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AirDroidGreen else MaterialTheme.colorScheme.surface),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = name.first().toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                      )
                    }
                    Column {
                      Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Text(phone, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }
                }
              }
            }
          }
        }

        // Right: Active Conversation
        val activeThread = threads.find { it.first == selectedThreadId } ?: threads.first()
        val currentMessages = smsMessages.filter { it.threadId == selectedThreadId }

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            // Conversation Header
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = activeThread.second,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${activeThread.third} • Via Child Phone SIM",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = AirDroidGreen.copy(alpha = 0.15f)
              ) {
                Text(
                  text = "Remote SMS Relay",
                  fontSize = 10.sp,
                  color = AirDroidGreen,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Message Bubbles
            LazyColumn(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(currentMessages) { msg ->
                val isMe = msg.isFromMe
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                  Surface(
                    shape = RoundedCornerShape(
                      topStart = 12.dp,
                      topEnd = 12.dp,
                      bottomStart = if (isMe) 12.dp else 2.dp,
                      bottomEnd = if (isMe) 2.dp else 12.dp
                    ),
                    color = if (isMe) AirDroidGreen else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.widthIn(max = 340.dp)
                  ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                      Text(
                        text = msg.text,
                        fontSize = 13.sp,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = "${msg.time} • ${msg.status}",
                        fontSize = 9.sp,
                        color = if (isMe) Color(0xCCFFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Compose SMS bar
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = smsDraft,
                onValueChange = onSmsDraftChange,
                placeholder = { Text("Compose SMS to send remotely...", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                  onSend = { onSendSms(activeThread.third) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = AirDroidGreen,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.weight(1f).testTag("sms_compose_input")
              )

              Button(
                onClick = { onSendSms(activeThread.third) },
                colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen),
                modifier = Modifier.testTag("sms_send_button")
              ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send Remote SMS", fontSize = 12.sp)
              }
            }
          }
        }
      }
    } else {
      // Notification Center (Full list with in-place Quick Reply and reply history)
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "Remote Mobile Notification Center (${notifications.size})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(10.dp))

          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(notifications) { notif ->
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(28.dp)
                          .clip(CircleShape)
                          .background(AirDroidGreen),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          imageVector = when {
                            notif.appName.contains("WhatsApp", ignoreCase = true) -> Icons.Filled.Chat
                            notif.appName.contains("Phone", ignoreCase = true) -> Icons.Filled.Call
                            else -> Icons.Filled.Notifications
                          },
                          contentDescription = null,
                          tint = Color.White,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                      Column {
                        Text(
                          text = "${notif.appName}: ${notif.title}",
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(notif.time, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }

                    IconButton(
                      onClick = { onDismissNotification(notif.id) },
                      modifier = Modifier.size(26.dp)
                    ) {
                      Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))
                  Text(text = notif.content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                  // Past Replies if any
                  if (notif.replies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .background(AirDroidGreen.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                    ) {
                      Text("Sent Quick Replies:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AirDroidGreen)
                      notif.replies.forEach { r ->
                        Text("• $r", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // In-place Quick Reply field
                  val currentReply = inlineReplyMap[notif.id] ?: ""
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedTextField(
                      value = currentReply,
                      onValueChange = { inlineReplyMap = inlineReplyMap + (notif.id to it) },
                      placeholder = { Text("Quick reply directly without screen mirror...", fontSize = 11.sp) },
                      singleLine = true,
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    )

                    Button(
                      onClick = {
                        if (currentReply.isNotBlank()) {
                          onReplyNotification(notif.id, currentReply)
                          inlineReplyMap = inlineReplyMap - notif.id
                        }
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                      Text("Reply", fontSize = 11.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
