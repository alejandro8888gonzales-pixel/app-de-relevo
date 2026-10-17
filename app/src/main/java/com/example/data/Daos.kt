package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuario WHERE id = 1 LIMIT 1")
    fun getUsuarioFlow(): Flow<Usuario?>

    @Query("SELECT * FROM usuario WHERE id = 1 LIMIT 1")
    suspend fun getUsuario(): Usuario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuario(usuario: Usuario)
}

@Dao
interface ActividadDao {
    @Query("SELECT * FROM actividad ORDER BY fecha DESC, hora DESC")
    fun getAllActividades(): Flow<List<Actividad>>

    @Query("SELECT * FROM actividad")
    suspend fun getActividadesList(): List<Actividad>

    @Query("SELECT * FROM actividad WHERE id = :id LIMIT 1")
    suspend fun getActividadById(id: Long): Actividad?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActividad(actividad: Actividad): Long

    @Update
    suspend fun updateActividad(actividad: Actividad)

    @Delete
    suspend fun deleteActividad(actividad: Actividad)
}

@Dao
interface EvidenciaDao {
    @Query("SELECT * FROM evidencia ORDER BY fechaRegistro DESC")
    fun getAllEvidencias(): Flow<List<Evidencia>>

    @Query("SELECT * FROM evidencia")
    suspend fun getEvidenciasList(): List<Evidencia>

    @Query("SELECT * FROM evidencia WHERE actividadId = :actividadId ORDER BY fechaRegistro DESC")
    fun getEvidenciasForActividad(actividadId: Long): Flow<List<Evidencia>>

    @Query("SELECT * FROM evidencia WHERE id = :id LIMIT 1")
    suspend fun getEvidenciaById(id: Long): Evidencia?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidencia(evidencia: Evidencia): Long

    @Update
    suspend fun updateEvidencia(evidencia: Evidencia)

    @Delete
    suspend fun deleteEvidencia(evidencia: Evidencia)
}

@Dao
interface CertificadoDao {
    @Query("SELECT * FROM certificado ORDER BY fecha DESC")
    fun getAllCertificados(): Flow<List<Certificado>>

    @Query("SELECT * FROM certificado WHERE id = :id LIMIT 1")
    suspend fun getCertificadoById(id: Long): Certificado?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCertificado(certificado: Certificado): Long

    @Update
    suspend fun updateCertificado(certificado: Certificado)

    @Delete
    suspend fun deleteCertificado(certificado: Certificado)
}

@Dao
interface LogroDao {
    @Query("SELECT * FROM logro ORDER BY estaDesbloqueado DESC, id ASC")
    fun getAllLogros(): Flow<List<Logro>>

    @Query("SELECT * FROM logro")
    suspend fun getLogrosList(): List<Logro>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogro(logro: Logro)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogros(logros: List<Logro>)

    @Update
    suspend fun updateLogro(logro: Logro)

    @Query("SELECT COUNT(*) FROM logro")
    suspend fun getLogrosCount(): Int
}

@Dao
interface DiarioDao {
    @Query("SELECT * FROM diario ORDER BY fecha DESC")
    fun getAllDiarios(): Flow<List<Diario>>

    @Query("SELECT * FROM diario WHERE id = :id LIMIT 1")
    suspend fun getDiarioById(id: Long): Diario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiario(diario: Diario): Long

    @Update
    suspend fun updateDiario(diario: Diario)

    @Delete
    suspend fun deleteDiario(diario: Diario)
}

@Dao
interface ConfiguracionDao {
    @Query("SELECT * FROM configuracion")
    fun getAllConfiguracionesFlow(): Flow<List<Configuracion>>

    @Query("SELECT valor FROM configuracion WHERE clave = :clave LIMIT 1")
    suspend fun getConfiguracion(clave: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfiguracion(configuracion: Configuracion)
}

@Dao
interface HistorialCambiosDao {
    @Query("SELECT * FROM historial_cambios ORDER BY fecha DESC")
    fun getAllHistorialFlow(): Flow<List<HistorialCambios>>

    @Query("SELECT * FROM historial_cambios ORDER BY fecha DESC")
    suspend fun getAllHistorial(): List<HistorialCambios>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistorial(historialCambios: HistorialCambios)
}

@Dao
interface GaleriaMediaDao {
    @Query("SELECT * FROM galeria_media ORDER BY fechaCreacion DESC")
    fun getAllMediaFlow(): Flow<List<GaleriaMedia>>

    @Query("SELECT * FROM galeria_media ORDER BY fechaCreacion DESC")
    suspend fun getAllMediaList(): List<GaleriaMedia>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: GaleriaMedia): Long

    @Update
    suspend fun updateMedia(media: GaleriaMedia)

    @Delete
    suspend fun deleteMedia(media: GaleriaMedia)
}
