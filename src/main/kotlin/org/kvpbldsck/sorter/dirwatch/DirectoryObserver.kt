package org.kvpbldsck.sorter.dirwatch

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*

private class DirectoryEventFactory(val rootDirectory: Path, val isRecursive: Boolean): ObservableOnSubscribe<FileEvent> {

    companion object {
        val log: Logger = LoggerFactory.getLogger(DirectoryEventFactory::class.java)
    }

    val watchService: WatchService = rootDirectory.fileSystem.newWatchService()
    val watchKeys: MutableSet<WatchKey> = mutableSetOf()
    val observablePaths: MutableSet<Path> = mutableSetOf()

    init {
        if (isRecursive)
            registerDirectoryTree()
        else
            registerDirectory(rootDirectory)
    }

    private fun registerDirectory(directory: Path) {
        try {
            log.debug("register new directory $directory")
            watchKeys.add(directory.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE))
            observablePaths.add(directory)
        } catch (e: IOException) {
            log.warn("Cannot register directory $directory")
            log.warn("", e)
        }
    }

    private fun registerDirectoryTree() {
        try {
            Files.walk(rootDirectory)
                 .filter { Files.isDirectory(it) }
                 .forEach { registerDirectory(it) }
        } catch (e: IOException) {
            log.error("Cannot walk tree begins in $rootDirectory")
            log.error("", e)
        }
    }

    private fun unregisterDirectoryTree(rootDirectory: Path) {
        println("Unregister directory tree $rootDirectory")
        for (key in watchKeys) {
            val path = key.watchable()
            if (path is Path)
                if (path.startsWith(rootDirectory)) {
                    key.cancel()
                    watchKeys.remove(key)
                    observablePaths.remove(path)
                }
        }
    }

    override fun subscribe(emitter: ObservableEmitter<FileEvent>) {
        val currThread = Thread.currentThread()

        emitter.setCancellable(currThread::interrupt)

        try {
            while (watchKeys.isNotEmpty()) {
                val watchKey = watchService.take()

                log.debug("New watch key at ${watchKey.watchable()}")

                for (event in watchKey.pollEvents()) {

                    if (event.kind() == OVERFLOW)
                        continue

                    val castedEvent = event as WatchEvent<Path>
                    val watchedDirectory = watchKey.watchable() as Path

                    val fileEvent = FileEvent(castedEvent.kind(),
                                              watchedDirectory.resolve(event.context ()))

                    if (fileEvent.isDirectoryCreated())
                        registerDirectory(fileEvent.filePath)

                    if ((fileEvent.filePath in observablePaths) && (ENTRY_DELETE == fileEvent.kind))
                        unregisterDirectoryTree(fileEvent.filePath)

                    emitter.onNext(fileEvent)
                }

                if (!watchKey.reset()) {
                    log.debug("Cannot reset watch key")
                    watchKeys.remove(watchKey)
                    observablePaths.remove(watchKey.watchable())
                } else
                    log.debug("Release last watch key")
            }
        } catch (ignored: InterruptedException) {
        } finally {
            log.debug("Stop observe directory")

            emitter.onComplete()
            watchKeys.forEach(WatchKey::cancel)
            watchKeys.clear()

            try {
                watchService.close()
            } catch (e: IOException) {
                log.warn("Cannot close watch service")
                log.warn("", e)
            }
        }
    }

}

fun createDirectoryObserver(directory: Path, isRecursive: Boolean = true): Observable<FileEvent> {
    if (!Files.isDirectory(directory))
        throw DirectoryObserverException("Path $directory not a directory")

    return Observable.create(DirectoryEventFactory(directory, isRecursive))
}