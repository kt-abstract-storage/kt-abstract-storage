package io.github.ktabstractstorage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * A closeable object which can notify of changes to a folder.
 */
interface FolderWatcher : AutoCloseable {
    /**
     * Gets the folder being watched for changes.
     */
    val folder: MutableFolder

    /**
     * A stream of structured change notifications for the watched [folder].
     */
    val changes: Flow<FolderChange>

    /**
     * A stream of items that were changed in the watched [folder].
     *
     * This is a convenience view over [changes] when only the affected
     * [Storable] values are needed.
     */
    val updatedStorables: Flow<Storable>
        get() = changes.map { it.item }

    /**
     * Asynchronously closes this watcher.
     */
    suspend fun closeAsync() = withContext(Dispatchers.IO) { close() }
}
