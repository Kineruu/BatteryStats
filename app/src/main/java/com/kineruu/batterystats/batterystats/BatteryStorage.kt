package com.kineruu.batterystats

import androidx.compose.runtime.mutableStateListOf
import com.kineruu.batterystats.batterystats.BatteryRecord

object BatteryStorage {
    val records = mutableStateListOf<BatteryRecord>()
}