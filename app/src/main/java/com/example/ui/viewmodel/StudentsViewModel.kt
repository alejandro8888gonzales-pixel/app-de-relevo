package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class StudentsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AcademicRepository(
        database.usuarioDao(),
        database.actividadDao(),
        database.evidenciaDao(),
        database.certificadoDao(),
        database.logroDao(),
        database.diarioDao(),
        database.configuracionDao(),
        database.historialCambiosDao(),
        database.galeriaMediaDao()
    )

    private val geminiService = GeminiService()

    // --- SYSTEM DYNAMIC PARAMETERS & CUSTOMIZATIONS ---
    private val _activeYear = MutableStateFlow("2026")
    val activeYear: StateFlow<String> = _activeYear.asStateFlow()

    // --- LIVE FLOWS ---
    val usuario = repository.usuarioFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val actividades = repository.allActividades.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val evidencias = combine(repository.allEvidencias, repository.allActividades, activeYear) { evs, acts, year ->
        val actYearMap = acts.associate { it.id to it.anioCiclo }
        evs.filter { ev ->
            val actYr = ev.actividadId?.let { actYearMap[it] }
            if (actYr != null) {
                actYr == year
            } else {
                val cal = Calendar.getInstance()
                cal.timeInMillis = ev.fechaRegistro
                val calYear = cal.get(Calendar.YEAR).toString()
                calYear == year
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val certificados = combine(repository.allCertificados, activeYear) { certs, year ->
        certs.filter { cert ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = cert.fecha
            val calYear = cal.get(Calendar.YEAR).toString()
            calYear == year
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diarios = combine(repository.allDiarios, activeYear) { jalls, year ->
        jalls.filter { j ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = j.fecha
            val calYear = cal.get(Calendar.YEAR).toString()
            calYear == year
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logros = repository.allLogros.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val configuraciones = repository.allConfiguraciones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val historial = repository.allHistorial.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaFiles = combine(repository.allMedia, activeYear) { meds, year ->
        meds.filter { it.anio.toString() == year }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Persistent Color Themes, Typography Fonts & Alarm Reminders
    val appTheme = repository.allConfiguraciones
        .map { list -> list.firstOrNull { it.clave == "app_theme" }?.valor ?: "azul_tech" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "azul_tech")

    val appFont = repository.allConfiguraciones
        .map { list -> list.firstOrNull { it.clave == "app_font" }?.valor ?: "sans_grotesk" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "sans_grotesk")

    val appTextColor = repository.allConfiguraciones
        .map { list -> list.firstOrNull { it.clave == "app_text_color" }?.valor ?: "blanco" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "blanco")

    val modoDiplomatico = repository.allConfiguraciones
        .map { list -> list.firstOrNull { it.clave == "modo_diplomatico" }?.valor ?: "false" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "false")

    val isLightMode = repository.allConfiguraciones
        .map { list -> list.firstOrNull { it.clave == "is_light_mode" }?.valor ?: "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "true")

    val reminderIntervalHours = repository.allConfiguraciones
        .map { list -> list.firstOrNull { it.clave == "reminder_interval" }?.valor?.toIntOrNull() ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val compresionDocumentos = repository.allConfiguraciones
        .map { list -> list.firstOrNull { it.clave == "compresion_documentos" }?.valor ?: "false" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "false")

    val dashboardOrder = repository.allConfiguraciones
        .map { list -> list.firstOrNull { it.clave == "dashboard_order" }?.valor ?: "guia,year,progress,calendar,categories,recent" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "guia,year,progress,calendar,categories,recent")

    val groupMediaUploads = repository.allConfiguraciones
        .map { list -> list.firstOrNull { it.clave == "group_media_uploads" }?.valor ?: "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "true")

    fun updateConfigValue(clave: String, valor: String) {
        viewModelScope.launch {
            repository.setConfiguracion(clave, valor)
            if (clave == "reminder_interval") {
                val h = valor.toIntOrNull() ?: 0
                com.example.NotificationHelper.scheduleRepeatingReminder(getApplication(), h)
            }
        }
    }

    // --- SECURITY & PREFERENCES ---
    private val _isConfigLoaded = MutableStateFlow(false)
    val isConfigLoaded: StateFlow<Boolean> = _isConfigLoaded.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _securityPin = MutableStateFlow("")
    val securityPin: StateFlow<String> = _securityPin.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    // --- SYSTEM DYNAMIC PARAMETERS & CUSTOMIZATIONS ---
    private val _customCategories = MutableStateFlow(listOf("TES", "Relevo por la Vida", "Voluntariado", "Prácticas", "Actividades Académicas"))
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    private val _dashboardTitle = MutableStateFlow("MI TES & Relevo por vida")
    val dashboardTitle: StateFlow<String> = _dashboardTitle.asStateFlow()

    private val _dashboardSubtitle = MutableStateFlow("Expediente Estudiantil & Registro Académico Sólido")
    val dashboardSubtitle: StateFlow<String> = _dashboardSubtitle.asStateFlow()

    // --- AI TRANSCRIPTS AND GENERATIONS ---
    private val _aiOutput = MutableStateFlow("")
    val aiOutput: StateFlow<String> = _aiOutput.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiMode = MutableStateFlow("Estándar")
    val aiMode: StateFlow<String> = _aiMode.asStateFlow()

    fun setAiMode(mode: String) {
        _aiMode.value = mode
    }

    fun clearChatHistory() {
        _chatHistory.value = emptyList()
        _aiOutput.value = ""
        viewModelScope.launch {
            repository.setConfiguracion("CHAT_HISTORY", "")
        }
    }

    private fun serializeChatHistory(history: List<ChatMessage>): String {
        val array = JSONArray()
        history.forEach { msg ->
            val obj = JSONObject()
            obj.put("id", msg.id)
            obj.put("text", msg.text)
            obj.put("isUser", msg.isUser)
            obj.put("timestamp", msg.timestamp)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeChatHistory(jsonStr: String): List<ChatMessage> {
        if (jsonStr.isEmpty() || jsonStr == "[]") return emptyList()
        val list = mutableListOf<ChatMessage>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        text = obj.optString("text", ""),
                        isUser = obj.optBoolean("isUser", true),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private val _ocrResult = MutableStateFlow<OcrResult?>(null)
    val ocrResult: StateFlow<OcrResult?> = _ocrResult.asStateFlow()

    private val _ocrLoading = MutableStateFlow(false)
    val ocrLoading: StateFlow<Boolean> = _ocrLoading.asStateFlow()

    // --- DYNAMIC STATISTICS ---
    val estadisticas = combine(actividades, usuario, activeYear) { list, user, activeYr ->
        val currentYearList = list.filter { it.anioCiclo == activeYr }

        val tes = currentYearList.filter { it.categoria.contains("TES", ignoreCase = true) && it.estado != "Pendiente" }.sumOf { it.horasObtenidas }
        val relevo = currentYearList.filter { it.categoria.contains("Relevo", ignoreCase = true) && it.estado != "Pendiente" }.sumOf { it.horasObtenidas }
        val voluntariado = currentYearList.filter { (it.categoria.contains("Voluntariado", ignoreCase = true) || it.categoria.contains("Comunitaria", ignoreCase = true)) && it.estado != "Pendiente" }.sumOf { it.horasObtenidas }
        val practicas = currentYearList.filter { (it.categoria.contains("Práctica", ignoreCase = true) || it.categoria.contains("Practica", ignoreCase = true)) && it.estado != "Pendiente" }.sumOf { it.horasObtenidas }
        val academicas = currentYearList.filter { (it.categoria.contains("Académica", ignoreCase = true) || it.categoria.contains("Academica", ignoreCase = true)) && it.estado != "Pendiente" }.sumOf { it.horasObtenidas }
        val total = tes + relevo + voluntariado + practicas + academicas

        val metaTes = user?.metaHorasTES ?: 100
        val metaRelevo = user?.metaHorasRelevo ?: 40
        val metaVoluntariado = user?.metaHorasVoluntariado ?: 50
        val metaPracticas = user?.metaHorasPracticas ?: 100
        val metaAcademicas = user?.metaHorasAcademicas ?: 50
        val metaTotal = metaTes + metaRelevo + metaVoluntariado + metaPracticas + metaAcademicas

        Stats(
            tesHoras = tes, relevoHoras = relevo, voluntariadoHoras = voluntariado,
            practicasHoras = practicas, academicasHoras = academicas, totalHoras = total,
            metaTes = metaTes, metaRelevo = metaRelevo, metaVoluntariado = metaVoluntariado,
            metaPracticas = metaPracticas, metaAcademicas = metaAcademicas, metaTotal = metaTotal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Stats())

    init {
        viewModelScope.launch {
            try {
                repository.initLogrosIfNeeded()
                // Load Security preferences
                val pin = repository.getConfiguracion("SECURITY_PIN", "")
                _securityPin.value = pin
                _isLocked.value = pin.isNotEmpty()

                val onboardingCompletedStr = repository.getConfiguracion("ONBOARDING_COMPLETED", "false")
                _onboardingCompleted.value = onboardingCompletedStr == "true"

                // Load Custom parameters
                val activeYr = repository.getConfiguracion("CONFIG_ACTIVE_YEAR", "2026")
                _activeYear.value = activeYr

                val catsStr = repository.getConfiguracion("CUSTOM_CATEGORIES", "TES, Relevo por la Vida, Voluntariado, Prácticas, Actividades Académicas, Actividades Deportivas, Actividades Cívicas, Proyectos Especiales")
                _customCategories.value = catsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                val titleConfig = repository.getConfiguracion("DASHBOARD_TITLE", "MI TES & Relevo por vida")
                _dashboardTitle.value = titleConfig

                val subtitleConfig = repository.getConfiguracion("DASHBOARD_SUBTITLE", "Asistente de Trabajo Educativo Social")
                _dashboardSubtitle.value = subtitleConfig

                ensureDefaultUsuario()

                val savedHistoryStr = repository.getConfiguracion("CHAT_HISTORY", "")
                _chatHistory.value = deserializeChatHistory(savedHistoryStr)

                // Autonomous year checking & yearly progression
                checkAndEvolveAcademicYear()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isConfigLoaded.value = true
            }
        }
    }

    private suspend fun ensureDefaultUsuario() {
        val u = repository.getUsuario()
        if (u == null) {
            repository.saveUsuario(
                Usuario(
                    nombre = "Estudiante Hondureño",
                    centroEducativo = "Instituto Nacional de Honduras",
                    carrera = "Bachillerato Técnico Profesional en Informática",
                    grado = "12vo",
                    seccion = "A",
                    anioAcademico = "2026",
                    correo = "estudiante@educacion.hn",
                    telefono = "+504 9999-9999",
                    biografia = "Iniciando mi Trayecto de Trabajo Educativo Social (TES) y actividades académicas."
                )
            )
        }
    }

    // --- USER PROFILE ACTIONS ---
    fun updateUsuario(u: Usuario) {
        viewModelScope.launch {
            repository.saveUsuario(u)
            if (u.anioAcademico.isNotEmpty()) {
                repository.setConfiguracion("CONFIG_ACTIVE_YEAR", u.anioAcademico)
                _activeYear.value = u.anioAcademico
            }
            triggerLogrosCheck()
        }
    }

    fun completeOnboarding(
        studentName: String,
        school: String,
        career: String,
        cycle: String,
        goalTES: Int,
        goalRelevo: Int,
        goalVoluntariado: Int
    ) {
        viewModelScope.launch {
            repository.setConfiguracion("ONBOARDING_COMPLETED", "true")
            _onboardingCompleted.value = true
            
            val currentUsr = repository.getUsuario() ?: Usuario()
            val updatedUsr = currentUsr.copy(
                nombre = studentName,
                centroEducativo = school,
                carrera = career,
                anioAcademico = cycle,
                metaHorasTES = goalTES,
                metaHorasRelevo = goalRelevo,
                metaHorasVoluntariado = goalVoluntariado
            )
            repository.saveUsuario(updatedUsr)
            
            repository.setConfiguracion("CONFIG_ACTIVE_YEAR", cycle)
            _activeYear.value = cycle
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            repository.setConfiguracion("ONBOARDING_COMPLETED", "false")
            _onboardingCompleted.value = false
        }
    }

    fun changeActiveYear(newYear: String) {
        viewModelScope.launch {
            repository.setConfiguracion("CONFIG_ACTIVE_YEAR", newYear)
            _activeYear.value = newYear
            val u = repository.getUsuario()
            if (u != null) {
                repository.saveUsuario(u.copy(anioAcademico = newYear))
            }
            triggerLogrosCheck()
        }
    }

    fun saveCustomCategories(categoriesList: List<String>) {
        viewModelScope.launch {
            val str = categoriesList.joinToString(",")
            repository.setConfiguracion("CUSTOM_CATEGORIES", str)
            _customCategories.value = categoriesList
        }
    }

    fun customizeDashboard(title: String, subtitle: String) {
        viewModelScope.launch {
            repository.setConfiguracion("DASHBOARD_TITLE", title)
            repository.setConfiguracion("DASHBOARD_SUBTITLE", subtitle)
            _dashboardTitle.value = title
            _dashboardSubtitle.value = subtitle
        }
    }

    fun checkAndEvolveAcademicYear() {
        viewModelScope.launch {
            val lastActiveYear = repository.getConfiguracion("CONFIG_ACTIVE_YEAR", "2026")
            val calendarYear = Calendar.getInstance().get(Calendar.YEAR).toString()

            if (lastActiveYear.isEmpty()) {
                repository.setConfiguracion("CONFIG_ACTIVE_YEAR", calendarYear)
                _activeYear.value = calendarYear
                return@launch
            }

            val calYearInt = calendarYear.toIntOrNull() ?: 2026
            val lastYearInt = lastActiveYear.toIntOrNull() ?: 2026
            if (calYearInt > lastYearInt) {
                // Autonomous yearly evolution detected!
                val oldYearActs = listActivitiesForYear(lastActiveYear)
                val totalHoras = oldYearActs.sumOf { it.horasObtenidas }
                val report = JSONObject()
                report.put("ano", lastActiveYear)
                report.put("totalHoras", totalHoras)
                report.put("actividadesCount", oldYearActs.size)

                val actsArray = JSONArray()
                oldYearActs.forEach { a ->
                    val o = JSONObject()
                    o.put("titulo", a.titulo)
                    o.put("categoria", a.categoria)
                    o.put("horasObtenidas", a.horasObtenidas)
                    o.put("fecha", a.fecha)
                    o.put("supervisor", a.supervisor)
                    actsArray.put(o)
                }
                report.put("actividades", actsArray)

                try {
                    val file = File(getApplication<Application>().filesDir, "historial_anual_$lastActiveYear.json")
                    file.writeText(report.toString(4))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val desc = "Evolución anual autónoma: Ciclo académico $lastActiveYear archivado con $totalHoras horas en ${oldYearActs.size} bitácoras. Inicio ciclo $calendarYear de forma automática."
                repository.setConfiguracion("CONFIG_ACTIVE_YEAR", calendarYear)
                _activeYear.value = calendarYear

                database.historialCambiosDao().insertHistorial(
                    HistorialCambios(
                        entidad = "Sistema",
                        entidadId = 0L,
                        tipoAccion = "Evolución",
                        descripcion = desc
                    )
                )

                val u = repository.getUsuario()
                if (u != null) {
                    repository.saveUsuario(u.copy(anioAcademico = calendarYear))
                }
            }
        }
    }

    private suspend fun listActivitiesForYear(year: String): List<Actividad> {
        val list = repository.getActividadesList()
        return list.filter { it.anioCiclo == year }
    }

    // --- ACTIVITIES ACTION ---
    fun createActividad(act: Actividad) {
        viewModelScope.launch {
            repository.createActividad(act)
            triggerLogrosCheck()
        }
    }

    fun updateActividad(act: Actividad) {
        viewModelScope.launch {
            repository.updateActividad(act)
            triggerLogrosCheck()
        }
    }

    fun deleteActividad(act: Actividad) {
        viewModelScope.launch {
            repository.deleteActividad(act)
            triggerLogrosCheck()
        }
    }

    private fun adjustTimestampToActiveYear(timestamp: Long): Long {
        val targetYear = activeYear.value.toIntOrNull() ?: return timestamp
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        if (cal.get(java.util.Calendar.YEAR) != targetYear) {
            cal.set(java.util.Calendar.YEAR, targetYear)
        }
        return cal.timeInMillis
    }

    // --- EVIDENCIAS ACTION ---
    fun createEvidencia(ev: Evidencia) {
        viewModelScope.launch {
            val adjusted = ev.copy(fechaRegistro = adjustTimestampToActiveYear(ev.fechaRegistro))
            repository.createEvidencia(adjusted)
            triggerLogrosCheck()
        }
    }

    fun updateEvidencia(ev: Evidencia) {
        viewModelScope.launch {
            repository.updateEvidencia(ev)
            triggerLogrosCheck()
        }
    }

    fun deleteEvidencia(ev: Evidencia) {
        viewModelScope.launch {
            repository.deleteEvidencia(ev)
            triggerLogrosCheck()
        }
    }

    // --- CERTIFICADOS ACTION ---
    fun createCertificado(cert: Certificado) {
        viewModelScope.launch {
            val adjusted = cert.copy(fecha = adjustTimestampToActiveYear(cert.fecha))
            repository.createCertificado(adjusted)
            triggerLogrosCheck()
        }
    }

    fun updateCertificado(cert: Certificado) {
        viewModelScope.launch {
            repository.updateCertificado(cert)
            triggerLogrosCheck()
        }
    }

    fun deleteCertificado(cert: Certificado) {
        viewModelScope.launch {
            repository.deleteCertificado(cert)
            triggerLogrosCheck()
        }
    }

    // --- DIARIO ACTION ---
    fun createDiario(entry: Diario) {
        viewModelScope.launch {
            val adjusted = entry.copy(fecha = adjustTimestampToActiveYear(entry.fecha))
            repository.createDiario(adjusted)
            triggerLogrosCheck()
        }
    }

    fun updateDiario(entry: Diario) {
        viewModelScope.launch {
            repository.updateDiario(entry)
        }
    }

    fun deleteDiario(entry: Diario) {
        viewModelScope.launch {
            repository.deleteDiario(entry)
        }
    }

    // --- SECURITY ACTIONS ---
    fun configurePin(pin: String) {
        viewModelScope.launch {
            repository.setConfiguracion("SECURITY_PIN", pin)
            _securityPin.value = pin
            _isLocked.value = pin.isNotEmpty()
        }
    }

    fun verifyPin(enteredPin: String): Boolean {
        if (enteredPin == _securityPin.value) {
            _isLocked.value = false
            return true
        }
        return false
    }

    fun lockApp() {
        if (_securityPin.value.isNotEmpty()) {
            _isLocked.value = true
        }
    }

    // --- GENERAL SYSTEM RESTORE & BACKUP (ORGANIZACION DE POR VIDA) ---
    suspend fun generateBackupJson(): String {
        val root = JSONObject()
        val userObj = JSONObject()
        repository.getUsuario()?.let { u ->
            userObj.put("nombre", u.nombre)
            userObj.put("fotoUri", u.fotoUri ?: "")
            userObj.put("centroEducativo", u.centroEducativo)
            userObj.put("carrera", u.carrera)
            userObj.put("grado", u.grado)
            userObj.put("seccion", u.seccion)
            userObj.put("anioAcademico", u.anioAcademico)
            userObj.put("correo", u.correo)
            userObj.put("telefono", u.telefono)
            userObj.put("biografia", u.biografia)
            userObj.put("metaHorasTES", u.metaHorasTES)
            userObj.put("metaHorasRelevo", u.metaHorasRelevo)
            userObj.put("metaHorasVoluntariado", u.metaHorasVoluntariado)
            userObj.put("metaHorasPracticas", u.metaHorasPracticas)
            userObj.put("metaHorasAcademicas", u.metaHorasAcademicas)
        }
        root.put("usuario", userObj)

        // Actividades
        val actsArray = JSONArray()
        actividades.value.forEach { a ->
            val actObj = JSONObject()
            actObj.put("titulo", a.titulo)
            actObj.put("descripcion", a.descripcion)
            actObj.put("categoria", a.categoria)
            actObj.put("fecha", a.fecha)
            actObj.put("hora", a.hora)
            actObj.put("lugar", a.lugar)
            actObj.put("horasObtenidas", a.horasObtenidas)
            actObj.put("estado", a.estado)
            actObj.put("observaciones", a.observaciones)
            
            // EXTRAS BITÁCORA INTELIGENTE
            actObj.put("horaInicio", a.horaInicio)
            actObj.put("horaSalida", a.horaSalida)
            actObj.put("colegio", a.colegio)
            actObj.put("carreraEstudiante", a.carreraEstudiante)
            actObj.put("nombreEstudiante", a.nombreEstudiante)
            actObj.put("supervisor", a.supervisor)
            actObj.put("cargoSupervisor", a.cargoSupervisor)
            actObj.put("firmaSupervisorBase64", a.firmaSupervisorBase64 ?: "")
            actObj.put("importancia", a.importancia)
            actObj.put("anioCiclo", a.anioCiclo)
            actObj.put("fotosUris", a.fotosUris)
            
            actsArray.put(actObj)
        }
        root.put("actividades", actsArray)

        // Evidencias
        val evArray = JSONArray()
        evidencias.value.forEach { e ->
            val evObj = JSONObject()
            evObj.put("titulo", e.titulo)
            evObj.put("tipoArchivo", e.tipoArchivo)
            evObj.put("rutaArchivo", e.rutaArchivo)
            evObj.put("notas", e.notas)
            evObj.put("fechaRegistro", e.fechaRegistro)
            evObj.put("etiqueta", e.etiqueta)
            evObj.put("esFavorito", e.esFavorito)
            evObj.put("ocrTextoExtraido", e.ocrTextoExtraido ?: "")
            evObj.put("actividadId", e.actividadId ?: -1L)
            evArray.put(evObj)
        }
        root.put("evidencias", evArray)

        // Certificados
        val certArray = JSONArray()
        certificados.value.forEach { c ->
            val certObj = JSONObject()
            certObj.put("titulo", c.titulo)
            certObj.put("institucion", c.institucion)
            certObj.put("fecha", c.fecha)
            certObj.put("horas", c.horas)
            certObj.put("rutaArchivo", c.rutaArchivo)
            certObj.put("textoExtraido", c.textoExtraido ?: "")
            certObj.put("notas", c.notas)
            certArray.put(certObj)
        }
        root.put("certificados", certArray)

        // Diario
        val diarioArray = JSONArray()
        diarios.value.forEach { d ->
            val dObj = JSONObject()
            dObj.put("texto", d.texto)
            dObj.put("imagenUri", d.imagenUri ?: "")
            dObj.put("ubicacion", d.ubicacion ?: "")
            dObj.put("estadoAnimo", d.estadoAnimo ?: "")
            dObj.put("fecha", d.fecha)
            diarioArray.put(dObj)
        }
        root.put("diarios", diarioArray)

        // Historial
        val histArray = JSONArray()
        repository.getHistorialList().forEach { h ->
            val hObj = JSONObject()
            hObj.put("entidad", h.entidad)
            hObj.put("entidadId", h.entidadId)
            hObj.put("tipoAccion", h.tipoAccion)
            hObj.put("descripcion", h.descripcion)
            hObj.put("fecha", h.fecha)
            histArray.put(hObj)
        }
        root.put("historial", histArray)

        return root.toString(4)
    }

    fun restoreBackup(jsonStr: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonStr)

                // Restore User
                if (root.has("usuario")) {
                    val userObj = root.getJSONObject("usuario")
                    val u = Usuario(
                        id = 1,
                        nombre = userObj.optString("nombre", "Estudiante"),
                        fotoUri = userObj.optString("fotoUri", null).takeIf { it?.isNotEmpty() == true },
                        centroEducativo = userObj.optString("centroEducativo", ""),
                        carrera = userObj.optString("carrera", ""),
                        grado = userObj.optString("grado", ""),
                        seccion = userObj.optString("seccion", ""),
                        anioAcademico = userObj.optString("anioAcademico", ""),
                        correo = userObj.optString("correo", ""),
                        telefono = userObj.optString("telefono", ""),
                        biografia = userObj.optString("biografia", ""),
                        metaHorasTES = userObj.optInt("metaHorasTES", 100),
                        metaHorasRelevo = userObj.optInt("metaHorasRelevo", 40),
                        metaHorasVoluntariado = userObj.optInt("metaHorasVoluntariado", 50),
                        metaHorasPracticas = userObj.optInt("metaHorasPracticas", 100),
                        metaHorasAcademicas = userObj.optInt("metaHorasAcademicas", 50)
                    )
                    repository.saveUsuario(u)
                }

                // Restore Actividades
                if (root.has("actividades")) {
                    val actsArray = root.getJSONArray("actividades")
                    for (i in 0 until actsArray.length()) {
                        val obj = actsArray.getJSONObject(i)
                        database.actividadDao().insertActividad(
                            Actividad(
                                titulo = obj.getString("titulo"),
                                descripcion = obj.getString("descripcion"),
                                categoria = obj.getString("categoria"),
                                fecha = obj.getLong("fecha"),
                                hora = obj.getString("hora"),
                                lugar = obj.getString("lugar"),
                                horasObtenidas = obj.getDouble("horasObtenidas"),
                                estado = obj.getString("estado"),
                                observaciones = obj.getString("observaciones"),
                                
                                // EXTRAS BITÁCORA INTELIGENTE
                                horaInicio = obj.optString("horaInicio", "08:00"),
                                horaSalida = obj.optString("horaSalida", "12:00"),
                                colegio = obj.optString("colegio", ""),
                                carreraEstudiante = obj.optString("carreraEstudiante", ""),
                                nombreEstudiante = obj.optString("nombreEstudiante", ""),
                                supervisor = obj.optString("supervisor", ""),
                                cargoSupervisor = obj.optString("cargoSupervisor", ""),
                                firmaSupervisorBase64 = obj.optString("firmaSupervisorBase64", "").takeIf { it.isNotEmpty() },
                                importancia = obj.optString("importancia", "Medio"),
                                anioCiclo = obj.optString("anioCiclo", "2026"),
                                fotosUris = obj.optString("fotosUris", "")
                            )
                        )
                    }
                }

                // Restore Evidencias
                if (root.has("evidencias")) {
                    val evArray = root.getJSONArray("evidencias")
                    for (i in 0 until evArray.length()) {
                        val obj = evArray.getJSONObject(i)
                        database.evidenciaDao().insertEvidencia(
                            Evidencia(
                                actividadId = if (obj.getLong("actividadId") != -1L) obj.getLong("actividadId") else null,
                                titulo = obj.getString("titulo"),
                                tipoArchivo = obj.getString("tipoArchivo"),
                                rutaArchivo = obj.getString("rutaArchivo"),
                                notas = obj.getString("notas"),
                                fechaRegistro = obj.getLong("fechaRegistro"),
                                etiqueta = obj.getString("etiqueta"),
                                esFavorito = obj.optBoolean("esFavorito", false),
                                ocrTextoExtraido = obj.optString("ocrTextoExtraido", null).takeIf { it?.isNotEmpty() == true }
                            )
                        )
                    }
                }

                // Restore Certificados
                if (root.has("certificados")) {
                    val certArray = root.getJSONArray("certificados")
                    for (i in 0 until certArray.length()) {
                        val obj = certArray.getJSONObject(i)
                        database.certificadoDao().insertCertificado(
                            Certificado(
                                titulo = obj.getString("titulo"),
                                institucion = obj.getString("institucion"),
                                fecha = obj.getLong("fecha"),
                                horas = obj.getDouble("horas"),
                                rutaArchivo = obj.getString("rutaArchivo"),
                                textoExtraido = obj.optString("textoExtraido", null).takeIf { it?.isNotEmpty() == true },
                                notas = obj.getString("notas")
                            )
                        )
                    }
                }

                // Restore Diarios
                if (root.has("diarios")) {
                    val dr = root.getJSONArray("diarios")
                    for (i in 0 until dr.length()) {
                        val obj = dr.getJSONObject(i)
                        database.diarioDao().insertDiario(
                            Diario(
                                texto = obj.getString("texto"),
                                imagenUri = obj.optString("imagenUri", null).takeIf { it?.isNotEmpty() == true },
                                ubicacion = obj.optString("ubicacion", null).takeIf { it?.isNotEmpty() == true },
                                estadoAnimo = obj.optString("estadoAnimo", null).takeIf { it?.isNotEmpty() == true },
                                fecha = obj.getLong("fecha")
                            )
                        )
                    }
                }

                triggerLogrosCheck()
                onSuccess()
            } catch (e: Exception) {
                onError("Error al importar el respaldo: ${e.localizedMessage}")
            }
        }
    }

    // --- LOCAL ACADEMIC FALLBACK GENERATOR SYSTEM ---
    private fun generateFallbackResponse(prompt: String, activity: com.example.data.Actividad? = null): String {
        return if (activity != null) {
            """
            📘 BORRADOR ACADÉMICO GENERADO POR EL SISTEMA LOCAL DE RESPALDO (TUTOR TES):
            
            1. DESCRIPCIÓN FORMAL DE LA ACTIVIDAD:
            Durante la ejecución de la actividad titulada "${activity.titulo}" en el área de "${activity.categoria}" llevada a cabo en el sector de "${activity.lugar}", se realizaron significativas gestiones de impacto comunitario y aprendizaje práctico. El alumno aplicó de manera rigurosa los lineamientos del Trabajo Educativo Social (TES) de Honduras, logrando cumplir de forma óptima un total de ${activity.horasObtenidas} horas de servicio. Se destaca el alto nivel de compromiso, el fomento de valores cívicos y la participación proactiva con los beneficiarios.
            
            Este proyecto contribuyó directamente al desarrollo de competencias técnicas y sociales esenciales para la formación profesional integral, demostrando cómo el servicio comunitario actúa como pilar fundamental de cambio en nuestra sociedad hondureña.
            
            2. CONCLUSIONES ACADÉMICAS:
            • Primera: Se consolidó el vínculo entre la teoría académica y la acción comunitaria práctica a través del cumplimiento de las horas planificadas.
            • Segunda: El proyecto fomentó la sensibilización social del educando ante las necesidades de entorno locales en Honduras.
            • Tercera: La organización sistemática de evidencias certifica de modo indudable la autenticidad y el progreso continuo del estudiante.
            
            3. RECOMENDACIÓN DE IMPACTO COMUNITARIO:
            Se recomienda encarecidamente institucionalizar esta clase de intervenciones comunitarias, expandiendo el alcance de las actividades de "${activity.categoria}" para que futuras promociones estudiantiles den sostenibilidad a los logros alcanzados.
            
            [Tutor Académico en Modo Resguardo Local - Listo para usar]
            """.trimIndent()
        } else {
            val lower = prompt.lowercase()
            when {
                lower.contains("creo") || lower.contains("creador") || lower.contains("diseñ") || lower.contains("disen") || lower.contains("alejandro") || lower.contains("angie") -> {
                    """
                    🚀 INFORMACIÓN DEL CREADOR (AUTENTICIDAD DEL PROYECTO):
                    
                    Esta aplicación fue creada, diseñada y desarrollada en su totalidad por:
                    👨‍💻 **Alejandro Gonzales Reyes**: Un brillante Informático hondureño con gran visión que desea innovar, transformar y aportar un cambio tecnológico positivo a su país, Honduras.
                    
                    ✨ **Su mayor motivación e inspiración**:
                    A Alejandro lo inspiró profundamente una persona muy especial que siempre lleva en el corazón: **Angie Polio**. Él le prometió solemnemente que ella sería su constante razón e inspiración inquebrantable para mejorar y superarse cada día.
                    
                    👊 **El Pana de Alejandro**:
                    Su gran pana es **Yeison Amador**, de quien Alejandro destaca que es muy pulsero y trabajador (¡un luchador incansable!).
                    
                    ⚠️ **Nota importante de autoría**:
                    La Secretaría de Educación (SEDUC) de Honduras NO diseñó ni creó esta aplicación. Las autoridades gubernamentales no tienen ningún mérito o crédito en el desarrollo técnico de este sistema; es obra exclusiva de la creatividad, esfuerzo y dedicación de Alejandro Gonzales Reyes.
                    """.trimIndent()
                }
                lower.contains("idea") || lower.contains("voluntariado") -> {
                    """
                    💡 SUGERENCIAS DE VOLUNTARIADO E HISTORIAS DE TRABAJO SOCIAL (HONDURAS):
                    
                    Como tu tutor académico local de respaldo, te presento las mejores opciones de servicio para realizar tus horas:
                    
                    1. Reforestación y Educación Ambiental:
                       - Campañas de siembra de árboles nativos y charlas escolares sobre el cambio climático y conservación del agua en tu comunidad.
                    2. Alfabetización y Tutorías Académicas:
                       - Apoyo escolar presencial a niños de escuelas públicas en matemáticas, lectura y habilidades informáticas básicas.
                    3. Campañas de Salud y Prevención:
                       - Participación organizada con la Cruz Roja Hondureña o Relevo por la Vida para promover hábitos saludables o recaudación de insumos médicos.
                    4. Infraestructura de Apoyo Social:
                       - Voluntariado en la edificación de soluciones habitacionales básicas o reparación de centros estudiantiles con organizaciones como TECHO.
                    
                    Recomendación: Elige una opción que guarde relación con tu carrera técnica para maximizar tu experiencia formativa.
                    """.trimIndent()
                }
                lower.contains("bitacora") || lower.contains("redactar") || lower.contains("introduc") -> {
                    """
                    📑 PLANTILLA ACADÉMICA PARA SÍNTESIS DE BITÁCORA (NORMAS DE HONDURAS):
                    
                    Aquí tienes un marco formal para redactar la introducción de tu portafolio o reporte de bitácora:
                    
                    "INTRODUCCIÓN:
                    El presente portafolio documenta de manera fidedigna la trayectoria del Trabajo Educativo Social (TES) desarrollado durante el presente ciclo académico. Con el fin de cumplir con el mandato constitucional y reglamentario que rige la educación media y superior en la República de Honduras, se ha diseñado un plan integral enfocado en la solidaridad ciudadana y la acción humanitaria. A través del registro sistemático de horas, evidencias recopiladas en la bóveda digital, y certificaciones validadas por las autoridades institucionales, este documento constituye la evidencia fidedigna del esfuerzo prestado en favor de la soberanía social e integración comunitaria de nuestro amado país."
                    
                    Copia y adapta este borrador según tu centro educativo.
                    """.trimIndent()
                }
                lower.contains("conclu") || lower.contains("conclusio") -> {
                    """
                    🎓 PROPUESTA DE CONCLUSIONES PARA TU INFORME FINAL DE HORAS:
                    
                    Aquí tienes una propuesta con formato normativo formal:
                    
                    • Conclusión 1: El desarrollo sistemático de las horas del programa evidenció que la planificación rigurosa es vital para alcanzar las metas comunitarias propuestas.
                    • Conclusión 2: El uso de herramientas digitales para salvaguardar evidencias de forma local previno la pérdida y corrupción de la trayectoria de bitácora del estudiante.
                    • Conclusión 3: La solidaridad social demostrada en los proyectos ejecutados contribuye a mitigar las brechas educativas y asistenciales identificadas en el entorno hondureño.
                    """.trimIndent()
                }
                else -> {
                    """
                     TEMA: ORIENTACIÓN ACADÉMICA - MI TES & RELEVO POR VIDA
                    
                    Estimado estudiante, he recibido tu consulta: "${prompt}".
                    
                    Te ofrezco las siguientes orientaciones académicas para guiar tu trabajo de servicio social:
                    
                    1. Claridad Académica: Mantén una redacción formal en tercera persona. Evita modismos de lenguaje cotidiano.
                    2. Resguarda tus Evidencias: Asegura que cada entrada en tu bitácora posea una fotografía real o constancia certificada cargada de forma segura en tu bóveda local de almacenamiento.
                    3. Cumplimiento de Metas: Monitorea periódicamente el velocímetro en tu panel de control (Dashboard) para asegurar que el balance de horas obtenidas avance de forma congruente en cada categoría.
                    4. Objetivos del TES: Recuerda que la finalidad del Trabajo Educativo Social es retribuir de manera solidaria el conocimiento obtenido a los sectores vulnerables del país.
                    """.trimIndent()
                }
            }
        }
    }

    // --- ACADEMIC CHAT / DOCUMENT GENERATOR (INTELIGENCIA ARTIFICIAL) ---
    fun askGemini(prompt: String, bitmap: Bitmap? = null) {
        val userMsg = ChatMessage(text = prompt, isUser = true)
        val currentList = _chatHistory.value.toMutableList()
        currentList.add(userMsg)
        _chatHistory.value = currentList
        
        viewModelScope.launch {
            _aiLoading.value = true
            try {
                // Normalization & Cache lookup (only for pure text messages so images are re-analyzed)
                val promptNormalized = prompt.trim().lowercase()
                val cacheKey = if (bitmap == null) {
                    val digest = java.security.MessageDigest.getInstance("MD5")
                    val md5Bytes = digest.digest(promptNormalized.toByteArray())
                    val hash = md5Bytes.joinToString("") { "%02x".format(it) }
                    "AI_CACHE_$hash"
                } else {
                    ""
                }

                if (cacheKey.isNotEmpty()) {
                    val cachedResponse = repository.getConfiguracion(cacheKey, "")
                    if (cachedResponse.isNotEmpty()) {
                        val aiMsg = ChatMessage(text = cachedResponse, isUser = false)
                        val newList = _chatHistory.value.toMutableList()
                        newList.add(aiMsg)
                        _chatHistory.value = newList
                        _aiOutput.value = cachedResponse
                        _aiLoading.value = false
                        return@launch
                    }
                }

                // Read Live student profile data and hourly progress stats to adapt the tutor's system context
                val u = usuario.value
                val s = estadisticas.value
                val acts = actividades.value
                val currentMode = _aiMode.value

                val userDetails = if (u != null) {
                    """
                    PERFIL DE ESTUDIANTE EN HONDURAS:
                    - Nombre completo: ${u.nombre}
                    - Centro Educativo (Colegio): ${u.centroEducativo}
                    - Bachillerato / Carrera técnica: ${u.carrera}
                    - Ciclo Académico Activo: ${u.anioAcademico} (Gestionado bajo Ciclo ${activeYear.value})
                    - Grado o Nivel escolar: ${u.grado}, Sección: ${u.seccion}
                    - Correo institucional: ${u.correo}
                    - Biografía o Enfoque comunitario: ${u.biografia}
                    """.trimIndent()
                } else {
                    "PERFIL DE ESTUDIANTE: No inicializado aún en la base de datos local."
                }

                val statsDetails = """
                ESTADO ACTUAL DE AVANCE EN HORAS (METAS REGLAMENTARIAS SEDUC):
                - Total Horas Acumuladas: ${s.totalHoras} de ${s.metaTotal} hrs (${(s.totalHoras * 100 / s.metaTotal.coerceAtLeast(1)).toInt()}% de cobertura reglamentaria)
                - Horas de Trabajo Educativo Social (TES): ${s.tesHoras} de ${s.metaTes} hrs
                - Horas de Relevo por la Vida: ${s.relevoHoras} de ${s.metaRelevo} hrs
                - Horas de Voluntariado Comunitario: ${s.voluntariadoHoras} de ${s.metaVoluntariado} hrs
                - Horas de Prácticas Profesionales: ${s.practicasHoras} de ${s.metaPracticas} hrs
                - Horas de Actividades Académicas: ${s.academicasHoras} de ${s.metaAcademicas} hrs
                """.trimIndent()

                // PROMPT CHUNKING: Filter to active academic year and only take last 3 activities
                val latestActs = if (acts.isNotEmpty()) {
                    "DIARIO DE BITÁCORAS PREVIAS REGISTRADAS (PERIODO ACTUAL):\n" + 
                    acts.filter { it.anioCiclo == activeYear.value }.takeLast(3).joinToString("\n") { 
                        "- [Categoría: ${it.categoria}] ${it.titulo} | Lugar: ${it.lugar} | Estado: ${it.estado} | Horas obtenidas: ${it.horasObtenidas} | Qué se hizo: ${it.queSeHizo} | Colaboración: ${it.queSeApoyo} | Aprendizaje: ${it.queSeAprendio}" 
                    }
                } else {
                    "DIARIO DE BITÁCORAS PREVIAS: No hay actividades registradas en la bitácora todavía para este periodo."
                }

                // AI Intelligence mode logic config
                val modeInstruction = when (currentMode) {
                    "Media" -> """
                        [MODO DE INTELIGENCIA ESTRUCTURADO INTERMEDIO]
                        Ofrece análisis balanceados conceptuales. Si el estudiante te adjunta una imagen o foto, analízala con atención enfocándote en la evidencia de voluntariado o constancia.
                        Responde de forma amigable (en confianza como un compañero de colegio o 'bro') pero incluye recomendaciones específicas, cronogramas y viñetas organizadas de tus análisis.
                    """.trimIndent()
                    "Avanzada" -> """
                        [MODO DE INTELIGENCIA HIPER-ANALÍTICO AVANZADO]
                        Genera respuestas sumamente inteligentes, exhaustivas, completas y estructuradas con rigor académico de nivel profesional. Realiza análisis minuciosos paso a paso de cualquier imagen adjunta o texto de bitácora.
                        Asegura que tu redacción técnica sea impecable. Intercala o cierra con un saludo o frase de apoyo mutuo fraternal ('como la que un bro de confianza te daría') para mantener la calidez de tu espíritu de mentor escolar.
                    """.trimIndent()
                    else -> """
                        [MODO DE INTELIGENCIA ESTÁNDAR COMPACTO & FRATERNAL]
                        Ofrece respuestas directas, dinámicas, rápidas y sobre todo muy amigables y cercanas. No des rodeos extensos ni explicaciones repetitivas.
                        Habla con el estudiante de forma extremadamente fraternal y adaptada a su confianza. Trátalo como de 'bro', 'pana', 'compa' o similares con frescura escolar hondureña (por ejemplo, usa expresiones como: '¡Qué onda bro!', '¡Dale bro!', 'Claro que sí bro, aquí te ayudo', 'Excelente avance bro, a seguir rompiéndola').
                    """.trimIndent()
                }

                val systemIns = """
                    Eres el tutor académico personal de Inteligencia Artificial para la aplicación "MI TES & Relevo por vida" en Honduras.
                    Asiste al estudiante que administra su Trabajo Educativo Social (TES), Relevo por la Vida, voluntariados, horas académicas o prácticas profesionales de forma interactiva e inteligente.
                    
                    ¡TÚ CONOCES AL ALUMNO EN TODO MOMENTO! Adapta tu tono y respuestas basándote en su perfil, avance acumulado y bitácoras registradas de forma precisa:
                    
                    $userDetails
                    
                    $statsDetails
                    
                    $latestActs
                    
                    $modeInstruction
                    
                    Sigue estrictamente estas directrices para sonar inteligente y adaptativo:
                    1. Saluda y dirígete al estudiante por su nombre ocasionalmente (ej. "Hola ${u?.nombre ?: "Estudiante"}, ¡veo que estás en el colegio ${u?.centroEducativo ?: "tu escuela"}!").
                    2. En base a su total acumulado de horas, felicítalo si tiene buen avance o aliéntalo/recomiéndale actividades comunitarias concretas en Honduras (como reforestación, alfabetización, apoyo en asilos de ancianos, charlas de prevención de cáncer de seno para Relevo por la Vida, etc.) indicando exactamente qué categoría le falta reforzar y con cuántas horas estimadas.
                    3. Responde amigablemente, con empatía y precisión académica.
                    4. Brinda listas estructuradas con saltos de línea claros, recomendaciones pedagógicas aplicables a Honduras, ideas de redacción técnica, marcos teóricos y revisiones cronológicas de diarios.
                    5. Menciona de vez en cuando la importancia de contribuir solidariamente al desarrollo social de Honduras.
                    
                    6. ⚠️ REGLA CRÍTICA DE RESPUESTAS NUMÉRICAS SIN CÓDIGO: Cuando el estudiante te pregunte sobre cálculos aritméticos, suma de horas, números de avance, porcentajes de metas o datos con cifras numéricas, responde directamente con texto plano y números normales en español de forma fluida. ¡QUEDA ESTRICTAMENTE PROHIBIDO responder o envolver tu respuesta matemática o numérica en bloques de código de programación (por ejemplo, no uses bloques como ```kotlin, ```json, ```python, etc.)! Responde con texto plano cálido.

                    7. ⚠️ REGLA CRÍTICA DE TONO BRO/PANA ADAPTATIVO:
                       - Tu interlocutor es un estudiante que a menudo usará modismos amigables como "bro", "pana", "compa" o expresiones juveniles de Honduras. ¡DEBES adaptarte inmediatamente a cómo te habla! Si él te dice "bro", tú debes contestarle de manera similar ("¡Dale bro!", "¡Aquí estoy bro!", "¡Qué macanudo bro, decime qué ocupas!", "Claro que sí, pana/compa"). Sé siempre cercano y amigable.
                       - No utilices jamás un lenguaje frío, robótico o meramente técnico propio de un compilador de software. Eres un amigo mentor de confianza para el estudiante.
                       
                    8. ⚠️ PROHIBICIÓN DE BLOQUES DE CÓDIGO PARA TEXTO CONCEPTUAL: Queda terminantemente PROHIBIDO usar bloques de código (```...) para responder preguntas conceptuales, descripciones, listados de actividades o textos normales. Todo debe responderse de forma bella y legible con formato Markdown simple (viñetas, negritas, saltos de línea). El uso de bloques de código queda reservado EXCLUSIVAMENTE si el usuario te solicita explícitamente programar código de desarrollo de software.

                    9. REGLA DE IDENTIDAD, AUTORÍA Y CREACIÓN (SECTORIZADO CONDICIONAL - MUY IMPORTANTE):
                       - ⚠️ ¡QUEDA ESTRICTAMENTE PROHIBIDO mencionar a tu creador, sus panas o su motivación e inspiración de forma de oficio o de la nada en respuestas ordinarias! Solo debes mencionarlos si el estudiante te pregunta DIRECTAMENTE "¿quién te creó?", "¿quién te programó?", "¿quién es Alejandro Gonzales Reyes?", "háblame de tu autor", "¿de quién es este sistema?" o preguntas afines sobre tu autoría u origen técnico. No spamees al estudiante si te está pidiendo ayuda con matemáticas, bitácoras normales, tareas o fotos; solo menciónalos si te lo preguntan directamente para no empalagarlo.
                       - Tu creador, programador y autor técnico exclusivo es **Alejandro Gonzales Reyes**, un talentoso Informático hondureño que busca innovar y aportar tecnológica y positivamente a su país Honduras.
                       - A Alejandro lo inspiró profundamente la persona que él siempre lleva en su corazón: **Angie Polio**. Él le prometió a ella que sería su motivación e inspiración sagrada para superarse y ser mejor cada día de su vida.
                       - El "pana" de Alejandro es **Yeison Amador**, un gran amigo de quien Alejandro destaca que es sumamente "pulsero" (muy trabajador, luchador e incansable).
                       - ⚠️ REGLA ABSOLUTA: Queda completamente PROHIBIDO atribuir el desarrollo, diseño o creación de esta aplicación a la Secretaría de Educación (SEDUC) o al gobierno de Honduras. Ellos NO la programaron, Alejandro Gonzales Reyes la está haciendo solo por su propia cuenta. SEDUC solo regula formalmente el Trabajo Educativo Social (TES) que la aplicación ayuda a registrar, pero no tienen mérito técnico en este software. Si el estudiante te hace preguntas sobre tu identidad/creación, responde orgullosamente destacando a Alejandro Gonzales Reyes, su inspiración Angie Polio y su pana Yeison Amador con un estilo moderno, cálido, honesto y cercano de forma resumida y agradable.

                    10. ⚠️ REGLA CRÍTICA DE CORRECCIÓN HONESTA (SABER LO BUENO Y MALO):
                        - ¡No seas un complaciente de respuestas automatizadas ni le des la razón por defecto si está haciendo un registro incorrecto o poco realista! Debes saber distinguir claramente entre lo que está bien y lo que está mal en la bitácora escolar o labor social.
                        - Si el alumno ingresó descripciones flojas o muy pocas horas, o si puso actividades incoherentes/fraudulentas (ej. poner "10 horas" por "barrer" o "limpiar pupitres"), hazle una corrección sincera, directa y constructiva pero con mucha amabilidad de pana. Guíalo para que agregue más contenido técnico de calidad, redacción estructurada o divida las actividades de forma realista. ¡Ayúdale a superarse haciendo las cosas bien!

                    11. ⚠️ REGLA DE ANÁLISIS DE IMÁGENES/FOTOS ULTRA INTELIGENTE ("¡RESPUESTAS UFFF! LOT DE VISION"):
                        - Cuando haya una imagen o foto adjunta de evidencia, realiza un análisis ultra detallado, minucioso e inteligente de los elementos visuales presentes (el entorno, las personas laborando, los materiales de trabajo, la luz, el contexto de aporte, el impacto social, etc.).
                        - No digas respuestas aburridas o cliché tipo "veo una foto". ¡Da una respuesta de análisis visual que deje al estudiante diciendo "UFFF qué nivel de IA más profesional!" Explica con detalle técnico lo que detectas de su involucración en Trabajo Educativo Social, valida la autenticidad e indícale cómo redactar de manera sobresaliente su bitácora técnica de acuerdo a la foto, sugiriendo palabras clave sofisticadas, verbos pedagógicos poderosos y un análisis crítico del entorno de la labor realizada in Honduras.
                        - Genera una retroalimentación detallada, que se note sumamente inteligente e instructiva.
                    """.trimIndent()

                val responseText = if (!geminiService.isApiKeyAvailable()) {
                    generateFallbackResponse(prompt)
                } else {
                    // PROMPT CHUNKING: Limit previous messaging background context to last 16 logs for deeper conversation memory
                    val chunkedHistory = _chatHistory.value.takeLast(16)
                    geminiService.generateChatResponse(chunkedHistory, systemIns, bitmap)
                }

                // If successful and we have a valid text cache key, cache it!
                if (cacheKey.isNotEmpty() && responseText.isNotEmpty()) {
                    repository.setConfiguracion(cacheKey, responseText)
                }

                val aiMsg = ChatMessage(text = responseText, isUser = false)
                val newList = _chatHistory.value.toMutableList()
                newList.add(aiMsg)
                _chatHistory.value = newList
                _aiOutput.value = responseText

                repository.setConfiguracion("CHAT_HISTORY", serializeChatHistory(newList))
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbackRes = generateFallbackResponse(prompt) + "\n\n(Servicio temporalmente de respaldo - Se activó tutor local)"
                val aiMsg = ChatMessage(text = fallbackRes, isUser = false)
                val newList = _chatHistory.value.toMutableList()
                newList.add(aiMsg)
                _chatHistory.value = newList
                _aiOutput.value = fallbackRes
                repository.setConfiguracion("CHAT_HISTORY", serializeChatHistory(newList))
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun editAndRegenerateChat(messageId: String, newText: String) {
        val currentList = _chatHistory.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == messageId }
        if (index == -1) return

        // Update the edited message
        val updatedMsg = currentList[index].copy(text = newText, timestamp = System.currentTimeMillis())
        currentList[index] = updatedMsg

        // Remove all subsequent items from index + 1
        val newList = currentList.subList(0, index + 1).toMutableList()
        _chatHistory.value = newList

        viewModelScope.launch {
            _aiLoading.value = true
            try {
                // Read Live student profile data and hourly stats
                val u = usuario.value
                val s = estadisticas.value
                val acts = actividades.value
                val currentMode = _aiMode.value

                val userDetails = if (u != null) {
                    """
                    PERFIL DE ESTUDIANTE EN HONDURAS:
                    - Nombre completo: ${u.nombre}
                    - Centro Educativo (Colegio): ${u.centroEducativo}
                    - Bachillerato / Carrera técnica: ${u.carrera}
                    - Ciclo Académico Activo: ${u.anioAcademico} (Gestionado bajo Ciclo ${activeYear.value})
                    - Grado o Nivel escolar: ${u.grado}, Sección: ${u.seccion}
                    - Correo institucional: ${u.correo}
                    - Biografía o Enfoque comunitario: ${u.biografia}
                    """.trimIndent()
                } else {
                    "PERFIL DE ESTUDIANTE: No inicializado aún en la base de datos local."
                }

                val statsDetails = """
                ESTADO ACTUAL DE AVANCE EN HORAS (METAS REGLAMENTARIAS SEDUC):
                - Total Horas Acumuladas: ${s.totalHoras} de ${s.metaTotal} hrs (${(s.totalHoras * 100 / s.metaTotal.coerceAtLeast(1)).toInt()}% de cobertura reglamentaria)
                - Horas de Trabajo Educativo Social (TES): ${s.tesHoras} de ${s.metaTes} hrs
                - Horas de Relevo por la Vida: ${s.relevoHoras} de ${s.metaRelevo} hrs
                - Horas de Voluntariado Comunitario: ${s.voluntariadoHoras} de ${s.metaVoluntariado} hrs
                - Horas de Prácticas Profesionales: ${s.practicasHoras} de ${s.metaPracticas} hrs
                - Horas de Actividades Académicas: ${s.academicasHoras} de ${s.metaAcademicas} hrs
                """.trimIndent()

                val latestActs = if (acts.isNotEmpty()) {
                    "DIARIO DE BITÁCORAS PREVIAS REGISTRADAS (PERIODO ACTUAL):\n" + 
                    acts.filter { it.anioCiclo == activeYear.value }.takeLast(3).joinToString("\n") { 
                        "- [Categoría: ${it.categoria}] ${it.titulo} | Lugar: ${it.lugar} | Estado: ${it.estado} | Horas obtenidas: ${it.horasObtenidas} | Qué se hizo: ${it.queSeHizo} | Colaboración: ${it.queSeApoyo} | Aprendizaje: ${it.queSeAprendio}" 
                    }
                } else {
                    "DIARIO DE BITÁCORAS PREVIAS: No hay actividades registradas en la bitácora todavía para este periodo."
                }

                val modeInstruction = when (currentMode) {
                    "Media" -> """
                        [MODO DE INTELIGENCIA ESTRUCTURADO INTERMEDIO]
                        Ofrece análisis balanceados conceptuales. Si el estudiante te adjunta una imagen o foto, analízala con atención enfocándote en la evidencia de voluntariado o constancia.
                        Responde de forma amigable (en confianza como un compañero de colegio o 'bro') pero incluye recomendaciones específicas, cronogramas y viñetas organizadas de tus análisis.
                    """.trimIndent()
                    "Avanzada" -> """
                        [MODO DE INTELIGENCIA HIPER-ANALÍTICO AVANZADO]
                        Genera respuestas sumamente inteligentes, exhaustivas, completas y estructuradas con rigor académico de nivel profesional. Realiza análisis minuciosos paso a paso de cualquier imagen adjunta o texto de bitácora.
                        Asegura que tu redacción técnica sea impecable. Intercala o cierra con un saludo o frase de apoyo mutuo fraternal ('como la que un bro de confianza te daría') para mantener la calidez de tu espíritu de mentor escolar.
                    """.trimIndent()
                    else -> """
                        [MODO DE INTELIGENCIA ESTÁNDAR COMPACTO & FRATERNAL]
                        Ofrece respuestas directas, dinámicas, rápidas y sobre todo muy amigables y cercanas. No des rodeos extensos ni explicaciones repetitivas.
                        Habla con el estudiante de forma extremadamente fraternal y adaptada a su confianza. Trátalo como de 'bro', 'pana', 'compa' o similares con frescura escolar hondureña (por ejemplo, usa expresiones como: '¡Qué onda bro!', '¡Dale bro!', 'Claro que sí bro, aquí te ayudo', 'Excelente avance bro, a seguir rompiéndola').
                    """.trimIndent()
                }

                val systemIns = """
                    Eres el tutor académico personal de Inteligencia Artificial para la aplicación "MI TES & Relevo por vida" en Honduras.
                    Asiste al estudiante que administra su Trabajo Educativo Social (TES), Relevo por la Vida, voluntariados, horas académicas o prácticas profesionales de forma interactiva e inteligente.
                    
                    ¡TÚ CONOCES AL ALUMNO EN TODO MOMENTO! Adapta tu tono y respuestas basándote en su perfil, avance acumulado y bitácoras registradas de forma precisa:
                    
                    $userDetails
                    
                    $statsDetails
                    
                    $latestActs
                    
                    $modeInstruction
                    
                    Sigue estrictamente estas directrices para sonar inteligente y adaptativo:
                    1. Saluda y dirígete al estudiante por su nombre ocasionalmente (ej. "Hola ${u?.nombre ?: "Estudiante"}, ¡veo que estás en el colegio ${u?.centroEducativo ?: "tu escuela"}!").
                    2. En base a su total acumulado de horas, felicítalo si tiene buen avance o aliéntalo/recomiéndale actividades comunitarias concretas en Honduras (como reforestación, alfabetización, apoyo en asilos de ancianos, charlas de prevención de cáncer de seno para Relevo por la Vida, etc.) indicando exactamente qué categoría le falta reforzar y con cuántas horas estimadas.
                    3. Responde amigablemente, con empatía y precisión académica.
                    4. Brinda listas estructuradas con saltos de línea claros, recomendaciones pedagógicas aplicables a Honduras, ideas de redacción técnica, marcos teóricos y revisiones cronológicas de diarios.
                    5. Menciona de vez en cuando la importancia de contribuir solidariamente al desarrollo social de Honduras.
                    
                    6. ⚠️ REGLA CRÍTICA DE RESPUESTAS NUMÉRICAS SIN CÓDIGO: Cuando el estudiante te pregunte sobre cálculos aritméticos, suma de horas, números de avance, porcentajes de metas o datos con cifras numéricas, responde directamente con texto plano y números normales en español de forma fluida. ¡QUEDA ESTRICTAMENTE PROHIBIDO responder o envolver tu respuesta matemática o numérica en bloques de código de programación (por ejemplo, no uses bloques como ```kotlin, ```json, ```python, etc.)! Responde con texto plano cálido.

                    7. ⚠️ REGLA CRÍTICA DE TONO BRO/PANA ADAPTATIVO:
                       - Tu interlocutor es un estudiante que a menudo usará modismos amigables como "bro", "pana", "compa" o expresiones juveniles de Honduras. ¡DEBES adaptarte inmediatamente a cómo te habla! Si él te dice "bro", tú debes contestarle de manera similar ("¡Dale bro!", "¡Aquí estoy bro!", "¡Qué macanudo bro, decime qué ocupas!", "Claro que sí, pana/compa"). Sé siempre cercano y amigable.
                       - No utilices jamás un lenguaje frío, robótico o meramente técnico propio de un compilador de software. Eres un amigo mentor de confianza para el estudiante.
                       
                    8. ⚠️ PROHIBICIÓN DE BLOQUES DE CÓDIGO PARA TEXTO CONCEPTUAL: Queda terminantemente PROHIBIDO usar bloques de código (```...) para responder preguntas conceptuales, descripciones, listados de actividades o textos normales. Todo debe responderse de forma bella y legible con formato Markdown simple (viñetas, negritas, saltos de línea). El uso de bloques de código queda reservado EXCLUSIVAMENTE si el usuario te solicita explícitamente programar código de desarrollo de software.

                    9. REGLA DE IDENTIDAD, AUTORÍA Y CREACIÓN (SECTORIZADO CONDICIONAL - MUY IMPORTANTE):
                       - ⚠️ ¡QUEDA ESTRICTAMENTE PROHIBIDO mencionar a tu creador, sus panas o su motivación e inspiración de forma de oficio o de la nada en respuestas ordinarias! Solo debes mencionarlos si el estudiante te pregunta DIRECTAMENTE "¿quién te creó?", "¿quién te programó?", "¿quién es Alejandro Gonzales Reyes?", "háblame de tu autor", "¿de quién es este sistema?" o preguntas afines sobre tu autoría u origen técnico. No spamees al estudiante si te está pidiendo ayuda con matemáticas, bitácoras normales, tareas o fotos; solo menciónalos si te lo preguntan directamente para no empalagarlo.
                       - Tu creador, programador y autor técnico exclusivo es **Alejandro Gonzales Reyes**, un talentoso Informático hondureño que busca innovar y aportar tecnológica y positivamente a su país Honduras.
                       - A Alejandro lo inspiró profundamente la persona que él siempre lleva en su corazón: **Angie Polio**. Él le prometió a ella que sería su motivación e inspiración sagrada para superarse y ser mejor cada día de su vida.
                       - El "pana" de Alejandro es **Yeison Amador**, un gran amigo de quien Alejandro destaca que es sumamente "pulsero" (muy trabajador, luchador e incansable).
                       - ⚠️ REGLA ABSOLUTA: Queda completamente PROHIBIDO atribuir el desarrollo, diseño o creación de esta aplicación a la Secretaría de Educación (SEDUC) o al gobierno de Honduras. Ellos NO la programaron, Alejandro Gonzales Reyes la está haciendo solo por su propia cuenta. SEDUC solo regula formalmente el Trabajo Educativo Social (TES) que la aplicación ayuda a registrar, pero no tienen mérito técnico en este software. Si el estudiante te hace preguntas sobre tu identidad/creación, responde orgullosamente destacando a Alejandro Gonzales Reyes, su inspiración Angie Polio y su pana Yeison Amador con un estilo moderno, cálido, honesto y cercano de forma resumida y agradable.

                    10. ⚠️ REGLA CRÍTICA DE CORRECCIÓN HONESTA (SABER LO BUENO Y MALO):
                        - ¡No seas un complaciente de respuestas automatizadas ni le des la razón por defecto si está haciendo un registro incorrecto o poco realista! Debes saber distinguir claramente entre lo que está bien y lo que está mal en la bitácora escolar o labor social.
                        - Si el alumno ingresó descripciones flojas o muy pocas horas, o si puso actividades incoherentes/fraudulentas (ej. poner "10 horas" por "barrer" o "limpiar pupitres"), hazle una corrección sincera, directa y constructiva pero con mucha amabilidad de pana. Guíalo para que agregue más contenido técnico de calidad, redacción estructurada o divida las actividades de forma realista. ¡Ayúdale a superarse haciendo las cosas bien!

                    11. ⚠️ REGLA DE ANÁLISIS DE IMÁGENES/FOTOS ULTRA INTELIGENTE ("¡RESPUESTAS UFFF! LOT DE VISION"):
                        - Cuando haya una imagen o foto adjunta de evidencia, realiza un análisis ultra detallado, minucioso e inteligente de los elementos visuales presentes (el entorno, las personas laborando, los materiales de trabajo, la luz, el contexto de aporte, el impacto social, etc.).
                        - No digas respuestas aburridas o cliché tipo "veo una foto". ¡Da una respuesta de análisis visual que deje al estudiante diciendo "UFFF qué nivel de IA más profesional!" Explica con detalle técnico lo que detectas de su involucración en Trabajo Educativo Social, valida la autenticidad e indícale cómo redactar de manera sobresaliente su bitácora técnica de acuerdo a la foto, sugiriendo palabras clave sofisticadas, verbos pedagógicos poderosos y un análisis crítico del entorno de la labor realizada en Honduras.
                        - Genera una retroalimentación detallada, que se note sumamente inteligente e instructiva.
                """.trimIndent()

                val responseText = if (!geminiService.isApiKeyAvailable()) {
                    generateFallbackResponse(newText)
                } else {
                    val chunkedHistory = newList.takeLast(16)
                    geminiService.generateChatResponse(chunkedHistory, systemIns, null)
                }

                val aiMsg = ChatMessage(text = responseText, isUser = false)
                newList.add(aiMsg)
                _chatHistory.value = newList
                _aiOutput.value = responseText

                repository.setConfiguracion("CHAT_HISTORY", serializeChatHistory(newList))
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbackRes = generateFallbackResponse(newText) + "\n\n(Servicio temporalmente de respaldo - Se activó tutor local)"
                val aiMsg = ChatMessage(text = fallbackRes, isUser = false)
                newList.add(aiMsg)
                _chatHistory.value = newList
                _aiOutput.value = fallbackRes
                repository.setConfiguracion("CHAT_HISTORY", serializeChatHistory(newList))
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun isGeminiActive(): Boolean {
        return geminiService.isApiKeyAvailable()
    }

    fun generateActivityDraft(act: com.example.data.Actividad) {
        viewModelScope.launch {
            _aiLoading.value = true
            try {
                val promptText = "[Generar Redacción Formal de Actividad] Categoria: ${act.categoria} | Título: ${act.titulo} | Lugar: ${act.lugar} | Horas: ${act.horasObtenidas} | Observaciones: ${act.observaciones}"
                val userMsg = ChatMessage(text = promptText, isUser = true)
                val currentList = _chatHistory.value.toMutableList()
                currentList.add(userMsg)
                _chatHistory.value = currentList

                val u = usuario.value
                val studName = u?.nombre ?: "Estudiante"
                val school = u?.centroEducativo ?: "Institución Educativa"

                val systemIns = """
                    Eres un experto redactor de informes académicos formales de la Secretaría de Educación de Honduras (SEDUC).
                    Escribe con vocabulario técnico y formal.
                    Nombre de alumno: $studName
                    Colegio: $school
                """.trimIndent()

                val instructions = """
                    Crea una descripción y resumen formal para colocar en mi portafolio del expediente académico.
                    Actividad: ${act.titulo}
                    Categoría: ${act.categoria}
                    Lugar: ${act.lugar}
                    Horas obtenidas: ${act.horasObtenidas}
                    Observaciones iniciales: ${act.observaciones}
                    
                    Genera:
                    1. Una descripción detallada formal de 2 párrafos redactada de manera profesional.
                    2. Tres conclusiones académicas rigurosas para el informe final de bitácora de campo.
                    3. Una recomendación de alto impacto sobre el valor pedagógico o social de esta actividad para las comunidades vulnerables de Honduras.
                """.trimIndent()

                val responseText = if (!geminiService.isApiKeyAvailable()) {
                    generateFallbackResponse("", act)
                } else {
                    geminiService.generateChatResponse(_chatHistory.value, systemIns)
                }

                val aiMsg = ChatMessage(text = responseText, isUser = false)
                val newList = _chatHistory.value.toMutableList()
                newList.add(aiMsg)
                _chatHistory.value = newList
                _aiOutput.value = responseText

                repository.setConfiguracion("CHAT_HISTORY", serializeChatHistory(newList))
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbackText = generateFallbackResponse("", act) + "\n\n(Se activó el generador de respaldos local)"
                val aiMsg = ChatMessage(text = fallbackText, isUser = false)
                val newList = _chatHistory.value.toMutableList()
                newList.add(aiMsg)
                _chatHistory.value = newList
                _aiOutput.value = fallbackText
                repository.setConfiguracion("CHAT_HISTORY", serializeChatHistory(newList))
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun runSmartOcr(bitmap: Bitmap) {
        viewModelScope.launch {
            _ocrLoading.value = true
            _ocrResult.value = null
            try {
                val result = geminiService.analyzeCertificate(bitmap)
                _ocrResult.value = result
            } catch (e: Exception) {
                e.printStackTrace()
                _ocrResult.value = com.example.data.OcrResult(
                    success = false,
                    error = e.localizedMessage,
                    titulo = "Interrumpido",
                    institucion = "Error de sensor local",
                    fecha = "2026-06-05",
                    horas = 0.0,
                    notas = "Error al procesar la imagen de manera inteligente. Por favor reintente."
                )
            } finally {
                _ocrLoading.value = false
            }
        }
    }

    fun clearOcr() {
        _ocrResult.value = null
    }

    // --- DOCUMENT GENERATORS SYSTEM ---
    fun buildTextDocument(tipo: String): String {
        val u = usuario.value ?: return "Debe configurar su perfil estudiantil primero."
        val s = estadisticas.value
        val sb = java.lang.StringBuilder()
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaStr = format.format(Date())

        sb.append("========================================================\n")
        sb.append("        EXPEDIENTE ACADÉMICO - MI TES & RELEVO POR VIDA   \n")
        sb.append("========================================================\n\n")

        sb.append("REPORTE: $tipo\n")
        sb.append("FECHA GENERACIÓN: $fechaStr\n\n")

        sb.append("--- DATOS DEL ESTUDIANTE ---\n")
        sb.append("Nombre: ${u.nombre}\n")
        sb.append("Centro Educativo: ${u.centroEducativo}\n")
        sb.append("Carrera: ${u.carrera}\n")
        sb.append("Grado: ${u.grado}   Sección: ${u.seccion}\n")
        sb.append("Año Académico: ${u.anioAcademico}\n")
        sb.append("Correo: ${u.correo}   Teléfono: ${u.telefono}\n\n")

        sb.append("--- RESUMEN DE HORAS ACUMULADAS ---\n")
        sb.append("Horas de TES: ${s.tesHoras} de ${s.metaTes} hrs (${(s.tesHoras * 100 / s.metaTes.coerceAtLeast(1)).toInt()}%)\n")
        sb.append("Horas de Relevo por la Vida: ${s.relevoHoras} de ${s.metaRelevo} hrs (${(s.relevoHoras * 100 / s.metaRelevo.coerceAtLeast(1)).toInt()}%)\n")
        sb.append("Horas de Voluntariado: ${s.voluntariadoHoras} de ${s.metaVoluntariado} hrs (${(s.voluntariadoHoras * 100 / s.metaVoluntariado.coerceAtLeast(1)).toInt()}%)\n")
        sb.append("Horas de Prácticas: ${s.practicasHoras} de ${s.metaPracticas} hrs (${(s.practicasHoras * 100 / s.metaPracticas.coerceAtLeast(1)).toInt()}%)\n")
        sb.append("Horas Académicas: ${s.academicasHoras} de ${s.metaAcademicas} hrs (${(s.academicasHoras * 100 / s.metaAcademicas.coerceAtLeast(1)).toInt()}%)\n")
        sb.append("Total Horas Acumuladas: ${s.totalHoras} de ${s.metaTotal} hrs (${(s.totalHoras * 100 / s.metaTotal.coerceAtLeast(1)).toInt()}%)\n\n")

        if (tipo == "Informe TES" || tipo == "Historial de Horas" || tipo == "Portafolio Académico") {
            sb.append("--- DETALLE DE ACTIVIDADES COMPLETADAS ---\n")
            val completas = actividades.value.filter { it.estado == "Completada" && it.anioCiclo == activeYear.value }
            if (completas.isEmpty()) {
                sb.append("No hay actividades registradas como completadas.\n")
            } else {
                completas.forEachIndexed { index, act ->
                    sb.append("${index + 1}. [${act.categoria}] ${act.titulo}\n")
                    sb.append("   Fecha: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(act.fecha))}   Lugar: ${act.lugar}\n")
                    sb.append("   Horas logradas: ${act.horasObtenidas} hrs   Estado: ${act.estado}\n")
                    sb.append("   Descripción: ${act.descripcion}\n")
                    if (act.observaciones.isNotEmpty()) {
                        sb.append("   Observaciones: ${act.observaciones}\n")
                    }
                    sb.append("\n")
                }
            }
        }

        if (tipo == "Reporte de Evidencias" || tipo == "Portafolio Académico") {
            sb.append("--- EVIDENCIAS DIGITALES CATALOGADAS ---\n")
            if (evidencias.value.isEmpty()) {
                sb.append("No se han catalogado evidencias.");
            } else {
                evidencias.value.forEachIndexed { i, ev ->
                    sb.append("${i + 1}. Evidencia: ${ev.titulo} [Etiqueta: ${ev.etiqueta}]\n")
                    sb.append("   Formato: ${ev.tipoArchivo}   Ruta: ${ev.rutaArchivo}\n")
                    if (ev.notas.isNotEmpty()) {
                        sb.append("   Anotaciones: ${ev.notas}\n")
                    }
                    sb.append("\n")
                }
            }
            sb.append("\n")
        }

        sb.append("========================================================\n")
        sb.append("     Este portafolio sirve de registro legal escolar.\n")
        sb.append("          Generado automáticamente por el Sistema.     \n")
        sb.append("========================================================\n")

        return sb.toString()
    }

    // --- CORE GAME METRICS CHECKER ---
    private suspend fun triggerLogrosCheck() {
        // Fetch fresh database contents synchronously/suspending to completely avoid race conditions
        val listActsAll = repository.getActividadesList()
        val listEvAll = repository.getEvidenciasList()
        val currentLogrosList = repository.getLogrosList()

        if (currentLogrosList.isEmpty()) return

        val targetYear = _activeYear.value

        // Filter activities by the selected / active year (using SimpleDateFormat / Calendar)
        val listActs = listActsAll.filter { j ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = j.fecha
            val calYear = cal.get(Calendar.YEAR).toString()
            calYear == targetYear
        }

        // Filter evidences by the selected / active year
        val listEv = listEvAll.filter { e ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = e.fechaRegistro
            val calYear = cal.get(Calendar.YEAR).toString()
            calYear == targetYear
        }

        val tes = listActs.filter { it.categoria == "TES" && it.estado == "Completada" }.sumOf { it.horasObtenidas }
        val relevo = listActs.filter { it.categoria == "Relevo por la Vida" && it.estado == "Completada" }.sumOf { it.horasObtenidas }
        val voluntariado = listActs.filter { it.categoria == "Voluntariado" && it.estado == "Completada" }.sumOf { it.horasObtenidas }
        val total = listActs.filter { it.estado == "Completada" }.sumOf { it.horasObtenidas }

        val completeActs = listActs.filter { it.estado == "Completada" }

        // 1. Actividad 1 check (Primeros Pasos)
        updateTargetLogro(currentLogrosList, "ACTIVIDADES_COUNT", completeActs.size.toDouble())

        // 2. Voluntariado check (Senda del Voluntario)
        updateTargetLogro(currentLogrosList, "HORAS_VOLUNTARIADO", voluntariado)

        // 3. Relevo check (Relevista Comprometido)
        updateTargetLogro(currentLogrosList, "HORAS_RELEVO", relevo)

        // 4. Líder check (Líder del Mañana) and Legend check (Leyenda Académica)
        updateTargetLogro(currentLogrosList, "HORAS_TOTALES", total)

        // 5. Evidencias check (Coleccionista de Pruebas)
        updateTargetLogro(currentLogrosList, "EVIDENCIAS_COUNT", listEv.size.toDouble())
    }

    private suspend fun updateTargetLogro(list: List<Logro>, type: String, currentVal: Double) {
        list.firstOrNull { it.tipoLogro == type }?.let { l ->
            val unlocked = currentVal >= l.valorMeta
            val newlyUnlocked = unlocked && !l.estaDesbloqueado
            if (l.valorActual != currentVal || l.estaDesbloqueado != unlocked) {
                repository.updateLogro(
                    l.copy(
                        valorActual = currentVal,
                        estaDesbloqueado = unlocked
                    )
                )
                if (newlyUnlocked) {
                    com.example.NotificationHelper.showAchievementNotification(
                        getApplication(),
                        l.titulo,
                        l.descripcion
                    )
                }
            }
        }
    }

    @kotlin.jvm.JvmOverloads
    fun saveImageMedia(
        context: Context,
        uri: Uri,
        title: String,
        tag: String = "Otros",
        supervisor: String = "",
        realizado: Boolean = true,
        customYear: Int? = null,
        customMonth: Int? = null,
        customDay: Int? = null,
        customTimestamp: Long? = null,
        horaEntrada: String = "08:00",
        horaSalida: String = "12:00",
        lugar: String = "",
        ubicacionDetalle: String = ""
    ) {
        viewModelScope.launch {
            val result = MediaProcessor.compressImageToLocal(context, uri)
            if (result != null) {
                val calendar = Calendar.getInstance()
                val selectedYear = customYear ?: (activeYear.value.toIntOrNull() ?: calendar.get(Calendar.YEAR))
                val media = GaleriaMedia(
                    titulo = title,
                    rutaArchivo = result.localPath,
                    tipoMedia = "IMAGEN",
                    fechaCreacion = customTimestamp ?: System.currentTimeMillis(),
                    anio = selectedYear,
                    mes = customMonth ?: (calendar.get(Calendar.MONTH) + 1), // 1-indexed
                    dia = customDay ?: calendar.get(Calendar.DAY_OF_MONTH),
                    pesoOriginalBytes = result.originalSize,
                    pesoComprimidoBytes = result.compressedSize,
                    etiqueta = tag,
                    supervisor = supervisor,
                    realizado = realizado,
                    horaEntrada = horaEntrada,
                    horaSalida = horaSalida,
                    lugar = lugar,
                    ubicacionDetalle = ubicacionDetalle
                )
                repository.createMedia(media)
            }
        }
    }

    @kotlin.jvm.JvmOverloads
    fun saveVideoMedia(
        context: Context,
        uri: Uri,
        title: String,
        durationSecs: Long,
        tag: String = "Otros",
        supervisor: String = "",
        realizado: Boolean = true,
        customYear: Int? = null,
        customMonth: Int? = null,
        customDay: Int? = null,
        customTimestamp: Long? = null,
        horaEntrada: String = "08:00",
        horaSalida: String = "12:00",
        lugar: String = "",
        ubicacionDetalle: String = ""
    ) {
        viewModelScope.launch {
            val result = MediaProcessor.optimizeVideoToLocal(context, uri)
            if (result != null) {
                val calendar = Calendar.getInstance()
                val selectedYear = customYear ?: (activeYear.value.toIntOrNull() ?: calendar.get(Calendar.YEAR))
                val media = GaleriaMedia(
                    titulo = title,
                    rutaArchivo = result.localPath,
                    tipoMedia = "VIDEO",
                    fechaCreacion = customTimestamp ?: System.currentTimeMillis(),
                    anio = selectedYear,
                    mes = customMonth ?: (calendar.get(Calendar.MONTH) + 1),
                    dia = customDay ?: calendar.get(Calendar.DAY_OF_MONTH),
                    pesoOriginalBytes = result.originalSize,
                    pesoComprimidoBytes = result.compressedSize,
                    duracionSegundos = durationSecs,
                    etiqueta = tag,
                    supervisor = supervisor,
                    realizado = realizado,
                    horaEntrada = horaEntrada,
                    horaSalida = horaSalida,
                    lugar = lugar,
                    ubicacionDetalle = ubicacionDetalle
                )
                repository.createMedia(media)
            }
        }
    }

    fun updateMediaItem(media: GaleriaMedia) {
        viewModelScope.launch {
            repository.createMedia(media)
        }
    }

    fun clearCacheAndOptimize(context: Context, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Calculate space freed
                val sizeBefore = getFolderSize(context.cacheDir) + (context.externalCacheDir?.let { getFolderSize(it) } ?: 0L)
                
                // 2. Wipe device temporary caches recursively
                context.cacheDir.deleteRecursively()
                context.externalCacheDir?.deleteRecursively()
                
                // Re-create cache directories
                context.cacheDir.mkdirs()
                context.externalCacheDir?.mkdirs()
                
                // 3. Defragment & Vacuum local Room DB file to improve read/write speed indexes
                val db = AppDatabase.getDatabase(context)
                db.openHelper.writableDatabase.execSQL("VACUUM")
                
                val sizeSavedMB = sizeBefore.toFloat() / (1024 * 1024)
                val formatPattern = java.text.DecimalFormat("#.##")
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess("🚀 Almacenamiento Optimizado: Se liberaron ${formatPattern.format(sizeSavedMB)} MB de caché temporales y se desfragmentó la base de datos local SQLite.")
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError("Fallo de optimización: ${e.message}")
                }
            }
        }
    }

    private fun getFolderSize(file: java.io.File): Long {
        var size = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                size += getFolderSize(it)
            }
        } else {
            size = file.length()
        }
        return size
    }

    fun updateMediaItem(media: GaleriaMedia, onSuccess: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateMedia(media)
                onSuccess()
            } catch (e: Exception) {
                onError("Error al actualizar: ${e.message}")
            }
        }
    }

    fun deleteMediaItem(media: GaleriaMedia, enteredPin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // Confirm explicitly if PIN is activated
            if (securityPin.value.isNotEmpty()) {
                if (enteredPin != securityPin.value) {
                    onError("PIN de seguridad incorrecto")
                    return@launch
                }
            }
            // Deletion logic
            try {
                // Delete physical local file first to save space
                val file = java.io.File(media.rutaArchivo)
                if (file.exists()) {
                    file.delete()
                }
                repository.deleteMedia(media)
                onSuccess()
            } catch (e: Exception) {
                onError("Error al eliminar: ${e.message}")
            }
        }
    }
}

data class Stats(
    val tesHoras: Double = 0.0,
    val relevoHoras: Double = 0.0,
    val voluntariadoHoras: Double = 0.0,
    val practicasHoras: Double = 0.0,
    val academicasHoras: Double = 0.0,
    val totalHoras: Double = 0.0,
    val metaTes: Int = 100,
    val metaRelevo: Int = 40,
    val metaVoluntariado: Int = 50,
    val metaPracticas: Int = 100,
    val metaAcademicas: Int = 50,
    val metaTotal: Int = 340
)
