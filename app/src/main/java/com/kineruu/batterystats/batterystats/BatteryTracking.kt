package com.kineruu.batterystats

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

import androidx.core.app.NotificationCompat

import com.kineruu.batterystats.batterystats.BatteryRecord

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class BatteryTrackingService : Service() {

    companion object {
        var isRunning = false

    }

    private val serviceScope =
        CoroutineScope(
            Dispatchers.Default + SupervisorJob()
        )

    private var trackingJob: Job? = null
    private val CHANNEL_ID =
        "BatteryTrackingChannel"

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Battery Tracking",
                    NotificationManager.IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (isRunning) {
            return START_STICKY
        }
        isRunning = true

        createNotificationChannel()

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    "Battery Tracking"
                )
                .setContentText(
                    "Collecting battery data..."
                )
                .setSmallIcon(
                    android.R.drawable.ic_lock_idle_charging
                )
                .setOngoing(true)
                .build()

        startForeground(
            1,
            notification
        )

        trackingJob =
            serviceScope.launch {

                val batteryReader =
                    BatteryReader(
                        this@BatteryTrackingService
                    )

                while (isActive) {

                    val percentage =
                        batteryReader.getPercentage()

                    val microAmps =
                        batteryReader.getCurrentMicroAmps()

                    val voltage =
                        batteryReader.getVoltage()

                    val temperature =
                        batteryReader.getTemperature()

                    BatteryStorage.records.add(
                        BatteryRecord(
                            timestamp =
                                System.currentTimeMillis(),
                            percentage =
                                percentage,
                            microAmps =
                                microAmps,
                            batteryVoltage =
                                voltage,
                            temperature =
                                temperature
                        )
                    )
                    delay(5000)
                }
            }
        return START_STICKY
    }

    override fun onDestroy() {

        trackingJob?.cancel()

        serviceScope.cancel()

        isRunning = false
        super.onDestroy()

    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null

    }
}