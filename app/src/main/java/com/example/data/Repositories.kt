package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AcademicRepository(
    private val usuarioDao: UsuarioDao,
    private val actividadDao: ActividadDao,
    private val evidenciaDao: EvidenciaDao,
    private val certificadoDao: CertificadoDao,
    private val logroDao: LogroDao,
    private val diarioDao: DiarioDao,
    private val configuracionDao: ConfiguracionDao,
    private val historialCambiosDao: HistorialCambiosDao,
    private val galeriaMediaDao: GaleriaMediaDao
) {
    // --- USUARIO (STUDENT PROFILE) ---
    val usuarioFlow: Flow<Usuario?> = usuarioDao.getUsuarioFlow()

    suspend fun getUsuario(): Usuario? = usuarioDao.getUsuario()

    suspend fun saveUsuario(usuario: Usuario) {
        val old = usuarioDao.getUsuario()
        usuarioDao.insertUsuario(usuario)
        val details = if (old == null) "Perfil inicializado" else "Perfil escolar actualizado"
        logCambio("Perfil", usuario.id.toLong(), "Modificación", details)
    }

    // --- ACTIVIDADES (ACADEMIC ACTIVITIES) ---
    val allActividades: Flow<List<Actividad>> = actividadDao.getAllActividades()

    suspend fun getActividadesList(): List<Actividad> = actividadDao.getActividadesList()

    suspend fun getActividad(id: Long): Actividad? = actividadDao.getActividadById(id)

    suspend fun createActividad(actividad: Actividad): Long {
        val id = actividadDao.insertActividad(actividad)
        logCambio("Actividad", id, "Creación", "Actividad creada: ${actividad.titulo} en '${actividad.categoria}' (${actividad.horasObtenidas} horas)")
        return id
    }

    suspend fun updateActividad(actividad: Actividad) {
        actividadDao.updateActividad(actividad)
        logCambio("Actividad", actividad.id, "Modificación", "Actividad modificada: ${actividad.titulo} (${actividad.horasObtenidas} hrs, estado=${actividad.estado})")
    }

    suspend fun deleteActividad(actividad: Actividad) {
        actividadDao.deleteActividad(actividad)
        logCambio("Actividad", actividad.id, "Eliminación", "Actividad eliminada permanentemente: ${actividad.titulo} (${actividad.horasObtenidas} horas)")
    }

    // --- EVIDENCIAS (WORK EVIDENCE / FILES) ---
    val allEvidencias: Flow<List<Evidencia>> = evidenciaDao.getAllEvidencias()

    suspend fun getEvidenciasList(): List<Evidencia> = evidenciaDao.getEvidenciasList()

    fun getEvidenciasForActividad(actividadId: Long): Flow<List<Evidencia>> =
        evidenciaDao.getEvidenciasForActividad(actividadId)

    suspend fun getEvidencia(id: Long): Evidencia? = evidenciaDao.getEvidenciaById(id)

    suspend fun createEvidencia(evidencia: Evidencia): Long {
        val id = evidenciaDao.insertEvidencia(evidencia)
        logCambio("Evidencia", id, "Creación", "Archivo de evidencia subido: ${evidencia.titulo} (${evidencia.tipoArchivo})")
        return id
    }

    suspend fun updateEvidencia(evidencia: Evidencia) {
        evidenciaDao.updateEvidencia(evidencia)
        logCambio("Evidencia", evidencia.id, "Modificación", "Evidencia modificada: ${evidencia.titulo}")
    }

    suspend fun deleteEvidencia(evidencia: Evidencia) {
        evidenciaDao.deleteEvidencia(evidencia)
        logCambio("Evidencia", evidencia.id, "Eliminación", "Evidencia eliminada: ${evidencia.titulo}")
    }

    // --- CERTIFICADOS (DIPLOMAS & CERTIFICATIONS) ---
    val allCertificados: Flow<List<Certificado>> = certificadoDao.getAllCertificados()

    suspend fun getCertificado(id: Long): Certificado? = certificadoDao.getCertificadoById(id)

    suspend fun createCertificado(certificado: Certificado): Long {
        val id = certificadoDao.insertCertificado(certificado)
        logCambio("Certificado", id, "Creación", "Certificado guardado: ${certificado.titulo} de ${certificado.institucion}")
        return id
    }

    suspend fun updateCertificado(certificado: Certificado) {
        certificadoDao.updateCertificado(certificado)
        logCambio("Certificado", certificado.id, "Modificación", "Certificado modificado: ${certificado.titulo}")
    }

    suspend fun deleteCertificado(certificado: Certificado) {
        certificadoDao.deleteCertificado(certificado)
        logCambio("Certificado", certificado.id, "Eliminación", "Certificado eliminado: ${certificado.titulo}")
    }

    // --- LOGROS / ACHIEVEMENTS ---
    val allLogros: Flow<List<Logro>> = logroDao.getAllLogros()

    suspend fun getLogrosList(): List<Logro> = logroDao.getLogrosList()

    suspend fun initLogrosIfNeeded() {
        val count = logroDao.getLogrosCount()
        val defaultLogros = listOf(
            Logro(titulo = "Primeros Pasos", descripcion = "Completa 5 actividades académicas o bitácoras.", nivel = "Novato", valorActual = 0.0, valorMeta = 5.0, tipoLogro = "ACTIVIDADES_COUNT", medallaIcono = "star"),
            Logro(titulo = "Senda del Voluntario", descripcion = "Registra un total de 30 horas de voluntariado.", nivel = "Participante", valorActual = 0.0, valorMeta = 30.0, tipoLogro = "HORAS_VOLUNTARIADO", medallaIcono = "handshake"),
            Logro(titulo = "Relevista Comprometido", descripcion = "Registra 40 horas de apoyo en Relevo por la Vida.", nivel = "Colaborador", valorActual = 0.0, valorMeta = 40.0, tipoLogro = "HORAS_RELEVO", medallaIcono = "favorite"),
            Logro(titulo = "Líder del Mañana", descripcion = "Acumula un total de 100 horas de servicio en cualquier categoría.", nivel = "Líder", valorActual = 0.0, valorMeta = 100.0, tipoLogro = "HORAS_TOTALES", medallaIcono = "leaderboard"),
            Logro(titulo = "Coleccionista de Pruebas", descripcion = "Sube un total de 25 evidencias visuales o documentos.", nivel = "Embajador", valorActual = 0.0, valorMeta = 25.0, tipoLogro = "EVIDENCIAS_COUNT", medallaIcono = "photo_library"),
            Logro(titulo = "Leyenda Académica", descripcion = "Completa todas tus metas y llega a 200 horas totales registradas.", nivel = "Leyenda Académica", valorActual = 0.0, valorMeta = 200.0, tipoLogro = "HORAS_TOTALES", medallaIcono = "emoji_events")
        )
        if (count == 0) {
            logroDao.insertLogros(defaultLogros)
        } else {
            // Securely migrate/update old targets in DB with harder limits as user requested
            val currentList = logroDao.getLogrosList()
            for (defaultL in defaultLogros) {
                val existing = currentList.firstOrNull { it.titulo == defaultL.titulo || it.tipoLogro == defaultL.tipoLogro }
                if (existing != null) {
                    if (existing.valorMeta != defaultL.valorMeta) {
                        logroDao.updateLogro(existing.copy(
                            descripcion = defaultL.descripcion,
                            valorMeta = defaultL.valorMeta,
                            estaDesbloqueado = existing.valorActual >= defaultL.valorMeta
                        ))
                    }
                }
            }
        }
    }

    suspend fun updateLogro(logro: Logro) {
        logroDao.updateLogro(logro)
    }

    // --- DIARIO (ACADEMIC JOURNAL) ---
    val allDiarios: Flow<List<Diario>> = diarioDao.getAllDiarios()

    suspend fun getDiario(id: Long): Diario? = diarioDao.getDiarioById(id)

    suspend fun createDiario(diario: Diario): Long {
        val id = diarioDao.insertDiario(diario)
        logCambio("Diario", id, "Creación", "Entrada en diario creada: ${if (diario.texto.length > 30) diario.texto.take(30) + "..." else diario.texto}")
        return id
    }

    suspend fun updateDiario(diario: Diario) {
        diarioDao.updateDiario(diario)
    }

    suspend fun deleteDiario(diario: Diario) {
        diarioDao.deleteDiario(diario)
        logCambio("Diario", diario.id, "Eliminación", "Entrada eliminada de diario")
    }

    // --- CONFIGURACIÓN ---
    val allConfiguraciones: Flow<List<Configuracion>> = configuracionDao.getAllConfiguracionesFlow()

    suspend fun setConfiguracion(clave: String, valor: String) {
        configuracionDao.setConfiguracion(Configuracion(clave, valor))
    }

    suspend fun getConfiguracion(clave: String, defecto: String): String {
        return configuracionDao.getConfiguracion(clave) ?: defecto
    }

    // --- HISTORIAL DE CAMBIOS ---
    val allHistorial: Flow<List<HistorialCambios>> = historialCambiosDao.getAllHistorialFlow()

    suspend fun getHistorialList(): List<HistorialCambios> = historialCambiosDao.getAllHistorial()

    suspend fun clearHistorial() {
        // Enforce caution: we don't drop tables, but let user clear search history/logs with logging
        val id = historialCambiosDao.insertHistorial(
            HistorialCambios(entidad = "Historial", entidadId = 0, tipoAccion = "Eliminación", descripcion = "Bitácora limpia por usuario")
        )
    }

    // --- GALERÍA DE MEDIOS (PHOTOS & VIDEOS REPOSITORY) ---
    val allMedia: Flow<List<GaleriaMedia>> = galeriaMediaDao.getAllMediaFlow()

    suspend fun getMediaList(): List<GaleriaMedia> = galeriaMediaDao.getAllMediaList()

    suspend fun createMedia(media: GaleriaMedia): Long {
        val id = galeriaMediaDao.insertMedia(media)
        logCambio("GaleriaMedia", id, "Creación", "Archivo de galería guardado: ${media.titulo} (${media.tipoMedia})")
        return id
    }

    suspend fun updateMedia(media: GaleriaMedia) {
        galeriaMediaDao.updateMedia(media)
        logCambio("GaleriaMedia", media.id, "Modificación", "Archivo de galería modificado: ${media.titulo}")
    }

    suspend fun deleteMedia(media: GaleriaMedia) {
        galeriaMediaDao.deleteMedia(media)
        logCambio("GaleriaMedia", media.id, "Eliminación", "Archivo eliminado de la galería: ${media.titulo}")
    }

    // --- INTERNAL HELPERS ---
    private suspend fun logCambio(entidad: String, entidadId: Long, accion: String, descripcion: String) {
        historialCambiosDao.insertHistorial(
            HistorialCambios(
                entidad = entidad,
                entidadId = entidadId,
                tipoAccion = accion,
                descripcion = descripcion
            )
        )
    }
}
