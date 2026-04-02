package io.github.ktabstractstorage.memory

import io.github.ktabstractstorage.FolderChange
import io.github.ktabstractstorage.FolderWatcher
import io.github.ktabstractstorage.MutableFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.concurrent.locks.ReentrantLock
import kotlin.concurrent.locks.withLock

/**
 * Watches a [MemoryFolder] for changes.
 *
 * @param watchedFolder The folder being watched.
 */
class MemoryFolderWatcher internal constructor(
    private val watchedFolder: MemoryFolder,
) : FolderWatcher {
    private var isClosed = false
    private val lock = ReentrantLock()
    private val mutableChanges = MutableSharedFlow<FolderChange>(extraBufferCapacity = 32)

    override val folder: MutableFolder
        get() = watchedFolder

    override val changes: Flow<FolderChange> = mutableChanges.asSharedFlow()

    internal fun emit(change: FolderChange) {
        if (!isClosed) {
            mutableChanges.tryEmit(change)
        }
    }

    override fun close() {
        lock.withLock {
            if (isClosed) {
                return
            }
            isClosed = true
        }
        watchedFolder.unregisterWatcher(this)
    }
}

