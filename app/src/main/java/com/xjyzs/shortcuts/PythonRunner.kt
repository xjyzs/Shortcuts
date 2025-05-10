package com.xjyzs.shortcuts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.xjyzs.shortcuts.ui.theme.ShortcutsTheme
import java.io.File

class PythonRunner : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShortcutsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FileExplorer()
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnrememberedMutableState")
@Composable
fun FileExplorer() {
    val fileList: MutableState<List<String>> = mutableStateOf(emptyList())
    var useLinux by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val directoryFile = File(context.filesDir.absolutePath+"/directories")
    var directories= arrayOf("/sdcard/")
    if (directoryFile.exists()){
        directories=directoryFile.readLines().toTypedArray()
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:com.xjyzs.shortcuts".toUri()
                context.startActivity(intent)
                (context as ComponentActivity).finish()
            }
        } else{
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(context as Activity,arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),1001)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Python") },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多选项"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("设置") },
                            onClick = {
                                showMenu = false
                                val intent = Intent(context, SettingsActivity::class.java)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            )
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    {
                        useLinux = !useLinux
                    },
                    colors = ButtonDefaults.buttonColors(
                        Color.Transparent,
                        LocalContentColor.current
                    ),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Linux", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = useLinux, onCheckedChange = { useLinux = it })
                }
            }
            Button(
                {
                    Runtime.getRuntime().exec(
                        arrayOf(
                            "su",
                            "-c",
                            "am start -W com.termux/com.termux.HomeActivity;input text 'cd /storage/emulated/0/\n';am start -f 0x20000000 com.xjyzs.shortcuts/com.xjyzs.shortcuts.PythonRunner"
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(Color.Transparent, LocalContentColor.current),
                shape = RectangleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(Modifier.weight(1f)) {
                    Text("cd", fontSize = 24.sp, fontWeight = FontWeight.Normal)
                }
            }
            // 可选：显示Dialog弹窗
            for (i in directories) {
                var dir=i
                if (i.takeLast(1)=="/"){
                    dir=dir.dropLast(1)
                }
                fileList.value = getFileList(dir)
                Text(
                    dir,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                for (j in fileList.value) {
                    Button(
                        {
                            if (useLinux) {
                                Runtime.getRuntime().exec(
                                    arrayOf(
                                        "su",
                                        "-c",
                                        """am start -W com.termux/com.termux.HomeActivity
if ! pgrep -x "gitstatusd-linu"> /dev/null; then
    input text "debian
"
fi
input text "source .venv/bin/activate
"
input text "python3 ${dir}/${j}.py
""""
                                    )
                                )
                            } else {
                                Runtime.getRuntime().exec(
                                    arrayOf(
                                        "su",
                                        "-c",
                                        "am start -W com.termux/com.termux.HomeActivity;input text 'python ${dir}/${j}.py\n'"
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            Color.Transparent,
                            LocalContentColor.current
                        ),
                        shape = RectangleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp)) {
                            Text(
                                j,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getFileList(path: String): List<String> {
    val fileList = mutableStateListOf<String>()
        val directory = File(path)
        val files = directory.listFiles()
        for (file in files ?: emptyArray()) {
            if (file.name.endsWith(".py")) {
                fileList.add(file.name.substring(0,file.name.length-3))
            }
        }
    return fileList
}
