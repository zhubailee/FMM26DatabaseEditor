package com.zhucode.fm26editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF100B20)
private val Panel = Color(0xFF211638)
private val Violet = Color(0xFF8B35D8)
private val Gold = Color(0xFFFFD36A)
private val Soft = Color(0xFFE8DFF5)

data class DbFile(val name: String, val size: Long)
data class ScanResult(
    val folder: String,
    val version: String?,
    val files: List<DbFile>,
    val hasConfig: Boolean,
    val referencedFiles: List<String>,
    val missingReferencedFiles: List<String>,
    val warnings: List<String>
)

class MainActivity : ComponentActivity() {
    private var selectedUri by mutableStateOf<Uri?>(null)

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            selectedUri = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Violet,
                    secondary = Gold,
                    background = Ink,
                    surface = Panel,
                    onBackground = Soft,
                    onSurface = Soft
                )
            ) {
                EditorHome(
                    selectedUri = selectedUri,
                    onChooseFolder = { folderPicker.launch(null) },
                    onScan = { uri -> scanDatabase(uri) }
                )
            }
        }
    }

    private suspend fun scanDatabase(uri: Uri): ScanResult = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(this@MainActivity, uri)
            ?: error("Folder tidak dapat dibuka")
        require(folder.isDirectory) { "Lokasi yang dipilih bukan folder" }

        val children = folder.listFiles()
        val config = children.firstOrNull {
            it.isFile && it.name.equals("config.xml", ignoreCase = true)
        }
        val configText = config?.let {
            contentResolver.openInputStream(it.uri)?.bufferedReader()?.use { reader -> reader.readText() }
        }.orEmpty()

        val version = Regex("db_archive_(\\d+)", RegexOption.IGNORE_CASE)
            .find(folder.name.orEmpty() + " " + configText)?.groupValues?.getOrNull(1)

        val referencedPaths = Regex("""from="([^"]+)"\s+to="([^"]+)"""")
            .findAll(configText)
            .map { it.groupValues[2].trim('/') }
            .toList()

        fun findRelative(path: String): DocumentFile? {
            var current: DocumentFile = folder
            for (segment in path.split('/').filter { it.isNotBlank() }) {
                current = current.findFile(segment) ?: return null
            }
            return current
        }

        val missingPaths = referencedPaths.filter { findRelative(it) == null }
        val datFiles = buildList {
            fun collect(dir: DocumentFile, prefix: String) {
                dir.listFiles().forEach { item ->
                    val name = item.name ?: return@forEach
                    val relative = if (prefix.isEmpty()) name else "$prefix/$name"
                    if (item.isDirectory) collect(item, relative)
                    else if (name.endsWith(".dat", ignoreCase = true)) {
                        add(DbFile(relative, item.length()))
                    }
                }
            }
            collect(folder, "")
        }.sortedBy { it.name.lowercase() }

        val warnings = buildList {
            if (datFiles.isEmpty()) add("Tidak ditemukan file .dat pada folder ini.")
            if (config == null) add("config.xml tidak ditemukan.")
            if (missingPaths.isNotEmpty()) add("File yang dirujuk config.xml belum ditemukan: " + missingPaths.joinToString())
            if (version != null && version != "2603") {
                add("Versi terdeteksi $version. Aplikasi ini ditargetkan untuk database 2603.")
            }
            if (version == null) add("Versi database belum dapat dipastikan dari nama folder/config.xml.")
        }
        ScanResult(
            folder.name ?: "Database",
            version,
            datFiles,
            config != null,
            referencedPaths,
            missingPaths,
            warnings
        )
    }
}

@Composable
private fun EditorHome(
    selectedUri: Uri?,
    onChooseFolder: () -> Unit,
    onScan: suspend (Uri) -> ScanResult
) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<ScanResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }

    Scaffold(containerColor = Ink) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF492078), Color(0xFF171026), Color(0xFF2B1745))),
                            RoundedCornerShape(26.dp)
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♛", color = Gold, fontSize = 30.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("ZHU CODE", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text("FMM26 DATABASE", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                            Text("EDITOR", color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text("EDIT  •  MANAGE  •  ENHANCE", color = Soft, fontSize = 11.sp, letterSpacing = 1.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag("DATABASE 2603")
                        Tag("ANDROID")
                        Tag("OFFLINE READY*")
                    }
                }
            }
            item {
                Text("Database workspace", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Pilih folder database FMM26 Mobile untuk memeriksa struktur file.", color = Soft)
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("01  •  SOURCE DATABASE", color = Gold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(selectedUri?.lastPathSegment ?: "Belum ada folder yang dipilih", color = Color.White)
                        Button(
                            onClick = onChooseFolder,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Violet)
                        ) { Text("Pilih folder database") }
                        if (selectedUri != null) {
                            OutlinedButton(
                                enabled = !scanning,
                                onClick = {
                                    scanning = true
                                    error = null
                                    result = null
                                    scope.launch {
                                        try { result = onScan(selectedUri) }
                                        catch (t: Throwable) { error = t.message ?: "Gagal membaca folder." }
                                        finally { scanning = false }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
                            ) { Text(if (scanning) "Memindai..." else "Pindai database") }
                        }
                    }
                }
            }
            error?.let { message ->
                item { Text("Kesalahan: $message", color = Color(0xFFFF8A8A)) }
            }
            result?.let { scan ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("02  •  SCAN REPORT", color = Gold, fontWeight = FontWeight.Bold)
                            Text(scan.folder, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Versi: ${scan.version ?: "Belum diketahui"}")
                            Text("File DAT ditemukan: ${scan.files.size}  |  config.xml: ${if (scan.hasConfig) "Ada" else "Tidak ada"}")
                            Text("Referensi DAT di config: ${scan.referencedFiles.size}  |  Belum ditemukan: ${scan.missingReferencedFiles.size}")
                            scan.warnings.forEach { Text("• $it", color = Color(0xFFFFC477), style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                item { Text("Database files", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(scan.files) { file ->
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(file.name, color = Color.White, modifier = Modifier.weight(1f))
                            Text(formatBytes(file.size), color = Gold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Text(
                    "*Mode offline untuk pemindaian lokal. Saat ini aplikasi baru membaca daftar file dan config.xml; pengeditan serta penulisan file DAT belum tersedia.",
                    color = Color(0xFFB9AFC8),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun Tag(label: String) {
    Surface(color = Color(0x553F2361), shape = RoundedCornerShape(50)) {
        Text(label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
