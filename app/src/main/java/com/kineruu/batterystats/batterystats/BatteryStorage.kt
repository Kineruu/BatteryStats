package com.kineruu.batterystats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kineruu.batterystats.batterystats.BatteryRecord
import com.kineruu.batterystats.batterystats.CurrentInput
import com.kineruu.batterystats.batterystats.CurrentOutput


object BatteryStorage {

    // All collected battery measurements
    val records = mutableStateListOf<BatteryRecord>()


    // Format the phone gives us
    var currentInput by mutableStateOf(CurrentInput.MICROAMPS)


    // Format the user wants to see
    var currentOutput by mutableStateOf(CurrentOutput.MILLIAMPS)

}