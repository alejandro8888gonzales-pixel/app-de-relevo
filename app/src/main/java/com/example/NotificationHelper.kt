package com.example

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_ACHIEVEMENTS = "achievement_channel"
    const val CHANNEL_REMINDERS = "reminder_channel"
    private const val ALARM_REQUEST_CODE = 9991

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val achievementsChannel = NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Logros y Medallas 🏆",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando desbloqueas un nuevo logro en tu TES."
                enableVibration(true)
            }

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Recordatorios de Horas ⏰",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recordatorios periódicos para ingresar tus horas de servicio social."
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(achievementsChannel)
            manager.createNotificationChannel(remindersChannel)
        }
    }

    fun showAchievementNotification(context: Context, title: String, description: String) {
        initNotificationChannels(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(android.R.drawable.btn_star_big_on) // Standard high-contrast system star icon
            .setContentTitle("🏆 ¡Logro Desbloqueado!")
            .setContentText("$title: $description")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText("¡Felicidades, has alcanzado una nueva meta!\n\n🏅 Logro: $title\n📝 Requisito: $description\n\nSigue sumando más horas valiosas para el desarrollo de Honduras."))

        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(title.hashCode(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showReminderNotification(context: Context, title: String, message: String) {
        initNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(2002, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleRepeatingReminder(context: Context, intervalHours: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel previous if any
        alarmManager.cancel(pendingIntent)

        if (intervalHours == 0) {
            return
        }

        val intervalMs = when (intervalHours) {
            -1 -> 1000L          // 1 second for instant fun testing/demo
            -2 -> 60000L         // 1 minute
            else -> intervalHours * 60 * 60 * 1000L
        }
        val triggerAt = System.currentTimeMillis() + intervalMs

        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                intervalMs,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
