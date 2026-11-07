package com.example.ui.screens.recordings

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.storage.FileManager
import com.example.storage.TwinGuardDatabase
import com.example.storage.entities.RecordingEntity
import com.example.ui.components.CctvTopAppBar
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.StatusLiveRed
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { TwinGuardDatabase.getDatabase(context) }
    val recordings by db.recordingsDao().getAllRecordings().collectAsState(initial = emptyList())

    var activePlayingRecording by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .testTag("screen_recordings")
    ) {
        CctvTopAppBar(
            title = "Recordings",
            subtitle = "${recordings.size} local MP4 video clips",
            onBack = onBack
        )

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(DarkNavyCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = TextMutedSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No CCTV Recordings Found",
                        color = TextWhitePrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recorded MP4 clips from Camera Mode or Monitor Mode will appear here.",
                        color = TextMutedSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recordings, key = { it.id }) { item ->
                    RecordingListItem(
                        recording = item,
                        onPlay = { activePlayingRecording = item },
                        onDelete = { recordingToDelete = item }
                    )
                }
            }
        }
    }

    // Video Player Dialog
    if (activePlayingRecording != null) {
        val file = File(activePlayingRecording!!.filePath)
        AlertDialog(
            onDismissRequest = { activePlayingRecording = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activePlayingRecording!!.title,
                        color = TextWhitePrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { activePlayingRecording = null },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhitePrimary)
                    }
                }
            },
            text = {
                Column {
                    if (file.exists()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoPath(file.absolutePath)
                                        val mc = MediaController(ctx)
                                        mc.setAnchorView(this)
                                        setMediaController(mc)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Text("Video file not found at: ${file.absolutePath}", color = StatusLiveRed)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Saved on: ${if (activePlayingRecording!!.isSavedOnCamera) "Camera Node" else "Monitor Node"}",
                        color = TextMutedSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Size: ${FileManager.formatBytes(activePlayingRecording!!.fileSizeBytes)}",
                        color = CyanNeon,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { activePlayingRecording = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkNavyBackground)
                ) {
                    Text("Close Player")
                }
            },
            containerColor = DarkNavySurface
        )
    }

    // Delete Confirmation Dialog
    if (recordingToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("Delete Recording?", color = TextWhitePrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${recordingToDelete!!.title}? This action is irreversible.", color = TextMutedSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val item = recordingToDelete!!
                        recordingToDelete = null
                        scope.launch(Dispatchers.IO) {
                            try {
                                File(item.filePath).delete()
                            } catch (e: Exception) {}
                            db.recordingsDao().deleteRecording(item)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusLiveRed, contentColor = Color.White)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) {
                    Text("Cancel", color = CyanNeon)
                }
            },
            containerColor = DarkNavySurface
        )
    }
}

@Composable
private fun RecordingListItem(
    recording: RecordingEntity,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.US).format(Date(recording.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkNavyCard)
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onPlay)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (recording.isMotionTriggered) StatusLiveRed.copy(alpha = 0.2f) else DarkNavySurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (recording.isMotionTriggered) Icons.Default.MotionPhotosOn else Icons.Default.Videocam,
                    contentDescription = null,
                    tint = if (recording.isMotionTriggered) StatusLiveRed else CyanNeon,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recording.title,
                        color = TextWhitePrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (recording.isMotionTriggered) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StatusLiveRed)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("MOTION", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = dateStr,
                    color = TextMutedSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = FileManager.formatBytes(recording.fileSizeBytes),
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${if (recording.isSavedOnCamera) "Camera Phone" else "Monitor Phone"}",
                        color = TextMutedSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPlay, modifier = Modifier.testTag("btn_play_recording_${recording.id}")) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = CyanNeon)
            }
            IconButton(onClick = onDelete, modifier = Modifier.testTag("btn_delete_recording_${recording.id}")) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMutedSecondary)
            }
        }
    }
}
