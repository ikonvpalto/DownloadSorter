package org.kvpbldsck.sorter.dirwatch

import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent

import org.kvpbldsck.sorter.common.*

data class FileEvent(val kind: WatchEvent.Kind<Path>, val filePath: Path) {

    fun getObservableDirectory() = filePath.root

    fun isEntryCreated() = ENTRY_CREATE == kind

    fun isEntryModify() = ENTRY_MODIFY == kind

    fun isEntryDelete() = ENTRY_DELETE == kind

    fun isDirectoryCreated() = isEntryCreated() && filePath.isDirectory()

    fun isDirectoryDeleted() = isEntryDelete() && filePath.isDirectory()

}