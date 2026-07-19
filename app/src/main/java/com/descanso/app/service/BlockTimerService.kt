package com.descanso.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.descanso.app.MainActivity
import com.descanso.app.R
import com.descanso.app.data.BlockPrefs
import java.util.concurrent.TimeUnit

/** Servicio en primer plano que muestra la cuenta regresiva del descanso. */
class BlockTimerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (updateOrStop()) handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startInForeground(buildNotification(BlockPrefs.endTime(this) - System.currentTimeMillis()))
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        return START_STICKY
    }

    /** Devuelve false cuando ya terminó (y se detiene solo). */
    private fun updateOrStop(): Boolean {
        val remaining = BlockPrefs.endTime(this) - System.currentTimeMillis()
        if (remaining <= 0) {
            BlockPrefs.clear(this)
            notifyFinished()
            stopSelf()
            return false
        }
        manager().notify(NOTIF_ID, buildNotification(remaining))
        return true
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(remainingMillis: Long): Notification {
        val remaining = remainingMillis.coerceAtLeast(0)
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Descanso activo")
            .setContentText("Quedan ${format(remaining)}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pending)
            .build()
    }

    private fun notifyFinished() {
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Descanso terminado")
            .setContentText("Ya podés volver a usar tus apps.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        manager().notify(NOTIF_ID + 1, n)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Temporizador de Descanso", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Muestra el tiempo restante del bloqueo" }
            manager().createNotificationChannel(channel)
        }
    }

    private fun manager() =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "descanso_timer"
        private const val NOTIF_ID = 1001

        fun format(millis: Long): String {
            val h = TimeUnit.MILLISECONDS.toHours(millis)
            val m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            val s = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
            else String.format("%02d:%02d", m, s)
        }
    }
}
