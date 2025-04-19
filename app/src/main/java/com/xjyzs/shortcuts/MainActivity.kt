package com.xjyzs.shortcuts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xjyzs.shortcuts.ui.theme.ShortcutsTheme
import java.io.BufferedReader
import java.io.InputStreamReader

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

@Composable
fun Ui() {
    //Initialize
    val context = LocalContext.current
    val s = context.getSharedPreferences("s", Context.MODE_PRIVATE)
    //MT
    var mtPref = Color.Red
    if (s.getBoolean("mt", false)) {
        mtPref = Color.Green
    }
    var mt by remember { mutableStateOf(mtPref) }
    Column(Modifier.statusBarsPadding().wrapContentSize(Alignment.Center).padding(10.dp)) {
        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MT管理器", fontSize = 24.sp)
            Text("•", color = mt, fontSize = 30.sp)
            Spacer(Modifier.weight(1f))
            Button({
                with(s.edit()) {
                    putBoolean("mt", true)
                    apply()
                }
                mt = Color.Green
                Runtime.getRuntime().exec(
                    arrayOf(
                        "su",
                        "-c",
                        """pm unhide bin.mt.plus.canary;am start bin.mt.plus.canary/bin.mt.plus.Main"""))}) { Text("启动") }
            Button({
                with(s.edit()) {
                    putBoolean("mt", false)
                    apply()
                }
                mt = Color.Red
                Runtime.getRuntime().exec(arrayOf("su", "-c", "pm hide bin.mt.plus.canary"))
            }) { Text("关闭") }
        }
        //Charge
        var chargePref = Color.Red
        if (s.getBoolean("charge", false)) {
            chargePref = Color.Green
        }
        var charge by remember { mutableStateOf(chargePref) }
        var threshold by remember { mutableStateOf("83") }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("充电管理", fontSize = 24.sp)
            Text("•", color = charge, fontSize = 30.sp)
            TextField(
                label = { Text("阈值") },
                singleLine = true,
                value = threshold,
                onValueChange = { threshold = it },
                modifier = Modifier.width(57.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.weight(1f))
            Button({
                with(s.edit()) {
                    putBoolean("charge", true)
                    apply()
                }
                charge = Color.Green
                Runtime.getRuntime().exec(
                    arrayOf("su", "-c", """
dir="/sys/class/power_supply/battery/capacity"
while true; do
    capacity=$(cat ${'$'}dir)
    if [ ${'$'}capacity -gt $threshold ]; then
        echo 1 > /sys/class/power_supply/battery/input_suspend
        echo "已停止充电"
        break
    fi
    sleep 3
done
"""
                    )
                )
            }) {
                Text("启动")
            }
            Button({
                with(s.edit()) {
                    putBoolean("charge", false)
                    apply()
                }
                charge = Color.Red
                Runtime.getRuntime().exec(
                    arrayOf(
                        "su",
                        "-c",
                        "echo 0 > /sys/class/power_supply/battery/input_suspend;pkill -f \"已停止充电\""
                    )
                )
            }) {
                Text("关闭")
            }
        }

        //adb
        val process=Runtime.getRuntime().exec(arrayOf("su", "-c", "getprop service.adb.tcp.port"))
        val inputReader = BufferedReader(InputStreamReader(process.inputStream)).readLine()
        var adb by remember { mutableStateOf(Color.Red) }
        var adbport by remember { mutableStateOf("33445") }
        LaunchedEffect(Unit) {
            if (inputReader.isNotEmpty()) {
                adb = Color.Green
                adbport = inputReader
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ADB", fontSize = 24.sp)
            Text("•", color = adb, fontSize = 30.sp)
            TextField(
                label = { Text("端口") },
                singleLine = true,
                value = adbport,
                onValueChange = { adbport = it },
                modifier = Modifier.width(104.dp)
            )
            Spacer(Modifier.weight(1f))
            Button({
                with(s.edit()) {
                    putBoolean("adb", true)
                    apply()
                }
                adb = Color.Green
                Runtime.getRuntime().exec(arrayOf("su", "-c", "setprop service.adb.tcp.port ${adbport};stop adbd;start adbd"))
            }) {
                Text("启动")
            }
            Button({
                with(s.edit()) {
                    putBoolean("adb", false)
                    apply()
                }
                adb = Color.Red
                Runtime.getRuntime().exec(
                    arrayOf("su", "-c", "setprop service.adb.tcp.port '';stop adbd;start adbd")
                )
            }) {
                Text("关闭")
            }
        }

        //PythonRunner
        Row {
            Text("Python", fontSize = 24.sp)
            Spacer(Modifier.weight(1f))
            Button({
                val intent = Intent(context, PythonRunner::class.java)
                context.startActivity(intent)
            }) {
                Text("启动")
            }
        }
    }
}