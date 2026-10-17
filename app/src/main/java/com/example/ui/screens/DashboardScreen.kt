package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Actividad
import com.example.data.Logro
import com.example.data.Usuario
import com.example.ui.theme.*
import com.example.ui.viewmodel.Stats
import com.example.ui.viewmodel.StudentsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: StudentsViewModel,
    onNavigateToActivities: () -> Unit,
    onNavigateToPortfolio: () -> Unit
) {
    val userState by viewModel.usuario.collectAsState()
    val statsState by viewModel.estadisticas.collectAsState()
    val actsList by viewModel.actividades.collectAsState()
    val logsList by viewModel.logros.collectAsState()
    val activeYearState by viewModel.activeYear.collectAsState()
    val filteredActsList = remember(actsList, activeYearState) {
        actsList.filter { it.anioCiclo == activeYearState }
    }

    val scrollState = rememberScrollState()
    var showHelpTour by remember { mutableStateOf(false) }
    val dashboardOrderState by viewModel.dashboardOrder.collectAsState()
    var isReorderMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp)
        ) {
            // Header: Welcoming Student Card
            HeaderStudentCard(userState)

            // Reorder Controller Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Panel de Control",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )
                TextButton(
                    onClick = { isReorderMode = !isReorderMode },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isReorderMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("toggle_reorder_button")
                ) {
                    Icon(
                        imageVector = if (isReorderMode) Icons.Default.CheckCircle else Icons.Default.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isReorderMode) "Guardar Posición ✔" else "Mover Apartados ⚡",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val sectionKeys = dashboardOrderState.split(",").filter { it.isNotBlank() }

            sectionKeys.forEachIndexed { idx, key ->
                if (isReorderMode) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = when (key) {
                                        "guia" -> Icons.Default.Lightbulb
                                        "year" -> Icons.Default.CalendarMonth
                                        "progress" -> Icons.Default.DonutLarge
                                        "calendar" -> Icons.Default.Today
                                        "categories" -> Icons.Default.Category
                                        else -> Icons.Default.CardMembership
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = when (key) {
                                            "guia" -> "Guía de Éxito Rápida"
                                            "year" -> "Selector de Año Lectivo"
                                            "progress" -> "Meta de Horas de Servicio"
                                            "calendar" -> "Historial Mensual Activo"
                                            "categories" -> "Categorías de Proyección"
                                            else -> "Logros y Bitácoras Recientes"
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Posición actual: #${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        if (idx > 0) {
                                            val mutated = sectionKeys.toMutableList()
                                            val temp = mutated[idx]
                                            mutated[idx] = mutated[idx - 1]
                                            mutated[idx - 1] = temp
                                            viewModel.updateConfigValue("dashboard_order", mutated.joinToString(","))
                                        }
                                    },
                                    enabled = idx > 0,
                                    modifier = Modifier.size(32.dp).testTag("move_up_$key")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Mover arriba",
                                        tint = if (idx > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (idx < sectionKeys.size - 1) {
                                            val mutated = sectionKeys.toMutableList()
                                            val temp = mutated[idx]
                                            mutated[idx] = mutated[idx + 1]
                                            mutated[idx + 1] = temp
                                            viewModel.updateConfigValue("dashboard_order", mutated.joinToString(","))
                                        }
                                    },
                                    enabled = idx < sectionKeys.size - 1,
                                    modifier = Modifier.size(32.dp).testTag("move_down_$key")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Mover abajo",
                                        tint = if (idx < sectionKeys.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Render the real component based on order string safely
                    when (key) {
                        "guia" -> {
                            GuiaExitoQuickCard(onConfigureTour = { showHelpTour = true })
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        "year" -> {
                            AcademicYearQuickSwitcher(viewModel = viewModel)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        "progress" -> {
                            MainHoursProgressCard(statsState)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        "calendar" -> {
                            AndroidCalendarioInteligente(filteredActsList)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        "categories" -> {
                            CategoriasStatsRow()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        "recent" -> {
                            RecentSummarySection(
                                actsList = filteredActsList,
                                logsList = logsList,
                                onNavigateToActivities = onNavigateToActivities,
                                onNavigateToPortfolio = onNavigateToPortfolio
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        if (showHelpTour) {
            InteractiveGuideDialog(viewModel = viewModel, onDismiss = { showHelpTour = false })
        }
    }
}

@Composable
fun HeaderStudentCard(user: Usuario?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        HondurasBlueLogo,
                        SlateDarkBackground
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Profile Circular Icon Placeholder / Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(HondurasCyanSkein.copy(alpha = 0.2f))
                    .border(2.dp, HondurasCyanSkein, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = HondurasCyanSkein,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hola, ${user?.nombre ?: "Estudiante"}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${user?.carrera ?: "Bachillerato en Informática"}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = CoralAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = user?.centroEducativo ?: "Instituto de Honduras",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun MainHoursProgressCard(stats: Stats) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("hours_progress_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Resumen de Meta General",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Progress circular representation and numeric summary side-by-side
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular percentage progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    val progressRatio = (stats.totalHoras / stats.metaTotal.coerceAtLeast(1)).toFloat()
                    val progressPercentage = (progressRatio * 100).coerceAtMost(100f).toInt()

                    CircularProgressIndicator(
                        progress = { progressRatio.coerceAtMost(1f) },
                        modifier = Modifier.fillMaxSize(),
                        color = HondurasCyanSkein,
                        strokeWidth = 10.dp,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$progressPercentage%",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = HondurasCyanSkein,
                                fontSize = 22.sp
                            )
                        )
                        Text(
                            text = "Honduras",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Horas Logradas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("${stats.totalHoras} hrs", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = HondurasBluePrimary))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Meta Académica", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("${stats.metaTotal} hrs", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = HondurasBluePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (stats.totalHoras >= stats.metaTotal) "¡Felicidades! Meta completada" else "Sigue registrando para graduarte",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (stats.totalHoras >= stats.metaTotal) HondurasBlueTertiary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(16.dp))

            // Individual Sub-metas Categories Bars
            SubcategoryProgressBar(
                categoryName = "Trabajo Educativo Social (TES)",
                hours = stats.tesHoras,
                goal = stats.metaTes,
                color = HondurasBluePrimary
            )
            SubcategoryProgressBar(
                categoryName = "Relevo por la Vida",
                hours = stats.relevoHoras,
                goal = stats.metaRelevo,
                color = CoralAccent
            )
            SubcategoryProgressBar(
                categoryName = "Voluntariado Social",
                hours = stats.voluntariadoHoras,
                goal = stats.metaVoluntariado,
                color = HondurasCyanSkein
            )
            SubcategoryProgressBar(
                categoryName = "Prácticas Profesionales",
                hours = stats.practicasHoras,
                goal = stats.metaPracticas,
                color = SlateDarkCard
            )
            SubcategoryProgressBar(
                categoryName = "Actividades Académicas",
                hours = stats.academicasHoras,
                goal = stats.metaAcademicas,
                color = HondurasBlueSecondary
            )
        }
    }
}

@Composable
fun SubcategoryProgressBar(
    categoryName: String,
    hours: Double,
    goal: Int,
    color: Color
) {
    Column(modifier = Modifier.run { fillMaxWidth().padding(vertical = 6.dp) }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Text(
                text = "$hours / $goal hrs",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val ratio = (hours / goal.coerceAtLeast(1)).toFloat()
        LinearProgressIndicator(
            progress = { ratio.coerceAtMost(1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    }
}

@Composable
fun AndroidCalendarioInteligente(activities: List<Actividad>) {
    var calendarInstance by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedCalendarDay by remember { mutableStateOf<Calendar?>(null) }

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "HN"))
    val displayedMonthName = monthFormat.format(calendarInstance.time).replaceFirstChar { it.uppercase() }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with navigation arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calendario Académico",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                    Text(
                        text = displayedMonthName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HondurasBluePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val newCal = calendarInstance.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            calendarInstance = newCal
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Mes anterior",
                            tint = HondurasBluePrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            val newCal = calendarInstance.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            calendarInstance = newCal
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Siguiente mes",
                            tint = HondurasBluePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekdays Header: Dom, Lun, Mar...
            val weekdays = listOf("Do", "Lu", "Ma", "Mi", "Ju", "Vi", "Sá")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            val monthCal = calendarInstance.clone() as Calendar
            monthCal.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday...
            val maxDays = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            // Number of empty slots in the first row
            // In Sunday-first, Sunday=1, so empty slots = Sunday - 1
            val emptySlots = firstDayOfWeek - 1

            val totalCells = emptySlots + maxDays
            val rowsCount = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (r in 0 until rowsCount) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0..6) {
                            val cellIndex = r * 7 + c
                            val dayNumber = cellIndex - emptySlots + 1

                            if (cellIndex < emptySlots || dayNumber > maxDays) {
                                // Empty placeholder cell
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                // Valid day cell
                                val cellCal = calendarInstance.clone() as Calendar
                                cellCal.set(Calendar.DAY_OF_MONTH, dayNumber)

                                val isToday = isSameDay(cellCal, Calendar.getInstance())
                                
                                // Count activities for this day
                                val count = activities.count { act ->
                                    val actCal = Calendar.getInstance().apply { timeInMillis = act.fecha }
                                    isSameDay(cellCal, actCal)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isToday -> HondurasBluePrimary
                                                count > 0 -> HondurasCyanSkein.copy(alpha = 0.15f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable {
                                            selectedCalendarDay = cellCal
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isToday || count > 0) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = when {
                                                isToday -> Color.White
                                                count > 0 -> HondurasBluePrimary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (count > 0 && !isToday) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(HondurasBlueSecondary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedCalendarDay != null) {
        val dateCal = selectedCalendarDay!!
        val formattedDateHeader = SimpleDateFormat("EEEE d 'de' MMMM yyyy", Locale("es", "HN")).format(dateCal.time).replaceFirstChar { it.uppercase() }
        
        val dayActivities = activities.filter { actCal ->
            val itemCal = Calendar.getInstance().apply { timeInMillis = actCal.fecha }
            isSameDay(dateCal, itemCal)
        }

        AlertDialog(
            onDismissRequest = { selectedCalendarDay = null },
            title = {
                Text(
                    text = formattedDateHeader,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = HondurasBluePrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (dayActivities.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No hay actividades registradas",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                )
                                Text(
                                    text = "Agrega horas en la pestaña Bitácora.",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                )
                            }
                        }
                    } else {
                        dayActivities.forEach { a ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(HondurasBluePrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = a.horasObtenidas.toString(),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = HondurasBluePrimary
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = a.titulo,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${a.categoria} • ${a.lugar}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        )
                                        if (a.supervisor.isNotEmpty()) {
                                            Text(
                                                text = "Supervisado por: ${a.supervisor}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, color = HondurasCyanSkein)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedCalendarDay = null },
                    colors = ButtonDefaults.buttonColors(containerColor = HondurasBluePrimary)
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun CategoriasStatsRow() {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            QuickMetricBadge(
                title = "100% Local",
                subtitle = "Sin Internet",
                icon = Icons.Outlined.CloudOff,
                tint = HondurasBluePrimary
            )
        }
        item {
            QuickMetricBadge(
                title = "Segura",
                subtitle = "PIN Activado",
                icon = Icons.Outlined.Lock,
                tint = HondurasCyanSkein
            )
        }
        item {
            QuickMetricBadge(
                title = "Copias Auto",
                subtitle = "Protegido",
                icon = Icons.Outlined.Backup,
                tint = HondurasBlueSecondary
            )
        }
    }
}

@Composable
fun QuickMetricBadge(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.width(140.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)), maxLines = 1)
            }
        }
    }
}

@Composable
fun RecentSummarySection(
    actsList: List<Actividad>,
    logsList: List<Logro>,
    onNavigateToActivities: () -> Unit,
    onNavigateToPortfolio: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Achievements summary header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Logros Desbloqueados",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Grid list of achievements unlocked or in progress
        val recentLogros = logsList.filter { it.estaDesbloqueado }.take(2)
        if (recentLogros.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "No se han desbloqueado logros todavía.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    )
                }
            }
        } else {
            recentLogros.forEach { l ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = GoldRank,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(l.titulo, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(l.descripcion, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recent Work list header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Últimas Horas Registradas",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            TextButton(onClick = onNavigateToActivities) {
                Text("Ver todo", color = HondurasBluePrimary)
            }
        }

        // List
        val rActs = actsList.take(3)
        if (rActs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Presiona el menú Tareas para agregar tu primera actividad.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                    )
                }
            }
        } else {
            rActs.forEach { a ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (a.categoria) {
                                        "TES" -> HondurasBluePrimary.copy(alpha = 0.1f)
                                        "Relevo por la Vida" -> CoralAccent.copy(alpha = 0.1f)
                                        else -> HondurasCyanSkein.copy(alpha = 0.1f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (a.categoria.length >= 3) a.categoria.take(3) else a.categoria,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = when (a.categoria) {
                                        "TES" -> HondurasBluePrimary
                                        "Relevo por la Vida" -> CoralAccent
                                        else -> HondurasCyanSkein
                                    }
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(a.titulo, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "${a.lugar} • ${SimpleDateFormat("dd MMM yyyy", Locale("es", "HN")).format(Date(a.fecha))}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("+${a.horasObtenidas} hrs", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, color = HondurasCyanSkein))
                            Text(
                                text = a.estado,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (a.estado == "Completada") HondurasBlueTertiary else CoralAccent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuiaExitoQuickCard(onConfigureTour: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        border = BorderStroke(1.dp, HondurasCyanSkein.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(HondurasCyanSkein.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = HondurasCyanSkein,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Guía de Éxito Académico",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = "Domina el registro inteligente, el escaneo de firmas y la exportación de bitácoras oficiales de Honduras.",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f))
                )
            }

            IconButton(
                onClick = onConfigureTour,
                colors = IconButtonDefaults.iconButtonColors(containerColor = HondurasCyanSkein)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Ver guía",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun InteractiveGuideDialog(viewModel: StudentsViewModel, onDismiss: () -> Unit) {
    var guideStep by remember { mutableStateOf(1) } // 1, 2, 3
    var showConfirmReset by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = HondurasCyanSkein
                )
                Text(
                    text = "Guía de Éxito Académico",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick segment controller
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (guideStep == i) HondurasCyanSkein else Color.White.copy(alpha = 0.15f)
                                )
                                .clickable { guideStep = i }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                when (guideStep) {
                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "🚀 Bitácora & Tiempo Inteligente",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black, color = HondurasCyanSkein)
                            )
                            Text(
                                "Todas tus horas se calculan de manera autónoma al definir las horas de inicio y salida. El mes, el año escolar y los días de la semana se infieren instantáneamente evitando que reescribas datos repetitivos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Text(
                                "✍️ Firmas Digitales en Pantalla: Puedes recopilar la firma del supervisor dibujando directamente desde el módulo de nueva actividad para asegurar la veracidad de tus registros.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                    2 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "📸 Bóveda de Evidencias Multimedia",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black, color = HondurasCyanSkein)
                            )
                            Text(
                                "Guarda hasta 6 fotografías en formato local de alta fidelidad vinculadas a cada actividad. Cuenta con un carrusel dinámico y visualizador de zoom con soporte para compartir copias oficiales a asesores.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Text(
                                "📂 Generador de Documentos Oficiales: Estructura reportes técnicos listos en formatos exportables para descargar o compartir por canales directos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                    3 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "📄 Portafolio y Validación Oficial",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black, color = HondurasCyanSkein)
                            )
                            Text(
                                "Exporta tu bitácora de Trabajo Educativo Social en formatos PDF listos para firmar y folios DOCX estructurados según las pautas de plantillas de la Secretaría de Educación de Honduras.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Text(
                                "🛡️ Credencial QR Única: Genera tu carné electrónico con un hash de control cifrado y token corporal dinámico para la auditoría y verificación transparente de tus acumulados escolares.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.1f))

                            Text(
                                "Prueba de Re-inicialización",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
                            )
                            Button(
                                onClick = {
                                    showConfirmReset = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralAccent.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reiniciar Onboarding (Volver a Configurar Todo)", color = CoralAccent, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (guideStep > 1) {
                    TextButton(onClick = { guideStep-- }) {
                        Text("Atrás")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (guideStep < 3) {
                            guideStep++
                        } else {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein)
                ) {
                    Text(if (guideStep == 3) "Entendido" else "Siguiente")
                }
            }
        }
    )

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            title = {
                Text(
                    text = "⚠️ ¿Seguro de reiniciar todo?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Si reinicias el asistente de inicio, se borrará la configuración y el perfil completado del estudiante. Tendrás que re-introducir todos los datos maestros para usar el asistente. ¿Deseas continuar?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetOnboarding()
                        showConfirmReset = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
                ) {
                    Text("Sí, Reiniciar Todo", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmReset = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicYearQuickSwitcher(viewModel: StudentsViewModel) {
    val activeYearState by viewModel.activeYear.collectAsState()
    val availableYears = remember { (2024..2100).map { it.toString() } }
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = HondurasBluePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ciclo Académico Activo",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HondurasCyanSkein.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Activo: $activeYearState",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateDarkBackground
                    )
                }
            }
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(availableYears) { year ->
                    val isSelected = year == activeYearState
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.changeActiveYear(year) },
                        label = {
                            Text(
                                text = "Año $year",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HondurasBluePrimary.copy(alpha = 0.2f),
                            selectedLabelColor = HondurasBluePrimary,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = HondurasBluePrimary,
                            borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    }
}
