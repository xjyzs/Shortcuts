package com.xjyzs.shortcuts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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

class CppRunner : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShortcutsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    FileExplorer()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FileExplorer() {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val directoryFile = File(context.filesDir.absolutePath + "/directories")
    var directories = arrayOf(Environment.getExternalStorageDirectory().path)
    if (directoryFile.exists()) {
        directories = directoryFile.readLines().toTypedArray()
    }
    val pref = context.getSharedPreferences("main", Context.MODE_PRIVATE)
    var options by remember { mutableStateOf(pref.getString("options", "-std=c++26")!!) }
    var testExamples by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:com.xjyzs.shortcuts".toUri()
                context.startActivity(intent)
                (context as ComponentActivity).finish()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    1001
                )
            }
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text("C++") },
                navigationIcon = {
                    IconButton(
                        onClick = { (context as ComponentActivity).finish() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            )
        }, modifier = Modifier.padding(horizontal = 8.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            var exampleExpanded by remember { mutableStateOf(false) }

            Row(
                Modifier
                    .padding(top = 10.dp, bottom = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { exampleExpanded = !exampleExpanded }) {
                Row(Modifier.padding(12.dp)) {
                    AnimatedContent(
                        targetState = exampleExpanded,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                        }, modifier = Modifier.weight(1f)
                    ) { isExpanded ->
                        if (isExpanded) {
                            TextField(
                                testExamples,
                                { testExamples = it },
                                maxLines = 8,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 6.dp)
                            )
                        } else {
                            Text(
                                "测试样例: $testExamples",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 20.sp,
                                maxLines = 1
                            )
                        }
                    }
                    val exampleExpandedRotation by animateFloatAsState(
                        targetValue = if (exampleExpanded) 180f else 0f, animationSpec = tween(
                            durationMillis = 300, easing = FastOutSlowInEasing
                        )
                    )
                    IconButton({ exampleExpanded = !exampleExpanded }, Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.ExpandMore,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.rotate(exampleExpandedRotation)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Row(
                    Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .clickable {
                            Runtime.getRuntime().exec(
                                arrayOf(
                                    "su",
                                    "-c",
                                    "am start -W com.termux/com.termux.HomeActivity;input text 'cd /storage/emulated/0/\n';am start -f 0x20000000 com.xjyzs.shortcuts/com.xjyzs.shortcuts.CppRunner"
                                )
                            )
                        }, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "cd /sdcard/", fontSize = 24.sp, modifier = Modifier.padding(start = 12.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp).padding(end = 8.dp)
                    )
                }
            }
            for (i in directories) {
                var dir = i
                if (i.takeLast(1) == "/") {
                    dir = dir.dropLast(1)
                }
                val fileList = getFileList(dir, ".cpp")
                Text(
                    dir,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp)
                )
                Column(Modifier.clip(RoundedCornerShape(12.dp))) {
                    for (j in 0..<fileList.size) {
                        Button(
                            {
                                Runtime.getRuntime().exec(
                                    arrayOf(
                                        "su",
                                        "-c",
                                        "am start -W com.termux/com.termux.HomeActivity;input text 'clang++ $options \"${dir}/${fileList[j]}.cpp\"\n./a.out\n${
                                            if (testExamples.isNotEmpty()) testExamples + if (!testExamples.endsWith(
                                                    "\n"
                                                )
                                            ) "\n" else "" else ""
                                        }'"
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                Color.Transparent,
                                LocalContentColor.current
                            ),
                            shape = RectangleShape,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .height(48.dp), verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(fileList[j], fontSize = 24.sp, fontWeight = FontWeight.Normal, modifier = Modifier.padding(start = 12.dp))
                            }
                        }
                        if (j < fileList.size - 1) {
                            Spacer(Modifier.size(2.dp))
                        }
                    }
                }
            }
        }
    }
}