package com.example.myfirstapplication

import android.content.Context
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter


class BatteryReader(
    private val context: Context
) {

    private val batteryManager =
        context.getSystemService(
            Context.BATTERY_SERVICE
        ) as BatteryManager


    // Battery percentage: 0-100%
    fun getPercentage(): Int {
        return batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        )
    }


    // Current: Gives microamps (µA)
    // Convert it to milliamps (mA)
    fun getCurrentMilliAmps(): Int {

        return batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
        ) / 1000
    }


    // Voltage: Gives millivolts (mV)
    // Convert it to volts (V)
    fun getVoltage(): Float {

        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val voltageMV = intent?.getIntExtra(
            BatteryManager.EXTRA_VOLTAGE,
            -1
        ) ?: -1

        return voltageMV / 1000f
    }

    // Temperature: Gives tenths of a degree Celsius
    fun getTemperature(): Float {

        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val temperature =
            intent?.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE,
                -1
            ) ?: -1

        return temperature / 10f
    }
}