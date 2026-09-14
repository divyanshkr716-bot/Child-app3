package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.data.model.FamilyChatMessage
import com.example.data.model.FamilyMember
import com.example.ui.theme.*

@Composable
fun FamilyChatScreen(
  members: List<FamilyMember>,
  messages: List<FamilyChatMessage>,
  onSendMessage: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Family Member Presence Banner
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Family Hub & Secure Chat",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = AirDroidGreen.copy(alpha = 0.15f)
          ) {
            Text(
              text = "End-to-End Encrypted",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = AirDroidGreen,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Members Horizontal Row
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(members) { member ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(if (member.isOnline) AirDroidGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (member.role == "Parent") Icons.Filled.SupervisorAccount else Icons.Filled.Person,
                  contentDescription = null,
                  tint = if (member.isOnline) AirDroidGreen else Color.Gray,
                  modifier = Modifier.size(16.dp)
                )
              }
              Column {
                Text(member.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                  Box(
                    modifier = Modifier
                      .size(5.dp)
                      .clip(CircleShape)
                      .background(if (member.isOnline) AirDroidGreen else Color.Gray)
                  )
                  Text(member.lastSeen, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      }
    }

    // Chat Messages Area
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      LazyColumn(
        state = listState,
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().testTag("family_chat_list")
      ) {
        items(messages, key = { it.id }) { msg ->
          ChatMessageBubble(message = msg)
        }
      }
    }

    // Quick Action Chips
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      val quickPhrases = listOf(
        "Where are you right now?",
        "Dinner is ready, head home!",
        "Finish homework first",
        "Call me when you can"
      )
      items(quickPhrases) { phrase ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.testTag("quick_phrase_$phrase")
        ) {
          TextButton(
            onClick = { onSendMessage(phrase) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp)
          ) {
            Text(phrase, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
          }
        }
      }
    }

    // Message Input Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = inputText,
        onValueChange = { inputText = it },
        placeholder = { Text("Message child or family...", fontSize = 12.sp) },
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("family_chat_input"),
        maxLines = 3
      )

      IconButton(
        onClick = {
          if (inputText.isNotBlank()) {
            onSendMessage(inputText)
            inputText = ""
          }
        },
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(AirDroidGreen)
          .testTag("send_family_message_button")
      ) {
        Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
      }
    }
  }
}

@Composable
private fun ChatMessageBubble(
  message: FamilyChatMessage
) {
  val isFromParent = message.isFromParent

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isFromParent) Alignment.End else Alignment.Start
  ) {
    Text(
      text = message.senderName,
      fontSize = 10.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )

    Surface(
      shape = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = if (isFromParent) 12.dp else 2.dp,
        bottomEnd = if (isFromParent) 2.dp else 12.dp
      ),
      color = if (isFromParent) AirDroidGreen else MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.widthIn(max = 280.dp)
    ) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
          text = message.message,
          fontSize = 13.sp,
          color = if (isFromParent) Color.White else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier.align(Alignment.End),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = message.timestamp,
            fontSize = 9.sp,
            color = if (isFromParent) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (isFromParent) {
            Icon(
              imageVector = Icons.Filled.DoneAll,
              contentDescription = message.status,
              tint = Color.White.copy(alpha = 0.8f),
              modifier = Modifier.size(12.dp)
            )
          }
        }
      }
    }
  }
}
