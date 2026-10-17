package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.GaleriaMedia
import com.example.ui.theme.HondurasBlueLogo
import com.example.ui.theme.HondurasBluePrimary
import com.example.ui.theme.HondurasBlueSecondary
import com.example.ui.theme.HondurasCyanSkein
import com.example.ui.theme.diplomatic
import com.example.ui.viewmodel.StudentsViewModel
import java.io.File
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaGalleryScreen(viewModel: StudentsViewModel) {
    val context = LocalContext.current
    val mediaItems by viewModel.mediaFiles.collectAsState()
    val securityPin by viewModel.securityPin.collectAsState()
    val isDiplomatico by viewModel.modoDiplomatico.collectAsState()
    val isDipl = isDiplomatico == "true"

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var inputTitle by remember { mutableStateOf("") }
    var mediaTypeSelected by remember { mutableStateOf("IMAGEN") } // "IMAGEN" or "VIDEO"

    // Custom date selection state initialized with today's values
    val todayCal = remember { Calendar.getInstance() }
    var inputDay by remember { mutableStateOf(todayCal.get(Calendar.DAY_OF_MONTH).toString()) }
    var inputMonth by remember { mutableStateOf((todayCal.get(Calendar.MONTH) + 1).toString()) }
    var inputYear by remember { mutableStateOf(todayCal.get(Calendar.YEAR).toString()) }

    // Bulk upload state variables
    var showAddMultipleDialog by remember { mutableStateOf(false) }
    var selectedUrisList by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var inputSupervisor by remember { mutableStateOf("") }
    var inputRealizado by remember { mutableStateOf(true) }

    // New gallery details fields
    var inputHoraEntrada by remember { mutableStateOf("08:00") }
    var inputHoraSalida by remember { mutableStateOf("12:00") }
    var inputLugar by remember { mutableStateOf("") }
    var inputUbicacionDetalle by remember { mutableStateOf("") }

    // Dialog state for viewing full media
    var zoomedMedia by remember { mutableStateOf<GaleriaMedia?>(null) }
    var mediaEditTarget by remember { mutableStateOf<GaleriaMedia?>(null) }

    // State for secure password/PIN delete verification
    var mediaToDelete by remember { mutableStateOf<GaleriaMedia?>(null) }
    var enteredPinDelete by remember { mutableStateOf("") }
    var pinDeleteError by remember { mutableStateOf("") }

    var selectedFilterTag by remember { mutableStateOf("Todos") }
    var selectedMediaBlock by remember { mutableStateOf<List<GaleriaMedia>?>(null) }
    var mediaBlockToDelete by remember { mutableStateOf<List<GaleriaMedia>?>(null) }

    val groupMediaUploadsConfig by viewModel.groupMediaUploads.collectAsState()
    val groupMultipleUploads = groupMediaUploadsConfig == "true"

    var selectedTagForNewMedia by remember { mutableStateOf("Otros") }
    var showActionChooser by remember { mutableStateOf(false) }

    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Activity launchers for selecting assets
    val multipleImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedUrisList = uris
            mediaTypeSelected = "IMAGEN"
            showAddMultipleDialog = true
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            mediaTypeSelected = "IMAGEN"
            showAddDialog = true
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            mediaTypeSelected = "VIDEO"
            showAddDialog = true
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            selectedUri = tempCameraUri
            mediaTypeSelected = "IMAGEN"
            showAddDialog = true
        }
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && tempCameraUri != null) {
            selectedUri = tempCameraUri
            mediaTypeSelected = "VIDEO"
            showAddDialog = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permiso de cámara concedido. Por favor presiona capturar de nuevo.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para tomar fotos y grabar vídeos.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Galería de Medios".diplomatic(isDipl),
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = "Gallery Header Icon",
                        tint = Color.White,
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = HondurasBlueLogo
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showActionChooser = true },
                containerColor = HondurasCyanSkein,
                contentColor = HondurasBluePrimary,
                modifier = Modifier.testTag("add_evidence_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = "Add Evidence")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registrar Evidencia", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->

        // Calculate storage benefits
        val totalCount = mediaItems.size
        val totalOriginalBytes = mediaItems.sumOf { it.pesoOriginalBytes }
        val totalCompressedBytes = mediaItems.sumOf { it.pesoComprimidoBytes }
        val finalSavingsMB = (totalOriginalBytes - totalCompressedBytes).toFloat() / (1024 * 1024)
        val savingPercent = if (totalOriginalBytes > 0) {
            ((totalOriginalBytes - totalCompressedBytes).toFloat() / totalOriginalBytes.toFloat() * 100).toInt()
        } else {
            0
        }

        val filteredMediaItems = if (selectedFilterTag == "Todos") {
            mediaItems
        } else {
            mediaItems.filter { it.etiqueta == selectedFilterTag }
        }

        val groupedByDate = remember(filteredMediaItems) {
            val sortedList = filteredMediaItems.sortedWith(Comparator { a, b ->
                b.fechaCreacion.compareTo(a.fechaCreacion)
            })
            sortedList.groupBy { Pair(it.anio, it.mes) }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 90.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Storage Health/Compression Savings Header Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .testTag("compression_stats_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = HondurasBluePrimary.copy(alpha = 0.05f)
                    ),
                    border = BorderStroke(1.dp, HondurasCyanSkein.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background( HondurasCyanSkein.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflineBolt,
                                    contentDescription = "Savings Icon",
                                    tint = HondurasCyanSkein
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Rendimiento del Almacenamiento",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = HondurasBluePrimary
                                )
                                Text(
                                    text = "Compresión inteligente para tu Trabajo Social",
                                    fontSize = 11.sp,
                                    color = HondurasBlueSecondary.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = HondurasCyanSkein.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Archivos", fontSize = 10.sp, color = HondurasBlueSecondary)
                                Text("$totalCount", fontSize = 18.sp, fontWeight = FontWeight.Black, color = HondurasBluePrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Peso Original", fontSize = 10.sp, color = HondurasBlueSecondary)
                                Text(
                                    text = formatBytes(totalOriginalBytes),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HondurasBluePrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Guardado Local", fontSize = 10.sp, color = HondurasBlueSecondary)
                                Text(
                                    text = formatBytes(totalCompressedBytes),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HondurasCyanSkein
                                )
                            }
                        }

                        if (totalOriginalBytes > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HondurasCyanSkein.copy(alpha = 0.2f))
                                    .padding(vertical = 4.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "📉 Ahorro total de ${String.format("%.2f", finalSavingsMB)} MB ($savingPercent% menos de peso en el teléfono)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HondurasBlueLogo
                                )
                            }
                        }
                    }
                }
            }

            // 🏷️ Category Filter Chips
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Filtrar por Etiqueta:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HondurasBluePrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tags = listOf("Todos", "TES", "Relevo por la Vida", "Voluntariado", "Otros")
                        tags.forEach { tag ->
                            val isSelected = selectedFilterTag == tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) HondurasCyanSkein else HondurasBluePrimary.copy(alpha = 0.05f))
                                    .clickable { selectedFilterTag = tag }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) HondurasBluePrimary else HondurasBlueSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(HondurasBluePrimary.copy(alpha = 0.05f))
                            .clickable { viewModel.updateConfigValue("group_media_uploads", if (groupMultipleUploads) "false" else "true") }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (groupMultipleUploads) Icons.Default.Folder else Icons.Default.GridView,
                                contentDescription = null,
                                tint = HondurasBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Agrupar Cargas Masivas 📦",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HondurasBluePrimary
                                )
                                Text(
                                    text = if (groupMultipleUploads) "Las fotos de un mismo lote se muestran juntas (Álbumes)" else "Las fotos se muestran sueltas",
                                    fontSize = 9.sp,
                                    color = HondurasBlueSecondary.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Switch(
                            checked = groupMultipleUploads,
                            onCheckedChange = { viewModel.updateConfigValue("group_media_uploads", if (it) "true" else "false") },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HondurasCyanSkein,
                                checkedTrackColor = HondurasBluePrimary,
                                uncheckedThumbColor = Color.LightGray
                            ),
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                }
            }

            if (mediaItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PermMedia,
                            contentDescription = "No items",
                            tint = HondurasBluePrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aún no tienes fotos ni vídeos",
                            fontWeight = FontWeight.Bold,
                            color = HondurasBluePrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Presiona el botón 'Registrar Evidencia' para tomar fotos/vídeos o seleccionarlos de tu galería.",
                            fontSize = 11.sp,
                            color = HondurasBlueSecondary.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else if (filteredMediaItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "No matching items",
                            tint = HondurasBluePrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No hay evidencias para esta etiqueta",
                            fontWeight = FontWeight.Bold,
                            color = HondurasBluePrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No se encontraron archivos etiquetados como '$selectedFilterTag'.",
                            fontSize = 11.sp,
                            color = HondurasBlueSecondary.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                groupedByDate.forEach { (yearMonth, items) ->
                    val year = yearMonth.first
                    val monthName = getMonthName(yearMonth.second)

                    // Chronological Header section
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "",
                                tint = HondurasBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$monthName de $year",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = HondurasBluePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Divider(modifier = Modifier.weight(1f), color = HondurasBluePrimary.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(HondurasBluePrimary.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${items.size} arch.",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HondurasBluePrimary
                                )
                            }
                        }
                    }

                    // Render grid items with Group-by-Upload support
                    val processedItemsList = if (groupMultipleUploads) {
                        items.groupBy { it.fechaCreacion }
                            .values
                            .toList()
                    } else {
                        items.map { listOf(it) }
                    }

                    items(processedItemsList.size, key = { idx ->
                        val group = processedItemsList[idx]
                        val prefix = if (group.size > 1) "group_${group[0].fechaCreacion}_" else "single_"
                        prefix + group[0].id
                    }) { idx ->
                        val group = processedItemsList[idx]
                        if (group.size > 1) {
                            MediaBlockCard(
                                items = group,
                                onClick = { selectedMediaBlock = group },
                                onDeleteClick = { mediaBlockToDelete = group }
                            )
                        } else {
                            val item = group[0]
                            MediaItemCard(
                                media = item,
                                onClick = { zoomedMedia = item },
                                onDeleteClick = { mediaToDelete = item },
                                onExportClick = {
                                    exportToSystemGallery(
                                        context,
                                        item,
                                        onSuccess = {
                                            Toast.makeText(context, "✅ ¡Guardado en la galería del celular con éxito!", Toast.LENGTH_LONG).show()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "Error al exportar: $error", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Add File Metadata Dialog
        if (showAddDialog && selectedUri != null) {
            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    selectedUri = null
                    inputTitle = ""
                    val calendarReset = Calendar.getInstance()
                    inputDay = calendarReset.get(Calendar.DAY_OF_MONTH).toString()
                    inputMonth = (calendarReset.get(Calendar.MONTH) + 1).toString()
                    inputYear = calendarReset.get(Calendar.YEAR).toString()
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (mediaTypeSelected == "IMAGEN") Icons.Default.AddPhotoAlternate else Icons.Default.VideoCall,
                            contentDescription = null,
                            tint = HondurasBluePrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (mediaTypeSelected == "IMAGEN") "Registrar Fotografía" else "Registrar Vídeo",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Por favor ingresa una descripción para identificar esta evidencia en tu reporte chronological.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = inputTitle,
                            onValueChange = { inputTitle = it },
                            label = { Text("Título / Descripción corta *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("media_title_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputSupervisor,
                            onValueChange = { inputSupervisor = it },
                            label = { Text("Supervisor del Evento") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputLugar,
                            onValueChange = { inputLugar = it },
                            label = { Text("Lugar / Sitio del Evento *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputUbicacionDetalle,
                            onValueChange = { inputUbicacionDetalle = it },
                            label = { Text("Ubicación Específica / Por dónde *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // ⏰ Clocks: Hora Entrada & Hora Salida (Native Clock TimePicker Dialogs)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hora Entrada 🕒", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                                OutlinedButton(
                                    onClick = {
                                        val parts = inputHoraEntrada.split(":")
                                        val initialHr = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                        val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, selectedHour, selectedMinute ->
                                                inputHoraEntrada = String.format("%02d:%02d", selectedHour, selectedMinute)
                                            },
                                            initialHr, initialMin, true
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HondurasBluePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = HondurasBluePrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(inputHoraEntrada, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hora Salida 🕒", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                                OutlinedButton(
                                    onClick = {
                                        val parts = inputHoraSalida.split(":")
                                        val initialHr = parts.getOrNull(0)?.toIntOrNull() ?: 12
                                        val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, selectedHour, selectedMinute ->
                                                inputHoraSalida = String.format("%02d:%02d", selectedHour, selectedMinute)
                                            },
                                            initialHr, initialMin, true
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HondurasBluePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = HondurasBluePrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(inputHoraSalida, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Fecha de Registro (Día / Mes / Año) *",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HondurasBluePrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                val currentCal = java.util.Calendar.getInstance()
                                val yr = inputYear.toIntOrNull() ?: currentCal.get(java.util.Calendar.YEAR)
                                val mo = (inputMonth.toIntOrNull() ?: (currentCal.get(java.util.Calendar.MONTH) + 1)) - 1
                                val dy = inputDay.toIntOrNull() ?: currentCal.get(java.util.Calendar.DAY_OF_MONTH)

                                android.app.DatePickerDialog(
                                    context,
                                    { _, selectedYr, selectedMo, selectedDy ->
                                        inputDay = selectedDy.toString()
                                        inputMonth = (selectedMo + 1).toString()
                                        inputYear = selectedYr.toString()
                                    },
                                    yr, mo, dy
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HondurasBluePrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = HondurasBluePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fecha: $inputDay / $inputMonth / $inputYear",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = inputRealizado,
                                onCheckedChange = { inputRealizado = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("¿Evento o actividad realizada?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Selecciona una etiqueta de clasificación:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HondurasBluePrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val tags = listOf("Otros", "TES", "Relevo por la Vida", "Voluntariado")
                            tags.forEach { tag ->
                                val isSelected = selectedTagForNewMedia == tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) HondurasCyanSkein else HondurasBluePrimary.copy(alpha = 0.05f))
                                        .clickable { selectedTagForNewMedia = tag }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) HondurasBluePrimary else HondurasBlueSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(HondurasBluePrimary.copy(alpha = 0.05f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = HondurasBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "El sistema guardará de forma irreversible este registro aplicando compresión optimizada en segundo plano.",
                                fontSize = 10.sp,
                                color = HondurasBluePrimary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputTitle.trim().isNotEmpty() && selectedUri != null) {
                                val parsedDay = inputDay.toIntOrNull()
                                val parsedMonth = inputMonth.toIntOrNull()
                                val parsedYear = inputYear.toIntOrNull()

                                if (mediaTypeSelected == "IMAGEN") {
                                    viewModel.saveImageMedia(
                                        context = context,
                                        uri = selectedUri!!,
                                        title = inputTitle.trim(),
                                        tag = selectedTagForNewMedia,
                                        supervisor = inputSupervisor.trim(),
                                        realizado = inputRealizado,
                                        customYear = parsedYear,
                                        customMonth = parsedMonth,
                                        customDay = parsedDay,
                                        horaEntrada = inputHoraEntrada,
                                        horaSalida = inputHoraSalida,
                                        lugar = inputLugar.trim(),
                                        ubicacionDetalle = inputUbicacionDetalle.trim()
                                    )
                                } else {
                                    viewModel.saveVideoMedia(
                                        context = context,
                                        uri = selectedUri!!,
                                        title = inputTitle.trim(),
                                        durationSecs = 10,
                                        tag = selectedTagForNewMedia,
                                        supervisor = inputSupervisor.trim(),
                                        realizado = inputRealizado,
                                        customYear = parsedYear,
                                        customMonth = parsedMonth,
                                        customDay = parsedDay,
                                        horaEntrada = inputHoraEntrada,
                                        horaSalida = inputHoraSalida,
                                        lugar = inputLugar.trim(),
                                        ubicacionDetalle = inputUbicacionDetalle.trim()
                                    )
                                }
                                showAddDialog = false
                                selectedUri = null
                                inputTitle = ""
                                inputSupervisor = ""
                                inputLugar = ""
                                inputUbicacionDetalle = ""
                                inputHoraEntrada = "08:00"
                                inputHoraSalida = "12:00"
                                inputRealizado = true
                                selectedTagForNewMedia = "Otros"
                                val calendarReset = Calendar.getInstance()
                                inputDay = calendarReset.get(Calendar.DAY_OF_MONTH).toString()
                                inputMonth = (calendarReset.get(Calendar.MONTH) + 1).toString()
                                inputYear = calendarReset.get(Calendar.YEAR).toString()
                                Toast.makeText(context, "Procesando y guardando evidencia...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Ingrese un título válido", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary),
                        modifier = Modifier.testTag("confirm_media_add_button")
                    ) {
                        Text("Guardar con Compresión", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddDialog = false
                        selectedUri = null
                        inputTitle = ""
                        inputLugar = ""
                        inputUbicacionDetalle = ""
                        inputHoraEntrada = "08:00"
                        inputHoraSalida = "12:00"
                        selectedTagForNewMedia = "Otros"
                        val calendarReset = Calendar.getInstance()
                        inputDay = calendarReset.get(Calendar.DAY_OF_MONTH).toString()
                        inputMonth = (calendarReset.get(Calendar.MONTH) + 1).toString()
                        inputYear = calendarReset.get(Calendar.YEAR).toString()
                    }) {
                        Text("Cancelar", color = HondurasBlueSecondary)
                    }
                }
            )
        }

        // Fullscreen Overlay Zoom Media Dialog
        if (zoomedMedia != null) {
            val media = zoomedMedia!!
            val dateForm = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(media.fechaCreacion))
            AlertDialog(
                onDismissRequest = { zoomedMedia = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
                title = null,
                text = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 32.dp, horizontal = 12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Header controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { zoomedMedia = null },
                                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }

                                Text(
                                    text = media.titulo,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                )

                                IconButton(
                                    onClick = {
                                        exportToSystemGallery(
                                            context,
                                            media,
                                            onSuccess = {
                                                Toast.makeText(context, "✅ ¡Imagen guardada en la galería!", Toast.LENGTH_LONG).show()
                                            },
                                            onError = { error ->
                                                Toast.makeText(context, "Fallo: $error", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    modifier = Modifier.background(HondurasCyanSkein, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SaveAlt,
                                        contentDescription = "Native Export",
                                        tint = HondurasBluePrimary
                                    )
                                }
                            }

                            // Central Content Container
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (media.tipoMedia == "VIDEO") {
                                    androidx.compose.ui.viewinterop.AndroidView(
                                        factory = { ctx ->
                                            android.widget.VideoView(ctx).apply {
                                                setVideoPath(media.rutaArchivo)
                                                val mediaController = android.widget.MediaController(ctx)
                                                mediaController.setAnchorView(this)
                                                setMediaController(mediaController)
                                                setOnPreparedListener { mp ->
                                                    mp.isLooping = true
                                                    start()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                    )
                                } else {
                                    AsyncImage(
                                        model = File(media.rutaArchivo),
                                        contentDescription = media.titulo,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                    )
                                }
                            }

                            // Stats & Edit Dialog Footer Panel View Only, click "Editar Datos" to open scrollable dialog
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(HondurasBluePrimary)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = media.etiqueta.uppercase(),
                                                color = HondurasCyanSkein,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "📅 $dateForm",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Text(
                                        text = media.titulo,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        style = androidx.compose.ui.text.TextStyle(
                                            shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black, blurRadius = 3f)
                                        )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Responsable / Supervisor:",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                text = if (media.supervisor.isNotEmpty()) media.supervisor else "Pendiente de Asignar",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (media.realizado) Color(0xFF2ECC71).copy(alpha = 0.2f) else Color(0xFFE74C3C).copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (media.realizado) "✓ REALIZADO" else "✗ PENDIENTE",
                                                color = if (media.realizado) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    // Custom visual additions of Lugar / Horarios
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "📍 LUGAR:",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (media.lugar.isNotEmpty()) media.lugar else "No Especificado",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "🗺️ UBICACIÓN ESPECÍFICA:",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (media.ubicacionDetalle.isNotEmpty()) media.ubicacionDetalle else "No Especificada",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = HondurasCyanSkein,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Hora Entrada: ${media.horaEntrada}",
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = HondurasCyanSkein,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Hora Salida: ${media.horaSalida}",
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Divider(color = Color.White.copy(alpha = 0.15f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Main Edit Button
                                        Button(
                                            onClick = { 
                                                mediaEditTarget = media 
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein, contentColor = Color.Black),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Editar Datos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Quick delete from inside viewer
                                        Button(
                                            onClick = { 
                                                mediaToDelete = media
                                                zoomedMedia = null
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C), contentColor = Color.White),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Eliminar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }

        // Deletion confirmation Authenticated PIN Dialog
        if (mediaToDelete != null) {
            val media = mediaToDelete!!
            val isPinEnabled = securityPin.isNotEmpty()

            AlertDialog(
                onDismissRequest = {
                    mediaToDelete = null
                    enteredPinDelete = ""
                    pinDeleteError = ""
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("¿Eliminar Archivo?", color = MaterialTheme.colorScheme.error)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Estás a punto de borrar permanentemente la evidencia inmutable: \"${media.titulo}\". Esta acción no se puede deshacer.",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (isPinEnabled) {
                            Text(
                                text = "🔐 Protección de Integridad Activada: Escribe tu PIN escolar de seguridad para confirmar el borrado autorizado:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HondurasBluePrimary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            OutlinedTextField(
                                value = enteredPinDelete,
                                onValueChange = {
                                    if (it.length <= 6) {
                                        enteredPinDelete = it
                                        pinDeleteError = ""
                                    }
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("PIN de Seguridad") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("delete_media_pin_input"),
                                isError = pinDeleteError.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (pinDeleteError.isNotEmpty()) {
                                Text(
                                    text = pinDeleteError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "⚠️ No has configurado un PIN escolar. Te recomendamos establecer uno en la pestaña 'Expediente' para proteger tus bitácoras de borrados accidentales.",
                                fontSize = 10.sp,
                                color = HondurasBlueSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteMediaItem(
                                media = media,
                                enteredPin = enteredPinDelete,
                                onSuccess = {
                                    mediaToDelete = null
                                    enteredPinDelete = ""
                                    pinDeleteError = ""
                                    Toast.makeText(context, "Evidencia eliminada correctamente de la base de datos.", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    if (isPinEnabled) {
                                        pinDeleteError = error
                                    } else {
                                        Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_media_button")
                    ) {
                        Text("Confirmar Borrado", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        mediaToDelete = null
                        enteredPinDelete = ""
                        pinDeleteError = ""
                    }) {
                        Text("Cancelar", color = HondurasBlueSecondary)
                    }
                }
            )
        }

        // 🔘 Dialog: Edit individual media entry with visual DatePicker
        if (mediaEditTarget != null) {
            val mediaToEdit = mediaEditTarget!!
            
            var mutableTitle by remember(mediaToEdit.id) { mutableStateOf(mediaToEdit.titulo) }
            var mutableTag by remember(mediaToEdit.id) { mutableStateOf(mediaToEdit.etiqueta) }
            var mutableSupervisor by remember(mediaToEdit.id) { mutableStateOf(mediaToEdit.supervisor) }
            var mutableRealizado by remember(mediaToEdit.id) { mutableStateOf(mediaToEdit.realizado) }
            
            var mutableHoraEntrada by remember(mediaToEdit.id) { mutableStateOf(mediaToEdit.horaEntrada) }
            var mutableHoraSalida by remember(mediaToEdit.id) { mutableStateOf(mediaToEdit.horaSalida) }
            var mutableLugar by remember(mediaToEdit.id) { mutableStateOf(mediaToEdit.lugar) }
            var mutableUbicacionDetalle by remember(mediaToEdit.id) { mutableStateOf(mediaToEdit.ubicacionDetalle) }
            
            // Extract existing date parameters
            var editDay by remember(mediaToEdit.id) { mutableStateOf(mediaToEdit.dia.toString()) }
            var editMonth by remember(mediaToEdit.id) { mutableStateOf(mediaToEdit.mes.toString()) }
            var editYear by remember(mediaToEdit.id) { mutableStateOf(mediaToEdit.anio.toString()) }

            AlertDialog(
                onDismissRequest = { mediaEditTarget = null },
                shape = RoundedCornerShape(18.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = HondurasBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editar Evidencia", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = mutableTitle,
                            onValueChange = { mutableTitle = it },
                            label = { Text("Título / Descripción *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = mutableSupervisor,
                            onValueChange = { mutableSupervisor = it },
                            label = { Text("Supervisor del Evento") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = mutableLugar,
                            onValueChange = { mutableLugar = it },
                            label = { Text("Lugar / Sitio del Evento *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = mutableUbicacionDetalle,
                            onValueChange = { mutableUbicacionDetalle = it },
                            label = { Text("Ubicación Específica / Por dónde *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Clocks: mutableHoraEntrada & mutableHoraSalida
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hora Entrada 🕒", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                                OutlinedButton(
                                    onClick = {
                                        val parts = mutableHoraEntrada.split(":")
                                        val initialHr = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                        val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, selectedHour, selectedMinute ->
                                                mutableHoraEntrada = String.format("%02d:%02d", selectedHour, selectedMinute)
                                            },
                                            initialHr, initialMin, true
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HondurasBluePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = HondurasBluePrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(mutableHoraEntrada, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hora Salida 🕒", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                                OutlinedButton(
                                    onClick = {
                                        val parts = mutableHoraSalida.split(":")
                                        val initialHr = parts.getOrNull(0)?.toIntOrNull() ?: 12
                                        val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, selectedHour, selectedMinute ->
                                                mutableHoraSalida = String.format("%02d:%02d", selectedHour, selectedMinute)
                                            },
                                            initialHr, initialMin, true
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HondurasBluePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = HondurasBluePrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(mutableHoraSalida, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = mutableRealizado,
                                onCheckedChange = { mutableRealizado = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("¿Actividad Realizada?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Text(
                            text = "🏷️ Categoría de Clasificación:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HondurasBluePrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val tagsList = listOf("Otros", "TES", "Relevo por la Vida", "Voluntariado")
                            tagsList.forEach { tagItem ->
                                val isSel = mutableTag == tagItem
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) HondurasCyanSkein else HondurasBluePrimary.copy(alpha = 0.05f))
                                        .clickable { mutableTag = tagItem }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tagItem,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) HondurasBluePrimary else HondurasBlueSecondary
                                    )
                                }
                            }
                        }

                        Text(
                            text = "📅 Cambiar Fecha de Registro:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HondurasBluePrimary
                        )

                        OutlinedButton(
                            onClick = {
                                val yr = editYear.toIntOrNull() ?: 2026
                                val mo = (editMonth.toIntOrNull() ?: 1) - 1
                                val dy = editDay.toIntOrNull() ?: 1

                                android.app.DatePickerDialog(
                                    context,
                                    { _, selectedYr, selectedMo, selectedDy ->
                                        editDay = selectedDy.toString()
                                        editMonth = (selectedMo + 1).toString()
                                        editYear = selectedYr.toString()
                                    },
                                    yr, mo, dy
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HondurasBluePrimary),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fecha: $editDay / $editMonth / $editYear",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (mutableTitle.trim().isNotEmpty()) {
                                val cal = java.util.Calendar.getInstance()
                                val targetYr = editYear.toIntOrNull() ?: mediaToEdit.anio
                                val targetMo = (editMonth.toIntOrNull() ?: mediaToEdit.mes) - 1
                                val targetDy = editDay.toIntOrNull() ?: mediaToEdit.dia
                                cal.set(targetYr, targetMo, targetDy)
                                val updatedTimestamp = cal.timeInMillis

                                val updatedMedia = mediaToEdit.copy(
                                    titulo = mutableTitle.trim(),
                                    etiqueta = mutableTag,
                                    supervisor = mutableSupervisor.trim(),
                                    realizado = mutableRealizado,
                                    dia = targetDy,
                                    mes = targetMo + 1,
                                    anio = targetYr,
                                    fechaCreacion = updatedTimestamp,
                                    horaEntrada = mutableHoraEntrada,
                                    horaSalida = mutableHoraSalida,
                                    lugar = mutableLugar.trim(),
                                    ubicacionDetalle = mutableUbicacionDetalle.trim()
                                )

                                viewModel.updateMediaItem(updatedMedia)
                                mediaEditTarget = null
                                // Auto-update zoomedMedia detail state so that user immediately notices the change
                                if (zoomedMedia?.id == mediaToEdit.id) {
                                    zoomedMedia = updatedMedia
                                }
                                Toast.makeText(context, "✅ ¡Evidencia actualizada corectamente!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein, contentColor = Color.Black)
                    ) {
                        Text("Guardar Cambios ⚡", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mediaEditTarget = null }) {
                        Text("Cancelar", color = HondurasBlueSecondary)
                    }
                }
            )
        }

        // 🔘 Dialog: Edit/View selected upload Media Block
        if (selectedMediaBlock != null) {
            val block = selectedMediaBlock!!
            val dateForm = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(block.first().fechaCreacion))
            val cleanTitle = block.first().titulo.substringBefore(" - Foto ")
            
            var mutableBlockTitle by remember(block.first().id) { mutableStateOf(cleanTitle) }
            var mutableBlockTag by remember(block.first().id) { mutableStateOf(block.first().etiqueta) }
            var mutableBlockSupervisor by remember(block.first().id) { mutableStateOf(block.first().supervisor) }
            var mutableBlockRealizado by remember(block.first().id) { mutableStateOf(block.first().realizado) }

            var mutableBlockHoraEntrada by remember(block.first().id) { mutableStateOf(block.first().horaEntrada) }
            var mutableBlockHoraSalida by remember(block.first().id) { mutableStateOf(block.first().horaSalida) }
            var mutableBlockLugar by remember(block.first().id) { mutableStateOf(block.first().lugar) }
            var mutableBlockUbicacionDetalle by remember(block.first().id) { mutableStateOf(block.first().ubicacionDetalle) }

            // Extract existing date parameters for block edit
            var blockDay by remember(block.first().id) { mutableStateOf(block.first().dia.toString()) }
            var blockMonth by remember(block.first().id) { mutableStateOf(block.first().mes.toString()) }
            var blockYear by remember(block.first().id) { mutableStateOf(block.first().anio.toString()) }

            AlertDialog(
                onDismissRequest = { selectedMediaBlock = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                title = null,
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Bloque de Evidencias (${block.size} fotos)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = HondurasBluePrimary
                            )
                            IconButton(onClick = { selectedMediaBlock = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Modifica la descripción o los datos de todo el bloque para corregir errores rápidamente.",
                                fontSize = 11.sp,
                                color = HondurasBlueSecondary.copy(alpha = 0.8f)
                            )

                            // Horizontally displaying the thumbnails inside the block
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                block.forEach { item ->
                                    Card(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .fillMaxHeight()
                                            .clickable { 
                                                selectedMediaBlock = null
                                                zoomedMedia = item
                                            },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = File(item.rutaArchivo),
                                                contentDescription = item.titulo,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = item.titulo.substringAfter(" - "),
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = HondurasBluePrimary.copy(alpha = 0.04f)),
                                border = BorderStroke(1.dp, HondurasCyanSkein.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        "✍️ Edición en Masa",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = HondurasBluePrimary
                                    )

                                    OutlinedTextField(
                                        value = mutableBlockTitle,
                                        onValueChange = { mutableBlockTitle = it },
                                        label = { Text("Descripción Base Común") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = mutableBlockSupervisor,
                                        onValueChange = { mutableBlockSupervisor = it },
                                        label = { Text("Supervisor de Actividad") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = mutableBlockLugar,
                                        onValueChange = { mutableBlockLugar = it },
                                        label = { Text("Lugar / Sitio del Evento *") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = mutableBlockUbicacionDetalle,
                                        onValueChange = { mutableBlockUbicacionDetalle = it },
                                        label = { Text("Ubicación Específica / Por dónde *") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    // Clocks: mutableBlockHoraEntrada & mutableBlockHoraSalida
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Hora Entrada 🕒", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                                            OutlinedButton(
                                                onClick = {
                                                    val parts = mutableBlockHoraEntrada.split(":")
                                                    val initialHr = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                                    val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                                    android.app.TimePickerDialog(
                                                        context,
                                                        { _, selectedHour, selectedMinute ->
                                                            mutableBlockHoraEntrada = String.format("%02d:%02d", selectedHour, selectedMinute)
                                                        },
                                                        initialHr, initialMin, true
                                                    ).show()
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HondurasBluePrimary),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.4f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccessTime,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = HondurasBluePrimary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(mutableBlockHoraEntrada, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Hora Salida 🕒", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                                            OutlinedButton(
                                                onClick = {
                                                    val parts = mutableBlockHoraSalida.split(":")
                                                    val initialHr = parts.getOrNull(0)?.toIntOrNull() ?: 12
                                                    val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                                    android.app.TimePickerDialog(
                                                        context,
                                                        { _, selectedHour, selectedMinute ->
                                                            mutableBlockHoraSalida = String.format("%02d:%02d", selectedHour, selectedMinute)
                                                        },
                                                        initialHr, initialMin, true
                                                    ).show()
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HondurasBluePrimary),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.4f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccessTime,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = HondurasBluePrimary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(mutableBlockHoraSalida, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }

                                    Text(
                                        text = "📅 Fecha de este Bloque:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HondurasBluePrimary
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            val yr = blockYear.toIntOrNull() ?: 2026
                                            val mo = (blockMonth.toIntOrNull() ?: 1) - 1
                                            val dy = blockDay.toIntOrNull() ?: 1

                                            android.app.DatePickerDialog(
                                                context,
                                                { _, selectedYr, selectedMo, selectedDy ->
                                                    blockDay = selectedDy.toString()
                                                    blockMonth = (selectedMo + 1).toString()
                                                    blockYear = selectedYr.toString()
                                                },
                                                yr, mo, dy
                                            ).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HondurasBluePrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.4f))
                                    ) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Fecha: $blockDay / $blockMonth / $blockYear",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = mutableBlockRealizado,
                                                onCheckedChange = { mutableBlockRealizado = it }
                                            )
                                            Text("Actividad Realizada ✅", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        Button(
                                            onClick = {
                                                if (mutableBlockTitle.trim().isNotEmpty()) {
                                                    val cal = java.util.Calendar.getInstance()
                                                    val targetYr = blockYear.toIntOrNull() ?: block.first().anio
                                                    val targetMo = (blockMonth.toIntOrNull() ?: block.first().mes) - 1
                                                    val targetDy = blockDay.toIntOrNull() ?: block.first().dia
                                                    cal.set(targetYr, targetMo, targetDy)
                                                    val updatedTimestamp = cal.timeInMillis

                                                    block.forEachIndexed { idx, item ->
                                                        viewModel.updateMediaItem(
                                                            item.copy(
                                                                titulo = "${mutableBlockTitle.trim()} - Foto ${idx + 1}",
                                                                etiqueta = mutableBlockTag,
                                                                supervisor = mutableBlockSupervisor.trim(),
                                                                realizado = mutableBlockRealizado,
                                                                dia = targetDy,
                                                                mes = targetMo + 1,
                                                                anio = targetYr,
                                                                fechaCreacion = updatedTimestamp,
                                                                horaEntrada = mutableBlockHoraEntrada,
                                                                horaSalida = mutableBlockHoraSalida,
                                                                lugar = mutableBlockLugar.trim(),
                                                                ubicacionDetalle = mutableBlockUbicacionDetalle.trim()
                                                            )
                                                        )
                                                     }
                                                     selectedMediaBlock = null
                                                     Toast.makeText(context, "✅ ¡Lote actualizado con éxito!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein, contentColor = Color.Black),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Aplicar Cambios ⚡", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        }
                                    }

                                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                    Text("🏷️ Clasificación en Masa:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val tagsList = listOf("Otros", "TES", "Relevo por la Vida", "Voluntariado")
                                        tagsList.forEach { tagItem ->
                                            val isSel = mutableBlockTag == tagItem
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSel) HondurasCyanSkein else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                    .clickable { mutableBlockTag = tagItem }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(tagItem, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) HondurasBluePrimary else MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🕒 Fecha del Bloque: $dateForm", fontSize = 10.sp, color = HondurasBlueSecondary)
                                    Text("📦 Archivos del Lote: ${block.size} fotografías guardadas de forma inmutable", fontSize = 10.sp, color = HondurasBlueSecondary)
                                    Text("💡 Consejo: Toca cualquiera de las fotos de arriba para ver su tamaño original, exportarla individualmente o borrarla del lote de forma independiente.", fontSize = 9.sp, color = HondurasBlueSecondary.copy(alpha = 0.7f), style = androidx.compose.ui.text.TextStyle(lineHeight = 13.sp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }

        // Deletion confirmation for block
        if (mediaBlockToDelete != null) {
            val block = mediaBlockToDelete!!
            val isPinEnabled = securityPin.isNotEmpty()

            AlertDialog(
                onDismissRequest = {
                    mediaBlockToDelete = null
                    enteredPinDelete = ""
                    pinDeleteError = ""
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("¿Eliminar Bloque completo?", color = MaterialTheme.colorScheme.error)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Estás a punto de borrar permanentemente un bloque completo de ${block.size} fotos: \"${block.first().titulo.substringBefore(" - Foto ")}\". Esta acción borrará todas estas evidencias físicas y lógicas, y no se puede deshacer.",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (isPinEnabled) {
                            Text(
                                text = "🔐 Escribe tu PIN escolar de seguridad para confirmar el borrado autorizado:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HondurasBluePrimary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            OutlinedTextField(
                                value = enteredPinDelete,
                                onValueChange = {
                                    if (it.length <= 6) {
                                        enteredPinDelete = it
                                        pinDeleteError = ""
                                    }
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("PIN de Seguridad") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                isError = pinDeleteError.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (pinDeleteError.isNotEmpty()) {
                                Text(
                                    text = pinDeleteError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            var processedCount = 0
                            var lastError = ""
                            block.forEach { item ->
                                viewModel.deleteMediaItem(
                                    media = item,
                                    enteredPin = enteredPinDelete,
                                    onSuccess = {
                                        processedCount++
                                        if (processedCount == block.size) {
                                            mediaBlockToDelete = null
                                            enteredPinDelete = ""
                                            pinDeleteError = ""
                                            Toast.makeText(context, "✅ Lote de evidencias eliminado correctamente.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onError = { error ->
                                        processedCount++
                                        lastError = error
                                        if (processedCount == block.size) {
                                            if (isPinEnabled) {
                                                pinDeleteError = lastError
                                            } else {
                                                Toast.makeText(context, "Error: $lastError", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirmar Borrado de Lote", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        mediaBlockToDelete = null
                        enteredPinDelete = ""
                        pinDeleteError = ""
                    }) {
                        Text("Cancelar", color = HondurasBlueSecondary)
                    }
                }
            )
        }

        // 🔘 Dialog: Choose between capture photo/video and pick gallery photo/video
        if (showActionChooser) {
            AlertDialog(
                onDismissRequest = { showActionChooser = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, tint = HondurasBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registrar Evidencia", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Por favor selecciona el origen de tu evidencia para continuar:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        // Capture Options
                        Text("📌 " + "Capturar Cámara en Vivo".diplomatic(isDipl), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                        
                        Button(
                            onClick = {
                                showActionChooser = false
                                val hasCameraPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                if (!hasCameraPerm) {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                } else {
                                    try {
                                        val (file, uri) = createTmpFileUri(context, isVideo = false)
                                        tempCameraFile = file
                                        tempCameraUri = uri
                                        takePictureLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al preparar cámara: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein, contentColor = HondurasBluePrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tomar Foto".diplomatic(isDipl), fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                showActionChooser = false
                                val hasCameraPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                if (!hasCameraPerm) {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                } else {
                                    try {
                                        val (file, uri) = createTmpFileUri(context, isVideo = true)
                                        tempCameraFile = file
                                        tempCameraUri = uri
                                        captureVideoLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al preparar cámara: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein, contentColor = HondurasBluePrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grabar Vídeo".diplomatic(isDipl), fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 4.dp))
                        
                        // Local Pickers Options
                        Text("📁 Seleccionar Archivo Local", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)

                        Button(
                            onClick = {
                                showActionChooser = false
                                imagePickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HondurasBlueSecondary, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Elegir de Galería", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                showActionChooser = false
                                videoPickerLauncher.launch("video/*")
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HondurasBlueSecondary, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Elegir Vídeo de Galería", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                showActionChooser = false
                                multipleImagesLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Carga Masiva (Múltiples Fotos) ⚡", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showActionChooser = false }) {
                        Text("Cancelar", color = HondurasBlueSecondary)
                    }
                }
            )
        }

        // 🔘 Dialog: Add Multiple Images in Bulk
        if (showAddMultipleDialog && selectedUrisList.isNotEmpty()) {
            var baseTitle by remember { mutableStateOf("") }
            var multiTag by remember { mutableStateOf("Otros") }
            var multiSupervisor by remember { mutableStateOf("") }
            var multiRealizado by remember { mutableStateOf(true) }

            var multiHoraEntrada by remember { mutableStateOf("08:00") }
            var multiHoraSalida by remember { mutableStateOf("12:00") }
            var multiLugar by remember { mutableStateOf("") }
            var multiUbicacionDetalle by remember { mutableStateOf("") }

            val currentCal = remember { java.util.Calendar.getInstance() }
            var multiDay by remember { mutableStateOf(currentCal.get(java.util.Calendar.DAY_OF_MONTH).toString()) }
            var multiMonth by remember { mutableStateOf((currentCal.get(java.util.Calendar.MONTH) + 1).toString()) }
            var multiYear by remember { mutableStateOf(currentCal.get(java.util.Calendar.YEAR).toString()) }

            AlertDialog(
                onDismissRequest = {
                    showAddMultipleDialog = false
                    selectedUrisList = emptyList()
                    baseTitle = ""
                },
                title = {
                    Text("Carga Masiva (${selectedUrisList.size} fotos)", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Se subirán de golpe todas las fotos elegidas con una descripción base común descrita abajo.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        OutlinedTextField(
                            value = baseTitle,
                            onValueChange = { baseTitle = it },
                            label = { Text("Descripción o Título Base *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = multiSupervisor,
                            onValueChange = { multiSupervisor = it },
                            label = { Text("Supervisor del Evento") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = multiLugar,
                            onValueChange = { multiLugar = it },
                            label = { Text("Lugar / Sitio del Evento *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = multiUbicacionDetalle,
                            onValueChange = { multiUbicacionDetalle = it },
                            label = { Text("Ubicación Específica / Por dónde *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Clocks: multiHoraEntrada & multiHoraSalida
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hora Entrada 🕒", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                                OutlinedButton(
                                    onClick = {
                                        val parts = multiHoraEntrada.split(":")
                                        val initialHr = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                        val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, selectedHour, selectedMinute ->
                                                multiHoraEntrada = String.format("%02d:%02d", selectedHour, selectedMinute)
                                            },
                                            initialHr, initialMin, true
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HondurasBluePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = HondurasBluePrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(multiHoraEntrada, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hora Salida 🕒", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                                OutlinedButton(
                                    onClick = {
                                        val parts = multiHoraSalida.split(":")
                                        val initialHr = parts.getOrNull(0)?.toIntOrNull() ?: 12
                                        val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, selectedHour, selectedMinute ->
                                                multiHoraSalida = String.format("%02d:%02d", selectedHour, selectedMinute)
                                            },
                                            initialHr, initialMin, true
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HondurasBluePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = HondurasBluePrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(multiHoraSalida, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Text(
                            text = "📅 Fecha del Registro del Lote:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HondurasBluePrimary
                        )

                        OutlinedButton(
                            onClick = {
                                val yr = multiYear.toIntOrNull() ?: currentCal.get(java.util.Calendar.YEAR)
                                val mo = (multiMonth.toIntOrNull() ?: (currentCal.get(java.util.Calendar.MONTH) + 1)) - 1
                                val dy = multiDay.toIntOrNull() ?: currentCal.get(java.util.Calendar.DAY_OF_MONTH)

                                android.app.DatePickerDialog(
                                    context,
                                    { _, selectedYr, selectedMo, selectedDy ->
                                        multiDay = selectedDy.toString()
                                        multiMonth = (selectedMo + 1).toString()
                                        multiYear = selectedYr.toString()
                                    },
                                    yr, mo, dy
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HondurasBluePrimary),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fecha: $multiDay / $multiMonth / $multiYear",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = multiRealizado, onCheckedChange = { multiRealizado = it })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("¿Evento o actividad realizada?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Text(
                            text = "Selecciona una etiqueta de clasificación:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HondurasBluePrimary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val tags = listOf("Otros", "TES", "Relevo por la Vida", "Voluntariado")
                            tags.forEach { tag ->
                                val isSelected = multiTag == tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) HondurasCyanSkein else HondurasBluePrimary.copy(alpha = 0.05f))
                                        .clickable { multiTag = tag }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) HondurasBluePrimary else HondurasBlueSecondary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (baseTitle.trim().isNotEmpty()) {
                                val targetYr = multiYear.toIntOrNull()
                                val targetMo = multiMonth.toIntOrNull()
                                val targetDy = multiDay.toIntOrNull()

                                val cal = java.util.Calendar.getInstance()
                                if (targetYr != null && targetMo != null && targetDy != null) {
                                    cal.set(targetYr, targetMo - 1, targetDy)
                                }
                                val batchTime = cal.timeInMillis

                                selectedUrisList.forEachIndexed { index, uri ->
                                    val finalTitle = "${baseTitle.trim()} - Foto ${index + 1}"
                                    viewModel.saveImageMedia(
                                        context = context,
                                        uri = uri,
                                        title = finalTitle,
                                        tag = multiTag,
                                        supervisor = multiSupervisor.trim(),
                                        realizado = multiRealizado,
                                        customYear = targetYr,
                                        customMonth = targetMo,
                                        customDay = targetDy,
                                        customTimestamp = batchTime,
                                        horaEntrada = multiHoraEntrada,
                                        horaSalida = multiHoraSalida,
                                        lugar = multiLugar.trim(),
                                        ubicacionDetalle = multiUbicacionDetalle.trim()
                                    )
                                }
                                showAddMultipleDialog = false
                                selectedUrisList = emptyList()
                                baseTitle = ""
                                multiLugar = ""
                                multiUbicacionDetalle = ""
                                multiHoraEntrada = "08:00"
                                multiHoraSalida = "12:00"
                                multiSupervisor = ""
                                multiRealizado = true
                                Toast.makeText(context, "¡Subiendo lote de ${selectedUrisList.size} fotos de golpe!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Ingrese una descripción base", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary)
                    ) {
                        Text("Subir Selección ⚡", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddMultipleDialog = false
                        selectedUrisList = emptyList()
                        baseTitle = ""
                        multiLugar = ""
                        multiUbicacionDetalle = ""
                        multiHoraEntrada = "08:00"
                        multiHoraSalida = "12:00"
                        multiSupervisor = ""
                        multiRealizado = true
                    }) {
                        Text("Cancelar", color = HondurasBlueSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun rememberVideoThumbnail(videoPath: String): ImageBitmap? {
    var bitmap by remember(videoPath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(videoPath) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(videoPath)
                if (file.exists()) {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(file.absolutePath)
                    val frame = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    retriever.release()
                    frame?.asImageBitmap()?.let {
                        bitmap = it
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    return bitmap
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemCard(
    media: GaleriaMedia,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val mediaWeekdayName = remember(media.fechaCreacion) {
        try {
            val sdf = java.text.SimpleDateFormat("EEEE", java.util.Locale("es", "ES"))
            sdf.format(java.util.Date(media.fechaCreacion)).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            ""
        }
    }

    val mediaDayNo = remember(media.fechaCreacion) {
        try {
            val sdf = java.text.SimpleDateFormat("d", java.util.Locale("es", "ES"))
            sdf.format(java.util.Date(media.fechaCreacion))
        } catch (e: Exception) {
            media.dia.toString()
        }
    }

    val mediaMonthName = remember(media.fechaCreacion) {
        try {
            val sdf = java.text.SimpleDateFormat("MMMM", java.util.Locale("es", "ES"))
            sdf.format(java.util.Date(media.fechaCreacion)).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            ""
        }
    }

    val mediaYearName = remember(media.fechaCreacion) {
        try {
            val sdf = java.text.SimpleDateFormat("yyyy", java.util.Locale("es", "ES"))
            sdf.format(java.util.Date(media.fechaCreacion))
        } catch (e: Exception) {
            media.anio.toString()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDeleteClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (media.tipoMedia == "VIDEO") {
                val thumbnail = rememberVideoThumbnail(media.rutaArchivo)
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = media.titulo,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Highly polished modern background placeholder for video content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF2C3E50),
                                        Color(0xFF16222F)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Vídeo",
                                tint = HondurasCyanSkein,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "VÍDEO DE EVIDENCIA 📹",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            if (media.duracionSegundos > 0) {
                                Text(
                                    text = "${media.duracionSegundos} seg",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Background preview image for normal photographs
                AsyncImage(
                    model = File(media.rutaArchivo),
                    contentDescription = media.titulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Dynamic bottom gradients mask for typography readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Tag category overlay badge and Day Highlight Tag
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Unified Day, Month, Year & Weekday Tag 🏷️
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HondurasCyanSkein)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📅 " + (if (mediaWeekdayName.isNotEmpty()) "$mediaWeekdayName, " else "") + "$mediaDayNo de $mediaMonthName, $mediaYearName",
                        color = Color.Black,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                if (media.etiqueta.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = "Etiqueta",
                                tint = HondurasCyanSkein,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = media.etiqueta,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Video overlay tag helper
            if (media.tipoMedia == "VIDEO") {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Top Quick Right Actions panel
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Native Export Arrow
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onExportClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = "Export to System Gallery",
                        tint = HondurasCyanSkein,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Authorized delete trash action
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onDeleteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Title and weight metadata card labels
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Text(
                    text = media.titulo,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "📅 ${media.dia}/${media.mes}/${media.anio}",
                    color = HondurasCyanSkein,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💾 " + formatBytes(media.pesoComprimidoBytes),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (media.tipoMedia == "VIDEO") "📹 Vídeo" else "📷 Foto",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaBlockCard(
    items: List<GaleriaMedia>,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cover = items.first()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDeleteClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (cover.tipoMedia == "VIDEO") {
                val thumbnail = rememberVideoThumbnail(cover.rutaArchivo)
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = cover.titulo,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF2C3E50), Color(0xFF16222F))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Vídeo",
                            tint = HondurasCyanSkein,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = File(cover.rutaArchivo),
                    contentDescription = cover.titulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Block indicator badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HondurasBluePrimary)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Bloque",
                        tint = HondurasCyanSkein,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "BLOQUE • ${items.size} arch.",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top Quick Right Actions panel
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onDeleteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Metadata info layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                val cleanTitle = cover.titulo.substringBefore(" - Foto ")
                Text(
                    text = cleanTitle,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "💼 Lote de Evidencias • ${cover.etiqueta}",
                    color = HondurasCyanSkein,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val totalWeight = items.sumOf { it.pesoComprimidoBytes }
                    Text(
                        text = "💾 " + formatBytes(totalWeight),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "📦 Álbum de Fotos",
                        color = HondurasCyanSkein.copy(alpha = 0.9f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// Byte unit formatter
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val numStr = DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble()))
    return "$numStr ${units[digitGroups]}"
}

// Get Spanish name of Month
private fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "Enero"
        2 -> "Febrero"
        3 -> "Marzo"
        4 -> "Abril"
        5 -> "Mayo"
        6 -> "Junio"
        7 -> "Julio"
        8 -> "Agosto"
        9 -> "Septiembre"
        10 -> "Octubre"
        11 -> "Noviembre"
        12 -> "Diciembre"
        else -> "Mes"
    }
}

/**
 * Custom export function to safely write the processed image/video into the device's shared photo/movies library.
 */
fun exportToSystemGallery(
    context: Context,
    media: GaleriaMedia,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val file = File(media.rutaArchivo)
    if (!file.exists()) {
        onError("El archivo físico local no existe.")
        return
    }
    
    try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "TES_${System.currentTimeMillis()}_" + file.name)
            if (media.tipoMedia == "IMAGEN") {
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/TES_Master")
                }
            } else {
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/TES_Master")
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collectionUri = if (media.tipoMedia == "IMAGEN") {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val targetUri = resolver.insert(collectionUri, contentValues)
        if (targetUri == null) {
            onError("Error al insertar el registro en la base de datos de medios.")
            return
        }

        resolver.openOutputStream(targetUri).use { outStream ->
            file.inputStream().use { inStream ->
                if (outStream != null) {
                    inStream.copyTo(outStream)
                }
            }
        }

        // Release "pending" lock on android systems Q+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(targetUri, contentValues, null, null)
        }
        
        onSuccess()
    } catch (e: Exception) {
        e.printStackTrace()
        onError(e.message ?: "Excepción desconocida al exportar.")
    }
}

/**
 * Creates temporary file and its corresponding FileProvider Uri for Camera App interaction
 */
private fun createTmpFileUri(context: Context, isVideo: Boolean): Pair<File, Uri> {
    val prefix = if (isVideo) "VID_CAP_" else "IMG_CAP_"
    val suffix = if (isVideo) ".mp4" else ".jpg"
    val targetDir = context.externalCacheDir ?: context.cacheDir
    val tempFile = File.createTempFile(
        prefix + System.currentTimeMillis() + "_",
        suffix,
        targetDir
    ).apply {
        createNewFile()
    }
    val providerAuthority = "${context.packageName}.fileprovider"
    val uri = androidx.core.content.FileProvider.getUriForFile(context, providerAuthority, tempFile)
    return Pair(tempFile, uri)
}
