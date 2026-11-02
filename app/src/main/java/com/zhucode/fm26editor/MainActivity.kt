package com.zhucode.fm26editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DbFile(val name: String, val size: Long)

data class ScanResult(
    val folder: String,
    val version: String?,
    val files: List<DbFile>,
    val hasConfig: Boolean,
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
            MaterialTheme {
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
        val datFiles = children
            .filter { it.isFile && it.name?.endsWith(".dat", ignoreCase = true) == true }
            .map { DbFile(it.name ?: "unknown.dat", it.length()) }
            .sortedBy { it.name.lowercase() }

        val config = children.firstOrNull {
            it.isFile && it.name.equals("config.xml", ignoreCase = true)
        }
        val configText = config?.let {
            contentResolver.openInputStream(it.uri)?.bufferedReader()?.use { reader -> reader.readText() }
        }.orEmpty()

        // Version is inferred only from known folder/config strings; this is not a binary DB parser.
        val version = Regex("db_archive_(\\d+)", RegexOption.IGNORE_CASE)
            .find(folder.name.orEmpty() + " " + configText)?.groupValues?.getOrNull(1)

        val warnings = buildList {
            if (datFiles.isEmpty()) add("Tidak ditemukan file .dat pada folder ini.")
            if (config == null) add("config.xml tidak ditemukan.")
            if (version != null && version != "2623") {
                add("Versi terdeteksi $version, sedangkan target aplikasi saat ini 2623.")
            }
            if (version == null) add("Versi database belum dapat dipastikan dari nama folder/config.xml.")
        }

        ScanResult(folder.name ?: "Database", version, datFiles, config != null, warnings)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FMM26 Database Editor") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Database tools", style = MaterialTheme.typography.headlineSmall)
            Text("Fondasi awal aplikasi untuk membuka dan memeriksa folder database FMM26.")
            Button(
                onClick = onChooseFolder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pilih folder database")
            }

            selectedUri?.let {
                Text("Folder telah dipilih. Tekan tombol untuk memindai file.")
                Button(
                    enabled = !scanning,
                    onClick = {
                        scanning = true
                        error = null
                        result = null
                        scope.launch {
                            try {
                                result = onScan(it)
                            } catch (t: Throwable) {
                                error = t.message ?: "Gagal membaca folder."
                            } finally {
                                scanning = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (scanning) "Memindai..." else "Pindai database")
                }
            }

            error?.let {
                Text("Kesalahan: $it", color = MaterialTheme.colorScheme.error)
            }

            result?.let { scan ->
                HorizontalDivider()
                Text("Hasil pemindaian", style = MaterialTheme.typography.titleLarge)
                Text("Folder: ${scan.folder}")
                Text("Versi terdeteksi: ${scan.version ?: "Belum diketahui"}")
                Text("Jumlah file .dat: ${scan.files.size}")
                Text("config.xml: ${if (scan.hasConfig) "Ada" else "Tidak ada"}")

                scan.warnings.forEach { warning ->
                    Text("Peringatan: $warning", color = MaterialTheme.colorScheme.error)
                }

                Text("Daftar file", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(scan.files) { file ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text(file.name, style = MaterialTheme.typography.bodyMedium)
                                Text("${file.size} byte", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Catatan: versi awal ini hanya memindai file. Fitur edit dan simpan .dat akan ditambahkan setelah format database 2623 tervalidasi.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
