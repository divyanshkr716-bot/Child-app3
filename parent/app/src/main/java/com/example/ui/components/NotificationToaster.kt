package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.ui.theme.*

@Composable
fun NotificationToaster(
  notification: RemoteNotification?,
  onDismiss: () -> Unit,
  onReply: (String, String) -> Unit,
  modifier: Modifier = Modifier
) {
  var replyText by remember(notification?.id) { mutableStateOf("") }
  var isReplying by remember { mutableStateOf(false) }

  AnimatedVisibility(
    visible = notification != null,
    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
    modifier = modifier
  ) {
    notification?.let { notif ->
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
          .widthIn(max = 420.dp)
          .padding(12.dp)
          .testTag("notification_toaster_popup")
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          // Header: App Name, time, dismiss button
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(20.dp)
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
                  modifier = Modifier.size(12.dp)
                )
              }
              Text(
                text = "${notif.appName} • Mobile Alert",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AirDroidGreen
              )
              Text(
                text = "(${notif.time})",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(24.dp).testTag("toaster_dismiss_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Body: Title & Content
          Text(
            text = notif.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = notif.content,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 2.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Interactive Quick Reply Box
          if (!isReplying) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(
                onClick = { isReplying = true },
                colors = ButtonDefaults.textButtonColors(contentColor = AirDroidGreen),
                modifier = Modifier.testTag("toaster_quick_reply_open")
              ) {
                Icon(
                  imageVector = Icons.Default.Reply,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Quick Reply", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                placeholder = { Text("Type remote reply...", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                  onSend = {
                    if (replyText.isNotBlank()) {
                      onReply(notif.id, replyText)
                      replyText = ""
                      isReplying = false
                    }
                  }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = AirDroidGreen,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("toaster_reply_input")
              )

              IconButton(
                onClick = {
                  if (replyText.isNotBlank()) {
                    onReply(notif.id, replyText)
                    replyText = ""
                    isReplying = false
                  }
                },
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(AirDroidGreen)
                  .testTag("toaster_reply_send_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Send,
                  contentDescription = "Send Reply",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}
