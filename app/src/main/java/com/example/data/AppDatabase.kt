package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Usuario::class,
        Actividad::class,
        Evidencia::class,
        Certificado::class,
        Logro::class,
        Diario::class,
        Configuracion::class,
        HistorialCambios::class,
        GaleriaMedia::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun actividadDao(): ActividadDao
    abstract fun evidenciaDao(): EvidenciaDao
    abstract fun certificadoDao(): CertificadoDao
    abstract fun logroDao(): LogroDao
    abstract fun diarioDao(): DiarioDao
    abstract fun configuracionDao(): ConfiguracionDao
    abstract fun historialCambiosDao(): HistorialCambiosDao
    abstract fun galeriaMediaDao(): GaleriaMediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tes_master_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
