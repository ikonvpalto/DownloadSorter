package org.kvpbldsck.sorter.common

import java.nio.file.Files
import java.nio.file.Path

fun Path.isDirectory() = Files.isDirectory(this)

fun Path.isFile() = Files.isRegularFile(this)

fun Path.isAudioFile() = isFile() && getMimeType().startsWith("audio")

fun Path.getMimeType(): String {
    return Files.probeContentType(this)
}