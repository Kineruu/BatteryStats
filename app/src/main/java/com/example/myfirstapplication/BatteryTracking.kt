package com.example.myfirstapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class BatteryTrackingService : Service() {

    // Coroutine for running the tracking loop
    private val serviceScope =
        CoroutineScope(
            Dispatchers.Default + SupervisorJob()
        )

    // Notification channel ID
    private val CHANNEL_ID = "BatteryTrackingChannel"

    // Creates the notification channel (Android 8+)
    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Tracking",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        // Create notification channel
        createNotificationChannel()

        // Build the notification
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Battery Tracking")
                .setContentText("Collecting battery data...")
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setOngoing(true)
                .build()

        startForeground(1, notification)

        serviceScope.launch {

            val batteryReader =
                BatteryReader(this@BatteryTrackingService)

            while (true) {

                val percentage = batteryReader.getPercentage()
                val milliAmps = batteryReader.getCurrentMilliAmps()
                val voltage = batteryReader.getVoltage()
                val temperature = batteryReader.getTemperature()

                BatteryStorage.records.add(
                    BatteryRecord(
                        timestamp = System.currentTimeMillis(),
                        percentage = percentage,
                        milliAmps = milliAmps,
                        batteryVoltage = voltage,
                        temperature = temperature
                    )
                )

                delay(5000)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}