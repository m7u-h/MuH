package com.m7u.videotomp3

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFBB86FC),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                VideoToMp3Screen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoToMp3Screen() {
    val context = LocalContext.current
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isConverting by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("ڤیدیۆیەک هەڵبژێرە بۆ گۆڕین") }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            statusText = "ڤیدیۆ هەڵبژێردرا. ئێستا کرتە لە گۆڕین بکە."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("گۆڕەری ڤیدیۆ بۆ MP3", color = Color.White) },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@m7u_h",
                            color = Color(0xFFBB86FC),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(text = "💬", fontSize = 20.sp) // واتساب
                        Text(text = "✈️", fontSize = 20.sp) // تليكرام
                        Text(text = "🎵", fontSize = 20.sp) // تيك توك
                        Text(text = "👻", fontSize = 20.sp) // سناب شات
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { videoPicker.launch("video/*") },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("هەڵبژاردنی ڤیدیۆ", fontSize = 18.sp, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = statusText,
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedVideoUri != null) {
                Button(
                    onClick = {
                        isConverting = true
                        statusText = "خەریکی گۆڕینە... تکایە چاوەڕێ بکە"
                        convertVideoToMp3(context, selectedVideoUri!!) { success, message ->
                            isConverting = false
                            statusText = message
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isConverting,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isConverting) {
                        CircularProgressIndicator(color = Color.Black)
                    } else {
                        Text("گۆڕین بۆ MP3", fontSize = 18.sp, color = Color.Black)
                    }
                }
            }
        }
    }
}

fun convertVideoToMp3(context: Context, videoUri: Uri, onResult: (Boolean, String) -> Unit) {
    try {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, videoUri, null)
        
        var audioTrackIndex = -1
        var format: MediaFormat? = null
        
        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                audioTrackIndex = i
                format = trackFormat
                break
            }
        }

        if (audioTrackIndex == -1 || format == null) {
            onResult(false, "ئەم ڤیدیۆیە دەنگی تێدا نییە!")
            return
        }

        extractor.selectTrack(audioTrackIndex)

        val tempFile = File(context.cacheDir, "temp_audio.mp3")
        val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val muxerTrackIndex = muxer.addTrack(format)
        muxer.start()

        val bufferSize = 1024 * 1024
        val buffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break
            
            bufferInfo.presentationTimeUs = extractor.sampleTime
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
            muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
            extractor.advance()
        }

        muxer.stop()
        muxer.release()
        extractor.release()

        val fileName = "M7U_Audio_${System.currentTimeMillis()}.mp3"
        saveToDownloads(context, tempFile, fileName)
        
        onResult(true, "بە سەرکەوتوویی گۆڕدرا بۆ: $fileName")

    } catch (e: Exception) {
        e.printStackTrace()
        onResult(false, "هەڵەیەک ڕوویدا: ${e.message}")
    }
}

fun saveToDownloads(context: Context, sourceFile: File, fileName: String) {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

    if (uri != null) {
        val outputStream = resolver.openOutputStream(uri)
        val inputStream = FileInputStream(sourceFile)
        inputStream.copyTo(outputStream!!)
        inputStream.close()
        outputStream.close()
        sourceFile.delete()
    }
}
