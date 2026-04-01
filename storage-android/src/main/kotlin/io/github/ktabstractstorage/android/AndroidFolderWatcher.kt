package io.github.ktabstractstorage.android

import io.github.ktabstractstorage.FolderChange
import io.github.ktabstractstorage.FolderWatcher
import io.github.ktabstractstorage.MutableFolder
import io.github.ktabstractstorage.SimpleStorableItem
import io.github.ktabstractstorage.enums.FolderChangeType
import android.os.FileObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Watches an [AndroidFolder] for filesystem changes using Android's [FileObserver].
 *
 * @param watchedFolder The folder to watch.
 */
class AndroidFolderWatcher internal constructor(
    private val watchedFolder: AndroidFolder,
) : FolderWatcher {

    private val isClosed = AtomicBoolean(false)
    private val mutableChanges = MutableSharedFlow<FolderChange>(extraBufferCapacity = 64)

    override val folder: MutableFolder get() = watchedFolder

    override val changes: Flow<FolderChange> = mutableChanges.asSharedFlow()

    @Suppress("DEPRECATION")
    private val observer = object : FileObserver(
        watchedFolder.file.absolutePath,
        FileObserver.CREATE or FileObserver.DELETE or FileObserver.MODIFY
            or FileObserver.MOVED_FROM or FileObserver.MOVED_TO,
    ) {
        override fun onEvent(event: Int, path: String?) {
            if (isClosed.get() || path == null) return

            val childFile = File(watchedFolder.file, path)
            val maskedEvent = event and FileObserver.ALL_EVENTS
            val changeType = when (maskedEvent) {
                FileObserver.CREATE, FileObserver.MOVED_TO -> FolderChangeType.ADDED
                FileObserver.DELETE, FileObserver.MOVED_FROM -> FolderChangeType.REMOVED
                FileObserver.MODIFY -> FolderChangeType.UPDATED
                else -> return
            }

            val storable = when {
                childFile.isDirectory -> AndroidFolder.createUnvalidated(childFile)
                childFile.isFile -> AndroidFile.createUnvalidated(childFile)
                else -> SimpleStorableItem(
                    id = runCatching { childFile.canonicalPath }.getOrElse { childFile.absolutePath },
                    name = path,
                )
            }

            mutableChanges.tryEmit(FolderChange(changeType, storable))
        }
    }

    init {
        observer.startWatching()
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            observer.stopWatching()
        }
    }
}


