package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuario")
data class Usuario(
    @PrimaryKey val id: Int = 1,
    val nombre: String = "",
    val fotoUri: String? = null,
    val centroEducativo: String = "",
    val carrera: String = "",
    val grado: String = "",
    val seccion: String = "",
    val anioAcademico: String = "",
    val correo: String = "",
    val telefono: String = "",
    val biografia: String = "",
    val metaHorasTES: Int = 100,
    val metaHorasRelevo: Int = 40,
    val metaHorasVoluntariado: Int = 50,
    val metaHorasPracticas: Int = 100,
    val metaHorasAcademicas: Int = 50
)

@Entity(tableName = "actividad")
data class Actividad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descripcion: String,
    val categoria: String, // TES, Relevo por la Vida, Voluntariado, Prácticas, Actividades Académicas, etc.
    val fecha: Long, // timestamp
    val hora: String, // format "HH:mm" (legacy / reference)
    val lugar: String,
    val horasObtenidas: Double, // calculated automatically: (horaSalida - horaInicio)
    val estado: String, // "Pendiente", "Completada", "Validada"
    val observaciones: String,

    // SISTEMA DE BITÁCORA INTELIGENTE (COREGROUND EXTRAS)
    val horaInicio: String = "08:00",
    val horaSalida: String = "12:00",
    val colegio: String = "",
    val carreraEstudiante: String = "",
    val nombreEstudiante: String = "",
    val supervisor: String = "",
    val cargoSupervisor: String = "",
    val firmaSupervisorBase64: String? = null,
    val importancia: String = "Medio", // "Bajo", "Medio", "Alto"
    val anioCiclo: String = "2026", // For historical separation e.g. "2026", "2027"
    val fotosUris: String = "", // serialised list or comma-separated URIs of photographs
    val gpsHabilitado: Boolean = false,
    val latitud: Double = 14.1,
    val longitud: Double = -87.2,
    val gpsDireccion: String = "Tegucigalpa, Honduras",
    val queSeAprendio: String = "",
    val queSeApoyo: String = "",
    val queSeHizo: String = ""
)

@Entity(tableName = "evidencia")
data class Evidencia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actividadId: Long?, // NULL if global
    val titulo: String,
    val tipoArchivo: String, // "Imagen", "Video", "PDF", "DOCX", "TXT"
    val rutaArchivo: String, // file path / content URI
    val notas: String,
    val fechaRegistro: Long,
    val etiqueta: String, // Mes, Año, etc.
    val esFavorito: Boolean = false,
    val ocrTextoExtraido: String? = null
)

@Entity(tableName = "certificado")
data class Certificado(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val institucion: String,
    val fecha: Long,
    val horas: Double,
    val rutaArchivo: String,
    val textoExtraido: String? = null,
    val notas: String
)

@Entity(tableName = "logro")
data class Logro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descripcion: String,
    val nivel: String, // "Novato", "Participante", "Colaborador", "Líder", "Embajador", "Leyenda Académica"
    val valorActual: Double,
    val valorMeta: Double,
    val estaDesbloqueado: Boolean = false,
    val tipoLogro: String, // "HORAS_TOTALES", "EVIDENCIAS_COUNT", "RACHA", "ACTIVIDADES_COUNT"
    val medallaIcono: String // icon key
)

@Entity(tableName = "diario")
data class Diario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val texto: String,
    val imagenUri: String? = null,
    val ubicacion: String? = null,
    val estadoAnimo: String? = null, // "Excelente", "Motivado", "Felicidad", "Neutral", "Cansado"
    val fecha: Long
)

@Entity(tableName = "configuracion")
data class Configuracion(
    @PrimaryKey val clave: String,
    val valor: String
)

@Entity(tableName = "historial_cambios")
data class HistorialCambios(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entidad: String, // "Perfil", "Actividad", "Evidencia", "Certificado", "Diario"
    val entidadId: Long,
    val tipoAccion: String, // "Creación", "Modificación", "Eliminación"
    val descripcion: String,
    val fecha: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "galeria_media")
data class GaleriaMedia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val rutaArchivo: String, // local physical file path
    val tipoMedia: String, // "IMAGEN", "VIDEO"
    val fechaCreacion: Long, // timestamp
    val anio: Int, // for fast chronological index grouping
    val mes: Int, // 1 to 12
    val dia: Int, // 1 to 31
    val pesoOriginalBytes: Long, // file size prior to local downsampling
    val pesoComprimidoBytes: Long, // downsampled file size optimized for cellular saves
    val duracionSegundos: Long = 0, // duration for files categorized as videos
    val etiqueta: String = "Otros", // "TES", "Relevo por la Vida", "Voluntariado", "Otros"
    val supervisor: String = "",
    val realizado: Boolean = true,
    val horaEntrada: String = "08:00",
    val horaSalida: String = "12:00",
    val lugar: String = "",
    val ubicacionDetalle: String = ""
)

