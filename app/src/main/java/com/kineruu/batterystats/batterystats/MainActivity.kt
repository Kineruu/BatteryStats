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

    val microAmps: Int?,

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

            val batteryRecords =
                BatteryStorage.records

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
                            percentage =
                                latestRecord?.percentage ?: 0,

                            rawMicroAmps =
                                latestRecord?.microAmps ?: 0,

                            voltage =
                                latestRecord?.batteryVoltage ?: 0f,

                            batteryTemperature =
                                latestRecord?.temperature ?: 0f
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
                                modifier =
                                    Modifier.weight(1f),
                                isTracking =
                                    isTracking,

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
                                modifier =
                                    Modifier.weight(1f),
                                records =
                                    batteryRecords
                            )
                            ClearButton(
                                modifier =
                                    Modifier.weight(1f),
                                onClear = {
                                    batteryRecords.clear()
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        CurrentModeSelector()
                        Spacer(Modifier.height(10.dp))

                        BatteryHistory(
                            records =
                                batteryRecords,
                            modifier =
                                Modifier.weight(1f)
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
                CurrentInput.MILLIAMPS,
                CurrentOutput.MILLIAMPS
            ),

            Pair(
                CurrentInput.MILLIAMPS,
                CurrentOutput.MICROAMPS
            ),

            Pair(
                CurrentInput.MICROAMPS,
                CurrentOutput.MILLIAMPS
            ),

            Pair(
                CurrentInput.MICROAMPS,
                CurrentOutput.MICROAMPS
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
                modifier =
                    Modifier.weight(1f),

                onClick = {
                    BatteryStorage.currentInput =
                        mode.first

                    BatteryStorage.currentOutput =
                        mode.second
                }
            ) {
                Text(
                    text =
                        "${if(mode.first == CurrentInput.MILLIAMPS) "mA" else "µA"} → " +
                                "${if(mode.second == CurrentOutput.MILLIAMPS) "mA" else "µA"}",
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
    rawMicroAmps: Int?,
    voltage: Float,
    batteryTemperature: Float
) {
    val chargingText =
        if (rawMicroAmps != null && rawMicroAmps > 0)
            BatteryReader.convertCurrent(
                rawMicroAmps,
                BatteryStorage.currentOutput
            )
        else
            "0"

    val dischargingText =
        if (rawMicroAmps != null && rawMicroAmps < 0)
            BatteryReader.convertCurrent(
                -rawMicroAmps,
                BatteryStorage.currentOutput
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
        modifier =
            modifier,

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
                        "Timestamp | Percentage | Charging µA | Discharging µA | Charging mA | Discharging mA | Voltage | Temperature"
                    )
                    records.forEach { record ->
                        val chargingMicro =
                            if (
                                record.microAmps != null &&
                                record.microAmps > 0
                            )
                                record.microAmps
                            else
                                0

                        val dischargingMicro =
                            if (
                                record.microAmps != null &&
                                record.microAmps < 0
                            )
                                -record.microAmps
                            else
                                0

                        val chargingMilli =
                            chargingMicro / 1000f

                        val dischargingMilli =
                            dischargingMicro / 1000f

                        appendLine(
                            "${formatTimestamp(record.timestamp)} | " +
                                    "${record.percentage}% | " +
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
                    record.microAmps != null &&
                    record.microAmps > 0
                )
                    BatteryReader.convertCurrent(
                        record.microAmps,
                        BatteryStorage.currentOutput
                    )
                else
                    "0"

            val dischargingText =
                if (
                    record.microAmps != null &&
                    record.microAmps < 0
                )
                    BatteryReader.convertCurrent(
                        -record.microAmps,
                        BatteryStorage.currentOutput
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
