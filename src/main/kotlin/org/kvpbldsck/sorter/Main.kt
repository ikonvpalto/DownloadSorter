package org.kvpbldsck.sorter

import io.reactivex.schedulers.Schedulers
import org.apache.tika.metadata.Metadata
import java.nio.file.Paths

import org.kvpbldsck.sorter.dirwatch.createDirectoryObserver
import org.kvpbldsck.sorter.common.*
import java.nio.file.Path

import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.BodyContentHandler
import java.io.FileInputStream

import java.nio.file.StandardWatchEventKinds.*

fun main(args: Array<String>) {
    val dir = Paths.get("D:", "tmp")
    val directoryEventObservable = createDirectoryObserver(dir, true)
            .subscribeOn(Schedulers.io())
            .filter { ENTRY_CREATE == it.kind }
            .filter { it.filePath.isAudioFile() }
    val disposable = directoryEventObservable.subscribe(
            {
                println(it)
                println(it.filePath.getMimeType())
            },
            { it.printStackTrace() },
            { println("On complete") }
    )

    readLine()
    disposable.dispose()
}

fun getMetadata(path: Path) {
    val parser = AutoDetectParser()
    val bodyContentHandler = BodyContentHandler()
    val metadata = Metadata()
    val parseContext = ParseContext()
    val fin = FileInputStream(path.toFile())

    parser.parse(fin, bodyContentHandler, metadata, parseContext)


}