package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= 33) { // Build.VERSION_CODES.TIRAMISU
            try {
                val launcher = registerForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        NotificationHelper.initNotificationChannels(this)
                    }
                }
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    NotificationHelper.initNotificationChannels(this)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            NotificationHelper.initNotificationChannels(this)
        }

        setContent {
            val viewModel: StudentsViewModel = viewModel()
            val currentTheme by viewModel.appTheme.collectAsState()
            val currentFont by viewModel.appFont.collectAsState()
            val currentTextColor by viewModel.appTextColor.collectAsState()
            val isDiplomático by viewModel.modoDiplomatico.collectAsState()
            val isLightMode by viewModel.isLightMode.collectAsState()

            MyApplicationTheme(
                themeChoice = currentTheme,
                fontChoice = currentFont,
                textColorChoice = currentTextColor,
                modoDiplomaticoChoice = isDiplomático,
                isLightModeChoice = isLightMode
            ) {
                val isConfigLoaded by viewModel.isConfigLoaded.collectAsState()
                val isLocked by viewModel.isLocked.collectAsState()
                val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!isConfigLoaded) {
                        FullscreenSplashView()
                    } else if (isLocked) {
                        PinLockOverlayView(viewModel = viewModel)
                    } else if (!onboardingCompleted) {
                        OnboardingTutorialView(viewModel = viewModel)
                    } else {
                        MainNavigationContainer(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenSplashView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080C14)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF00E5FF)
            )
            Text(
                text = "MI TES & RELEVO POR VIDA",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
            )
            CircularProgressIndicator(
                color = Color(0xFF00E5FF),
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
fun MainNavigationContainer(viewModel: StudentsViewModel) {
    var activeTab by remember { mutableStateOf("Inicio") } // "Inicio", "Bitácora", "Línea de Tiempo", "Portafolio", "IA Académica", "Sistema"
    val dashboardTitle by viewModel.dashboardTitle.collectAsState()
    val isDiplomático by viewModel.modoDiplomatico.collectAsState()
    val isDipl = isDiplomático == "true"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = dashboardTitle.diplomatic(isDipl),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Logo",
                            tint = HondurasCyanSkein,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.lockApp() }) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = HondurasBlueLogo
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("app_navigation_bar")
            ) {
                val destinations = listOf(
                    NavigationDest("Inicio", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
                    NavigationDest("Bitácora", Icons.Default.Assignment, Icons.Outlined.Assignment),
                    NavigationDest("Historial", Icons.Default.History, Icons.Outlined.History),
                    NavigationDest("Portafolio", Icons.Default.FolderOpen, Icons.Outlined.Folder),
                    NavigationDest("Galería", Icons.Default.PhotoLibrary, Icons.Outlined.PhotoLibrary),
                    NavigationDest("Expediente", Icons.Default.Badge, Icons.Outlined.Badge)
                )

                destinations.forEach { dest ->
                    val isSelected = activeTab == dest.title
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = dest.title },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) dest.activeIcon else dest.inactiveIcon,
                                contentDescription = dest.title,
                                tint = if (isSelected) HondurasBluePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        label = {
                            Text(
                                text = dest.title.diplomatic(isDipl),
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) HondurasBlueLogo else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = HondurasCyanSkein.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "Inicio" -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToActivities = { activeTab = "Bitácora" },
                    onNavigateToPortfolio = { activeTab = "Portafolio" }
                )
                "Bitácora" -> ActivitiesScreen(viewModel = viewModel)
                "Historial" -> TimelineScreen(viewModel = viewModel)
                "Portafolio" -> EvidenceAndOcrScreen(viewModel = viewModel)
                "Galería" -> MediaGalleryScreen(viewModel = viewModel)
                "Expediente" -> ProfileAndSystemScreen(viewModel = viewModel)
            }
        }
    }
}

data class NavigationDest(
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

@Composable
fun PinLockOverlayView(viewModel: StudentsViewModel) {
    var codeText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SlateDarkBackground, SlateDarkSurface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(320.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(HondurasCyanSkein.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = HondurasCyanSkein,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Expediente Protegido",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                )
                Text(
                    text = "MI TES & RELEVO POR VIDA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = HondurasCyanSkein,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            // Dot indicators represent digit counts
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                for (i in 1..4) {
                    val isActive = codeText.length >= i
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) HondurasCyanSkein else Color.White.copy(alpha = 0.15f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isActive) HondurasCyanSkein else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = CoralAccent,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Custom secure numeric keyboard layout
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val keyboardKeys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "OK")
                )

                keyboardKeys.forEach { rowKeys ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowKeys.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.5f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .clickable {
                                        when (key) {
                                            "C" -> {
                                                codeText = ""
                                                errorMessage = ""
                                            }
                                            "OK" -> {
                                                if (viewModel.verifyPin(codeText)) {
                                                    codeText = ""
                                                    errorMessage = ""
                                                } else {
                                                    codeText = ""
                                                    errorMessage = "PIN incorrecto. Reintente."
                                                }
                                            }
                                            else -> {
                                                if (codeText.length < 4) {
                                                    codeText += key
                                                    errorMessage = ""
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = if (key == "OK") 14.sp else 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (key == "OK") HondurasBlueTertiary else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingTutorialView(viewModel: StudentsViewModel) {
    var step by remember { mutableStateOf(1) } // 1, 2, 3
    
    // Step 2 Form States
    var nombre by remember { mutableStateOf("") }
    var centroEducativo by remember { mutableStateOf("") }
    var carrera by remember { mutableStateOf("BTP en Informática") }
    var cicloActivo by remember { mutableStateOf("2026") }

    // Step 3 Slider Goals
    var metaTES by remember { mutableStateOf(100f) }
    var metaRelevo by remember { mutableStateOf(40f) }
    var metaVoluntariado by remember { mutableStateOf(50f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SlateDarkBackground, SlateDarkSurface)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top App Brand logo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HondurasBluePrimary.copy(alpha = 0.15f))
                        .border(1.5.dp, HondurasCyanSkein, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = HondurasCyanSkein,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MI TES & RELEVO POR VIDA",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Ecosistema Académico Inteligente Autónomo",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = HondurasCyanSkein,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Step Card Slider
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (step) {
                        1 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(HondurasBlueSecondary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = HondurasBlueSecondary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = "¡Hola Estudiante!",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Te damos la bienvenida al sistema académico definitivo de Honduras para la administración del Trabajo Educativo Social (TES), relevo por la vida y voluntariados cívicos de forma 100% automatizada y privada por defecto.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Fast benefit checklist
                                OnboardingBenefitRow(icon = Icons.Default.Update, title = "Tiempo Inteligente", desc = "Auto-cálculo de duraciones exactas con relojes directos.")
                                OnboardingBenefitRow(icon = Icons.Default.GpsFixed, title = "Rutas & Geolocalización", desc = "Vincula coordenadas GPS opcionales con geocodificación.")
                                OnboardingBenefitRow(icon = Icons.Default.FolderZip, title = "Bóveda Segura", desc = "Guarda hasta 6 evidencias multimedia e importa PDF/DOCX.")
                            }
                        }
                        2 -> {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = HondurasBluePrimary)
                                    Text(
                                        text = "Tu Expediente Oficial",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                                    )
                                }
                                Text(
                                    text = "Ingresa tus datos del ciclo activo para personalizar plantillas y reportes institucionales automatizados.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                OutlinedTextField(
                                    value = nombre,
                                    onValueChange = { nombre = it },
                                    label = { Text("Nombre Completo del Alumno *") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = HondurasBluePrimary) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = centroEducativo,
                                    onValueChange = { centroEducativo = it },
                                    label = { Text("Colegio / Escuela de Honduras *") },
                                    leadingIcon = { Icon(Icons.Default.HomeWork, contentDescription = null, tint = HondurasBluePrimary) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = carrera,
                                    onValueChange = { carrera = it },
                                    label = { Text("Carrera Técnica o Bachillerato") },
                                    leadingIcon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = HondurasBluePrimary) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = cicloActivo,
                                    onValueChange = { if (it.all { char -> char.isDigit() }) cicloActivo = it },
                                    label = { Text("Ciclo de Año Escolar Activo") },
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = HondurasBluePrimary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        3 -> {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.QueryStats, contentDescription = null, tint = HondurasCyanSkein)
                                    Text(
                                        text = "Define tus Metas",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                                    )
                                }
                                Text(
                                    text = "Elige la meta de horas necesarias para graduarte. Podrás modificarlas en cualquier momento.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                OnboardingGoalSlider(
                                    title = "Horas TES (Trabajo Educativo Social)",
                                    value = metaTES,
                                    onValueChange = { metaTES = it },
                                    valueRange = 50f..200f,
                                    color = HondurasBluePrimary
                                )

                                OnboardingGoalSlider(
                                    title = "Relevo por la Vida",
                                    value = metaRelevo,
                                    onValueChange = { metaRelevo = it },
                                    valueRange = 20f..100f,
                                    color = CoralAccent
                                )

                                OnboardingGoalSlider(
                                    title = "Voluntariado Social",
                                    value = metaVoluntariado,
                                    onValueChange = { metaVoluntariado = it },
                                    valueRange = 10f..120f,
                                    color = HondurasCyanSkein
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Navigation & Progress Actions inside Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Carousel indicators
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in 1..3) {
                                val active = step == i
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) HondurasCyanSkein else Color.White.copy(alpha = 0.15f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (active) HondurasCyanSkein else Color.White.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (step > 1) {
                                OutlinedButton(
                                    onClick = { step-- },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("Atrás")
                                }
                            }

                            val isFormValid = if (step == 2) nombre.isNotBlank() && centroEducativo.isNotBlank() else true

                            Button(
                                onClick = {
                                    if (step < 3) {
                                        step++
                                    } else {
                                        viewModel.completeOnboarding(
                                            studentName = nombre.trim(),
                                            school = centroEducativo.trim(),
                                            career = carrera.trim(),
                                            cycle = cicloActivo.trim(),
                                            goalTES = metaTES.toInt(),
                                            goalRelevo = metaRelevo.toInt(),
                                            goalVoluntariado = metaVoluntariado.toInt()
                                        )
                                    }
                                },
                                enabled = isFormValid,
                                colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text(if (step == 3) "¡Listos, Comenzar!" else "Continuar")
                                if (step == 3) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingBenefitRow(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HondurasCyanSkein,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            Text(text = desc, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
        }
    }
}

@Composable
fun OnboardingGoalSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            Text(text = "${value.toInt()} hrs", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, color = color))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
    }
}
