package com.xjyzs.shortcuts

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import kotlinx.coroutines.delay
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

@Composable
fun Ui() {
    //Initialize
    val context = LocalContext.current
    var adb by remember { mutableStateOf(Color.Red) }
    var adbPort by remember { mutableStateOf("33445") }
    var charge by remember { mutableStateOf(Color.Red) }
    var threshold by remember { mutableStateOf("82") }
    var mt by remember { mutableStateOf(Color.Red) }
    var process: Process
    var outputStream by remember { mutableStateOf<OutputStream?>(null) }
    var reader: BufferedReader
    LaunchedEffect(Unit) {
        try {
            process = ProcessBuilder("su").apply {
                redirectErrorStream(true)
            }.start()
            outputStream = process.outputStream
            reader = BufferedReader(InputStreamReader(process.inputStream))
            var line= ""
            outputStream!!.write("getprop service.adb.tcp.port\n".toByteArray())
            outputStream!!.flush()
            line = reader.readLine()
            if (line.isNotEmpty()) {
                adb = Color.Green
                adbPort = line
            }
            outputStream!!.write("pgrep -fl 已停止充电; echo ''\n".toByteArray())
            outputStream!!.flush()
            line = reader.readLine()
            if (line.isNotEmpty()) {
                charge = Color.Green
                val regex =
                    Regex("capacity -gt (?<capacity>.*?) ]")
                val matchResult = regex.findAll(line)
                for (i in matchResult){
                    threshold=(i.groups["capacity"]?.value!!.toInt()+1).toString()
                }
                reader.readLine()
            }
            outputStream!!.write("pm list packages -3 | grep bin.mt.plus.canary; echo ''\n".toByteArray())
            outputStream!!.flush()
            line = reader.readLine()
            if (line.isNotEmpty()) {
                mt = Color.Green
                reader.readLine()
            }
        }catch(_:Exception) {
            Toast.makeText(context, "请先授予 ROOT 权限".toString(), Toast.LENGTH_SHORT).show()
        }
    }

    //MT
    Column(Modifier
        .statusBarsPadding()
        .wrapContentSize(Alignment.Center)
        .padding(10.dp)) {
        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MT管理器", fontSize = 24.sp)
            Text("•", color = mt, fontSize = 30.sp)
            Spacer(Modifier.weight(1f))
            Button({
                mt = Color.Green
                outputStream!!.write("pm unhide bin.mt.plus.canary;am start bin.mt.plus.canary/bin.mt.plus.Main\n".toByteArray())
                outputStream!!.flush()}) { Text("启动") }
            Button({
                mt = Color.Red
                outputStream!!.write("pm hide bin.mt.plus.canary\n".toByteArray())
                outputStream!!.flush() }) { Text("关闭") }
        }
        //Charge
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
                charge = Color.Green
                val controlType = if (threshold.toInt() > 79) {
                    "night_charging"
                } else {
                    "input_suspend"
                }
                thread {
                    outputStream!!.write("pkill -f 已停止充电\n".toByteArray())
                    outputStream!!.flush()
                    Thread.sleep(100)
                    Runtime.getRuntime().exec(
                        arrayOf(
                            "su", "-c", """
dir="/sys/class/power_supply/battery/capacity"
while true; do
    capacity=$(cat ${'$'}dir)
    if [ ${'$'}capacity -gt ${threshold.toInt() - 1} ]; then
        chmod 664 /sys/class/power_supply/battery/$controlType
        echo 1 > /sys/class/power_supply/battery/$controlType
        chmod 444 /sys/class/power_supply/battery/$controlType
        echo "已停止充电"
        break
    fi
    sleep 3
done
"""
                        )
                    )
                }
            }) {
                Text("启动")
            }
            Button({
                charge = Color.Red
                outputStream!!.write("chmod 664 /sys/class/power_supply/battery/night_charging;echo 0 > /sys/class/power_supply/battery/night_charging;chmod 664 /sys/class/power_supply/battery/input_suspend;echo 0 > /sys/class/power_supply/battery/input_suspend;pkill -f 已停止充电\n".toByteArray())
                outputStream!!.flush() }) {
                Text("关闭")
            }
        }

        //adb
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ADB", fontSize = 24.sp)
            Text("•", color = adb, fontSize = 30.sp)
            TextField(
                label = { Text("端口") },
                singleLine = true,
                value = adbPort,
                onValueChange = { adbPort = it },
                modifier = Modifier.width(104.dp)
            )
            Spacer(Modifier.weight(1f))
            Button({
                adb = Color.Green
                outputStream!!.write("setprop service.adb.tcp.port ${adbPort};stop adbd;start adbd\n".toByteArray())
                outputStream!!.flush()
            }) {
                Text("启动")
            }
            Button({
                adb = Color.Red
                outputStream!!.write("setprop service.adb.tcp.port '';stop adbd;start adbd\n".toByteArray())
                outputStream!!.flush()
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
