package com.example.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DocumentGenerator(
    private val student: Usuario?,
    private val activities: List<Actividad>,
    private val evidences: List<Evidencia>,
    private val selectedFormat: String, // "PDF", "DOCX", "DOC"
    private val styleMode: String, // "Normal", "Diplomático"
    private val selectedTemplate: String = "Portafolio Académico"
) {

    // Theme container config per category
    data class CategoryTheme(
        val categoryKey: String,
        val title: String,
        val subtitle: String,
        val authority: String,
        val legalBasis: String,
        val primaryColorHex: String,
        val accentColorHex: String,
        val stampText: String,
        val articleText: String
    )

    /**
     * Determines the dominant theme category based on the activities collection.
     */
    private val dominantCategory: String by lazy {
        if (activities.isEmpty()) {
            "GLOBAL"
        } else if (activities.size == 1) {
            activities.first().categoria
        } else {
            val groups = activities.map { it.categoria }.groupBy { it }
            if (groups.size == 1) {
                groups.keys.first()
            } else {
                "GLOBAL"
            }
        }
    }

    /**
     * Retrieves the visual theme layout specifications for a given category.
     */
    val theme: CategoryTheme by lazy {
        getThemeForCategory(dominantCategory)
    }

    private fun getThemeForCategory(cat: String): CategoryTheme {
        return when {
            cat.contains("TES", ignoreCase = true) -> CategoryTheme(
                categoryKey = "TES",
                title = "REGISTRO OFICIAL DE TRABAJO EDUCATIVO SOCIAL (TES)",
                subtitle = "CONSEJO NACIONAL DE EDUCACIÓN • REPÚBLICA DE HONDURAS",
                authority = "SECRETARÍA DE ESTADO EN EL DESPACHO DE EDUCACIÓN",
                legalBasis = "Sometido al Acuerdo Ejecutivo Ministerial N. 0432-SE-2015 en concordancia con el artículo constitucional de civismo e involucramiento estudiantil de educación media.",
                primaryColorHex = "#0D2C54", // Elegant Navy
                accentColorHex = "#FFC107",  // Amber-Gold
                stampText = "TES * HN",
                articleText = "Certifica el fiel devengamiento de horas comunitarias con enfoque en alfabetización, reforestación nacional activa, capacitación cívica y preservación de valores patrios."
            )
            cat.contains("Relevo", ignoreCase = true) -> CategoryTheme(
                categoryKey = "RELEVO",
                title = "CONSTANCIA OFICIAL: RELEVO POR LA VIDA HONDURAS",
                subtitle = "RED DE SOLIDARIDAD DE LA LIGA CONTRA EL CÁNCER",
                authority = "CENTRO ONCOLÓGICO NACIONAL Y COORDINACIÓN DE SALUD",
                legalBasis = "Validado bajo las pautas éticas del Convenio Colectivo de Proyección Social y Ayuda Humanitaria Directa bajo las metas de bitácoras académicas.",
                primaryColorHex = "#E91E63", // Pinkish-Rose
                accentColorHex = "#1A237E",  // Deep Sovereign Indigo
                stampText = "RELEVO * VIDA",
                articleText = "Certifica el meritorio servicio humanitario del alumno postulante en la campaña nacional de acompañamiento, recolección y concientización solidaria."
            )
            cat.contains("Voluntariado", ignoreCase = true) || cat.contains("Comunitaria", ignoreCase = true) -> CategoryTheme(
                categoryKey = "VOLUNTARIADO",
                title = "EXPEDIENTE DE PROYECCIÓN SOCIAL Y VOLUNTARIADO",
                subtitle = "DIRECCIÓN DE COOPERACIÓN Y ASISTENCIA COMUNITARIA LOCAL",
                authority = "CONSEJO MUNICIPAL DE DESARROLLO INTEGRAL",
                legalBasis = "Aprobado bajo el régimen local de promoción de valores éticos, solidaridad municipal y contribución urbana prioritaria de los educandos de Honduras.",
                primaryColorHex = "#00796B", // Dark Emerald Teal
                accentColorHex = "#00E5FF",  // Bright Cyan
                stampText = "VOLUNTARIO",
                articleText = "Acredita de forma estructurada las horas de auxilio local, proyectos de reordenamiento e iniciativas de cooperación humanitaria ejecutadas legítimamente."
            )
            cat.contains("Práct", ignoreCase = true) -> CategoryTheme(
                categoryKey = "PRACTICAS",
                title = "CERTIFICADO DE CAPACITACIÓN Y PRÁCTICA PROFESIONAL",
                subtitle = "SINDICATO DOCENTE Y COMISIÓN EVALUADORA DE HABILIDADES",
                authority = "COORDINACIÓN DE COMPETENCIAS PRE-UNIVERSITARIAS",
                legalBasis = "De conformidad con la Ley General de Educación Técnica del Estado, facultando la vinculación directa de los estudiantes con sectores productivos.",
                primaryColorHex = "#37474F", // Charcoal Steel
                accentColorHex = "#2196F3",  // Tech Blue
                stampText = "PRACTICA",
                articleText = "Hace constar las aptitudes funcionales y capacidades técnicas expresadas por el candidato en la realización de sus proyectos académicos prácticos."
            )
            cat.contains("Acadé", ignoreCase = true) -> CategoryTheme(
                categoryKey = "ACADEMICAS",
                title = "COMPENDIO ACADÉMICO CO-CURRICULAR Y PEDAGÓGICO",
                subtitle = "CONSEJO DEL CUERPO DOCENTE Y PLANIFICACIÓN LECTIVA",
                authority = "DIRECCIÓN ACADÉMICA GENERAL DE EXPEDIENTES",
                legalBasis = "Instaurado para validar talleres prácticos de ciencia aplicada, ponencias reflexivas de nivel superior y asistencia a eventos de desarrollo intelectual.",
                primaryColorHex = "#5C3A21", // Crimson-Chocolate
                accentColorHex = "#FF5722",  // Amber-Orange
                stampText = "ACADEMIA",
                articleText = "Registra las horas acumuladas en simulacros científicos, ponencias metodológicas y olimpiadas estatales de aprendizaje competitivo."
            )
            else -> CategoryTheme(
                categoryKey = "GLOBAL",
                title = "EXPEDIENTE CONSOLIDADO DE COMPROMISO ACADÉMICO",
                subtitle = "SISTEMA INTEGRAL DE BITÁCORAS DE CAMPO • MI TES & RELEVO POR VIDA",
                authority = "DIRECCIÓN NACIONAL DE EXPEDICIÓN DE CERTIFICACIONES",
                legalBasis = "Expediente digital unificado de control regular recopilando todas las acciones e informes de labor social para portafolios escolares.",
                primaryColorHex = "#0B1E36", // Rich Cyber Dark
                accentColorHex = "#00D2FF",  // Neon Cyan
                stampText = "GLOBAL * TES",
                articleText = "Certifica el compendio integral de actividades, horas aprobadas por coordinadores, bitácoras de campo estructuradas y el avance de metas sociales."
            )
        }
    }

    private fun getField(value: String?): String {
        return if (value.isNullOrBlank()) "[Dato Pendiente en Sistema]" else value
    }

    private fun getField(value: Int?, fallback: String = "0"): String {
        return value?.toString() ?: fallback
    }

    private fun getProgressBlock(value: Int, total: Int): String {
        val pct = if (total > 0) (value * 100) / total else 0
        val cappedPct = if (pct > 100) 100 else pct
        val filled = cappedPct / 10
        val sb = StringBuilder()
        for (i in 0 until 10) {
            if (i < filled) sb.append("█") else sb.append("░")
        }
        return "[$sb]  $pct% ($value de $total hrs acumuladas)"
    }

    /**
     * Helper to wrap text into a List of substrings fitting a maximum character limit per line.
     */
    private fun wrapTextInLines(text: String, limit: Int): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (paragraph in paragraphs) {
            if (paragraph.isBlank()) {
                result.add("")
                continue
            }
            val words = paragraph.split(" ")
            val lineBuilder = java.lang.StringBuilder()
            for (word in words) {
                if (lineBuilder.length + word.length + 1 > limit) {
                    result.add(lineBuilder.toString())
                    lineBuilder.setLength(0)
                }
                if (lineBuilder.isNotEmpty()) {
                    lineBuilder.append(" ")
                }
                lineBuilder.append(word)
            }
            if (lineBuilder.isNotEmpty()) {
                result.add(lineBuilder.toString())
            }
        }
        return result
    }

    /**
     * Generates a structural 3-page format with elegant descriptive text according to the category.
     */
    fun generatePages(): List<DocumentPreviewPage> {
        val uName = getField(student?.nombre)
        val uColegio = getField(student?.centroEducativo)
        val uCarrera = getField(student?.carrera)
        val uGrado = getField(student?.grado)
        val uSeccion = getField(student?.seccion)
        val uAnio = getField(student?.anioAcademico)
        val uCorreo = getField(student?.correo)
        val uTel = getField(student?.telefono)

        val totalHours = activities.sumOf { it.horasObtenidas }
        val dateString = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "HN")).format(Date())

        // Compile clean metrics
        val metaTes = student?.metaHorasTES ?: 100
        val metaRelevo = student?.metaHorasRelevo ?: 40
        val metaVol = student?.metaHorasVoluntariado ?: 50
        val metaPrac = student?.metaHorasPracticas ?: 100
        val metaAcad = student?.metaHorasAcademicas ?: 50

        val actTes = activities.filter { it.categoria.contains("TES", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val actRelevo = activities.filter { it.categoria.contains("relevo", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val actVol = activities.filter { it.categoria.contains("voluntar", ignoreCase = true) || it.categoria.contains("comunit", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val actPrac = activities.filter { it.categoria.contains("práct", ignoreCase = true) || it.categoria.contains("pract", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val actAcad = activities.filter { it.categoria.contains("acadé", ignoreCase = true) || it.categoria.contains("acade", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val totalHoursInt = totalHours.toInt()

        // Page 1: Official Header and Statement of Purpose
        val p1Title = "SECCIÓN I: DOSSIER ACADÉMICO REGULAR"
        val p1Content = buildString {
            append("CONSTITUCIÓN E IDENTIDAD INSTITUCIONAL\n\n")
            append("Fecha de Emisión: $dateString, Honduras.\n")
            append("Asunto: Acreditación y Constancia Regular de Bitácora\n\n")
            append("A las autoridades directivas, docentes asesores y entes evaluadores de la educación media:\n\n")
            append("Por medio de las facultades de control que ofrece el sistema 'MI TES & RELEVO POR VIDA', se hace certificar y rendir expediente autónomo del siguiente estudiante:\n\n")
            append("EXPEDIENTE NOMINAL CONSOLIDADO:\n")
            append("• Estudiante Contribuyente: $uName\n")
            append("• Centro de Enseñanza: $uColegio\n")
            append("• Especialidad Académica: $uCarrera\n")
            append("• Ubicación Lectiva: Grado $uGrado, Sección '$uSeccion'\n")
            append("• Registro de Ciclo: Ciclo de Campo $uAnio\n")
            append("• Vía de Contacto: Tel. $uTel | Correo: $uCorreo\n\n")
            append("MARCO LEGAL Y OPERATIVO:\n")
            append("${theme.legalBasis}\n\n")
            append("DECLARACIÓN DE FINES:\n")
            append("${theme.articleText}")
        }

        // Page 2: Chronological Log entries
        val p2Title = "SECCIÓN II: INFORME DE LABORES Y BITÁCORAS"
        val p2Content = buildString {
            append("CRÓNICA DE ACTIVIDADES DE CAMPO REPORTADAS\n\n")
            append("La presente nómina detalla los aportes, sedes físicas, duraciones reales y compromisos ejecutados formalmente:\n\n")

            if (activities.isEmpty()) {
                append("--- SIN CONTENIDO DE BITÁCORA ---\n")
                append("Actualmente no se reportan jornadas o trabajos prácticos validados para este reporte específico.")
            } else {
                activities.forEachIndexed { index, act ->
                    val actDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(act.fecha))
                    append("${index + 1}. PROYECTO: \"${act.titulo}\" [$actDate]\n")
                    append("   * Sede / Lugar: ${act.lugar} | Estado: ${act.estado.uppercase()}\n")
                    append("   * Duración: ${act.horaInicio} a ${act.horaSalida} (${act.horasObtenidas} hrs netas)\n")
                    append("   * Supervisor: ${act.supervisor} (${act.cargoSupervisor})\n")
                    append("   * Descripción: ${act.descripcion}\n")
                    if (!act.queSeApoyo.isNullOrBlank()) {
                        append("   * Aporte Real: ${act.queSeApoyo}\n")
                    }
                    if (!act.queSeAprendio.isNullOrBlank()) {
                        append("   * Lección Aprendida: ${act.queSeAprendio}\n")
                    }
                    append("\n")
                }
            }
        }

        // Page 3: Progression indicators and Formal Signing boxes
        val p3Title = "SECCIÓN III: BALANCE DE CRÉDITOS Y FIRMAS"
        val p3Content = buildString {
            append("MÉTRICAS DE RENDIMIENTO GLOBAL Y LOGRO DE METAS\n\n")
            append("A continuación se detalla el desglose y porcentaje de avance respecto a las horas académicas exigidas según el pensum nacional:\n\n")

            append("• TRABAJO EDUCATIVO SOCIAL (TES):\n")
            append("  $actTes de $metaTes hrs acumuladas (${if (metaTes > 0) (actTes * 100) / metaTes else 0}%)\n\n")

            append("• RELEVO POR LA VIDA (APOYO ONCOLÓGICO):\n")
            append("  $actRelevo de $metaRelevo hrs acumuladas (${if (metaRelevo > 0) (actRelevo * 100) / metaRelevo else 0}%)\n\n")

            append("• VOLUNTARIADO COMUNITARIO ACTIVO:\n")
            append("  $actVol de $metaVol hrs acumuladas (${if (metaVol > 0) (actVol * 100) / metaVol else 0}%)\n\n")

            append("• PRÁCTICA PROFESIONAL PRE-UNIVERSITARIA:\n")
            append("  $actPrac de $metaPrac hrs acumuladas (${if (metaPrac > 0) (actPrac * 100) / metaPrac else 0}%)\n\n")

            append("• ACTIVIDADES ACADÉMICAS COMPLEMENTARIAS:\n")
            append("  $actAcad de $metaAcad hrs acumuladas (${if (metaAcad > 0) (actAcad * 100) / metaAcad else 0}%)\n\n")

            val totalMetas = metaTes + metaRelevo + metaVol + metaPrac + metaAcad
            append("NIVEL DE CUMPLIMIENTO TOTAL INTEGRAL:\n")
            append("  $totalHoursInt de $totalMetas hrs acumuladas (${if (totalMetas > 0) (totalHoursInt * 100) / totalMetas else 0}%)\n\n")

            append("FÓRMULAS DE COMPROMISO Y FE DE ERRATAS:\n")
            append("Hacemos manifestar, de forma digna y honorable, que todos los registros de bitácoras académicas corresponden a servicios comunitarios verdaderos para el crecimiento cívico de la nación de Honduras.")
        }

        return listOf(
            DocumentPreviewPage(1, p1Title, p1Content),
            DocumentPreviewPage(2, p2Title, p2Content),
            DocumentPreviewPage(3, p3Title, p3Content)
        )
    }

    /**
     * Draws an ultra-professional, beautifully structured PDF representing official educational designs.
     */
    fun writePdf(context: Context, targetFile: File) {
        val pdfDocument = PdfDocument()
        val pages = generatePages()

        val metaTes = student?.metaHorasTES ?: 100
        val metaRelevo = student?.metaHorasRelevo ?: 40
        val metaVol = student?.metaHorasVoluntariado ?: 50
        val metaPrac = student?.metaHorasPracticas ?: 100
        val metaAcad = student?.metaHorasAcademicas ?: 50

        val actTes = activities.filter { it.categoria.contains("TES", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val actRelevo = activities.filter { it.categoria.contains("relevo", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val actVol = activities.filter { it.categoria.contains("voluntar", ignoreCase = true) || it.categoria.contains("comunit", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val actPrac = activities.filter { it.categoria.contains("práct", ignoreCase = true) || it.categoria.contains("pract", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val actAcad = activities.filter { it.categoria.contains("acadé", ignoreCase = true) || it.categoria.contains("acade", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val totalHours = activities.sumOf { it.horasObtenidas }.toInt()

        val isDiplomatico = styleMode == "Diplomático" || styleMode == "true"
        val priColor = if (isDiplomatico) Color.parseColor("#0A1E36") else Color.parseColor(theme.primaryColorHex)
        val accColor = if (isDiplomatico) Color.parseColor("#D4AF37") else Color.parseColor(theme.accentColorHex)
        val titleTypeface = if (isDiplomatico) android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD) else android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        val bodyTypeface = if (isDiplomatico) android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL) else android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)

        // Setup paints
        val borderPaint = Paint().apply {
            color = priColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }
        val thinBorderPaint = Paint().apply {
            color = accColor
            style = Paint.Style.STROKE
            strokeWidth = 1.0f
            isAntiAlias = true
        }
        val bannerPaint = Paint().apply {
            color = priColor
            style = Paint.Style.FILL
        }
        val textPaint = Paint().apply {
            color = Color.parseColor("#111111")
            textSize = 9.5f
            typeface = bodyTypeface
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = priColor
            textSize = 10f
            typeface = titleTypeface
            isAntiAlias = true
        }

        for ((idx, page) in pages.withIndex()) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, idx + 1).create() // Standard A4 points
            val pdfPage = pdfDocument.startPage(pageInfo)
            val canvas = pdfPage.canvas

            // Fill background white
            canvas.drawColor(Color.WHITE)

            // 1. Draw elegant double borders
            canvas.drawRect(RectF(25f, 25f, 570f, 817f), borderPaint)
            canvas.drawRect(RectF(29f, 29f, 566f, 813f), thinBorderPaint)

            // No background watermark draws as requested by the student (completely clean and professional)

            // 3. Draw elite top header banner
            canvas.drawRect(RectF(33f, 33f, 562f, 100f), bannerPaint)

            // Header labels
            val headerTitle = if (isDiplomatico) "REPÚBLICA DE HONDURAS" else theme.title
            val headerSubtitle = if (isDiplomatico) "SECRETARÍA DE DIRECCIÓN DE EDUCACIÓN" else theme.subtitle
            val headerAuth = if (isDiplomatico) "CERTIFICADO ACADÉMICO OFICIAL TES" else theme.authority.uppercase()

            canvas.drawText(headerTitle, 48f, 56f, Paint().apply {
                color = Color.WHITE
                textSize = 12f
                typeface = titleTypeface
                isAntiAlias = true
            })
            canvas.drawText(headerSubtitle, 48f, 74f, Paint().apply {
                color = accColor
                textSize = 8.5f
                typeface = titleTypeface
                isAntiAlias = true
            })
            canvas.drawText(headerAuth, 48f, 87f, Paint().apply {
                color = Color.argb(250, 255, 255, 255)
                textSize = 7.5f
                typeface = bodyTypeface
                isAntiAlias = true
            })

            // Rotated beautiful circular stamp on header right
            val stampPaint = Paint().apply {
                color = accColor
                style = Paint.Style.STROKE
                strokeWidth = 1.25f
                isAntiAlias = true
            }
            canvas.drawCircle(522f, 66f, 22f, stampPaint)
            val stPaint = Paint().apply {
                color = accColor
                textSize = 5.5f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("VALIDADO", 522f, 64f, stPaint)
            canvas.drawText(theme.stampText, 522f, 72f, stPaint)

            // 4. Draw detailed page credentials box on PAGE 1
            if (page.pageNumber == 1) {
                val boxBack = Paint().apply { color = Color.parseColor("#F8F9FA") }
                val boxLine = Paint().apply { color = Color.parseColor("#ECEFF1") }
                canvas.drawRect(RectF(40f, 112f, 555f, 230f), boxBack)
                canvas.drawRect(RectF(40f, 112f, 555f, 230f), Paint().apply {
                    color = Color.parseColor("#CFD8DC")
                    style = Paint.Style.STROKE
                    strokeWidth = 0.75f
                })

                val colLeftlbl = 55f
                val colLeftval = 195f
                val colRightlbl = 360f
                val colRightval = 465f

                // Metadata paints
                val lblP = Paint().apply {
                    color = priColor
                    textSize = 8.5f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    isAntiAlias = true
                }
                val valP = Paint().apply {
                    color = Color.parseColor("#37474F")
                    textSize = 8.5f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                    isAntiAlias = true
                }

                canvas.drawText("ESTUDIANTE REGISTRADO:", colLeftlbl, 134f, lblP)
                canvas.drawText(getField(student?.nombre), colLeftval, 134f, valP)

                canvas.drawText("CENTRO DE ENSEÑANZA:", colLeftlbl, 151f, lblP)
                canvas.drawText(getField(student?.centroEducativo), colLeftval, 151f, valP)

                canvas.drawText("ESPECIALIDAD / CARRERA:", colLeftlbl, 168f, lblP)
                canvas.drawText(getField(student?.carrera), colLeftval, 168f, valP)

                canvas.drawText("CICLO LECTIVO ACTIVO:", colRightlbl, 134f, lblP)
                canvas.drawText(getField(student?.anioAcademico), colRightval, 134f, valP)

                canvas.drawText("GRADO Y SECCIÓN:", colRightlbl, 151f, lblP)
                canvas.drawText("${getField(student?.grado)} \"${getField(student?.seccion)}\"", colRightval, 151f, valP)

                canvas.drawText("TOTAL HORAS ACREDITADAS:", colRightlbl, 168f, lblP)
                canvas.drawText("${activities.sumOf { it.horasObtenidas }} horas de servicio", colRightval, 168f, Paint(valP).apply {
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    color = priColor
                })

                canvas.drawLine(48f, 182f, 547f, 182f, boxLine)

                canvas.drawText("AVAL Y NORMATIVA LEGAL:", colLeftlbl, 196f, Paint(lblP).apply { color = Color.parseColor("#78909C"); textSize = 7.5f })
                val normLines = wrapTextInLines(theme.legalBasis, 95)
                var lawY = 196f
                for (nl in normLines.take(2)) {
                    canvas.drawText(nl, colLeftval, lawY, Paint(valP).apply { textSize = 7.5f; color = Color.parseColor("#546E7A") })
                    lawY += 9.5f
                }
            }

            // 5. Render Core Page lines dynamically with specific layouts for each page
            if (page.pageNumber == 1) {
                var currentY = 250f
                boldPaint.textSize = 9.5f
                boldPaint.color = priColor
                canvas.drawText("I. MARCO JURÍDICO Y NORMATIVA VIGENTE", 40f, currentY, boldPaint)
                currentY += 16f
                
                val legalLines = wrapTextInLines(theme.legalBasis, 95)
                for (ln in legalLines) {
                    canvas.drawText(ln, 44f, currentY, textPaint)
                    currentY += 12.5f
                }
                
                currentY += 18f
                canvas.drawText("II. DECLARACIÓN JURADA DE FINES Y OBJETIVOS", 40f, currentY, boldPaint)
                currentY += 16f
                
                val artLines = wrapTextInLines(theme.articleText, 95)
                for (ln in artLines) {
                    canvas.drawText(ln, 44f, currentY, textPaint)
                    currentY += 12.5f
                }
                
                currentY += 30f
                canvas.drawText("Emitido para efectos de validez escolar regular.", 44f, currentY, Paint(textPaint).apply { 
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.ITALIC)
                    color = Color.parseColor("#78909C")
                })
            } else if (page.pageNumber == 2) {
                var currentY = 120f
                boldPaint.textSize = 10f
                boldPaint.color = priColor
                canvas.drawText("II. CRÓNICA DETALLADA DE JORNADAS Y BITÁCORAS DE CAMPO", 40f, currentY, boldPaint)
                currentY += 20f
                
                // Draw Table Header
                val headerPaint = Paint().apply {
                    color = Color.parseColor("#F1F5F9")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(RectF(40f, currentY, 555f, currentY + 18f), headerPaint)
                canvas.drawRect(RectF(40f, currentY, 555f, currentY + 18f), Paint().apply {
                    color = Color.parseColor("#CBD5E1")
                    style = Paint.Style.STROKE
                    strokeWidth = 0.5f
                })
                
                val thPaint = Paint().apply {
                    color = priColor
                    textSize = 8f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    isAntiAlias = true
                }
                
                canvas.drawText("N°", 45f, currentY + 12f, thPaint)
                canvas.drawText("ACTIVIDAD / COMPROMISO", 75f, currentY + 12f, thPaint)
                canvas.drawText("LUGAR / SEDE", 345f, currentY + 12f, thPaint)
                canvas.drawText("ESTADO", 465f, currentY + 12f, thPaint)
                canvas.drawText("HORAS", 515f, currentY + 12f, thPaint)
                
                currentY += 18f
                
                if (activities.isEmpty()) {
                    canvas.drawText("No se registran actividades validadas en esta sección de bitácora.", 50f, currentY + 30f, Paint(textPaint).apply {
                        color = Color.GRAY
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.ITALIC)
                    })
                } else {
                    val rowLinePaint = Paint().apply {
                        color = Color.parseColor("#E2E8F0")
                        strokeWidth = 0.5f
                    }
                    val detailPaint = Paint().apply {
                        color = Color.parseColor("#64748B")
                        textSize = 7.5f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                        isAntiAlias = true
                    }
                    
                    val showActs = activities.take(9)
                    for ((idx, act) in showActs.withIndex()) {
                        val rowY = currentY
                        val rowHeight = 44f
                        
                        canvas.drawLine(40f, rowY + rowHeight, 555f, rowY + rowHeight, rowLinePaint)
                        canvas.drawText("${idx + 1}", 45f, rowY + 15f, boldPaint.apply { textSize = 8.5f })
                        
                        val titleText = if (act.titulo.length > 50) act.titulo.take(47) + "..." else act.titulo
                        canvas.drawText(titleText, 75f, rowY + 15f, boldPaint.apply { textSize = 8f; color = Color.parseColor("#1E293B") })
                        
                        val dateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(act.fecha))
                        canvas.drawText("Fecha: $dateFormatted | Supervisor: ${act.supervisor}", 75f, rowY + 26f, detailPaint)
                        canvas.drawText("Área: ${act.categoria}", 75f, rowY + 37f, detailPaint)
                        
                        val lugarText = if (act.lugar.length > 24) act.lugar.take(22) + "..." else act.lugar
                        canvas.drawText(lugarText, 345f, rowY + 18f, textPaint.apply { textSize = 7.5f })
                        
                        canvas.drawText(act.estado.uppercase(), 465f, rowY + 18f, Paint(thPaint).apply { 
                            textSize = 7.5f
                            color = if (act.estado == "Validada") Color.parseColor("#0F766E") else Color.parseColor("#D97706")
                        })
                        
                        canvas.drawText("${act.horasObtenidas} hrs", 515f, rowY + 18f, boldPaint.apply { textSize = 8f; color = priColor })
                        currentY += rowHeight
                    }
                    
                    if (activities.size > 9) {
                        canvas.drawText("... y ${activities.size - 9} registros adicionales listados en el sistema general.", 75f, currentY + 15f, Paint(textPaint).apply {
                            textSize = 7.5f
                            color = Color.parseColor("#94A3B8")
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.ITALIC)
                        })
                    }
                }
            } else if (page.pageNumber == 3) {
                var currentY = 120f
                boldPaint.textSize = 10f
                boldPaint.color = priColor
                canvas.drawText("III. MÉTRICAS DE DESEMPEÑO Y GRÁFICO DE METAS", 40f, currentY, boldPaint)
                currentY += 24f
                
                data class BarItem(val label: String, val actHrs: Int, val metaHrs: Int)
                val bars = listOf(
                    BarItem("Trabajo Educativo Social (TES)", actTes, metaTes),
                    BarItem("Relevo por la Vida Honduras", actRelevo, metaRelevo),
                    BarItem("Voluntariado Comunitario Activo", actVol, metaVol),
                    BarItem("Práctica Profesional Pre-Universitaria", actPrac, metaPrac),
                    BarItem("Actividades Académicas Complementarias", actAcad, metaAcad),
                    BarItem("LOGRO GLOBAL TOTAL DE BITÁCORA DE CAMPO", totalHours.toInt(), metaTes + metaRelevo + metaVol + metaPrac + metaAcad)
                )
                
                val trackPaint = Paint().apply {
                    color = Color.parseColor("#EEF2F6")
                    style = Paint.Style.FILL
                }
                val fillPaint = Paint().apply {
                    color = priColor
                    style = Paint.Style.FILL
                }
                val labelPaint = Paint().apply {
                    color = Color.parseColor("#1E293B")
                    textSize = 8f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    isAntiAlias = true
                }
                val detailsPaint = Paint().apply {
                    color = Color.parseColor("#64748B")
                    textSize = 8f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                    isAntiAlias = true
                }
                
                for ((bIdx, bar) in bars.withIndex()) {
                    val pct = if (bar.metaHrs > 0) (bar.actHrs * 100) / bar.metaHrs else 0
                    val cappedPct = if (pct > 100) 100 else pct
                    val barFillColor = if (bIdx == 5) accColor else priColor
                    fillPaint.color = barFillColor
                    
                    canvas.drawText(bar.label.uppercase(), 45f, currentY, labelPaint.apply {
                        if (bIdx == 5) typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    })
                    canvas.drawText("${bar.actHrs} de ${bar.metaHrs} hrs ($pct%)", 450f, currentY, detailsPaint)
                    currentY += 6f
                    
                    val trackRect = RectF(45f, currentY, 550f, currentY + 10f)
                    canvas.drawRoundRect(trackRect, 4f, 4f, trackPaint)
                    
                    if (cappedPct > 0) {
                        val fillWidth = (cappedPct / 100f) * 505f
                        val fillRect = RectF(45f, currentY, 45f + fillWidth, currentY + 10f)
                        canvas.drawRoundRect(fillRect, 4f, 4f, fillPaint)
                    }
                    currentY += 26f
                }
                
                currentY += 6f
                boldPaint.textSize = 8f
                boldPaint.color = priColor
                canvas.drawText("DECLARACIÓN JURADA DE ORIGINALIDAD:", 45f, currentY, boldPaint)
                currentY += 12f
                val declText = "Hacemos manifestar, de forma digna y honorable, que todos los registros de bitácoras académicas corresponden a servicios comunitarios verdaderos para el crecimiento cívico de la nación."
                val wrapDecl = wrapTextInLines(declText, 95)
                for (wd in wrapDecl) {
                    canvas.drawText(wd, 45f, currentY, Paint(textPaint).apply { textSize = 7.5f; color = Color.parseColor("#475569") })
                    currentY += 10f
                }
            }

            // 6. Draw extremely polished formal signing modules on page 3
            if (page.pageNumber == 3) {
                val fLineY = 700f
                val fTextY = 715f
                val signLinePaint = Paint().apply { color = priColor; strokeWidth = 1.2f }
                
                val centerPaint = Paint().apply {
                    color = priColor
                    textSize = 8.5f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                
                val centerSubPaint = Paint().apply {
                    color = Color.parseColor("#546E7A")
                    textSize = 7.5f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                // Left sign limit (Student)
                canvas.drawLine(60f, fLineY, 240f, fLineY, signLinePaint)
                canvas.drawText("FIRMA AUTÓGRAFA DEL ALUMNO", 150f, fTextY, centerPaint)
                canvas.drawText(getField(student?.nombre), 150f, fTextY + 11f, centerSubPaint)

                // Right sign limit (Supervision)
                canvas.drawLine(350f, fLineY, 530f, fLineY, signLinePaint)
                canvas.drawText("CORDINADOR / ASESOR DOCENTE", 440f, fTextY, centerPaint)
                canvas.drawText("Sello de Aprobación de Colegio", 440f, fTextY + 11f, centerSubPaint)

                // High-End round official seal background in center
                val sealX = 295f
                val sealY = 685f
                val wrRed = Color.red(priColor)
                val wrGreen = Color.green(priColor)
                val wrBlue = Color.blue(priColor)
                canvas.drawCircle(sealX, sealY, 24f, Paint().apply {
                    color = Color.argb(22, wrRed, wrGreen, wrBlue)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                })
                canvas.drawCircle(sealX, sealY, 24f, Paint().apply {
                    color = priColor
                    style = Paint.Style.STROKE
                    strokeWidth = 0.5f
                    isAntiAlias = true
                })
                canvas.drawText("CUMPLIDO", sealX, sealY + 2f, Paint().apply {
                    color = priColor
                    textSize = 5.5f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                })
                canvas.drawText("SEDUC HN", sealX, sealY + 10f, Paint().apply {
                    color = priColor
                    textSize = 4.5f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                })
            }

            // 7. Footer bar page index
            val bottomLinePaint = Paint().apply { color = Color.parseColor("#ECEFF1"); strokeWidth = 1f }
            canvas.drawLine(40f, 792f, 555f, 792f, bottomLinePaint)
            
            val footText = "Expediente Local Emitido por MI TES & Relevo por vida • Honduras • Página ${idx + 1} de ${pages.size}"
            canvas.drawText(footText, 40f, 804f, Paint().apply {
                color = Color.parseColor("#90A4AE")
                textSize = 7.5f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                isAntiAlias = true
            })

            pdfDocument.finishPage(pdfPage)
        }

        val out = FileOutputStream(targetFile)
        pdfDocument.writeTo(out)
        pdfDocument.close()
        out.flush()
        out.close()
    }

    /**
     * Generates a spectacular and highly stylized HTML file with beautiful styling matching
     * the selected dynamic category theme. Saved with .docx or .doc extension.
     */
    fun writeDocx(targetFile: File, customTemplateContent: String? = null) {
        val out = FileOutputStream(targetFile)
        val sb = java.lang.StringBuilder()

        // RTF header configuration with standard ANSI and Latin character-set parameters to avoid corruption
        // Color 1: Honduras Blue primary, Color 2: Charcoal slate, Color 3: Pure Black, Color 4: Cyan accent, Color 5: Light gray line
        sb.append("{\\rtf1\\ansi\\ansicpg1252\\deff0\\nouicompat{\\fonttbl{\\f0\\fnil\\fcharset0 Calibri;}{\\f1\\fswiss\\fprq2\\fcharset0 Arial;}}\n")
        sb.append("{\\colortbl ;\\red15\\green58\\blue95;\\red100\\green116\\blue139;\\red0\\green0\\blue0;\\red0\\green168\\blue150;\\red220\\green225\\blue230;}\n")
        sb.append("\\paperw11906\\paperh16838\\margl1440\\margr1440\\margt1440\\margb1440\n")
        sb.append("\\viewkind4\\uc1\n")

        // Main Institutional Header
        sb.append("\\pard\\sa100\\qc\\cf1\\b\\f1\\fs32 REP\\'daBLICA DE HONDURAS\\par\n")
        sb.append("\\cf1\\fs24 SECRETAR\\'cdA DE ESTADO EN EL DESPACHO DE EDUCACI\\'d3N\\par\n")
        sb.append("\\cf2\\fs18\\i \"La educaci\\'f3n es el primer derecho de un pueblo libre y democr\\'e1tico\"\\i0\\par\n")
        sb.append("\\cf1\\b\\fs26 CERTIFICADO DE ACREDITACI\\'d3N ACAD\\'c9MICA TES\\par\n")
        sb.append("\\cf4\\fs16 SISTEMA OFICIAL DE VALIDACI\\'d3N LOCAL DE EXPEDIENTES (RTF OFICIAL)\\par\n")
        sb.append("\\cf3\\b0\\fs20 _________________________________________________________________________________\\par\\par\n")

        // Student Expediente Profile Box
        sb.append("\\pard\\cf1\\b\\fs22 EXPEDIENTE COMPLETO DEL ESTUDIANTE CANDIDATO:\\par\n")
        sb.append("\\pard\\cf3\\b0\\fs20  * ALUMNO: \\b ${getField(student?.nombre).uppercase()}\\b0\\par\n")
        sb.append(" * CENTRO EDUCATIVO: ${getField(student?.centroEducativo).uppercase()}\\par\n")
        sb.append(" * ESPECIALIDAD O CARRERA: ${getField(student?.carrera).uppercase()}\\par\n")
        sb.append(" * GRADO Y SECCI\\'d3N: ${getField(student?.grado)} \"${getField(student?.seccion)}\"\\par\n")
        sb.append(" * CICLO DE MATR\\'cdCULA REGISTRAL: A\\'f1o ${student?.anioAcademico ?: 2026}\\par\n")
        sb.append(" * TIPO DE REPORTE ACAD\\'c9MICA: \\b ${selectedTemplate.uppercase()}\\b0\\par\n")
        sb.append(" * ENLACE DE FIRMA QR DE VERIFICACI\\'d3N: \\cf4\\i https://academic-id-verification-tes.web.app/verify/student?id=${student?.id ?: 1}\\cf3\\i0\\par\n")
        sb.append(" * AVAL REGLAMENTARIO LEGAL: \\cf2\\i ${theme.legalBasis}\\cf3\\i0\\par\n")
        sb.append("_________________________________________________________________________________\\par\\par\n")

        // Directives and Legal basis
        sb.append("\\pard\\cf1\\b\\fs22 DIRECTIVA DE FINES Y COMPROMISOS ACAD\\'c9MICOS DEL PROYECTO:\\par\n")
        sb.append("\\pard\\cf2\\b0\\fs18\\i ${theme.articleText}\\cf3\\i0\\par\n")
        sb.append("_________________________________________________________________________________\\par\\par\n")

        // Filter work list
        val filteredActivities = when (selectedTemplate) {
            "Informe Relevo" -> activities.filter { it.categoria.contains("relevo", ignoreCase = true) }
            "Informe TES" -> activities.filter { it.categoria.contains("TES", ignoreCase = true) }
            else -> activities
        }

        // Section Activities
        sb.append("\\pard\\cf1\\b\\fs22 SECCI\\'d3N DE REGISTRO DE BIT\\'c1CORA DE JORNADAS DE TRABAJO REALIZADO:\\par\n")
        if (filteredActivities.isEmpty()) {
            sb.append("\\pard\\cf2\\b0\\fs18 [Sin jornadas grabadas para exportar en este período académico]\\par\n")
        } else {
            filteredActivities.forEach { act ->
                val actDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(act.fecha))
                sb.append("\\pard\\cf1\\b\\fs20  * PROYECTO: \"${act.titulo.uppercase()}\"\\par\n")
                sb.append("\\pard\\cf3\\b0\\fs18    - AREA COMUNITARIA: ${act.categoria}\\par\n")
                sb.append("   - LUGAR O SEDE: ${act.lugar}  [$actDate]\\par\n")
                sb.append("   - TIEMPO DE SERVICIO DE DURACI\\'d3N: ${act.horaInicio} a ${act.horaSalida} (${act.horasObtenidas} horas logradas)\\par\n")
                sb.append("   - TUTOR SUPERVISOR: ${act.supervisor} (${act.cargoSupervisor})\\par\n")
                sb.append("   - RESUMEN DE JORNADA: \\cf2 ${act.descripcion}\\cf3\\par\n")
                if (act.observaciones.isNotEmpty()) {
                    sb.append("   - DETALLE DE TUTOR: \\cf4 ${act.observaciones}\\cf3\\par\n")
                }
                sb.append("\\par\n")
            }
        }
        sb.append("_________________________________________________________________________________\\par\\par\n")

        // Section Evidences
        sb.append("\\pard\\cf1\\b\\fs22 SECCI\\'d3N DE COMPROBANTES Y EVIDENCIAS DIGITALES ASOCIADAS:\\par\n")
        if (evidences.isEmpty()) {
            sb.append("\\pard\\cf2\\b0\\fs18 [No se registran evidencias ni comprobantes catalogados en este informe]\\par\n")
        } else {
            evidences.forEach { ev ->
                sb.append("\\pard\\cf1\\b\\fs20  - RECURSO DE SOPORTE: \"${ev.titulo.uppercase()}\"\\par\n")
                sb.append("\\pard\\cf3\\b0\\fs18    * CLASIFICACI\\'d3N: ${ev.etiqueta}\\par\n")
                sb.append("   * FORMATO EXTENSI\\'d3N: ${ev.tipoArchivo}\\par\n")
                sb.append("   * UBICACI\\'d3N DEL DEP\\'d3SITO LOCAL: ${ev.rutaArchivo}\\par\n")
                sb.append("   * DETALLES COGNITIVOS: \\cf2\\i ${if (ev.notas.isEmpty()) "Sin anotaciones" else ev.notas}\\cf3\\i0\\par\\par\n")
            }
        }
        sb.append("_________________________________________________________________________________\\par\\par\n")

        // Section Progress Stats Summary
        sb.append("\\pard\\cf1\\b\\fs22 CONSOLIDANTE TABULAR Y BALANCE GENERAL DE CR\\'c9DITOS ACUMULADOS (AVANCE):\\par\n")
        val metaTes = student?.metaHorasTES ?: 100
        val metaRelevo = student?.metaHorasRelevo ?: 40
        val metaVol = student?.metaHorasVoluntariado ?: 50
        val metaPrac = student?.metaHorasPracticas ?: 100
        val metaAcad = student?.metaHorasAcademicas ?: 50

        val actTes = activities.filter { it.categoria.contains("TES", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val actRelevo = activities.filter { it.categoria.contains("relevo", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val actVol = activities.filter { it.categoria.contains("voluntar", ignoreCase = true) || it.categoria.contains("comunit", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val actPrac = activities.filter { it.categoria.contains("pr\u00e1ct", ignoreCase = true) || it.categoria.contains("pract", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val actAcad = activities.filter { it.categoria.contains("acad\u00e9", ignoreCase = true) || it.categoria.contains("acade", ignoreCase = true) }.sumOf { it.horasObtenidas }.toInt()
        val totalHours = activities.sumOf { it.horasObtenidas }.toInt()

        sb.append("\\pard\\cf3\\b0\\fs20  * TRABAJO EDUCATIVO SOCIAL (TES): \\b $actTes de $metaTes hrs\\b0  (${if(metaTes>0) (actTes*100)/metaTes else 0}%)\\par\n")
        sb.append(" * RELEVO POR LA VIDA: \\b $actRelevo de $metaRelevo hrs\\b0  (${if(metaRelevo>0) (actRelevo*100)/metaRelevo else 0}%)\\par\n")
        sb.append(" * VOLUNTARIADO SOCIAL Y COMUNITARIO: \\b $actVol de $metaVol hrs\\b0  (${if(metaVol>0) (actVol*100)/metaVol else 0}%)\\par\n")
        sb.append(" * PR\\'c1CTICA PROFESIONAL: \\b $actPrac de $metaPrac hrs\\b0  (${if(metaPrac>0) (actPrac*100)/metaPrac else 0}%)\\par\n")
        sb.append(" * CURRICULUM ACAD\\'c9MICO COMPLEMENTARIO: \\b $actAcad de $metaAcad hrs\\b0  (${if(metaAcad>0) (actAcad*100)/metaAcad else 0}%)\\par\n")
        sb.append(" * \\b CONSERVACI\\'d3N TOTAL COMPLETADO GLOBAL:\\b0  \\b $totalHours de ${metaTes+metaRelevo+metaVol+metaPrac+metaAcad} hrs\\b0  (${if((metaTes+metaRelevo+metaVol+metaPrac+metaAcad)>0) (totalHours*100)/(metaTes+metaRelevo+metaVol+metaPrac+metaAcad) else 0}%)\\par\n")
        sb.append("_________________________________________________________________________________\\par\\par\\par\n")

        // Final Official validation block
        sb.append("\\pard\\cf3\\b0\\fs20  _____________________________________            _____________________________________\\par\n")
        sb.append("        FIRMA COLEGIAL DEL ESTUDIANTE                    COORDINADOR / TUTOR INSTITUCIONAL\\par\n")
        sb.append("        ${getField(student?.nombre)}              Secretar\\'eda de Educaci\\'f3n / Centro Escolar\\par\n")

        sb.append("}\n")

        out.write(sb.toString().toByteArray(Charsets.UTF_8))
        out.flush()
        out.close()
    }
}

data class DocumentPreviewPage(
    val pageNumber: Int,
    val title: String,
    val content: String
)

class DocxTemplateParser {
    companion object {
        fun extractTextFromDocx(filePath: String): String {
            return try {
                val file = java.io.File(filePath)
                if (!file.exists()) return ""
                val zipFile = java.util.zip.ZipFile(file)
                val entry = zipFile.getEntry("word/document.xml") ?: return ""
                val inputStream = zipFile.getInputStream(entry)
                val xmlContent = inputStream.bufferedReader().use { it.readText() }

                val textBuilder = java.lang.StringBuilder()
                var inTag = false
                val tagContent = java.lang.StringBuilder()
                for (char in xmlContent) {
                    if (char == '<') {
                        inTag = true
                        tagContent.setLength(0)
                    } else if (char == '>') {
                        inTag = false
                        val tagStr = tagContent.toString()
                        if (tagStr == "/w:p" || tagStr == "w:br" || tagStr == "w:tab" || tagStr == "/w:r") {
                            if (tagStr == "/w:p") {
                                textBuilder.append("\n")
                            } else if (tagStr == "w:tab") {
                                textBuilder.append("\t")
                            }
                        }
                    } else {
                        if (inTag) {
                            tagContent.append(char)
                        } else {
                            textBuilder.append(char)
                        }
                    }
                }
                zipFile.close()
                textBuilder.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                "[Error procesando formato docx externo: ${e.message}]"
            }
        }
    }
}
