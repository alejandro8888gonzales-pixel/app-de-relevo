package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Certificado
import com.example.data.Evidencia
import com.example.data.FileStorageHelper
import com.example.data.OcrResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceAndOcrScreen(viewModel: StudentsViewModel) {
    val evidenciasList by viewModel.evidencias.collectAsState()
    val certificadosList by viewModel.certificados.collectAsState()

    var activeTab by remember { mutableStateOf("Bóveda") } // "Bóveda", "OCR Inteligente", "Documentos"
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }
    
    // Dialog control
    var showEvidenceDialog by remember { mutableStateOf(false) }
    var showCertDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Sub tab bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val tabs = listOf("Bóveda", "Documentos de Usuario")
                    tabs.forEach { tab ->
                        val isSelected = activeTab == tab
                        TextButton(
                            onClick = { activeTab = tab },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isSelected) HondurasBluePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(tab, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .height(3.dp)
                                            .width(28.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(HondurasBluePrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Body
            when (activeTab) {
                "Bóveda" -> {
                    BovedaEvidenciasView(
                        evidencias = evidenciasList,
                        certificados = certificadosList,
                        onAddEvidence = { showEvidenceDialog = true },
                        onAddCertificado = { showCertDialog = true },
                        onDeleteEvidence = { viewModel.deleteEvidencia(it) },
                        onDeleteCertificado = { viewModel.deleteCertificado(it) },
                        onPreviewPhoto = { previewPhotoUri = it }
                    )
                }

                "Documentos de Usuario" -> {
                    DocumentosUsuarioView(viewModel)
                }
            }
        }

        if (showEvidenceDialog) {
            EvidenceFormDialog(
                onDismiss = { showEvidenceDialog = false },
                onSave = { ev ->
                    viewModel.createEvidencia(ev)
                    showEvidenceDialog = false
                }
            )
        }

        if (showCertDialog) {
            CertificadoFormDialog(
                ocrPresetResult = null,
                onDismiss = { showCertDialog = false },
                onSave = { c ->
                    viewModel.createCertificado(c)
                    showCertDialog = false
                }
            )
        }

        if (previewPhotoUri != null) {
            EvidenceDetailPreviewDialog(
                photoUri = previewPhotoUri!!,
                onDismiss = { previewPhotoUri = null }
            )
        }
    }
}

@Composable
fun BovedaEvidenciasView(
    evidencias: List<Evidencia>,
    certificados: List<Certificado>,
    onAddEvidence: () -> Unit,
    onAddCertificado: () -> Unit,
    onDeleteEvidence: (Evidencia) -> Unit,
    onDeleteCertificado: (Certificado) -> Unit,
    onPreviewPhoto: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Evidence Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Evidencias Digitales", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                Text("Fotos, archivos o enlaces catalogados.", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
            }
            Button(
                onClick = onAddEvidence,
                colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Subir", fontSize = 12.sp)
            }
        }

        // Image carousel list of evidence
        if (evidencias.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay fotos o evidencias visuales catalogadas.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)))
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(evidencias) { ev ->
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .clickable {
                                if (ev.tipoArchivo == "Imagen" && ev.rutaArchivo.isNotEmpty()) {
                                    onPreviewPhoto(ev.rutaArchivo)
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Actual image representation box matching beautiful colors
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background( HondurasCyanSkein.copy(alpha = 0.1f) ),
                                contentAlignment = Alignment.Center
                            ) {
                                val isLocal = ev.rutaArchivo.startsWith("/")
                                if (ev.tipoArchivo == "Imagen" && ev.rutaArchivo.isNotEmpty()) {
                                    AsyncImage(
                                        model = if (isLocal) File(ev.rutaArchivo) else ev.rutaArchivo,
                                        contentDescription = ev.titulo,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = when(ev.tipoArchivo) {
                                            "Imagen" -> Icons.Default.Image
                                            "Video" -> Icons.Default.Videocam
                                            "PDF" -> Icons.Default.PictureAsPdf
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = null,
                                        tint = HondurasCyanSkein,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(ev.titulo, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ev.etiqueta, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
                                IconButton(
                                    modifier = Modifier.size(24.dp),
                                    onClick = { onDeleteEvidence(ev) }
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Certificates / Diplomas list Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Certificados y Constancias", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                Text("Expedientes de diplomas acumulados.", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
            }
            Button(
                onClick = onAddCertificado,
                colors = ButtonDefaults.buttonColors(containerColor = HondurasBlueSecondary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Registrar", fontSize = 12.sp)
            }
        }

        // list
        if (certificados.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ningún certificado registrado.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)))
                }
            }
        } else {
            certificados.forEach { cert ->
                val hasBackup = cert.rutaArchivo.isNotEmpty()
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasBackup) { onPreviewPhoto(cert.rutaArchivo) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasBackup) Icons.Default.VerifiedUser else Icons.Default.CardMembership,
                            contentDescription = null,
                            tint = if (hasBackup) HondurasCyanSkein else GoldRank,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cert.titulo, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${cert.institucion} • ${cert.horas} horas", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
                            if (hasBackup) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = HondurasCyanSkein, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ver respaldo", style = MaterialTheme.typography.labelSmall.copy(color = HondurasCyanSkein, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                        IconButton(onClick = { onDeleteCertificado(cert) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = CoralAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OcrScannerView(viewModel: StudentsViewModel) {
    val ocrLoading by viewModel.ocrLoading.collectAsState()
    val ocrResult by viewModel.ocrResult.collectAsState()
    
    val context = LocalContext.current
    var activePresetText by remember { mutableStateOf("") }
    var showSaveCertDialog by remember { mutableStateOf(false) }

    // Camera temporary capture variables
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    var cameraTempFile by remember { mutableStateOf<File?>(null) }

    // Launchers for actual certificates
    val galleryOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri).use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        viewModel.runSmartOcr(bitmap)
                    } else {
                        Toast.makeText(context, "No se pudo descifrar la imagen.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch(e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraTempFile != null) {
            try {
                val bitmap = BitmapFactory.decodeFile(cameraTempFile?.absolutePath)
                if (bitmap != null) {
                    viewModel.runSmartOcr(bitmap)
                } else {
                    Toast.makeText(context, "No se pudo decodificar la foto capturada.", Toast.LENGTH_SHORT).show()
                }
            } catch(e: Exception) {
                Toast.makeText(context, "Error cámara: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            val res = FileStorageHelper.getTempImageUriForCamera(context)
            if (res != null) {
                cameraTempUri = res.uri
                cameraTempFile = res.file
                cameraOcrLauncher.launch(res.uri)
            }
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para el escaneo OCR.", Toast.LENGTH_LONG).show()
        }
    }

    // Mock certificates presets that simulate text scans
    val presets = listOf(
        OcrPreset("Cruz Roja Hondureña", "Diploma de Guardavidas y Primeros Auxilios", "40.0", "Tegucigalpa, Honduras", "Participación total certificada por capacitación."),
        OcrPreset("TECHO Honduras", "Constancia de Voluntariado Construcción", "15.0", "San Pedro Sula, Honduras", "Edificación escolar y apoyo de desastres comunitarios."),
        OcrPreset("UNAH", "Certificado de Conferencia Impacto Social", "5.0", "Honduras", "Seminario de Trabajo Educativo Social.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Escáner OCR Inteligente de Constancias", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
        Text(
            "Carga la foto de tu diploma real desde tu cámara o galería para realizar una extracción inteligente con Inteligencia Artificial, o selecciona un preset simulado.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        // Real capture action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    cameraPermissionOcrLauncher.launch(Manifest.permission.CAMERA)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cámara OCR", fontSize = 11.sp, maxLines = 1)
            }

            Button(
                onClick = {
                    galleryOcrLauncher.launch("image/*")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein)
            ) {
                Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Galería OCR", fontSize = 11.sp, maxLines = 1)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

        Text("Presets de Simulación Escolar", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))

        // Presets list
        presets.forEach { preset ->
            Card(
                border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        activePresetText = preset.institution
                        // Run simulated OCR preset
                        val fakeBitmap = BitmapFactory.decodeResource(context.resources, android.R.drawable.ic_menu_gallery)
                        if (fakeBitmap != null) {
                            viewModel.runSmartOcr(fakeBitmap)
                        } else {
                            // Empty bitmap helper fallback
                            val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                            viewModel.runSmartOcr(dummyBitmap)
                        }
                    }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = HondurasCyanSkein)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(preset.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Simular escaneo de ${preset.institution}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Loader
        if (ocrLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = HondurasBluePrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Analizando Diploma con IA...", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Result presentation card
        ocrResult?.let { res ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HondurasBlueLogo.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HondurasBlueTertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Texto Extraído Correctamente", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = HondurasBlueLogo))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Título: ${res.titulo}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold))
                    Text("Emisor: ${res.institucion}", style = MaterialTheme.typography.bodyMedium)
                    Text("Horas Asignadas: ${res.horas} hrs", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Fecha Detectada: ${res.fecha}", style = MaterialTheme.typography.bodyMedium)
                    Text("Notas: ${res.notas}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.clearOcr() }) {
                            Text("Limpiar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showSaveCertDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary)
                        ) {
                            Text("Importar al Sistema", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showSaveCertDialog && ocrResult != null) {
        CertificadoFormDialog(
            ocrPresetResult = ocrResult,
            onDismiss = { showSaveCertDialog = false },
            onSave = { c ->
                viewModel.createCertificado(c)
                viewModel.clearOcr()
                showSaveCertDialog = false
            }
        )
    }
}

data class OcrPreset(
    val institution: String,
    val title: String,
    val hours: String,
    val place: String,
    val details: String
)

@Composable
fun DocumentosUsuarioView(viewModel: StudentsViewModel) {
    val context = LocalContext.current
    val evidences by viewModel.evidencias.collectAsState()
    
    // Filter out images & videos, keeping user documents only
    val userDocs = remember(evidences) {
        evidences.filter { it.tipoArchivo != "Imagen" && it.tipoArchivo != "Video" }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDocForPreview by remember { mutableStateOf<Evidencia?>(null) }

    // Add Document State
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var inputTitle by remember { mutableStateOf("") }
    var inputNotes by remember { mutableStateOf("") }
    var inputTag by remember { mutableStateOf("TES") }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            // Resolve filename
            var resolvedName = ""
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        resolvedName = c.getString(nameIdx)
                    }
                }
            }
            if (resolvedName.isEmpty()) {
                resolvedName = "documento_${System.currentTimeMillis()}"
            }
            selectedFileName = resolvedName
            
            // Set default title
            if (inputTitle.isEmpty()) {
                inputTitle = resolvedName.substringBeforeLast(".")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HondurasBluePrimary.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HondurasBluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bandeja de Documentos de Usuario",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                        )
                        Text(
                            text = "Sube y organiza de forma centralizada todos tus archivos PDF, Word (doc/docx), hojas de cálculo, presentaciones o actas en tu base de datos local cifrada.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), lineHeight = 14.sp)
                        )
                    }
                }
            }

            // Quick trigger button
            Button(
                onClick = {
                    // Reset input states
                    selectedFileUri = null
                    selectedFileName = ""
                    inputTitle = ""
                    inputNotes = ""
                    inputTag = "TES"
                    showAddDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subir y Archivar Nuevo Documento 📤", fontWeight = FontWeight.Bold)
            }

            // List of documents
            if (userDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Aún no has archivado ningún archivo",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Presiona el botón de arriba para cargar tu primer oficio, TES o PDF de honor académico.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "Tus certificaciones y actas locales (${userDocs.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = HondurasBlueSecondary
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(userDocs) { doc ->
                        val dateString = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(doc.fechaRegistro))
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDocForPreview = doc },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Dynamic file icon based on type
                                val ext = doc.tipoArchivo.uppercase()
                                val iconTint = when (ext) {
                                    "PDF" -> Color(0xFFD32F2F)
                                    "DOCX", "DOC" -> Color(0xFF1976D2)
                                    "XLSX", "XLS" -> Color(0xFF388E3C)
                                    "TXT" -> Color(0xFF7B1FA2)
                                    else -> HondurasBlueSecondary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(iconTint.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (ext) {
                                            "PDF" -> Icons.Default.PictureAsPdf
                                            "TXT" -> Icons.Default.Article
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = "File Icon",
                                        tint = iconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.titulo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(HondurasCyanSkein.copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(doc.etiqueta, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                        Text(
                                            text = "$ext • $dateString",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add personal user document
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = HondurasBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Archivar Documento", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Selecciona cualquier archivo de tu almacenamiento (PDF, DOCX, DOC, etc.) para cargarlo automáticamente en la app.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Button(
                        onClick = { docPickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary.copy(alpha = 0.08f), contentColor = HondurasBluePrimary)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (selectedFileName.isEmpty()) "Seleccionar Archivo Físico" else "Cambiar Archivo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (selectedFileName.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HondurasCyanSkein.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, HondurasCyanSkein.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF388E3C), modifier = Modifier.size(16.dp))
                                Text(
                                    text = selectedFileName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("Título / Descripción corta *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = inputNotes,
                        onValueChange = { inputNotes = it },
                        label = { Text("Anotaciones / Notas opcionales") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text("🏷️ Categoría de Clasificación:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val docTags = listOf("TES", "Matrícula", "Actividades", "Certificado", "Contrato", "Otros")
                        docTags.forEach { tagItem ->
                            val isSelected = inputTag == tagItem
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) HondurasCyanSkein else HondurasBluePrimary.copy(alpha = 0.05f))
                                    .clickable { inputTag = tagItem }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(tagItem, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) HondurasBluePrimary else HondurasBlueSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputTitle.trim().isNotEmpty() && selectedFileUri != null) {
                            try {
                                val compressDocs = viewModel.compresionDocumentos.value == "true"
                                val copiedPath = com.example.data.FileStorageHelper.copyUriToLocal(
                                    context,
                                    selectedFileUri!!,
                                    "documentos_usuario",
                                    compress = compressDocs
                                )
                                var ext = selectedFileName.substringAfterLast(".", "doc").uppercase()
                                if (copiedPath != null && copiedPath.endsWith(".gz")) {
                                    ext = ext.removeSuffix(".GZ").removeSuffix(".gz")
                                    if (ext.isEmpty() || ext == "GZ") {
                                        ext = "DOCX"
                                    }
                                }
                                
                                if (copiedPath != null) {
                                    val newDoc = Evidencia(
                                        actividadId = null,
                                        titulo = inputTitle.trim(),
                                        tipoArchivo = ext,
                                        rutaArchivo = copiedPath,
                                        notas = if (compressDocs) "${inputNotes.trim()} (🗜️ Archivo Comprimido Sin Pérdida)" else inputNotes.trim(),
                                        fechaRegistro = System.currentTimeMillis(),
                                        etiqueta = inputTag
                                    )
                                    viewModel.createEvidencia(newDoc)
                                    showAddDialog = false
                                    Toast.makeText(context, "✅ ¡Documento guardado automáticamente en Documentos de Usuario!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No se pudo copiar el archivo localmente", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al copiar archivo local: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Por favor complete el título y seleccione un archivo", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary)
                ) {
                    Text("Archivar 💾", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = HondurasBlueSecondary)
                }
            }
        )
    }

    // Dialog: User document detail & actions (Share, view, delete)
    if (selectedDocForPreview != null) {
        val doc = selectedDocForPreview!!
        val isDiplomatico by viewModel.modoDiplomatico.collectAsState()
        val isDipl = isDiplomatico == "true"
        
        AlertDialog(
            onDismissRequest = { selectedDocForPreview = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (doc.tipoArchivo.uppercase() == "PDF") Icons.Default.PictureAsPdf else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = HondurasBluePrimary
                    )
                    Text(doc.titulo, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Información de Seguridad del Nodo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                            Text("🏷️ Clasificación: ${doc.etiqueta}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("📁 Ubicación Física: ${doc.rutaArchivo}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("🕒 Fecha Archivo: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(doc.fechaRegistro))}", fontSize = 9.sp)
                            Text("📄 Extensión: ${doc.tipoArchivo}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (doc.notas.isNotEmpty()) {
                        Text("📌 Notas / Anotaciones:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                        Text(doc.notas, fontSize = 12.sp, style = MaterialTheme.typography.bodyMedium)
                    }

                    // --- SECIÓN OJITO VISUAL (PREVISUALIZACIÓN INTEGRADA 👁️) ---
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Previsualización Rápida 👁️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                    
                    val pFile = java.io.File(doc.rutaArchivo)
                    if (pFile.exists()) {
                        val fileToPreview = if (pFile.name.endsWith(".gz")) {
                            com.example.data.FileStorageHelper.decompressIfNeeded(context, doc.rutaArchivo)
                        } else {
                            pFile
                        }
                        
                        val extUpper = doc.tipoArchivo.uppercase()
                        if (extUpper == "PDF") {
                            var pdfBitmaps by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
                            var pdfError by remember { mutableStateOf<String?>(null) }
                            
                            LaunchedEffect(fileToPreview) {
                                try {
                                    val list = mutableListOf<android.graphics.Bitmap>()
                                    val pfd = android.os.ParcelFileDescriptor.open(fileToPreview, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                                    val renderer = android.graphics.pdf.PdfRenderer(pfd)
                                    val pageCount = renderer.pageCount.coerceAtMost(4) // preview up to 4 pages maximum
                                    for (i in 0 until pageCount) {
                                        val page = renderer.openPage(i)
                                        val bitmap = android.graphics.Bitmap.createBitmap(page.width, page.height, android.graphics.Bitmap.Config.ARGB_8888)
                                        val canvas = android.graphics.Canvas(bitmap)
                                        canvas.drawColor(android.graphics.Color.WHITE)
                                        page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        list.add(bitmap)
                                        page.close()
                                    }
                                    renderer.close()
                                    pfd.close()
                                    pdfBitmaps = list
                                    if (list.isEmpty()) {
                                        pdfError = "El documento PDF no contiene páginas válidas."
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    pdfError = "Error en renderizado de PDF local."
                                }
                            }
                            
                            if (pdfError != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(pdfError!!, color = Color(0xFFC62828), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                                    }
                                }
                            } else if (pdfBitmaps.isNotEmpty()) {
                                Text("Páginas: ${pdfBitmaps.size} (Desliza para ver) 📄", fontSize = 10.sp, color = HondurasBlueSecondary, fontWeight = FontWeight.Bold)
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .background(Color.Black.copy(alpha = 0.05f))
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(pdfBitmaps.size) { index ->
                                        val bmp = pdfBitmaps[index]
                                        Card(
                                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxHeight().aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                                        ) {
                                            androidx.compose.foundation.Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Página PDF ${index + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = HondurasBluePrimary, modifier = Modifier.size(24.dp))
                                }
                            }
                        } else if (extUpper in listOf("DOCX", "DOC")) {
                            var docText by remember { mutableStateOf<String?>(null) }
                            LaunchedEffect(fileToPreview) {
                                docText = com.example.data.DocxTemplateParser.extractTextFromDocx(fileToPreview.absolutePath)
                            }
                            
                            if (docText != null) {
                                val cleanText = docText!!
                                if (cleanText.trim().isEmpty() || cleanText.contains("Error procesando")) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text(
                                            text = "Documento estructurado en formato Word. Presiona 'Abrir en el Dispositivo' para visualizarlo en Office / Google Docs de forma interactiva.",
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = cleanText,
                                                fontSize = 11.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                color = Color(0xFF2C3E50)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = HondurasBluePrimary, modifier = Modifier.size(24.dp))
                                }
                            }
                        } else if (extUpper == "TXT") {
                            var txtContent by remember { mutableStateOf("") }
                            LaunchedEffect(fileToPreview) {
                                txtContent = try { fileToPreview.readText() } catch(e: Exception) { "Error de lectura" }
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = txtContent,
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = HondurasBluePrimary.copy(alpha = 0.03f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = HondurasBlueSecondary, modifier = Modifier.size(16.dp))
                                    Text("Previsualización rápida no soportada para formato $extUpper. Puedes abrir el archivo directamente.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                    } else {
                        Text("⚠️ Archivo físico local no localizado en sandboxing de la app.", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Acciones Disponibles:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                    
                    // Share button
                    Button(
                        onClick = {
                            try {
                                val file = com.example.data.FileStorageHelper.getShareableFile(context, doc.rutaArchivo)
                                if (file != null && file.exists()) {
                                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = when (doc.tipoArchivo.uppercase()) {
                                            "PDF" -> "application/pdf"
                                            "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                            "DOC" -> "application/msword"
                                            "TXT" -> "text/plain"
                                            else -> "*/*"
                                        }
                                        putExtra(Intent.EXTRA_STREAM, fileUri)
                                        clipData = android.content.ClipData.newRawUri("", fileUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val chooser = Intent.createChooser(shareIntent, "Compartir Documento")
                                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.startActivity(chooser)
                                } else {
                                    Toast.makeText(context, "Archivo local no encontrado en el almacenamiento del dispositivo.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Fallo al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartir Archivo", fontWeight = FontWeight.Bold)
                    }

                    // Open / View button
                    Button(
                        onClick = {
                            try {
                                val file = com.example.data.FileStorageHelper.getShareableFile(context, doc.rutaArchivo)
                                if (file != null && file.exists()) {
                                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(fileUri, when (doc.tipoArchivo.uppercase()) {
                                            "PDF" -> "application/pdf"
                                            "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                            "DOC" -> "application/msword"
                                            "TXT" -> "text/plain"
                                            else -> "*/*"
                                        })
                                        clipData = android.content.ClipData.newRawUri("", fileUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(openIntent)
                                } else {
                                    Toast.makeText(context, "Archivo local no encontrado en el dispositivo.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ninguna aplicación disponible para abrir este tipo de archivo.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir en el Dispositivo", fontWeight = FontWeight.Bold)
                    }

                    // Download to public downloads folder button
                    Button(
                        onClick = {
                            val originalFile = java.io.File(doc.rutaArchivo)
                            if (originalFile.exists()) {
                                val nameToSave = if (doc.titulo.endsWith(".docx", ignoreCase = true) || doc.titulo.endsWith(".pdf", ignoreCase = true) || doc.titulo.endsWith(".doc", ignoreCase = true)) {
                                    doc.titulo
                                } else {
                                    "${doc.titulo}.${doc.tipoArchivo.lowercase()}"
                                }
                                val success = com.example.data.FileStorageHelper.downloadFileToPublicFolder(context, doc.rutaArchivo, nameToSave)
                                if (success) {
                                    Toast.makeText(context, "✅ ¡Descargado con éxito! Se guardó en la carpeta 'Descargas' del celular.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Error al descargar el archivo físicamente.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "El archivo local no existe.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasBlueSecondary, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Descargar y Guardar en Celular", fontWeight = FontWeight.Bold)
                    }

                    // Delete button
                    Button(
                        onClick = {
                            try {
                                val originalFile = java.io.File(doc.rutaArchivo)
                                if (originalFile.exists()) {
                                    originalFile.delete()
                                }
                                if (originalFile.name.endsWith(".gz")) {
                                    val decompressed = com.example.data.FileStorageHelper.decompressIfNeeded(context, doc.rutaArchivo)
                                    if (decompressed.exists()) {
                                        decompressed.delete()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            viewModel.deleteEvidencia(doc)
                            selectedDocForPreview = null
                            Toast.makeText(context, "✅ Archivo purgado físicamente del almacenamiento de la app.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eliminar de Forma Irreversible", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedDocForPreview = null }) {
                    Text("Cerrar", color = HondurasBlueSecondary)
                }
            }
        )
    }
}

// Dialog entry configurations
@Composable
fun EvidenceFormDialog(
    onDismiss: () -> Unit,
    onSave: (Evidencia) -> Unit
) {
    val context = LocalContext.current
    var titulo by remember { mutableStateOf("") }
    var tipoArchivo by remember { mutableStateOf("Imagen") }
    var notas by remember { mutableStateOf("") }
    var etiqueta by remember { mutableStateOf("Junio 2026") }

    // Private selected file states
    var selectedLocalPath by remember { mutableStateOf<String?>(null) }
    var selectedUriName by remember { mutableStateOf<String?>(null) }

    // Camera capture temporary storage Uri
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    var cameraTempFile by remember { mutableStateOf<File?>(null) }
    var formPhotoPreviewUri by remember { mutableStateOf<String?>(null) }

    // Activity launchers for safe copying
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = FileStorageHelper.copyUriToLocal(context, uri, "evidencias")
            if (localPath != null) {
                selectedLocalPath = localPath
                selectedUriName = uri.lastPathSegment ?: "Archivo Cargado"
            } else {
                Toast.makeText(context, "Error al guardar el archivo de la galería.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraTempFile != null) {
            val finalTarget = File(File(context.filesDir, "evidencias"), "cam_${System.currentTimeMillis()}.jpg").apply {
                parentFile?.mkdirs()
            }
            try {
                cameraTempFile?.copyTo(finalTarget, overwrite = true)
                selectedLocalPath = finalTarget.absolutePath
                selectedUriName = "Foto_${System.currentTimeMillis()}.jpg"
            } catch(e: Exception) {
                Toast.makeText(context, "Fallo al copiar imagen del sensor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            val res = FileStorageHelper.getTempImageUriForCamera(context)
            if (res != null) {
                cameraTempUri = res.uri
                cameraTempFile = res.file
                cameraLauncher.launch(res.uri)
            }
        } else {
            Toast.makeText(context, "Permiso de cámara rechazado. Use la galería.", Toast.LENGTH_LONG).show()
        }
    }

    val fileTypes = listOf("Imagen", "Video", "PDF", "DOCX", "TXT")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Nueva Evidencia", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Nombre o Descripción de la Evidencia *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Formato de Archivo *", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    fileTypes.forEach { type ->
                        val isSel = tipoArchivo == type
                        FilterChip(
                            selected = isSel,
                            onClick = { tipoArchivo = type },
                            label = { Text(type) }
                        )
                    }
                }

                // File Attachment / Capture Picker Row
                Text("Selector de Origen del Archivo *", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (tipoArchivo == "Imagen") {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                Toast.makeText(context, "La cámara solo admite capturar imágenes.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary),
                        enabled = (tipoArchivo == "Imagen")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cámara", fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            val mime = when(tipoArchivo) {
                                "Imagen" -> "image/*"
                                "Video" -> "video/*"
                                "PDF" -> "application/pdf"
                                else -> "*/*"
                            }
                            galleryLauncher.launch(mime)
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein)
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galería / Archivos", fontSize = 11.sp, maxLines = 1)
                    }
                }

                // Attached state and thumbnail preview!
                if (selectedLocalPath != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = HondurasBlueLogo.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = HondurasBlueTertiary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedUriName ?: "Archivo enlazado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HondurasBlueLogo,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Live thumbnail preview in dialog if Image
                            if (tipoArchivo == "Imagen") {
                                Spacer(modifier = Modifier.height(10.dp))
                                AsyncImage(
                                    model = File(selectedLocalPath!!),
                                    contentDescription = "Vista previa",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(2.dp, HondurasCyanSkein, RoundedCornerShape(8.dp))
                                        .clickable { formPhotoPreviewUri = selectedLocalPath }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = etiqueta,
                    onValueChange = { etiqueta = it },
                    label = { Text("Etiqueta Organizadora (Año, Mes, Actividad) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Anotaciones y Observaciones del Alumno") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isNotEmpty() && selectedLocalPath != null) {
                        onSave(
                            Evidencia(
                                id = 0L,
                                actividadId = null,
                                titulo = titulo,
                                tipoArchivo = tipoArchivo,
                                rutaArchivo = selectedLocalPath!!,
                                notas = notas,
                                fechaRegistro = System.currentTimeMillis(),
                                etiqueta = etiqueta,
                                esFavorito = false
                            )
                        )
                    } else if (selectedLocalPath == null) {
                        Toast.makeText(context, "Debe seleccionar o capturar un archivo de evidencia.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "El título es obligatorio.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary)
            ) {
                Text("Cargar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (formPhotoPreviewUri != null) {
        EvidenceDetailPreviewDialog(
            photoUri = formPhotoPreviewUri!!,
            onDismiss = { formPhotoPreviewUri = null }
        )
    }
}

@Composable
fun CertificadoFormDialog(
    ocrPresetResult: OcrResult?,
    onDismiss: () -> Unit,
    onSave: (Certificado) -> Unit
) {
    val context = LocalContext.current
    var titulo by remember { mutableStateOf(ocrPresetResult?.titulo ?: "") }
    var institucion by remember { mutableStateOf(ocrPresetResult?.institucion ?: "") }
    var horas by remember { mutableStateOf(ocrPresetResult?.horas?.toString() ?: "10.0") }
    var notas by remember { mutableStateOf(ocrPresetResult?.notas ?: "") }
    var fechaText by remember {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val defaultFecha = format.format(Date())
        mutableStateOf(ocrPresetResult?.fecha ?: defaultFecha)
    }

    val calendar = remember { java.util.Calendar.getInstance() }
    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, yr, mo, dy ->
                fechaText = String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d", dy, mo + 1, yr)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    // Attachment tracking
    var selectedLocalPath by remember { mutableStateOf<String?>(null) }
    var selectedUriName by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = FileStorageHelper.copyUriToLocal(context, uri, "certificados")
            if (localPath != null) {
                selectedLocalPath = localPath
                selectedUriName = uri.lastPathSegment ?: "Certificado cargado"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ocrPresetResult == null) "Registrar Certificado" else "Guardar Extracción OCR", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título de Diploma o Taller *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = institucion,
                    onValueChange = { institucion = it },
                    label = { Text("Institución Emisora *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = horas,
                        onValueChange = { horas = it },
                        label = { Text("Horas Certificadas *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fecha Emisión", fontSize = 10.sp, color = HondurasCyanSkein, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            color = Color.White.copy(alpha = 0.02f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { datePickerDialog.show() },
                            tonalElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = HondurasCyanSkein, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(fechaText, fontSize = 14.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // File Attachment UI for backup verification
                Text("Cargar Copia de Respaldo", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Subir Imagen/PDF", fontSize = 11.sp)
                    }
                    if (selectedLocalPath != null) {
                        Text(
                            text = selectedUriName ?: "Cargado",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Anotaciones de Certificado") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isNotEmpty() && institucion.isNotEmpty()) {
                        val parsedDate = try {
                            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            format.parse(fechaText)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        onSave(
                            Certificado(
                                id = 0L,
                                titulo = titulo,
                                institucion = institucion,
                                fecha = parsedDate,
                                horas = horas.toDoubleOrNull() ?: 10.0,
                                rutaArchivo = selectedLocalPath ?: ("content://media/external/images/" + System.currentTimeMillis()),
                                notas = notas,
                                textoExtraido = ocrPresetResult?.notas ?: "Cargado o extraído con éxito."
                            )
                        )
                    } else {
                        Toast.makeText(context, "Rellene los campos obligatorios.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary)
            ) {
                Text("Guardar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
