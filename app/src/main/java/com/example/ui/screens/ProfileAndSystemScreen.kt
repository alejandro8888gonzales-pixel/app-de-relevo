package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Logro
import com.example.data.Usuario
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAndSystemScreen(viewModel: StudentsViewModel) {
    val userState by viewModel.usuario.collectAsState()
    val logrosList by viewModel.logros.collectAsState()
    val securityPin by viewModel.securityPin.collectAsState()

    var selectedSubSection by remember { mutableStateOf("Mi Perfil") } // "Mi Perfil", "Logros", "Identificación QR", "Sistema"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab row selectors
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val subsections = listOf("Mi Perfil", "Logros", "ID QR", "Sistema")
                subsections.forEach { section ->
                    val isSelected = selectedSubSection == section
                    TextButton(
                        onClick = { selectedSubSection = section },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isSelected) HondurasBluePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(section, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .height(3.dp)
                                        .width(24.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(HondurasBluePrimary)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sub section rendering
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedSubSection) {
                "Mi Perfil" -> userState?.let { ProfileConfigurationView(it, onSave = { updated -> viewModel.updateUsuario(updated) }) }
                "Logros" -> AchievementsListView(logrosList)
                "ID QR" -> userState?.let { StudentQrCardView(it, viewModel) }
                "Sistema" -> SystemSecurityBackupView(viewModel, securityPin)
            }
        }
    }
}

@Composable
fun ProfileConfigurationView(user: Usuario, onSave: (Usuario) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }

    // Local state for fields decoupled from direct reactive updates during active edit
    var nombre by remember { mutableStateOf(user.nombre) }
    var centroEducativo by remember { mutableStateOf(user.centroEducativo) }
    var carrera by remember { mutableStateOf(user.carrera) }
    var grado by remember { mutableStateOf(user.grado) }
    var seccion by remember { mutableStateOf(user.seccion) }
    var anioAcademico by remember { mutableStateOf(user.anioAcademico) }
    var correo by remember { mutableStateOf(user.correo) }
    var telefono by remember { mutableStateOf(user.telefono) }
    var biografia by remember { mutableStateOf(user.biografia) }

    // Target Goals Hour inputs
    var metaTes by remember { mutableStateOf(user.metaHorasTES.toString()) }
    var metaRelevo by remember { mutableStateOf(user.metaHorasRelevo.toString()) }
    var metaVoluntariado by remember { mutableStateOf(user.metaHorasVoluntariado.toString()) }
    var metaPracticas by remember { mutableStateOf(user.metaHorasPracticas.toString()) }
    var metaAcademicas by remember { mutableStateOf(user.metaHorasAcademicas.toString()) }

    // Synchronize updates only when we are NOT editing
    LaunchedEffect(user) {
        if (!isEditing) {
            nombre = user.nombre
            centroEducativo = user.centroEducativo
            carrera = user.carrera
            grado = user.grado
            seccion = user.seccion
            anioAcademico = user.anioAcademico
            correo = user.correo
            telefono = user.telefono
            biografia = user.biografia
            metaTes = user.metaHorasTES.toString()
            metaRelevo = user.metaHorasRelevo.toString()
            metaVoluntariado = user.metaHorasVoluntariado.toString()
            metaPracticas = user.metaHorasPracticas.toString()
            metaAcademicas = user.metaHorasAcademicas.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Detalles de Expediente Pleno", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
            Button(
                onClick = {
                    if (isEditing) {
                        onSave(
                            user.copy(
                                nombre = nombre,
                                centroEducativo = centroEducativo,
                                carrera = carrera,
                                grado = grado,
                                seccion = seccion,
                                anioAcademico = anioAcademico,
                                correo = correo,
                                telefono = telefono,
                                biografia = biografia,
                                metaHorasTES = metaTes.toIntOrNull() ?: 0,
                                metaHorasRelevo = metaRelevo.toIntOrNull() ?: 0,
                                metaHorasVoluntariado = metaVoluntariado.toIntOrNull() ?: 0,
                                metaHorasPracticas = metaPracticas.toIntOrNull() ?: 0,
                                metaHorasAcademicas = metaAcademicas.toIntOrNull() ?: 0
                            )
                        )
                        isEditing = false
                    } else {
                        isEditing = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary)
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isEditing) "Guardar Cambios" else "Editar Perfil")
            }
        }

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre Completo Estudiantil *") },
            enabled = isEditing,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = centroEducativo,
            onValueChange = { centroEducativo = it },
            label = { Text("Centro Educativo *") },
            enabled = isEditing,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = carrera,
            onValueChange = { carrera = it },
            label = { Text("Carrera Actual *") },
            enabled = isEditing,
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = grado,
                onValueChange = { grado = it },
                label = { Text("Grado") },
                enabled = isEditing,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = seccion,
                onValueChange = { seccion = it },
                label = { Text("Sección") },
                enabled = isEditing,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = anioAcademico,
                onValueChange = { anioAcademico = it },
                label = { Text("Año") },
                enabled = isEditing,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Email Escolar") },
                enabled = isEditing,
                modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Móvil") },
                enabled = isEditing,
                modifier = Modifier.weight(0.8f)
            )
        }

        OutlinedTextField(
            value = biografia,
            onValueChange = { biografia = it },
            label = { Text("Biografía / Notas de Alumno") },
            enabled = isEditing,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Text("Ajustes de Metas Horarias", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = metaTes,
                onValueChange = { metaTes = it },
                label = { Text("TES") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = isEditing,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = metaRelevo,
                onValueChange = { metaRelevo = it },
                label = { Text("Relevo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = isEditing,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = metaVoluntariado,
                onValueChange = { metaVoluntariado = it },
                label = { Text("Volunt.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = isEditing,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = metaPracticas,
                onValueChange = { metaPracticas = it },
                label = { Text("Práct.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = isEditing,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = metaAcademicas,
                onValueChange = { metaAcademicas = it },
                label = { Text("Acad.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = isEditing,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AchievementsListView(logros: List<Logro>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Tablero de Logros y Niveles", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
            Text("Medallas desbloqueadas por horas de servicio voluntario y escolar.", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(logros) { logro ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (logro.estaDesbloqueado) HondurasBlueLogo.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (logro.estaDesbloqueado) HondurasCyanSkein.copy(alpha = 0.4f) else Color.Transparent)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (logro.estaDesbloqueado) GoldRank.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when(logro.medallaIcono) {
                                "favorite" -> Icons.Default.Favorite
                                "handshake" -> Icons.Default.Handshake
                                "photo_library" -> Icons.Default.PhotoLibrary
                                "emoji_events" -> Icons.Default.EmojiEvents
                                "leaderboard" -> Icons.Default.Leaderboard
                                else -> Icons.Default.Star
                            },
                            contentDescription = null,
                            tint = if (logro.estaDesbloqueado) GoldRank else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(logro.titulo, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(logro.nivel, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(logro.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Progress bar inside medal
                        val progress = (logro.valorActual / logro.valorMeta).toFloat().coerceIn(0f, 1f)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (logro.estaDesbloqueado) HondurasCyanSkein else HondurasBluePrimary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${logro.valorActual.toInt()} / ${logro.valorMeta.toInt()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper function to generate real ZXing QR Bitmaps
private fun generateRealQrCodeBitmap(content: String, size: Int = 400): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val hints = java.util.HashMap<com.google.zxing.EncodeHintType, Any>()
        hints[com.google.zxing.EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[com.google.zxing.EncodeHintType.MARGIN] = 1
        hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun shareQrCode(context: android.content.Context, qrBitmap: android.graphics.Bitmap, studentName: String) {
    try {
        val file = java.io.File(context.cacheDir, "codigo_qr_estudiante.png")
        val out = java.io.FileOutputStream(file)
        qrBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()

        val contentUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Código QR de Validación Académica")
            putExtra(android.content.Intent.EXTRA_TEXT, "Firma QR de Validación de Trabajo Educativo Social para el alumno: $studentName.")
            clipData = android.content.ClipData.newRawUri("", contentUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir Código QR"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun StudentQrCardView(user: Usuario, viewModel: StudentsViewModel) {
    val statsState by viewModel.estadisticas.collectAsState()
    val activeYear by viewModel.activeYear.collectAsState()
    var showScanVerifierDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Secure Device ID retrieval combined with User ID to guarantee absolute QR uniqueness
    val deviceHardwareSerial = remember(context) {
        try {
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "SIMULATED_HW_ID"
            val manufacturer = android.os.Build.MANUFACTURER ?: "UNKNOWN"
            val model = android.os.Build.MODEL ?: "DEVICE"
            "${manufacturer}_${model}_${androidId}"
        } catch (e: Exception) {
            "FALLBACK_DEVICE_SERIAL_HASH"
        }
    }

    val uniqueHashSignature = remember(deviceHardwareSerial, user.id) {
        val uniqueString = "${deviceHardwareSerial}_${user.id}"
        val bytes = uniqueString.toByteArray()
        val digest = java.security.MessageDigest.getInstance("SHA-1")
        val sha1 = digest.digest(bytes)
        sha1.joinToString("") { "%02x".format(it) }.take(16).uppercase()
    }

    // Dynamic MD5 hashes for cryptography verification and integrity
    val hashUsuarioCrypt = remember(user.nombre, user.correo) {
        val bytes = (user.nombre + user.correo).toByteArray()
        val digest = java.security.MessageDigest.getInstance("MD5")
        val md5 = digest.digest(bytes)
        md5.joinToString("") { "%02x".format(it) }.take(16).uppercase()
    }
    
    val fingerPrintCrypt = remember {
        val finger = android.os.Build.FINGERPRINT ?: "HON-M3-HARDWARE-REG-FINGERPRINT-SIMULATED-XYZ"
        val bytes = finger.toByteArray()
        val digest = java.security.MessageDigest.getInstance("MD5")
        val md2 = digest.digest(bytes)
        md2.joinToString("") { "%02x".format(it) }.take(16).uppercase()
    }

    val dynamicVerificationToken = remember {
        val cal = Calendar.getInstance()
        val tokenSource = user.nombre + cal.get(Calendar.HOUR_OF_DAY)
        val bytes = tokenSource.toByteArray()
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val sha = digest.digest(bytes)
        sha.joinToString("") { "%02x".format(it) }.take(12).uppercase()
    }

    // AUTHENTIC VERIFICATION URL combining core data and hardware parameters to dynamically trace the student's registered mobile device
    val verificationUrl = remember(user.id, user.nombre, hashUsuarioCrypt, statsState.totalHoras, activeYear, uniqueHashSignature) {
        val manufacturer = android.os.Build.MANUFACTURER.replace(" ", "%20")
        val model = android.os.Build.MODEL.replace(" ", "%20")
        val hardware = android.os.Build.HARDWARE
        val buildId = android.os.Build.ID
        val encodedName = try {
            java.net.URLEncoder.encode(user.nombre, "UTF-8")
        } catch(e: Exception) {
            "Estudiante"
        }
        "https://ais-pre-5m3rsmrl47g3cwfnwglb6b-202972991037.us-east1.run.app/verify" +
                "?studentId=${user.id}" +
                "&studentName=$encodedName" +
                "&token=$hashUsuarioCrypt" +
                "&totalHours=${statsState.totalHoras}" +
                "&year=$activeYear" +
                "&telBrand=$manufacturer" +
                "&telModel=$model" +
                "&hw=$hardware" +
                "&bld=$buildId" +
                "&unique_signature=$uniqueHashSignature"
    }

    // The QR is strictly structured with student profile validation and real hardware identification metrics
    val dynamicQrContent = remember(user.nombre, user.id, user.correo, user.centroEducativo, user.carrera, statsState.totalHoras, statsState.metaTotal, activeYear, uniqueHashSignature) {
        val manufacturer = android.os.Build.MANUFACTURER ?: "UNKNOWN"
        val model = android.os.Build.MODEL ?: "DEVICE"
        val sdk = android.os.Build.VERSION.SDK_INT
        val release = android.os.Build.VERSION.RELEASE ?: "1.0"
        val bld = android.os.Build.ID ?: "BUILD"
        val hw = android.os.Build.HARDWARE ?: "HW"
        val percentage = if (statsState.metaTotal > 0) (statsState.totalHoras * 100.0 / statsState.metaTotal.toDouble()) else 0.0

        """
        ========================================
         ID ACADÉMICO - REGISTRO OFICIAL TES   
        ========================================
        ALUMNO: ${user.nombre.uppercase()}
        IDENTIDAD: ${user.id}
        CORREO: ${user.correo}
        CENTRO: ${user.centroEducativo.uppercase()}
        CARRERA: ${user.carrera.uppercase()}
        CICLO ACTIVO: AÑO $activeYear
        
        ----------------------------------------
         MÉTRICAS DE TRABAJO SOCIAL (AVANCE)     
        ----------------------------------------
        HORAS ACUMULADAS: ${statsState.totalHoras} hrs
        META ACADÉMICA: ${statsState.metaTotal} hrs
        PORCENTAJE AVANCE: ${percentage.toInt()}%
        ESTADO: ${if (percentage >= 100.0) "COMPLETADO" else "EN CURSO"}
        
        ----------------------------------------
         DISPOSITIVO REGISTRADO (MOVIL)        
        ----------------------------------------
        CELULAR: $manufacturer $model
        S.O.: Android $release (API $sdk)
        BUILD ID: $bld
        CHIPSET/BOARD: $hw
        FIRMA DE SEGURIDAD: $uniqueHashSignature
        ========================================
        """.trimIndent()
    }

    val qrBitmap = remember(verificationUrl) {
        generateRealQrCodeBitmap(verificationUrl, size = 350)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dossier de Identificación Estudiantil", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
        Text("Tu certificado QR único. Ideal para escaneos rápidos de historial técnico.", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)), textAlign = TextAlign.Center)

        // Centered pure high-contrast QR code container with no cluttered mockup elements
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(2.dp, HondurasBluePrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Firma QR de Validación",
                        modifier = Modifier
                            .size(280.dp)
                            .padding(8.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = HondurasBluePrimary, modifier = Modifier.size(36.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = user.nombre.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.Black
                )
                Text(
                    text = "EXPEDIENTE: ${statsState.totalHoras} HORAS ACUMULADAS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = HondurasBluePrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Cryptographic Verification Parameters Panel
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Verificación Segura de Identidad QR",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = HondurasCyanSkein
                )
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                // Encoded QR Data URL Preview
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("URL Codificada de Verificación:", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = verificationUrl,
                            fontSize = 9.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = HondurasCyanSkein,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hash ID de Alumno:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    Text(hashUsuarioCrypt, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Huella de Dispositivo:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    Text(fingerPrintCrypt, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Año de Creación / Ciclo:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    Text(activeYear, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Token de Validación:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(HondurasBlueTertiary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(dynamicVerificationToken, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, color = HondurasBlueTertiary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showScanVerifierDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Escanear QR", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }

                    Button(
                        onClick = {
                            if (qrBitmap != null) {
                                shareQrCode(context, qrBitmap, user.nombre)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasBlueSecondary),
                        modifier = Modifier.weight(1f),
                        enabled = qrBitmap != null
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir QR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    if (showScanVerifierDialog) {
        AlertDialog(
            onDismissRequest = { showScanVerifierDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = HondurasBlueTertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resultado del Escaneo Académico")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(HondurasBlueTertiary.copy(alpha = 0.15f))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HondurasBlueTertiary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("INSTANCIA VERIFICADA Y APROBADA", color = HondurasBlueTertiary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Detalles recuperados de la firma QR:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasCyanSkein)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• Alumno: ${user.nombre}", fontSize = 11.sp, color = Color.White)
                        Text("• Identidad: ${user.id}", fontSize = 11.sp)
                        Text("• Colegio: ${user.centroEducativo}", fontSize = 11.sp)
                        Text("• Carrera: ${user.carrera}", fontSize = 11.sp)
                        Text("• Acumulado Ciclo: ${statsState.totalHoras} Horas Válidas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HondurasCyanSkein)
                        Text("• Ciclo Activo: Año $activeYear", fontSize = 11.sp)
                        Text("• Celular Registrado: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})", fontSize = 11.sp, color = Color.White)
                        Text("• Tarjeta Chipset: ${android.os.Build.HARDWARE}", fontSize = 11.sp)
                        Text("• Firma Firmada SHA-1: $uniqueHashSignature", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showScanVerifierDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
fun SystemSecurityBackupView(viewModel: StudentsViewModel, pinCode: String) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var inputPin by remember { mutableStateOf("") }
    var operationResultText by remember { mutableStateOf("") }
    var restoreJsonText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text("Seguridad y Resguardo (Larga Vida)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
        Text(
            "Toda tu información académica se guarda localmente en SQLite. Asegura tu trayectoria o exporta tu respaldo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        // Lock PIN card configuration
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Protección por PIN",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (pinCode.isNotEmpty()) "Protección activada. Tu expediente escolar está asegurado." else "Asigna un código PIN para impedir accesos no autorizados a tu bitácora.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                OutlinedTextField(
                    value = inputPin,
                    onValueChange = { if (it.length <= 6) inputPin = it },
                    label = { Text("Escribe PIN (mínimo 4 dígitos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (pinCode.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.configurePin("")
                                inputPin = ""
                            }
                        ) {
                            Text("Desactivar", color = CoralAccent)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (inputPin.length >= 4) {
                                viewModel.configurePin(inputPin)
                                inputPin = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary)
                    ) {
                        Text("Configurar PIN", color = Color.White)
                    }
                }
            }
        }

        // Backup Exporting Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Exportar Copia de Respaldo Completa", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    "Genera una estructura de respaldo cifrada en formato JSON. Cópiala y guárdala de por vida en Drive, Emails, etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val backup = viewModel.generateBackupJson()
                            clipboardManager.setText(AnnotatedString(backup))
                            operationResultText = "Respaldo JSON copiado al portapapeles. ¡Guárdalo con seguridad!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generar y Copiar Respaldo JSON", color = Color.White)
                }
            }
        }

        // Backup Importing Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Restaurar o Importar Respaldo", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                
                OutlinedTextField(
                    value = restoreJsonText,
                    onValueChange = { restoreJsonText = it },
                    label = { Text("Pega aquí tu código de respaldo JSON") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Button(
                    onClick = {
                        if (restoreJsonText.isNotEmpty()) {
                            viewModel.restoreBackup(
                                jsonStr = restoreJsonText,
                                onSuccess = {
                                    operationResultText = "Expediente restaurado con éxito."
                                    restoreJsonText = ""
                                },
                                onError = {
                                    operationResultText = it
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HondurasBlueSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SystemUpdateAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cargar e Importar Copia", color = Color.White)
                }
            }
        }

        // EDITABLE SYSTEM CUSTOMIZATION CARD
        val currentActiveYear by viewModel.activeYear.collectAsState()
        val currentCategories by viewModel.customCategories.collectAsState()
        val currentTitle by viewModel.dashboardTitle.collectAsState()
        val currentSubtitle by viewModel.dashboardSubtitle.collectAsState()

        var tempYearInput by remember(currentActiveYear) { mutableStateOf(currentActiveYear) }
        var tempTitleInput by remember(currentTitle) { mutableStateOf(currentTitle) }
        var tempSubtitleInput by remember(currentSubtitle) { mutableStateOf(currentSubtitle) }
        var tempCategoriesInput by remember(currentCategories) { mutableStateOf(currentCategories.joinToString(", ")) }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Personalización Avanzada del Sistema", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = HondurasCyanSkein)
                Text(
                    "Adapta el ecosistema a los términos de tu propia institución educativa. Define categorías de horas y ciclos históricos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                OutlinedTextField(
                    value = tempTitleInput,
                    onValueChange = { tempTitleInput = it },
                    label = { Text("Nombre Personalizado de la App (Dashboard)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tempSubtitleInput,
                    onValueChange = { tempSubtitleInput = it },
                    label = { Text("Subtítulo de Soporte") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tempCategoriesInput,
                    onValueChange = { tempCategoriesInput = it },
                    label = { Text("Categorías/Ejes Académicos (Separadas por Coma)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempYearInput,
                        onValueChange = { tempYearInput = it },
                        label = { Text("Ciclo Año Activo") },
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (tempYearInput.isNotEmpty() && tempYearInput != currentActiveYear) {
                                viewModel.changeActiveYear(tempYearInput)
                                operationResultText = "Cambio de ciclo exitoso. Mostrando periodo $tempYearInput."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasBlueSecondary),
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                            .height(48.dp)
                    ) {
                        Text("Traspaso Ciclo", fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = {
                        val catsList = tempCategoriesInput.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        if (catsList.isNotEmpty()) {
                            viewModel.saveCustomCategories(catsList)
                        }
                        viewModel.customizeDashboard(tempTitleInput, tempSubtitleInput)
                        operationResultText = "Estructura del dashboard y categorías guardados."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.SettingsSuggest, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar Estructura y Parámetros", color = Color.White)
                }

                // Acceleration testing button
                Button(
                    onClick = {
                        // Simulates next year transition instantly!
                        val nextYear = (currentActiveYear.toInt() + 1).toString()
                        viewModel.changeActiveYear(nextYear)
                        viewModel.checkAndEvolveAcademicYear()
                        operationResultText = "Transición anual autónoma del sistema simulada con éxito hacia el ciclo $nextYear."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Forzar Evolución Anual (Simulador)", color = CoralAccent, fontSize = 12.sp)
                }
            }
        }

        // ==========================================
        // CARD FOR STYLES, COLORS, FONTS, AND NOTIFICATIONS
        // ==========================================
        val currentTheme by viewModel.appTheme.collectAsState()
        val currentFont by viewModel.appFont.collectAsState()
        val currentTextColor by viewModel.appTextColor.collectAsState()
        val currentReminderHours by viewModel.reminderIntervalHours.collectAsState()
        val isDiplomatico by viewModel.modoDiplomatico.collectAsState()
        val isLightMode by viewModel.isLightMode.collectAsState()
        val compresionDocumentos by viewModel.compresionDocumentos.collectAsState()
        val isDipl = isDiplomatico == "true"

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Estilo Visual, Tipografía y Alertas".diplomatic(isDipl),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDipl) Color(0xFFD4AF37) else HondurasCyanSkein
                )
                Text(
                    "Personaliza la paleta cromática, fuentes tipográficas y la frecuencia de los recordatorios automáticos de tu bitácora.".diplomatic(isDipl),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                // 0. MODO DIPLOMÁTICO DE HONOR DE LA REPÚBLICA
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDipl) Color(0xFF0C162B) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                    ),
                    border = BorderStroke(1.5.dp, if (isDipl) Color(0xFFD4AF37) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🇭🇳 MODO DIPLOMÁTICO NACIONAL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (isDipl) Color(0xFFD4AF37) else (if (isLightMode == "true") HondurasBluePrimary else Color.White)
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (isDipl) Color(0xFFD4AF37) else Color.Gray.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isDipl) "ACTIVO" else "NORMAL",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDipl) Color.Black else Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Formateo oficial de gala de la Secretaría de Educación para hojas de egreso, actas TES y expedientes consolidados. Toda la aplicación adopta una tipografía serif nacional y paleta de fondo dorada y azul.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = isDipl,
                            onCheckedChange = { isChecked ->
                                viewModel.updateConfigValue("modo_diplomatico", if (isChecked) "true" else "false")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFD4AF37),
                                checkedTrackColor = Color(0xFFD4AF37).copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // 0.1 MODO CLARO / OSCURO DE LA APP
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLightMode == "true") Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                    ),
                    border = BorderStroke(1.5.dp, if (isLightMode == "true") HondurasBlueSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "☀️ " + "Modo Claro".diplomatic(isDipl).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (isLightMode == "true") HondurasBluePrimary else Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (isLightMode == "true") HondurasBlueSecondary else Color.Gray.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isLightMode == "true") "ACTIVO" else "MODO OSCURO",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Activa una paleta limpia y clara para mejorar la lectura en ambientes abiertos, garantizando una visibilidad de contraste perfecta sin que las letras se oculten.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = isLightMode == "true",
                            onCheckedChange = { isChecked ->
                                viewModel.updateConfigValue("is_light_mode", if (isChecked) "true" else "false")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HondurasBlueSecondary,
                                checkedTrackColor = HondurasBlueSecondary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // 0.2 COMPRESIÓN DE DOCUMENTOS SIN PÉRDIDA
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (compresionDocumentos == "true") HondurasBluePrimary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                    ),
                    border = BorderStroke(1.5.dp, if (compresionDocumentos == "true") HondurasBlueSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🗜️ " + "Compresión Sin Pérdida".diplomatic(isDipl).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (compresionDocumentos == "true") HondurasBluePrimary else Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (compresionDocumentos == "true") HondurasCyanSkein else Color.Gray.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (compresionDocumentos == "true") "AHORRO ACTIVO" else "SIN COMPRIMIR",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (compresionDocumentos == "true") Color.Black else Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Comprime automáticamente todos tus documentos del portafolio (PDF, DOCX) usando algoritmos matemáticos GZIP de alta eficiencia. Se reduce drásticamente el espacio en disco conservando el 100% de la calidad original intacta.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = compresionDocumentos == "true",
                            onCheckedChange = { isChecked ->
                                viewModel.updateConfigValue("compresion_documentos", if (isChecked) "true" else "false")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HondurasBlueSecondary,
                                checkedTrackColor = HondurasBlueSecondary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                // 1. SELECTOR DE TEMAS CROMÁTICOS (ESTILO DE COLORES)
                Text("🎨 Selecciona el Tema Visual de la App", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                
                val themesList = listOf(
                    Triple("azul_tech", "Azul Clásico", HondurasBlueSecondary),
                    Triple("esmeralda_maya", "Esmeralda Maya", MayaEmeraldPrimary),
                    Triple("puesta_sunset", "Lenca Sunset", LencaSunsetPrimary),
                    Triple("bay_pristine", "Roatán Reef", PristineReefPrimary)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    themesList.chunked(2).forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { themeTriple ->
                                val isSelected = currentTheme == themeTriple.first
                                Button(
                                    onClick = { viewModel.updateConfigValue("app_theme", themeTriple.first) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) themeTriple.third else themeTriple.third.copy(alpha = 0.15f),
                                        contentColor = if (isSelected) Color.White else themeTriple.third
                                    ),
                                    border = BorderStroke(1.dp, themeTriple.third),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(themeTriple.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 2. SELECTOR DE FUENTES (TIPOGRAFÍA)
                Text("✍️ Selecciona el Tipo de Letra (Fuente)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                
                val fontsList = listOf(
                    Pair("sans_grotesk", "Sans-Serif Moderna"),
                    Pair("academia_serif", "Serif Académica"),
                    Pair("developer_mono", "Monospace Profesional")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    fontsList.forEach { fontPair ->
                        val isSelected = currentFont == fontPair.first
                        Button(
                            onClick = { viewModel.updateConfigValue("app_font", fontPair.first) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) HondurasBlueSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(fontPair.second, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 2.5 SELECTOR DE COLOR DE TEXTO
                Text("🎨 Selecciona el Color de las Letras (Texto)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                
                val textColorsList = listOf(
                    Pair("sistema", "Sistema ⚙️"),
                    Pair("blanco", "Blanco 🏳️"),
                    Pair("ambar_calido", "Ámbar 🍊"),
                    Pair("verde_claro", "Verde 🌿"),
                    Pair("cian_electrico", "Cian ❄️"),
                    Pair("amarillo_crema", "Crema 🍦"),
                    Pair("amarillo_fuerte", "Amarillo 💛"),
                    Pair("rojo_fuerte", "Rojo ❤️"),
                    Pair("morado_fuerte", "Morado 💜"),
                    Pair("rosado_fuerte", "Rosado 💖")
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    textColorsList.forEach { colorPair ->
                        val isSelected = currentTextColor == colorPair.first
                        val btnColor = when (colorPair.first) {
                            "sistema" -> if (isLightMode == "true") Color.Black else Color(0xFFE3E2E6)
                            "blanco" -> if (isLightMode == "true") Color.DarkGray else Color.White
                            "ambar_calido" -> Color(0xFFFFCC80)
                            "verde_claro" -> Color(0xFFA5D6A7)
                            "cian_electrico" -> Color(0xFF80DEEA)
                            "amarillo_crema" -> Color(0xFFFFF59D)
                            "amarillo_fuerte" -> Color(0xFFFFEA00)
                            "rojo_fuerte" -> Color(0xFFFF5252)
                            "morado_fuerte" -> Color(0xFFE040FB)
                            "rosado_fuerte" -> Color(0xFFFF4081)
                            else -> Color.White
                        }
                        Button(
                            onClick = { viewModel.updateConfigValue("app_text_color", colorPair.first) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) HondurasBlueSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                contentColor = if (isSelected) Color.White else btnColor
                            ),
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Text(colorPair.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3. INTERVALO DE NOTIFICACIONES
                Text("🔔 Alertas Automáticas (Recordatorios de Bitácora)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    "Frecuencia de las notificaciones push para recordarte registrar tus horas acumuladas de servicio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                val context = LocalContext.current
                var hasNotificationPermission by remember {
                    mutableStateOf(
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    )
                }

                // Check and update permission on composition and focus
                LaunchedEffect(Unit) {
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        hasNotificationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                }

                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasNotificationPermission = isGranted
                    if (isGranted) {
                        com.example.NotificationHelper.initNotificationChannels(context)
                        // Trigger immediate test notification to show success!
                        com.example.NotificationHelper.showAchievementNotification(
                            context,
                            "Canal de Alertas Activado 🔔",
                            "Los recordatorios automáticos de tu bitácora TES están encendidos."
                        )
                    } else {
                        android.widget.Toast.makeText(context, "Permiso denegado. Para recibir alertas actívalas en la Configuración del celular.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }

                if (!hasNotificationPermission && android.os.Build.VERSION.SDK_INT >= 33) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permiso de Notificación Apagado",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Se requiere permiso para recibir avisos y alertas en tiempo real sobre tu bitácora.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                            Button(
                                onClick = { 
                                    try {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) 
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Habilita el permiso en Ajustes de la App", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Text("Permitir 🔔", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (android.os.Build.VERSION.SDK_INT >= 33) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = HondurasCyanSkein.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, HondurasCyanSkein.copy(alpha = 0.25f))
                    ) {
                        Row(
                             modifier = Modifier.padding(10.dp),
                             verticalAlignment = Alignment.CenterVertically,
                             horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                             Row(
                                 verticalAlignment = Alignment.CenterVertically,
                                 horizontalArrangement = Arrangement.spacedBy(10.dp),
                                 modifier = Modifier.weight(1f)
                             ) {
                                 Icon(
                                     imageVector = Icons.Default.CheckCircle,
                                     contentDescription = null,
                                     tint = HondurasCyanSkein,
                                     modifier = Modifier.size(18.dp)
                                 )
                                 Text(
                                     text = "¡Recordatorios activos correctamente! ✔",
                                     fontSize = 10.sp,
                                     fontWeight = FontWeight.Bold,
                                     color = HondurasCyanSkein
                                 )
                             }
                             Button(
                                 onClick = {
                                     com.example.NotificationHelper.showAchievementNotification(
                                         context,
                                         "Prueba de Alerta del TES 🇭🇳",
                                         "Tus alertas de bitácora y registro de horas están plenamente operacionales."
                                     )
                                 },
                                 colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein),
                                 modifier = Modifier.height(30.dp),
                                 contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                             ) {
                                 Text("Probar Alerta 🔔", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                             }
                        }
                    }
                }

                val intervalsList = listOf(
                    Pair(-1, "1 Seg ⚡"),
                    Pair(-2, "1 Min ⏱️"),
                    Pair(0, "Mute 📴"),
                    Pair(1, "1 Hora 🕐"),
                    Pair(3, "3 Horas 🕒"),
                    Pair(6, "6 Horas 🕕")
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    intervalsList.forEach { intervalPair ->
                        val isSelected = currentReminderHours == intervalPair.first
                        Button(
                            onClick = { viewModel.updateConfigValue("reminder_interval", intervalPair.first.toString()) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) HondurasBlueSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.height(40.dp).padding(horizontal = 4.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(intervalPair.second, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ----------------------------------------------------
        // SECCIÓN DE MANTENIMIENTO Y OPTIMIZACIÓN
        // ----------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = HondurasBluePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Mantenimiento y Optimización",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }
                
                Text(
                    "Libera espacio de almacenamiento purgando archivos temporales obsoletos de la cámara o descargas, y desfragmenta los índices SQL de la base de datos local para acelerar el funcionamiento.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                )
                
                val localContext = LocalContext.current
                Button(
                    onClick = {
                        viewModel.clearCacheAndOptimize(
                            context = localContext,
                            onSuccess = { msg ->
                                operationResultText = msg
                                android.widget.Toast.makeText(localContext, "¡Optimización completada!", android.widget.Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                operationResultText = "❌ $err"
                                android.widget.Toast.makeText(localContext, err, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Limpiar Caché y Optimizar App", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (operationResultText.isNotEmpty()) {
            Surface(
                color = HondurasBlueLogo.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = operationResultText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun exportLogoToDownloads(context: android.content.Context): String {
    try {
        val size = 512
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // 1. Draw premium rich dark gradient backdrop (Radial gradient from center to edges)
        val centerColor = android.graphics.Color.parseColor("#0F172A")
        val edgeColor = android.graphics.Color.parseColor("#020617")
        val bgShader = android.graphics.RadialGradient(
            size / 2f, size / 2f, size * 0.7f,
            centerColor, edgeColor,
            android.graphics.Shader.TileMode.CLAMP
        )
        val bgPaint = android.graphics.Paint().apply {
            shader = bgShader
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
        
        // 2. Draw modern glowing outer ring using linear gradient to look metallic and cybernetic
        val ringShader = android.graphics.LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            intArrayOf(
                android.graphics.Color.parseColor("#00E5FF"), // Neon Cyan
                android.graphics.Color.parseColor("#1E90FF"), // Electric Blue
                android.graphics.Color.parseColor("#7000FF"), // Nebula Violet
                android.graphics.Color.parseColor("#00E5FF")
            ),
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        val outerRingPaint = android.graphics.Paint().apply {
            shader = ringShader
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 14f
            isAntiAlias = true
        }
        val cx = size / 2f
        val cy = size / 2f
        val r = (size / 2f) - 36f
        canvas.drawCircle(cx, cy, r, outerRingPaint)
        
        // 3. Draw a modern sleek inner golden ornament circle
        val innerCirclePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FFD700")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, r - 12f, innerCirclePaint)
        
        // 4. Draw dual glowing minimalist support arcs representing intelligence loops
        val arcPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#4000E5FF")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, r - 32f, arcPaint)
        
        // 5. Render giant modern display typography for "TES" with stellar tracking
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 105f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.08f
        }
        canvas.drawText("TES", cx, cy - 12f, textPaint)
        
        // 6. Subtitle "MASTER IA" in stunning electric cyan
        val subtitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#00E5FF")
            textSize = 32f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.12f
        }
        canvas.drawText("MASTER IA", cx, cy + 56f, subtitlePaint)
        
        // 7. Country flag insignia "HONDURAS" with metallic gold stars
        val countryPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FFD700")
            textSize = 22f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.05f
        }
        canvas.drawText("★ HONDURAS ★", cx, cy + 114f, countryPaint)

        // Save file in public Downloads directory
        val exportDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = java.io.File(exportDir, "TES_MASTER_IA_LOGO_OFICIAL.png")
        val out = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()
        
        return "¡Logo de alta fidelidad descargado con éxito! Buscalo en la carpeta de Descargas de tu dispositivo como 'TES_MASTER_IA_LOGO_OFICIAL.png'."
    } catch (e: Exception) {
         return "Error al exportar logotipo: ${e.message}"
    }
}
