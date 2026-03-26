package com.itswin11.ktabstractstorage.ziparchive

import com.itswin11.ktabstractstorage.FolderChange
import com.itswin11.ktabstractstorage.FolderWatcher
import com.itswin11.ktabstractstorage.MutableFolder
import com.itswin11.ktabstractstorage.SimpleStorableItem
import com.itswin11.ktabstractstorage.enums.FolderChangeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean

internal data class ZipArchiveMutation(
    val type: FolderChangeType,
    val path: String,
    val isFolder: Boolean,
)

internal class ZipArchiveSharedState(
    val store: ZipArchiveStore,
    val mutations: MutableSharedFlow<ZipArchiveMutation> = MutableSharedFlow(extraBufferCapacity = 64),
)

class ZipArchiveFolderWatcher internal constructor(
    private val watchedFolder: ZipArchiveFolder,
    private val watchedPath: String,
) : FolderWatcher {
    private val isClosed = AtomicBoolean(false)

    override val folder: MutableFolder
        get() = watchedFolder

    override val changes: Flow<FolderChange> = watchedFolder.sharedState.mutations
        .filter { !isClosed.get() && watchedFolder.parentPath(it.path) == watchedPath }
        .map { mutation ->
            FolderChange(
                type = mutation.type,
                item = SimpleStorableItem(
                    id = watchedFolder.createEntryId(mutation.path, mutation.isFolder),
                    name = mutation.path.substringAfterLast('/'),
                ),
            )
        }

    override fun close() {
        isClosed.set(true)
    }
}

