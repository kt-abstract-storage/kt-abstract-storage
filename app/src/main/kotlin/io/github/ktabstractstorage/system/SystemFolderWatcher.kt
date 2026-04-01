package io.github.ktabstractstorage.system

import io.github.ktabstractstorage.FolderChange
import io.github.ktabstractstorage.FolderWatcher
import io.github.ktabstractstorage.MutableFolder
import io.github.ktabstractstorage.SimpleStorableItem
import io.github.ktabstractstorage.enums.FolderChangeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Watches a [SystemFolder] for filesystem changes.
 *
 * @param watchedFolder The folder being watched.
 */
class SystemFolderWatcher internal constructor(
    private val watchedFolder: SystemFolder,
) : FolderWatcher {
    private val isClosed = AtomicBoolean(false)
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableChanges = MutableSharedFlow<FolderChange>(extraBufferCapacity = 64)

    override val folder: MutableFolder
        get() = watchedFolder

    override val changes: Flow<FolderChange> = mutableChanges.asSharedFlow()

    init {
        watchedFolder.path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        scope.launch {
            processEvents()
        }
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            watchService.close()
            scope.cancel()
        }
    }

    private suspend fun processEvents() {
        while (scope.isActive && !isClosed.get()) {
            val key = try {
                withContext(Dispatchers.IO) { watchService.take() }
            } catch (_: ClosedWatchServiceException) {
                break
            }

            for (event in key.pollEvents()) {
                if (event.kind() == OVERFLOW) continue

                val relativePath = event.context() as? Path ?: continue
                val childPath = watchedFolder.path.resolve(relativePath).toAbsolutePath().normalize()
                val type = when (event.kind()) {
                    ENTRY_CREATE -> FolderChangeType.ADDED
                    ENTRY_DELETE -> FolderChangeType.REMOVED
                    ENTRY_MODIFY -> FolderChangeType.UPDATED
                    else -> continue
                }

                mutableChanges.emit(FolderChange(type, createStorableSnapshot(childPath, type)))
            }

            if (!key.reset()) {
                break
            }
        }
    }

    private fun createStorableSnapshot(path: Path, type: FolderChangeType) = when {
        type == FolderChangeType.REMOVED || !Files.exists(path) -> SimpleStorableItem(
            id = path.toString(),
            name = path.fileName?.toString() ?: path.toString(),
        )

        Files.isDirectory(path) -> SystemFolder.createUnvalidated(path)
        else -> SystemFile.createUnvalidated(path)
    }
}

