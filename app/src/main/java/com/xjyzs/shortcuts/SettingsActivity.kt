package com.xjyzs.shortcuts

import android.content.Context
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsUi() {
    val context = LocalContext.current
    val directoryFile = File(context.filesDir.absolutePath + "/directories")
    var readDirectories = Environment.getExternalStorageDirectory().path
    try {
        readDirectories = directoryFile.readText()
    } catch (_: Exception) {
    }
    val pref = context.getSharedPreferences("main", Context.MODE_PRIVATE)
    var directories by remember { mutableStateOf(readDirectories) }
    var options by remember { mutableStateOf(pref.getString("options", "-std=c++26")!!) }
    Scaffold(topBar = {
        LargeFlexibleTopAppBar(title = {
            Text("设置")
        }, navigationIcon = {
            IconButton(
                onClick = {
                    (context as ComponentActivity).finish()
                }, colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        })
    }) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .padding(innerPadding)
                .padding(30.dp)
        ) {
            TextField(
                label = { Text("目录") },
                value = directories,
                onValueChange = { directories = it },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                label = { Text("C++ 编译选项") },
                value = options,
                onValueChange = { options = it },
                modifier = Modifier.fillMaxWidth()
            )
            Button({
                directoryFile.writeText(directories)
                pref.edit {
                    putString("options", options)
                }
                val intent = Intent(context, PythonRunner::class.java)
                context.startActivity(intent)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("确认")
            }
        }
    }
}