package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.*

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val reminders = listOf(
            Pair("✍️ ¡Que no se te olvide reportar tu bitácora, pana!", "Dedica 2 minutos a registrar tus avances pedagógicos del TES hoy. ¡Cada paso cuenta! 🧠"),
            Pair("⏰ ¡Hora de actualizar tu bitácora escolar!", "Ingresa las horas de voluntariado y sube las fotos evidencia para impresionar a tus profesores. 🎓"),
            Pair("🚀 ¡Tu Trabajo Educativo Social va con ritmo!", "Sube tus evidencias o registra tus nuevas actividades para seguir acumulando horas hacia tu graduación. 🇭🇳"),
            Pair("🔥 ¡Mantén tu racha de aporte activa!", "Cada hora de esfuerzo cuenta para culminar tu TES y construir un cambio tecnológico en el país.")
        )
        
        // Pick a random motivational reminder
        val randomIndex = Random().nextInt(reminders.size)
        val reminder = reminders[randomIndex]
        
        NotificationHelper.showReminderNotification(context, reminder.first, reminder.second)
    }
}
