package com.kineruu.batterystats

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

import com.kineruu.batterystats.batterystats.DisplayCurrentUnit
import com.kineruu.batterystats.batterystats.RawCurrentUnit

import java.util.Locale

class BatteryReader(
    private val context: Context
) {
    private val batteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    // Battery percentage: 0-100%
    fun getPercentage(): Int {
        return batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        )
    }
    // Current: Android normally gives microamps (µA)
    // Positive = charging | Negative = discharging
    fun getRawCurrent(): Int? {
        val current = batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
        )

        if (current == Int.MIN_VALUE || current == 0) {
            return null
        }
        return current
    }
    // Voltage in volts
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
    // Temperature in Celsius
    fun getTemperature(): Float {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val temperature = intent?.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE,
            -1
        ) ?: -1
        return temperature / 10f
    }
    companion object {
        fun convertCurrent(
            rawCurrent: Int?,
            rawUnit: RawCurrentUnit,
            outputUnit: DisplayCurrentUnit
        ): String {
            if (rawCurrent == null) {
                return "N/A"
            }
            val convertedValue: Float = when {
                rawUnit == RawCurrentUnit.MICROAMPS && outputUnit == DisplayCurrentUnit.MICROAMPS -> {
                    rawCurrent.toFloat()
                }
                rawUnit == RawCurrentUnit.MICROAMPS && outputUnit == DisplayCurrentUnit.MILLIAMPS -> {
                    rawCurrent / 1000f
                }
                rawUnit == RawCurrentUnit.MILLIAMPS && outputUnit == DisplayCurrentUnit.MILLIAMPS -> {
                    rawCurrent.toFloat()
                }
                rawUnit == RawCurrentUnit.MILLIAMPS && outputUnit == DisplayCurrentUnit.MICROAMPS -> {
                    rawCurrent * 1000f
                }
                else -> rawCurrent.toFloat()
            }
            return when (outputUnit) {
                DisplayCurrentUnit.MICROAMPS -> {
                    "${convertedValue.toInt()} µA"
                }
                DisplayCurrentUnit.MILLIAMPS -> {
                    if (convertedValue % 1f == 0f) {
                        "${convertedValue.toInt()} mA"
                    } else {
                        String.format(
                            Locale.getDefault(),
                            "%.2f mA",
                            convertedValue
                        )
                    }
                }
            }
        }
    }
}