package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.data.model.ChildDevice
import com.example.data.model.WebHistoryRecord
import com.example.ui.theme.*

@Composable
fun WebMonitoringScreen(
  device: ChildDevice?,
  webHistory: List<WebHistoryRecord>,
  onToggleDomainBlock: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf("HISTORY") } // "HISTORY" or "FILTERS"
  var safeSearchEnabled by remember { mutableStateOf(true) }
  var blockAdult by remember { mutableStateOf(true) }
  var blockGambling by remember { mutableStateOf(true) }
  var blockMalware by remember { mutableStateOf(true) }
  var showAddDomainDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Header
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.Language,
            contentDescription = null,
            tint = AirDroidGreen,
            modifier = Modifier.size(22.dp)
          )
          Column {
            Text(
              text = "Web Browsing History & Content Filters",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${webHistory.size} visits logged today • SafeSearch Active",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          FilterChip(
            selected = selectedTab == "HISTORY",
            onClick = { selectedTab = "HISTORY" },
            label = { Text("Browsing Log", fontSize = 11.sp) },
            modifier = Modifier.testTag("tab_web_history")
          )
          FilterChip(
            selected = selectedTab == "FILTERS",
            onClick = { selectedTab = "FILTERS" },
            label = { Text("Content Protection", fontSize = 11.sp) },
            modifier = Modifier.testTag("tab_web_filters")
          )
        }
      }
    }

    when (selectedTab) {
      "HISTORY" -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(webHistory) { item ->
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (item.isBlocked) StatusOffline.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
              ),
              modifier = Modifier.fillMaxWidth().testTag("web_item_${item.id}")
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
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = if (item.isBlocked) StatusOffline.copy(alpha = 0.2f) else AirDroidCyan.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = if (item.isBlocked) Icons.Filled.Block else Icons.Filled.Public,
                        contentDescription = null,
                        tint = if (item.isBlocked) StatusOffline else AirDroidCyan,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }

                  Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text(text = item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                      if (item.isBlocked) {
                        Surface(shape = RoundedCornerShape(4.dp), color = StatusOffline.copy(alpha = 0.2f)) {
                          Text("BLOCKED ATTEMPT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = StatusOffline, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                      }
                    }
                    Text(text = item.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${item.category} • ${item.timestamp}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }

                OutlinedButton(
                  onClick = { onToggleDomainBlock(item.domain, item.isBlocked) },
                  shape = RoundedCornerShape(6.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text(if (item.isBlocked) "Unblock Site" else "Block Site", fontSize = 11.sp)
                }
              }
            }
          }
        }
      }

      "FILTERS" -> {
        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // SafeSearch Card
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Verified, contentDescription = null, tint = AirDroidGreen, modifier = Modifier.size(24.dp))
                Column {
                  Text("Force SafeSearch & Restricted Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  Text("Locks search engine queries to filter explicit results (Google, Bing, YouTube)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              Switch(checked = safeSearchEnabled, onCheckedChange = { safeSearchEnabled = it })
            }
          }

          // Category Filters Card
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("AUTOMATIC CATEGORY BLOCKING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Adult & Explicit Content", fontSize = 13.sp)
                Switch(checked = blockAdult, onCheckedChange = { blockAdult = it })
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Gambling & Betting Portals", fontSize = 13.sp)
                Switch(checked = blockGambling, onCheckedChange = { blockGambling = it })
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Phishing & Malware Domains", fontSize = 13.sp)
                Switch(checked = blockMalware, onCheckedChange = { blockMalware = it })
              }
            }
          }

          // Custom URL Blacklist Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("CUSTOM BLOCKED DOMAINS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
              onClick = { showAddDomainDialog = true },
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Domain", fontSize = 11.sp)
            }
          }

          val blockedDomains = listOf("discord.com", "omegle.com", "chatroulette.com", "4chan.org")
          LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(blockedDomains) { domain ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(domain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                  IconButton(onClick = { onToggleDomainBlock(domain, true) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = StatusOffline, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddDomainDialog) {
    var newDomain by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showAddDomainDialog = false },
      title = { Text("Block Custom Domain") },
      text = {
        OutlinedTextField(
          value = newDomain,
          onValueChange = { newDomain = it },
          label = { Text("e.g. reddit.com") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newDomain.isNotBlank()) {
              onToggleDomainBlock(newDomain.trim(), false)
              showAddDomainDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen)
        ) {
          Text("Block Domain")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDomainDialog = false }) { Text("Cancel") }
      }
    )
  }
}
