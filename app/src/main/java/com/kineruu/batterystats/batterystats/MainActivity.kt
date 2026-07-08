package com.kineruu.batterystats.batterystats

// woo hoo lots of imports... why
import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kineruu.batterystats.BatteryReader
import com.kineruu.batterystats.BatteryStorage
import com.kineruu.batterystats.BatteryTrackingService

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.kineruu.batterystats.ui.theme.MyFirstApplicationTheme

// I guess this is supposed to be above all normal classes?
// Creates an object each time the service checks the battery... At least it should work like that I think
data class BatteryRecord(
    // Long numbers
    val timestamp: Long,
    // If you somehow manage to go below 0 or above 100 then I guess you did something wrong
    val percentage: Int,
    // Current in an out your phone
    val milliAmps: Int,
    // Voltage in the phone
    val batteryVoltage: Float,
    // How hot is the phone
    val temperature: Float
)

// Main function
class MainActivity : ComponentActivity() {
    // I think this one is responsible for building the application screen
    override fun onCreate(savedInstanceState: Bundle?) {
        // Default android create screen function
        super.onCreate(savedInstanceState)

        // I don't know whether I like this one because it allows the application to use the WHOLE screen
        // Including the system bars - first time I added buttons they were on the notification section
        // And I didn't even know what happened or how the button even got there
        enableEdgeToEdge()
        // XML got replaced with this - UI section
        setContent {
            // This is the equivalent of "yo let me access your phone"
            // WHY DID ANDROID STUDIO HIGHLIGHT THE "yo" WORD
            val context = LocalContext.current

            // Remember prevent creating a new object every screen refresh
            val batteryReader = remember {
                BatteryReader(context)
            }

            // Literally the MOST basic on/off system
            var isTracking by remember {
                mutableStateOf(false)
            }

            // Data is saved here
            val batteryRecords = BatteryStorage.records

            // What you see on your phone is managed here
            MyFirstApplicationTheme {
                // Scaffold is basic structure from Material Design apparently
                Scaffold(
                    // Modifier... fillMaxSize speaks for itself
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(
                                top = 40.dp,
                                bottom = 30.dp
                            )
                            // Prevents from going above and beyond where it shouldn't be
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // no new data = null
                        val latestRecord = batteryRecords.lastOrNull()
                        // no data = show zero
                        ShowBatteryStatistics(
                            percentage = latestRecord?.percentage ?: 0,
                            milliAmps = latestRecord?.milliAmps ?: 0,
                            voltage = latestRecord?.batteryVoltage ?: 0f,
                            batteryTemperature = latestRecord?.temperature ?: 0f
                        )
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TrackingButton(
                                isTracking = isTracking,
                                onToggle = {

                                    isTracking = !isTracking
                                    if (isTracking) {
                                        val intent =
                                            Intent(
                                                context,
                                                BatteryTrackingService::class.java
                                            )
                                        ContextCompat.startForegroundService(
                                            context,
                                            intent
                                        )

                                    } else {
                                        val intent =
                                            Intent(
                                                context,
                                                BatteryTrackingService::class.java
                                            )
                                        context.stopService(intent)

                                    }
                                }
                            )
                            ExportData(
                                records = batteryRecords
                            )
                            ClearButton(
                                onClear = {
                                    batteryRecords.clear()
                                }
                            )
                        }
                        BatteryHistory(
                            records = batteryRecords
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ShowBatteryStatistics(
    percentage: Int,
    milliAmps: Int,
    voltage: Float,
    batteryTemperature: Float
) {

    val charging = if (milliAmps > 0) milliAmps else 0
    val discharging = if (milliAmps < 0) milliAmps else 0
    Text(
        text="$percentage % | $charging mA ~ $discharging mA | $voltage V | $batteryTemperature ℃",
        fontSize = 18.sp
    )
}

@Composable
fun TrackingButton(
    isTracking: Boolean,
    onToggle: () -> Unit
) {
    Button(
        onClick = {
            onToggle()
        }
    ) {
        if (isTracking) {
            Text("Stop Tracking")
        } else {
            Text("Start Tracking")
        }
    }
}

@Composable
fun ExportData(
    records: List<BatteryRecord>
) {

    val selectedFileLocation = LocalContext.current

    // CSV is temporarily stored while Android asks the user where to put the file
    var csvToSave by remember { mutableStateOf("") }

    val createFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {

            selectedFileLocation
                .contentResolver
                .openOutputStream(uri)
                ?.use { outputStream ->
                    outputStream.write(csvToSave.toByteArray()
                    )
            }
        }
    }

    Button(
        onClick = {
            csvToSave = buildString {
                appendLine("Timestamp | Percentage | Charging mA | Discharging mA | Voltage | Temperature")
                for (record in records) {
                    val charging = if (record.milliAmps > 0) record.milliAmps else 0
                    val discharging = if (record.milliAmps < 0) -record.milliAmps else 0
                    appendLine(
                    "${formatTimestamp(record.timestamp)} | " +
                            "${record.percentage} | " +
                            "${charging} | " +
                            "-${discharging} | " +
                            "${record.batteryVoltage} | " +
                            "${record.temperature}"
                    )
                }
            }
            createFile.launch("BatteryHistoryLog.csv")

        }
    ) {
        Text(
            text="Export Data"
        )
    }
}

@Composable
fun BatteryTimer(
    isTracking: Boolean,
    onTimerTick: () -> Unit
) {
    LaunchedEffect(isTracking) {
        while(isTracking) {
            delay(5000)
            onTimerTick()
        }
    }
}

@Composable
fun BatteryHistory(
    records: List<BatteryRecord>,
    modifier: Modifier = Modifier
) {

    val listState = rememberLazyListState()

    LaunchedEffect(records.size) {
        if (records.isNotEmpty()) {
            listState.animateScrollToItem( records.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState
    ) {

        items(records) { record ->
            val charging = if (record.milliAmps > 0) record.milliAmps else 0
            val discharging = if (record.milliAmps < 0) record.milliAmps else 0
            Text(
                text =
                    "${formatTimestamp(record.timestamp)} | " +
                            "${record.percentage}% | " +
                            "${charging} ~ " +
                            "${discharging} | " +
                            "${record.milliAmps}mA | " +
                            "${record.batteryVoltage}V | " +
                            "${record.temperature}℃",
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ClearButton(
    onClear: () -> Unit
) {
    Button(
        onClick = {
            onClear()
        }
    ) {
        Text(
            text = "Clear Data"
        )
    }
}

fun formatTimestamp(timestamp: Long): String {
    val format = SimpleDateFormat(
        "dd.MM.yyyy HH:mm:ss",
        Locale.getDefault()
    )
    return format.format(Date(timestamp))
}
