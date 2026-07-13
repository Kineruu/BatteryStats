package com.kineruu.batterystats.batterystats

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

import com.kineruu.batterystats.BatteryReader
import com.kineruu.batterystats.BatteryStorage
import com.kineruu.batterystats.BatteryTrackingService

import com.kineruu.batterystats.ui.theme.MyFirstApplicationTheme

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BatteryRecord(
    val timestamp: Long,
    val percentage: Int,
    val rawCurrent: Int?,
    val batteryVoltage: Float,
    val temperature: Float

)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var isTracking by remember {
                mutableStateOf(
                    BatteryTrackingService.isRunning
                )
            }
            val batteryRecords = BatteryStorage.records

            MyFirstApplicationTheme {
                Scaffold(
                    modifier =
                        Modifier.fillMaxSize()

                ) { innerPadding ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(
                                    top = 40.dp,
                                    bottom = 30.dp
                                )
                                .navigationBarsPadding(),


                        horizontalAlignment =
                            Alignment.CenterHorizontally

                    ) {

                        val latestRecord =
                            batteryRecords.lastOrNull()

                        ShowBatteryStatistics(
                            percentage = latestRecord?.percentage ?: 0,
                            rawCurrent = latestRecord?.rawCurrent ?: 0,
                            voltage = latestRecord?.batteryVoltage ?: 0f,
                            batteryTemperature = latestRecord?.temperature ?: 0f
                        )

                        Spacer(
                            Modifier.height(15.dp)
                        )

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),

                            horizontalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {
                            TrackingButton(
                                modifier = Modifier.weight(1f),
                                isTracking = isTracking,

                                onToggle = {
                                    isTracking = !isTracking
                                    val intent =
                                        Intent(
                                            context,
                                            BatteryTrackingService::class.java
                                        )
                                    if (isTracking) {
                                        ContextCompat.startForegroundService(
                                            context,
                                            intent
                                        )
                                    } else {
                                        context.stopService(intent)
                                    }
                                }
                            )
                            ExportData(
                                modifier = Modifier.weight(1f),
                                records = batteryRecords
                            )
                            ClearButton(
                                modifier = Modifier.weight(1f),
                                onClear = { batteryRecords.clear()
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        CurrentModeSelector()
                        Spacer(Modifier.height(10.dp))

                        BatteryHistory(
                            records = batteryRecords,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentModeSelector() {
    val modes =
        listOf(
            Pair(
                RawCurrentUnit.MILLIAMPS,
                DisplayCurrentUnit.MILLIAMPS
            ),

            Pair(
                RawCurrentUnit.MILLIAMPS,
                DisplayCurrentUnit.MICROAMPS
            ),

            Pair(
                RawCurrentUnit.MICROAMPS,
                DisplayCurrentUnit.MILLIAMPS
            ),

            Pair(
                RawCurrentUnit.MICROAMPS,
                DisplayCurrentUnit.MICROAMPS
            )
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),

        horizontalArrangement =
            Arrangement.spacedBy(4.dp)

    ) {
        modes.forEach { mode ->
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    BatteryStorage.rawCurrentUnit =
                        mode.first

                    BatteryStorage.displayCurrentUnit =
                        mode.second
                }
            ) {
                Text(
                    text =
                        "${if(mode.first == RawCurrentUnit.MILLIAMPS) "mA" else "µA"} → " + "${if(mode.second == DisplayCurrentUnit.MILLIAMPS) "mA" else "µA"}",
                    fontSize =
                        10.sp
                )
            }
        }
    }
}

@Composable
fun ShowBatteryStatistics(
    percentage: Int,
    rawCurrent: Int?,
    voltage: Float,
    batteryTemperature: Float
) {
    val chargingText =
        if (rawCurrent != null && rawCurrent > 0)
            BatteryReader.convertCurrent(
                rawCurrent,
                BatteryStorage.rawCurrentUnit,
                BatteryStorage.displayCurrentUnit
            )
        else
            "0"

    val dischargingText =
        if (rawCurrent != null && rawCurrent < 0)
            BatteryReader.convertCurrent(
                -rawCurrent,
                BatteryStorage.rawCurrentUnit,
                BatteryStorage.displayCurrentUnit
            )
        else
            "0"
    Text(
        text =
            "$percentage % | " +
            "$chargingText | " +
            "-$dischargingText | " +
            "$voltage V | " +
            "$batteryTemperature ℃",
        fontSize =
            16.sp
    )
}

@Composable
fun TrackingButton(
    modifier: Modifier = Modifier,
    isTracking: Boolean,
    onToggle: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = {
            onToggle()
        }
    ) {
        Text(
            text =
                if (isTracking)
                    "Stop"
                else
                    "Start",
            fontSize =
                11.sp
        )
    }
}

@Composable
fun ExportData(
    modifier: Modifier = Modifier,
    records: List<BatteryRecord>
) {
    val context = LocalContext.current
    var csvToSave by remember {
        mutableStateOf("")
    }
    val createFile =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.CreateDocument(
                    "text/csv"
                )
        ) { uri ->
            if (uri != null) {
                context.contentResolver
                    .openOutputStream(uri)
                    ?.use {
                        it.write(
                            csvToSave.toByteArray()
                        )
                    }
            }
        }
    Button(
        modifier = modifier,
        onClick = {
            csvToSave =
                buildString {
                    appendLine(
                        "Timestamp | Percentage | Raw Unit | Raw Charging | Raw Discharging | Charging µA | Discharging µA | Charging mA | Discharging mA | Voltage | Temperature"
                    )
                    records.forEach { record ->
                        val rawCharging =
                            if (
                                record.rawCurrent != null &&
                                record.rawCurrent > 0
                            )
                                record.rawCurrent
                            else
                                0

                        val rawDischarging =
                            if (
                                record.rawCurrent != null &&
                                record.rawCurrent < 0
                            )
                                -record.rawCurrent
                            else
                                0

                        val chargingMicro =
                            if (BatteryStorage.rawCurrentUnit == RawCurrentUnit.MICROAMPS)
                                rawCharging
                            else
                                rawCharging * 1000

                        val dischargingMicro =
                            if (BatteryStorage.rawCurrentUnit == RawCurrentUnit.MICROAMPS)
                                rawDischarging
                            else
                                rawDischarging * 1000

                        val chargingMilli =
                            if (BatteryStorage.rawCurrentUnit == RawCurrentUnit.MILLIAMPS)
                                rawCharging.toFloat()
                            else
                                rawCharging / 1000f

                        val dischargingMilli =
                            if (BatteryStorage.rawCurrentUnit == RawCurrentUnit.MILLIAMPS)
                                rawDischarging.toFloat()
                            else
                                rawDischarging / 1000f

                        appendLine(
                    "${formatTimestamp(record.timestamp)} | " +
                            "${record.percentage}% | " +
                            "${BatteryStorage.rawCurrentUnit} | " +
                            "$rawCharging | " +
                            "$rawDischarging | " +
                            "$chargingMicro | " +
                            "$dischargingMicro | " +
                            String.format(
                                Locale.getDefault(),
                                "%.2f",
                                chargingMilli
                            ) + " | " +
                            String.format(
                                Locale.getDefault(),
                                "%.2f",
                                dischargingMilli
                            ) + " | " +
                            "${record.batteryVoltage} | " +
                            "${record.temperature}"
                        )
                    }
                }
            createFile.launch(
                "BatteryHistoryLog.csv"
            )
        }
    ) {
        Text(
            "Export",
            fontSize = 11.sp
        )
    }
}

@Composable
fun BatteryHistory(
    records: List<BatteryRecord>,
    modifier: Modifier = Modifier
) {

    val listState =
        rememberLazyListState()

    LaunchedEffect(records.size) {
        if (records.isNotEmpty()) {
            listState.animateScrollToItem(
                records.size - 1
            )
        }
    }

    LazyColumn(
        modifier =
            modifier,

        state =
            listState
    ) {
        items(records) { record ->
            val chargingText =
                if (
                    record.rawCurrent != null &&
                    record.rawCurrent > 0
                )
                    BatteryReader.convertCurrent(
                        record.rawCurrent,
                        BatteryStorage.rawCurrentUnit,
                        BatteryStorage.displayCurrentUnit
                    )
                else
                    "0"

            val dischargingText =
                if (
                    record.rawCurrent != null &&
                    record.rawCurrent < 0
                )
                    BatteryReader.convertCurrent(
                        -record.rawCurrent,
                        BatteryStorage.rawCurrentUnit,
                        BatteryStorage.displayCurrentUnit
                    )
                else
                    "0"
            Text(
                text =
                    "${formatTimestamp(record.timestamp)} | " +
                    "${record.percentage}% | " +
                    "$chargingText | " +
                    "-$dischargingText | " +
                    "${record.batteryVoltage}V | " +
                    "${record.temperature}℃",
                fontSize =
                    10.sp
            )
        }
    }
}

@Composable
fun ClearButton(
    modifier: Modifier = Modifier,
    onClear: () -> Unit
) {
    Button(
        modifier =
            modifier,
        onClick = {
            onClear()
        }
    ) {
        Text(
            "Clear",
            fontSize =
                11.sp
        )
    }
}

fun formatTimestamp(
    timestamp: Long
): String {
    val format =
        SimpleDateFormat(
            "dd.MM.yyyy HH:mm:ss",
            Locale.getDefault()
        )
    return format.format(
        Date(timestamp)
    )
}
