package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.FileItem
import com.example.data.model.FileType
import com.example.data.model.TransferTask
import com.example.ui.theme.*

@Composable
fun FileTransferScreen(
  localFiles: List<FileItem>,
  remoteFiles: List<FileItem>,
  activeTransfers: List<TransferTask>,
  localCurrentPath: String,
  remoteCurrentPath: String,
  isGalleryMode: Boolean,
  onToggleGalleryMode: () -> Unit,
  onSetLocalPath: (String) -> Unit,
  onSetRemotePath: (String) -> Unit,
  onUploadFile: (FileItem) -> Unit,
  onDownloadFile: (FileItem) -> Unit,
  onCreateFolder: (isLocal: Boolean, name: String) -> Unit,
  onDeleteFile: (FileItem) -> Unit,
  onRenameFile: (FileItem, String) -> Unit,
  modifier: Modifier = Modifier
) {
  var showNewFolderDialog by remember { mutableStateOf<Boolean?>(null) } // true: local, false: remote, null: hidden
  var newFolderName by remember { mutableStateOf("") }
  var selectedLocalFile by remember { mutableStateOf<FileItem?>(null) }
  var selectedRemoteFile by remember { mutableStateOf<FileItem?>(null) }
  var previewMediaItem by remember { mutableStateOf<FileItem?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Header Controls & Mode Switcher
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.FolderShared,
            contentDescription = null,
            tint = AirDroidGreen,
            modifier = Modifier.size(22.dp)
          )
          Text(
            text = "Dual-Pane File Manager",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Media Gallery Mode Toggle
          FilterChip(
            selected = isGalleryMode,
            onClick = onToggleGalleryMode,
            leadingIcon = {
              Icon(
                imageVector = if (isGalleryMode) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            },
            label = { Text("Gallery Mode", fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = AirDroidCyan.copy(alpha = 0.2f),
              selectedLabelColor = AirDroidCyan
            ),
            modifier = Modifier.testTag("file_toggle_gallery_button")
          )

          // Upload action
          Button(
            onClick = {
              selectedLocalFile?.let { onUploadFile(it) }
            },
            enabled = selectedLocalFile != null,
            colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("file_upload_action_button")
          ) {
            Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Upload to Mobile", fontSize = 12.sp)
          }

          // Download action
          Button(
            onClick = {
              selectedRemoteFile?.let { onDownloadFile(it) }
            },
            enabled = selectedRemoteFile != null,
            colors = ButtonDefaults.buttonColors(containerColor = AirDroidCyan),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("file_download_action_button")
          ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Download to PC", fontSize = 12.sp)
          }
        }
      }
    }

    if (isGalleryMode) {
      // Media Gallery Grid View (Thumbnail preview mode for photos and videos)
      val mediaFiles = remoteFiles.filter { it.fileType == FileType.IMAGE || it.fileType == FileType.VIDEO }
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Child Device Media Gallery (${mediaFiles.size} photos & videos)",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Tap thumbnail to view or download",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
          ) {
            items(mediaFiles) { item ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                  .height(120.dp)
                  .clickable { previewMediaItem = item }
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                  verticalArrangement = Arrangement.SpaceBetween,
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Box(
                    modifier = Modifier
                      .size(50.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (item.fileType == FileType.VIDEO) Color(0xFFE11D48) else AirDroidCyan),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = if (item.fileType == FileType.VIDEO) Icons.Filled.PlayCircle else Icons.Filled.Image,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(28.dp)
                    )
                  }
                  Text(
                    text = item.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                  )
                  Text(
                    text = "${item.sizeBytes / (1024 * 1024)} MB",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    } else {
      // Dual-Pane File Explorer (PC Storage vs Mobile Storage)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Left Pane: PC Local Storage
        FileExplorerPane(
          title = "PC Local Storage",
          icon = Icons.Filled.Computer,
          currentPath = localCurrentPath,
          files = localFiles,
          selectedFile = selectedLocalFile,
          onSelectFile = { selectedLocalFile = it },
          onNavigatePath = onSetLocalPath,
          onNewFolder = { showNewFolderDialog = true },
          onDelete = onDeleteFile,
          accentColor = AirDroidGreen,
          tag = "pane_pc_storage",
          modifier = Modifier.weight(1f)
        )

        // Right Pane: Remote Mobile Storage
        FileExplorerPane(
          title = "Child Mobile Storage",
          icon = Icons.Filled.Smartphone,
          currentPath = remoteCurrentPath,
          files = remoteFiles,
          selectedFile = selectedRemoteFile,
          onSelectFile = { selectedRemoteFile = it },
          onNavigatePath = onSetRemotePath,
          onNewFolder = { showNewFolderDialog = false },
          onDelete = onDeleteFile,
          accentColor = AirDroidCyan,
          tag = "pane_remote_storage",
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Active Transfer Tasks Bar / Progress Indicator
    if (activeTransfers.isNotEmpty()) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().testTag("active_transfers_bar")
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text(
            text = "Active Transfer Queue (${activeTransfers.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AirDroidGreen
          )
          Spacer(modifier = Modifier.height(6.dp))
          activeTransfers.take(2).forEach { task ->
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "${task.fileName} (${if (task.isUpload) "Upload" else "Download"})",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${task.speedKbps.toInt()} KB/s • ${(task.progress * 100).toInt()}%",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(5.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = if (task.isUpload) AirDroidGreen else AirDroidCyan,
                trackColor = MaterialTheme.colorScheme.surface
              )
            }
          }
        }
      }
    }
  }

  // Create Folder Dialog
  showNewFolderDialog?.let { isLocal ->
    AlertDialog(
      onDismissRequest = { showNewFolderDialog = null },
      title = { Text(if (isLocal) "New Folder on PC" else "New Folder on Mobile") },
      text = {
        OutlinedTextField(
          value = newFolderName,
          onValueChange = { newFolderName = it },
          label = { Text("Folder Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newFolderName.isNotBlank()) {
              onCreateFolder(isLocal, newFolderName)
              newFolderName = ""
              showNewFolderDialog = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen)
        ) {
          Text("Create")
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewFolderDialog = null }) { Text("Cancel") }
      }
    )
  }

  // Media Preview Dialog
  previewMediaItem?.let { item ->
    AlertDialog(
      onDismissRequest = { previewMediaItem = null },
      title = { Text(item.name) },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(160.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(if (item.fileType == FileType.VIDEO) Color(0xFF881337) else Color(0xFF0C4A6E)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (item.fileType == FileType.VIDEO) Icons.Filled.PlayCircle else Icons.Filled.Image,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(64.dp)
            )
          }
          Text(
            text = "Size: ${item.sizeBytes / (1024 * 1024)} MB • Date: ${item.modifiedDate}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Path: ${item.path}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onDownloadFile(item)
            previewMediaItem = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = AirDroidCyan)
        ) {
          Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Download to PC")
        }
      },
      dismissButton = {
        TextButton(onClick = { previewMediaItem = null }) { Text("Close") }
      }
    )
  }
}

@Composable
fun FileExplorerPane(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  currentPath: String,
  files: List<FileItem>,
  selectedFile: FileItem?,
  onSelectFile: (FileItem) -> Unit,
  onNavigatePath: (String) -> Unit,
  onNewFolder: () -> Unit,
  onDelete: (FileItem) -> Unit,
  accentColor: Color,
  tag: String,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    modifier = modifier.fillMaxHeight().testTag(tag)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      // Pane Header with Path breadcrumbs and New Folder button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
          Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        Row {
          IconButton(onClick = onNewFolder, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Folder", tint = accentColor, modifier = Modifier.size(16.dp))
          }
          if (selectedFile != null) {
            IconButton(onClick = { onDelete(selectedFile) }, modifier = Modifier.size(26.dp)) {
              Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = StatusOffline, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      // Breadcrumb path display
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(Icons.Default.FolderOpen, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
          Text(
            text = currentPath,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // File Items List
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        items(files) { file ->
          val isSelected = selectedFile?.id == file.id
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onSelectFile(file)
                if (file.isDirectory) {
                  onNavigatePath(file.path)
                }
              }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = when {
                  file.isDirectory -> Icons.Filled.Folder
                  file.fileType == FileType.IMAGE -> Icons.Filled.Image
                  file.fileType == FileType.VIDEO -> Icons.Filled.Movie
                  file.fileType == FileType.AUDIO -> Icons.Filled.MusicNote
                  file.fileType == FileType.ARCHIVE -> Icons.Filled.Archive
                  file.fileType == FileType.APK -> Icons.Filled.Android
                  else -> Icons.Filled.Description
                },
                contentDescription = null,
                tint = if (file.isDirectory) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = file.name,
                  fontSize = 11.sp,
                  fontWeight = if (file.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1
                )
                Text(
                  text = if (file.isDirectory) "Folder • ${file.modifiedDate}" else "${file.sizeBytes / 1024} KB • ${file.modifiedDate}",
                  fontSize = 9.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }
  }
}
