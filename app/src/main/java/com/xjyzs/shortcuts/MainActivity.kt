package com.xjyzs.shortcuts

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.xjyzs.shortcuts.ui.theme.ShortcutsTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShortcutsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Ui()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Ui() {
    //Initialize
    val context = LocalContext.current
    var adb by remember { mutableStateOf(false) }
    var adbPort by remember { mutableStateOf("33445") }
    var charge by remember { mutableStateOf(false) }
    var mt by remember { mutableStateOf(false) }
    var process: Process
    var outputStream by remember { mutableStateOf<OutputStream?>(null) }
    var reader: BufferedReader
    val pref = context.getSharedPreferences("main", Context.MODE_PRIVATE)
    var threshold by remember { mutableStateOf(pref.getString("threshold", "80").toString()) }
    LaunchedEffect(Unit) {
        createNotificationChannel(context)
        try {
            process = ProcessBuilder("su").apply {
                redirectErrorStream(true)
            }.start()
            outputStream = process.outputStream
            reader = BufferedReader(InputStreamReader(process.inputStream))
            outputStream!!.write("getprop service.adb.tcp.port; echo ''\n".toByteArray())
            outputStream!!.flush()
            var line = reader.readLine()
            if (line.isNotEmpty()) {
                adb = true
                adbPort = line
            }
            reader.readLine()
            outputStream!!.write("pgrep -fl 已停止充电; echo ''\n".toByteArray())
            outputStream!!.flush()
            line = reader.readLine()
            if (line.isNotEmpty()) {
                charge = true
                val regex =
                    Regex("capacity -gt (?<capacity>.*?) ]")
                val matchResult = regex.findAll(line)
                for (i in matchResult) {
                    threshold = (i.groups["capacity"]?.value!!.toInt() + 1).toString()
                }
                reader.readLine()
            }
            outputStream!!.write("pm list packages -3 | grep bin.mt.plus.canary; echo ''\n".toByteArray())
            outputStream!!.flush()
            line = reader.readLine()
            if (line.isNotEmpty()) {
                mt = true
                reader.readLine()
            }
        } catch (_: Exception) {
            Toast.makeText(context, "请先授予 ROOT 权限", Toast.LENGTH_SHORT).show()
        }
        threshold = pref.getString("threshold", "80").toString()
    }

    //MT
    Scaffold(topBar = {
        LargeFlexibleTopAppBar(title = {
            Text(stringResource(R.string.app_name))
        })
    }, modifier = Modifier.padding(horizontal = 4.dp)) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(), verticalArrangement = Arrangement.Center
        ) {
            Row(
                Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MT管理器", fontSize = 24.sp)
                Spacer(Modifier.weight(1f))
                Switch(mt, {
                    if (mt) {
                        outputStream!!.write("pm hide bin.mt.plus.canary\n".toByteArray())
                        outputStream!!.flush()
                    } else {
                        outputStream!!.write("pm unhide bin.mt.plus.canary;am start bin.mt.plus.canary/bin.mt.plus.Main\n".toByteArray())
                        outputStream!!.flush()
                    }
                    mt = !mt
                })
            }
            //Charge
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("充电管理", fontSize = 24.sp)
                TextField(
                    label = { Text("阈值") },
                    singleLine = true,
                    value = threshold,
                    onValueChange = { threshold = it },
                    modifier = Modifier.width(57.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.weight(1f))
                Switch(charge, {
                    if (charge) {
                        outputStream!!.write("chmod 664 /sys/class/power_supply/battery/night_charging;echo 0 > /sys/class/power_supply/battery/night_charging;chmod 664 /sys/class/power_supply/battery/input_suspend;echo 0 > /sys/class/power_supply/battery/input_suspend;pkill -f 已停止充电\n".toByteArray())
                        outputStream!!.flush()
                    } else {
                        val controlType = if (threshold.toInt() >= 80) {
                            "night_charging"
                        } else {
                            "input_suspend"
                        }
                        thread {
                            pref.edit {
                                putString("threshold", threshold)
                            }
                            outputStream!!.write("pkill -f 已停止充电\n".toByteArray())
                            outputStream!!.flush()
                            Thread.sleep(100)
                            val p = Runtime.getRuntime().exec(
                                arrayOf(
                                    "su", "-c", """
dir="/sys/class/power_supply/battery/capacity"
while true; do
    capacity=$(cat ${'$'}dir)
    if [ ${'$'}capacity -gt ${threshold.toInt() - 1} ]; then
        chmod 664 /sys/class/power_supply/battery/$controlType
        echo 1 > /sys/class/power_supply/battery/$controlType
        chmod 444 /sys/class/power_supply/battery/$controlType
        if [ "$controlType" == "input_suspend" ]; then
            echo "已停止充电"
        else
            echo "已开启旁路供电"
        fi
        break
    fi
    sleep 3
done
"""
                                )
                            )
                            p.inputStream?.use { stream ->
                                val ln = BufferedReader(InputStreamReader(stream)).readLine()
                                if (ln != null) {
                                    if (ln.isNotEmpty()) {
                                        val notificationManager =
                                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                        val notification =
                                            NotificationCompat.Builder(context, "default")
                                                .setContentTitle("Shortcuts")
                                                .setContentText(ln)
                                                .setSmallIcon(R.drawable.icon)
                                                .build()
                                        notificationManager.notify(1, notification)
                                    }
                                }
                            }
                        }
                    }
                    charge = !charge
                })
            }

            //adb
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ADB", fontSize = 24.sp)
                TextField(
                    label = { Text("端口") },
                    singleLine = true,
                    value = adbPort,
                    onValueChange = { adbPort = it },
                    modifier = Modifier.width(104.dp)
                )
                Spacer(Modifier.weight(1f))
                Switch(adb, {
                    if (adb) {
                        outputStream!!.write("setprop service.adb.tcp.port '';stop adbd;start adbd\n".toByteArray())
                    } else {
                        outputStream!!.write("setprop service.adb.tcp.port ${adbPort};stop adbd;start adbd\n".toByteArray())
                    }
                    outputStream!!.flush()
                    adb=!adb
                })
            }

            //PythonRunner
            Row(
                Modifier
                    .height(48.dp)
                    .clickable {
                        val intent = Intent(context, PythonRunner::class.java)
                        context.startActivity(intent)
                    }, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Python", fontSize = 24.sp)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // cpp
            Row(
                Modifier
                    .height(48.dp)
                    .clickable {
                        val intent = Intent(context, CppRunner::class.java)
                        context.startActivity(intent)
                    }, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("C++", fontSize = 24.sp)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requestPermissions(context as Activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
    }
    val channel = NotificationChannel(
        "default",
        "默认通知渠道",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "默认"
    }

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}