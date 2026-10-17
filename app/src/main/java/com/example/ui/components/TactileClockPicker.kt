package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TactileClockPickerDialog(
    initialHour: Int = 8,
    initialMinute: Int = 0,
    title: String = "Seleccionar Hora",
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int, amPm: String) -> Unit
) {
    var selectedHour by remember { mutableStateOf(if (initialHour == 0) 12 else if (initialHour > 12) initialHour - 12 else initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    var isHoursMode by remember { mutableStateOf(true) }
    var amPmState by remember { mutableStateOf(if (initialHour >= 12) "PM" else "AM") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SlateDarkBackground,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = HondurasCyanSkein
                    )
                )

                // Large Time Readout & Toggle Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hour selector tag
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isHoursMode) HondurasBluePrimary.copy(alpha = 0.2f) else Color.Transparent,
                        modifier = Modifier.clickable { isHoursMode = true }
                    ) {
                        Text(
                            text = String.format("%02d", selectedHour),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHoursMode) HondurasCyanSkein else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Text(":", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    // Minute selector tag
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isHoursMode) HondurasBluePrimary.copy(alpha = 0.2f) else Color.Transparent,
                        modifier = Modifier.clickable { isHoursMode = false }
                    ) {
                        Text(
                            text = String.format("%02d", selectedMinute),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isHoursMode) HondurasCyanSkein else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // AM/PM state switch options
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (amPmState == "AM") HondurasBluePrimary else Color.White.copy(alpha = 0.05f))
                                .clickable { amPmState = "AM" }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("AM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (amPmState == "PM") HondurasBluePrimary else Color.White.copy(alpha = 0.05f))
                                .clickable { amPmState = "PM" }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("PM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Friendly real-time help summary
                val timeLabel = remember(selectedHour, selectedMinute, amPmState) {
                    val label = when {
                        selectedHour == 12 && amPmState == "PM" -> "Mediodía / Almuerzo"
                        selectedHour == 12 && amPmState == "AM" -> "Medianoche"
                        amPmState == "PM" && selectedHour >= 6 && selectedHour < 12 -> "Noche"
                        amPmState == "PM" -> "Tarde"
                        amPmState == "AM" && selectedHour >= 6 && selectedHour < 12 -> "Mañana"
                        else -> "Madrugada / Hora Temprana"
                    }
                    val hrs24 = if (amPmState == "PM" && selectedHour < 12) selectedHour + 12 else if (amPmState == "AM" && selectedHour == 12) 0 else selectedHour
                    String.format(java.util.Locale.getDefault(), "Formato Militas (24h): %02d:%02d hrs | %s", hrs24, selectedMinute, label)
                }

                Surface(
                    color = HondurasBluePrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Hora seleccionada: $selectedHour:${String.format("%02d", selectedMinute)} $amPmState\n$timeLabel",
                        fontSize = 11.sp,
                        color = HondurasCyanSkein,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                // Modes text label instructions
                Text(
                    text = if (isHoursMode) "Gira el dial para ajustar la HORA" else "Gira el dial para ajustar los MINUTOS",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                // ANALOG DIAL RADIAL CANVAS SELECTOR
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.02f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isHoursMode) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        // Compute angle
                                        val cx = size.width / 2f
                                        val cy = size.height / 2f
                                        val angleRad = atan2(offset.y - cy, offset.x - cx)
                                        var deg = Math.toDegrees(angleRad.toDouble()) + 90.0
                                        if (deg < 0) deg += 360.0

                                        if (isHoursMode) {
                                            selectedHour = Math.round(deg / 30.0).toInt().let {
                                                if (it == 0) 12 else if (it > 12) 12 else it
                                            }
                                        } else {
                                            selectedMinute = Math.round(deg / 6.0).toInt().let {
                                                if (it >= 60) 0 else it
                                            }
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        val cx = size.width / 2f
                                        val cy = size.height / 2f
                                        val angleRad = atan2(change.position.y - cy, change.position.x - cx)
                                        var deg = Math.toDegrees(angleRad.toDouble()) + 90.0
                                        if (deg < 0) deg += 360.0

                                        if (isHoursMode) {
                                            selectedHour = Math.round(deg / 30.0).toInt().let {
                                                if (it == 0) 12 else if (it > 12) 12 else it
                                            }
                                        } else {
                                            val minutesRaw = Math.round(deg / 6.0).toInt()
                                            // Round to nearest 5 for premium mechanical slider feel if they click/drag
                                            selectedMinute = (minutesRaw / 1).coerceAtMost(59)
                                        }
                                    }
                                )
                            }
                    ) {
                        val widthPx = size.width
                        val heightPx = size.height
                        val cx = widthPx / 2f
                        val cy = heightPx / 2f
                        val outerRadius = widthPx / 2f

                        // Draw Clock Circle boundaries
                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f),
                            radius = outerRadius,
                            center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )

                        // Draw central pin hub
                        drawCircle(
                            color = HondurasCyanSkein,
                            radius = 6.dp.toPx(),
                            center = Offset(cx, cy)
                        )

                        // Draw subtle tick ticks around the circle
                        for (i in 0 until 12) {
                            val rad = Math.toRadians((i * 30 - 90).toDouble())
                            val tickStart = Offset(
                                (cx + (outerRadius - 6.dp.toPx()) * cos(rad)).toFloat(),
                                (cy + (outerRadius - 6.dp.toPx()) * sin(rad)).toFloat()
                            )
                            val tickEnd = Offset(
                                (cx + outerRadius * cos(rad)).toFloat(),
                                (cy + outerRadius * sin(rad)).toFloat()
                            )
                            drawStroke(
                                color = Color.White.copy(alpha = 0.25f),
                                start = tickStart,
                                end = tickEnd,
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // Determine the selected item's pointing degree
                        val pointingDegrees = if (isHoursMode) {
                            (selectedHour * 30 - 90).toDouble()
                        } else {
                            (selectedMinute * 6 - 90).toDouble()
                        }

                        val handLength = outerRadius - 32.dp.toPx()
                        val pointingRad = Math.toRadians(pointingDegrees)
                        val handTip = Offset(
                            (cx + handLength * cos(pointingRad)).toFloat(),
                            (cy + handLength * sin(pointingRad)).toFloat()
                        )

                        // Draw sweeping indicator clock dial hand line
                        drawStroke(
                            color = HondurasCyanSkein,
                            start = Offset(cx, cy),
                            end = handTip,
                            strokeWidth = 3.dp.toPx()
                        )

                        // Draw sweeping circular handle dot
                        drawCircle(
                            color = HondurasCyanSkein,
                            radius = 12.dp.toPx(),
                            center = handTip
                        )

                        // Draw white core inside sweeping dot handle
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = handTip
                        )

                        // Draw inline clock Face numbering text
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 11.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }

                        val activeNumbers = if (isHoursMode) {
                            listOf("12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")
                        } else {
                            listOf("00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55")
                        }

                        for (i in 0 until 12) {
                            val numRad = Math.toRadians((i * 30 - 90).toDouble())
                            val textX = (cx + (outerRadius - 22.dp.toPx()) * cos(numRad)).toFloat()
                            val textY = (cy + (outerRadius - 22.dp.toPx()) * sin(numRad) + (textPaint.textSize / 3f)).toFloat()
                            
                            val isChosen = if (isHoursMode) {
                                val itemHour = if (i == 0) 12 else i
                                selectedHour == itemHour
                            } else {
                                selectedMinute / 5 == i
                            }

                            textPaint.color = if (isChosen) {
                                android.graphics.Color.parseColor("#00F0FF") // HondurasCyanSkein Color
                            } else {
                                android.graphics.Color.argb(128, 255, 255, 255)
                            }

                            drawContext.canvas.nativeCanvas.drawText(
                                activeNumbers[i],
                                textX,
                                textY,
                                textPaint
                            )
                        }
                    }
                }

                // Sub-Actions: Toggle Switch Between Hour and Minutes Mode manually
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { isHoursMode = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isHoursMode) HondurasCyanSkein else Color.White.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Modo Horas", fontWeight = if (isHoursMode) FontWeight.Bold else FontWeight.Medium)
                    }

                    TextButton(
                        onClick = { isHoursMode = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (!isHoursMode) HondurasCyanSkein else Color.White.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Modo Minutos", fontWeight = if (!isHoursMode) FontWeight.Bold else FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom CTA Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text("Cancelar", color = Color.White)
                    }

                    Button(
                        onClick = {
                            onTimeSelected(selectedHour, selectedMinute, amPmState)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HondurasCyanSkein),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Establecer", color = Color.White)
                    }
                }
            }
        }
    }
}

// Utility drawing helper extension
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(
    color: Color,
    start: Offset,
    end: Offset,
    strokeWidth: Float
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth
    )
}
