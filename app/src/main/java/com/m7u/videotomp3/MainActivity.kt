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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
                    primary = Color(0xFF00E5FF), // سماوي مضيء
                    background = Color(0xFF0F0F0F), // أسود عميق
                    surface = Color(0xFF1A1A1A), // رمادي غامق للبطاقات
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                M7UAudioScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M7UAudioScreen() {
    val context = LocalContext.current
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isConverting by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("ڤیدیۆیەک هەڵبژێرە بۆ گۆڕین") }
    var mp3List by remember { mutableStateOf(listOf<File>()) }
    var showDownloads by remember { mutableStateOf(false) }

    fun refreshList() {
        mp3List = getConvertedMp3s(context)
    }

    LaunchedEffect(Unit) {
        refreshList()
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            statusText = "ڤیدیۆ هەڵبژێردرا. دەتوانیت ئێستا بیگۆڕیت  ."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // زر التنزيلات على اليسار (يطابق الصورة)
                navigationIcon = {
                    Row(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2A2A2A))
                            .clickable { 
                                showDownloads = !showDownloads
                                refreshList()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download),
                            contentDescription = "Downloads",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("التنزيلات", color = Color.White, fontSize = 14.sp)
                    }
                },
                title = { },
                // اليوزر والأيقونات على اليمين (يطابق الصورة)
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2A2A2A))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // أيقونة سناب شات
                        Icon(
                            painter = painterResource(id = R.drawable.ic_snapchat),
                            contentDescription = "Snapchat",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // أيقونة تلكرام
                        Icon(
                            painter = painterResource(id = R.drawable.ic_telegram),
                            contentDescription = "Telegram",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // أيقونة تيك توك
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tiktok),
                            contentDescription = "TikTok",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // اليوزر
                        Text(
                            text = "@m7u_h",
                            color = Color(0xFF00E5FF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F0F))
            )
        },
        containerColor = Color(0xFF0F0F0F)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showDownloads) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("داگرتنەکان", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { refreshList() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF00E5FF))
                    }
                }
                
                if (mp3List.isEmpty()) {
                    Text("هیچ فایلێکی گۆڕدراو نییە", color = Color.Gray, modifier = Modifier.padding(top = 50.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mp3List) { file ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.fromFile(file), "audio/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try { context.startActivity(intent) } catch (e: Exception) {
                                        Toast.makeText(context, "ناتوانرێت فایلەکە بکرێتەوە", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = "Music", tint = Color(0xFF00E5FF))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = file.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(40.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable { videoPicker.launch("video/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📁", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("کرتە بکە بۆ هەڵبژاردنی ڤیدیۆ", color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = statusText,
                    color = Color.Gray,
                    fontSize = 14.sp,
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
                                if (success) refreshList()
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isConverting,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isConverting) {
                            CircularProgressIndicator(color = Color.Black)
                        } else {
                            Text("گۆڕین بۆ MP3", fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun getConvertedMp3s(context: Context): List<File> {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val files = downloadsDir.listFiles { _, name -> name.startsWith("M7U_Audio_") && name.endsWith(".mp3") }
    return files?.sortedByDescending { it.lastModified() } ?: emptyList()
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
