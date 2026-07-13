package com.kineruu.batterystats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kineruu.batterystats.batterystats.BatteryRecord
import com.kineruu.batterystats.batterystats.RawCurrentUnit
import com.kineruu.batterystats.batterystats.DisplayCurrentUnit

object BatteryStorage {
    // All collected battery measurements
    val records = mutableStateListOf<BatteryRecord>()
    // Format the phone gives us
    var rawCurrentUnit by mutableStateOf(RawCurrentUnit.MICROAMPS)
    // Format the user wants to see
    var displayCurrentUnit by mutableStateOf(DisplayCurrentUnit.MILLIAMPS)
}