package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Actividad
import com.example.data.ChatMessage
import com.example.data.FileStorageHelper
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicAiScreen(viewModel: StudentsViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val activitiesList by viewModel.actividades.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    var cameraTempFile by remember { mutableStateOf<File?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it).use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        selectedBitmap = bitmap
                    } else {
                        Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraTempFile != null) {
            try {
                val bitmap = BitmapFactory.decodeFile(cameraTempFile?.absolutePath)
                if (bitmap != null) {
                    selectedBitmap = bitmap
                } else {
                    Toast.makeText(context, "No se pudo cargar la foto", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error cámara: ${e.message}", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Se requiere permiso de cámara para tomar fotos.", Toast.LENGTH_SHORT).show()
        }
    }

    var customPromptText by remember { mutableStateOf("") }
    var selectedActivityToDraft by remember { mutableStateOf<Actividad?>(null) }
    var showActivityMenu by remember { mutableStateOf(false) }
    var isDraftorExpanded by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val listState = rememberLazyListState()

    // Smooth scroll to bottom on new messages
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    val presetSuggestions = listOf(
        "Ideas de voluntariado en Honduras",
        "Redactar marco introductorio para mi bitácora",
        "Corregir la aclaración formal de mi práctica",
        "Estructura reglamentaria general del TES"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App top header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Asistente Académico IA",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = HondurasBlueLogo)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (viewModel.isGeminiActive()) HondurasBlueTertiary else CoralAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (viewModel.isGeminiActive()) "Tutor Gemini Activado" else "Tutor de Respaldo Offline",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        )
                    }
                }

                // Force clear chat action
                if (chatHistory.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearChatHistory() },
                        colors = ButtonDefaults.textButtonColors(contentColor = CoralAccent)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limpiar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Main Area Layout split between scroll and actions
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // LazyColumn for persistent conversations
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (chatHistory.isEmpty()) {
                    // Welcome & instructions Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = HondurasBluePrimary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "¡Hola! Soy tu tutor de IA",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Estoy listo para apoyarte en la estructuración de bitácoras académicas, objetivos de TES, voluntariados, redacción de informes técnicos de prácticas y revisión ortográfica.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Temas sugeridos para iniciar hoy:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = HondurasBluePrimary)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        presetSuggestions.forEach { suggestion ->
                            Card(
                                onClick = { customPromptText = suggestion },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = HondurasBlueSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(suggestion, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                }
                            }
                        }
                    }
                } else {
                    // Chat messages list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chat_scroll_list")
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatHistory) { message ->
                            ChatBubbleItem(
                                message = message,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(message.text))
                                },
                                onEdit = {
                                    editingMessage = message
                                    customPromptText = message.text
                                }
                            )
                        }

                        if (aiLoading) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(HondurasBlueSecondary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Card(
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Pensando...", 
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            )
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.5.dp,
                                                color = HondurasCyanSkein
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Expanding Drawer Box for drafting activities right inside AI assistant
            Card(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Toggle header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDraftorExpanded = !isDraftorExpanded }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = GoldRank, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Redacción Automática de Actividad",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = HondurasBlueLogo)
                            )
                        }
                        Icon(
                            imageVector = if (isDraftorExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = isDraftorExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            Text(
                                "Selecciona una de tus actividades guardadas y la IA generará una descripción formal, conclusiones y recomendaciones directamente en la conversación.",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom Dropdown Picker
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    onClick = { showActivityMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedActivityToDraft?.titulo ?: "Seleccionar una actividad completada...",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (selectedActivityToDraft != null) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedActivityToDraft != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }

                                DropdownMenu(
                                    expanded = showActivityMenu,
                                    onDismissRequest = { showActivityMenu = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    if (activitiesList.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No hay actividades registradas en bitácora") },
                                            onClick = { showActivityMenu = false }
                                        )
                                    } else {
                                        activitiesList.forEach { act ->
                                            DropdownMenuItem(
                                                text = { Text("[${act.categoria}] ${act.titulo}", maxLines = 1) },
                                                onClick = {
                                                    selectedActivityToDraft = act
                                                    showActivityMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                enabled = (selectedActivityToDraft != null && !aiLoading),
                                onClick = {
                                    selectedActivityToDraft?.let {
                                        viewModel.generateActivityDraft(it)
                                        isDraftorExpanded = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HondurasBlueSecondary,
                                    disabledContainerColor = HondurasBlueSecondary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generar Redacción en Historial", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Input message box bar at absolute bottom
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (aiLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(HondurasBlueLogo.copy(alpha = 0.08f))
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = HondurasBluePrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Tutor IA escribiendo propuesta...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HondurasBlueLogo
                            )
                        }
                    }

                    // Editing indicator banner
                    if (editingMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(HondurasBlueSecondary.copy(alpha = 0.12f))
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = HondurasBlueSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Editando consulta anterior...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HondurasBlueSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingMessage = null
                                    customPromptText = ""
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancelar edición",
                                    modifier = Modifier.size(14.dp),
                                    tint = CoralAccent
                                )
                            }
                        }
                    }

                    // Suggestions quick-bar above input text field if writing
                    if (chatHistory.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(presetSuggestions) { item ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                        .clickable { customPromptText = item }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(item, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = HondurasBlueLogo)
                                }
                            }
                        }
                    }

                    // Image thumbnail preview for active multi-modal prompt
                    if (selectedBitmap != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, HondurasBluePrimary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = selectedBitmap!!.asImageBitmap(),
                                    contentDescription = "Imagen seleccionada",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Imagen lista para procesar",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = HondurasBlueLogo)
                                    )
                                    Text(
                                        text = "Se enviará al tutor IA con tu consulta, bro",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    )
                                }
                                IconButton(
                                    onClick = { selectedBitmap = null },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Eliminar imagen",
                                        modifier = Modifier.size(14.dp),
                                        tint = CoralAccent
                                    )
                                }
                            }
                        }
                    }

                    // Raw input bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Image attachments quick handlers
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Galería", tint = HondurasBlueLogo, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Tomar Foto", tint = HondurasBlueLogo, modifier = Modifier.size(20.dp))
                        }

                        OutlinedTextField(
                            value = customPromptText,
                            onValueChange = { customPromptText = it },
                            placeholder = { Text("Escribe una pregunta o copia tu texto...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_text_input"),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HondurasBluePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            ),
                            maxLines = 4,
                            leadingIcon = {
                                IconButton(
                                    onClick = {
                                        val clipText = clipboardManager.getText()?.text ?: ""
                                        if (clipText.isNotEmpty()) {
                                            customPromptText += clipText
                                            Toast.makeText(context, "Texto pegado del portapapeles, bro", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "El portapapeles está vacío, bro", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Pegar Texto",
                                        modifier = Modifier.size(16.dp),
                                        tint = HondurasBlueLogo.copy(alpha = 0.5f)
                                    )
                                }
                            },
                            trailingIcon = {
                                if (customPromptText.isNotEmpty()) {
                                    IconButton(onClick = { customPromptText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        )

                        FloatingActionButton(
                            onClick = {
                                val trimmed = customPromptText.trim()
                                if (trimmed.isNotEmpty() || selectedBitmap != null) {
                                    val editMsg = editingMessage
                                    if (editMsg != null) {
                                        viewModel.editAndRegenerateChat(editMsg.id, trimmed)
                                        editingMessage = null
                                    } else {
                                        viewModel.askGemini(trimmed, selectedBitmap)
                                    }
                                    customPromptText = ""
                                    selectedBitmap = null
                                }
                            },
                            containerColor = HondurasBluePrimary,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("submit_button"),
                            elevation = FloatingActionButtonDefaults.elevation(2.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Enviar", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    onCopy: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    val isUser = message.isUser
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
    }

    val bubbleBg = if (isUser) {
        HondurasBluePrimary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    val textColor = if (isUser) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Icon for Assistant on left
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(HondurasBlueSecondary)
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Message panel
            Card(
                shape = bubbleShape,
                colors = CardDefaults.cardColors(containerColor = bubbleBg),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    val textLength = message.text.length
                    var isExpanded by remember { mutableStateOf(false) }

                    if (textLength > 500 && !isExpanded) {
                        Box(
                            modifier = Modifier
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 2.dp)
                        ) {
                            SelectionContainer {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = textColor,
                                        lineHeight = 19.sp
                                    )
                                )
                            }
                        }
                        
                        TextButton(
                            onClick = { isExpanded = true },
                            modifier = Modifier.padding(top = 4.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isUser) Color.White else HondurasBlueLogo
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ver texto completo (Respuesta extensa)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) Color.White else HondurasBlueLogo
                                )
                            }
                        }
                    } else {
                        Column {
                            SelectionContainer {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = textColor,
                                        lineHeight = 19.sp
                                    )
                                )
                            }
                            if (textLength > 500 && isExpanded) {
                                TextButton(
                                    onClick = { isExpanded = false },
                                    modifier = Modifier.padding(top = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isUser) Color.White else HondurasBlueLogo
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Simplificar vista",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUser) Color.White else HondurasBlueLogo
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick click-copy icon for Assistant answers
                        if (!isUser) {
                            var showCopiedText by remember { mutableStateOf(false) }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        onCopy()
                                        showCopiedText = true
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (showCopiedText) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copiar texto",
                                    tint = HondurasBlueLogo.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (showCopiedText) "¡Copiado!" else "Copiar",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HondurasBlueLogo.copy(alpha = 0.6f)
                                )

                                LaunchedEffect(showCopiedText) {
                                    if (showCopiedText) {
                                        kotlinx.coroutines.delay(1800)
                                        showCopiedText = false
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        } else if (onEdit != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onEdit() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar mensaje",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Editar",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        // Time sticker
                        val sdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                        val formattedTime = remember(message.timestamp) { sdf.format(Date(message.timestamp)) }
                        Text(
                            text = formattedTime,
                            fontSize = 8.sp,
                            color = if (isUser) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    }
}
