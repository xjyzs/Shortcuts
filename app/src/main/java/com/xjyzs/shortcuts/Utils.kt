package com.xjyzs.shortcuts

import androidx.compose.runtime.mutableStateListOf
import java.io.File

fun getFileList(path: String,suffix: String): List<String> {
    val fileList = mutableStateListOf<String>()
    val directory = File(path)
    val files = directory.listFiles()
    for (file in files ?: emptyArray()) {
        if (file.name.endsWith(suffix)) {
            fileList.add(file.name.dropLast(suffix.length))
        }
    }
    return fileList
}
