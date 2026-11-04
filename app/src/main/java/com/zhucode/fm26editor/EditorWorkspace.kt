package com.zhucode.fm26editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val EditorGold = Color(0xFFFFD36A)
private val EditorPanel = Color(0xFF211638)
private val EditorText = Color(0xFFE8DFF5)

private enum class EditorModule(val title: String) {
    PLAYERS("Pemain"), CLUBS("Klub"), TRANSFERS("Transfer"), COMPETITIONS("Kompetisi")
}

@Composable
fun EditorWorkspace() {
    var module by remember { mutableStateOf(EditorModule.PLAYERS) }
    var query by remember { mutableStateOf("") }
    var selectedRecord by remember { mutableStateOf("Pilih record database") }
    var fullName by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var uid by remember { mutableStateOf("") }
    var currentClub by remember { mutableStateOf("") }
    var targetClub by remember { mutableStateOf("") }
    var ca by remember { mutableStateOf("100") }
    var pa by remember { mutableStateOf("150") }
    var reputation by remember { mutableStateOf("100") }
    var level by remember { mutableStateOf("1") }
    var status by remember { mutableStateOf("Professional") }
    var feedback by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = EditorPanel),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("EDITOR WORKSPACE", color = EditorGold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("Pilih modul lalu ubah field record yang ingin diedit.", color = EditorText)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EditorModule.entries.forEach { item ->
                    FilterChip(
                        selected = module == item,
                        onClick = { module = item; feedback = null },
                        label = { Text(item.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF60349A),
                            selectedLabelColor = Color.White,
                            labelColor = EditorText
                        )
                    )
                }
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cari nama / UID / ID") },
                singleLine = true
            )
            OutlinedTextField(
                value = selectedRecord, onValueChange = { selectedRecord = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Record terpilih / ID") },
                singleLine = true
            )
            when (module) {
                EditorModule.PLAYERS -> {
                    Text("PROFIL PEMAIN", color = EditorGold, fontWeight = FontWeight.Bold)
                    EditorField("Nama pemain", fullName, { fullName = it })
                    EditorField("UID pemain", uid, { uid = it }, numeric = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EditorField("CA", ca, { ca = it }, Modifier.weight(1f), true)
                        EditorField("PA", pa, { pa = it }, Modifier.weight(1f), true)
                    }
                    EditorField("Reputasi", reputation, { reputation = it }, numeric = true)
                    Text("ATRIBUT TEKNIS", color = EditorGold, fontWeight = FontWeight.Bold)
                    Text("Editor atribut lengkap (teknik, mental, fisik, kiper, posisi) disiapkan pada panel detail record.", color = EditorText)
                }
                EditorModule.CLUBS -> {
                    Text("PROFIL KLUB", color = EditorGold, fontWeight = FontWeight.Bold)
                    EditorField("Nama klub", fullName, { fullName = it })
                    EditorField("Nama pendek", shortName, { shortName = it })
                    EditorField("UID klub", uid, { uid = it }, numeric = true)
                    EditorField("Reputasi klub", reputation, { reputation = it }, numeric = true)
                    EditorField("Level fasilitas / akademi", level, { level = it }, numeric = true)
                    OutlinedTextField(value = status, onValueChange = { status = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Status klub") })
                }
                EditorModule.TRANSFERS -> {
                    Text("TRANSFER PEMAIN", color = EditorGold, fontWeight = FontWeight.Bold)
                    EditorField("UID pemain", uid, { uid = it }, numeric = true)
                    EditorField("Klub asal (ID)", currentClub, { currentClub = it }, numeric = true)
                    EditorField("Klub tujuan (ID)", targetClub, { targetClub = it }, numeric = true)
                    Text("Transfer mengubah relasi klub pada record person, bukan hanya mengganti teks klub.", color = EditorText)
                }
                EditorModule.COMPETITIONS -> {
                    Text("PROFIL KOMPETISI", color = EditorGold, fontWeight = FontWeight.Bold)
                    EditorField("Nama kompetisi", fullName, { fullName = it })
                    EditorField("Nama pendek", shortName, { shortName = it })
                    EditorField("ID kompetisi", uid, { uid = it }, numeric = true)
                    EditorField("Reputasi (0–200)", reputation, { reputation = it }, numeric = true)
                    EditorField("Level kompetisi", level, { level = it }, numeric = true)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    fullName = ""; shortName = ""; uid = ""; currentClub = ""; targetClub = ""
                    ca = "100"; pa = "150"; reputation = "100"; level = "1"; feedback = "Form dikosongkan."
                }, modifier = Modifier.weight(1f)) { Text("Reset") }
                Button(
                    onClick = { feedback = "Perubahan form siap diterapkan setelah record database dimuat oleh parser." },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF713BB1))
                ) { Text("Terapkan edit") }
            }
            feedback?.let { Text(it, color = EditorGold, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    numeric: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text)
    )
}
