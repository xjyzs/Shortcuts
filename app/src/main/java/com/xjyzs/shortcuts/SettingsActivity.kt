package com.xjyzs.shortcuts

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xjyzs.shortcuts.ui.theme.ShortcutsTheme
import java.io.File

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShortcutsTheme {
                Surface(Modifier.fillMaxSize()) {
                    SettingsUi()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsUi() {
    val context = LocalContext.current
    val directoryFile = File(context.filesDir.absolutePath+"/directories")
    var readDirectories=Environment.getExternalStorageDirectory().path
    try {
        readDirectories=directoryFile.readText()
    }catch(_:Exception){}
    var txt by remember { mutableStateOf(readDirectories) }
    Scaffold(topBar = {
        TopAppBar(title = {
            Text("设置")
        }, navigationIcon = {
            IconButton(onClick = { (context as ComponentActivity).finish() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
            }
        })
    }) { innerPadding ->
        Column(
            Modifier.fillMaxSize().wrapContentSize(Alignment.Center).padding(innerPadding)
                .padding(30.dp)
        ) {
            TextField(label = { Text("目录") }, value = txt, onValueChange = {txt=it}, modifier = Modifier.fillMaxWidth())
            Button({
                directoryFile.writeText(txt)
                val intent = Intent(context, PythonRunner::class.java)
                context.startActivity(intent)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("确认")
            }
        }
    }
}