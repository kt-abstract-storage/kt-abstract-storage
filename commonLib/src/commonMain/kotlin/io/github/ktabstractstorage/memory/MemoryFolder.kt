package io.github.ktabstractstorage.memory

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.ChildFolder
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.FolderChange
import io.github.ktabstractstorage.ModifiableFolder
import io.github.ktabstractstorage.StorableChild
import io.github.ktabstractstorage.enums.FolderChangeType
import io.github.ktabstractstorage.enums.StorableType
import io.github.ktabstractstorage.errors.StorageFileAlreadyExistsException
import io.github.ktabstractstorage.errors.StorageFileNotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.concurrent.locks.ReentrantLock
import kotlin.concurrent.locks.withLock
import kotlin.random.Random

/**
 * An in-memory implementation of a modifiable folder.
 *
 * @param name The display name of the folder.
 * @param parentFolder The containing folder, if any.
 * @param id Stable identifier for this folder instance.
 */
class MemoryFolder(
    override val name: String,
    private var parentFolder: MemoryFolder? = null,
    override val id: String = Random.nextLong().toString(),
) : ModifiableFolder, ChildFolder {
    private val children = linkedMapOf<String, StorableChild>()
    private var watcher: MemoryFolderWatcher? = null
    private val lock = ReentrantLock()

    override suspend fun getParentAsync(): Folder? = parentFolder

    override fun getItemsAsync(type: StorableType): Flow<StorableChild> = flow {
        require(type != StorableType.NONE) {
            "StorableType.NONE is invalid when enumerating folder contents."
        }
        snapshotChildren(type).forEach { emit(it) }
    }

    override suspend fun getFolderWatcherAsync(): MemoryFolderWatcher = lock.withLock {
        if (watcher == null) {
            watcher = MemoryFolderWatcher(this)
        }
        watcher!!
    }

    override suspend fun deleteAsync(item: StorableChild) {
        val removed = lock.withLock {
            val entry = children.entries.firstOrNull { it.value.id == item.id }
                ?: throw StorageFileNotFoundException("Item ${item.name} was not found in folder $name")
            children.remove(entry.key)!!.also { detachChild(it) }
        }

        notifyWatchers(FolderChange(FolderChangeType.REMOVED, removed))
    }

    override suspend fun createFolderAsync(name: String, overwrite: Boolean): ChildFolder {
        val result = lock.withLock {
            when (val existing = children[name]) {
                null -> {
                    val folder = MemoryFolder(name, this)
                    children[name] = folder
                    CreateResult(folder, FolderChange(FolderChangeType.ADDED, folder))
                }

                is MemoryFolder -> CreateResult(existing, null)

                is MemoryFile -> {
                    if (!overwrite) {
                        throw StorageFileAlreadyExistsException("$name already exists as a file")
                    }

                    existing.detach()
                    val folder = MemoryFolder(name, this)
                    children[name] = folder
                    CreateResult(
                        folder,
                        listOf(
                            FolderChange(FolderChangeType.REMOVED, existing),
                            FolderChange(FolderChangeType.ADDED, folder),
                        ),
                    )
                }

                else -> throw IllegalStateException("Unsupported child type: ${existing::class.qualifiedName}")
            }
        }

        result.changes.forEach(::notifyWatchers)
        return result.child as ChildFolder
    }

    override suspend fun createFileAsync(name: String, overwrite: Boolean): ChildFile {
        val result = lock.withLock {
            when (val existing = children[name]) {
                null -> {
                    val file = MemoryFile(name, this)
                    children[name] = file
                    CreateResult(file, FolderChange(FolderChangeType.ADDED, file))
                }

                is MemoryFile -> {
                    if (overwrite) {
                        existing.clearContent()
                        CreateResult(existing, FolderChange(FolderChangeType.UPDATED, existing))
                    } else {
                        CreateResult(existing, null)
                    }
                }

                is MemoryFolder -> {
                    if (!overwrite) {
                        throw StorageFileAlreadyExistsException("$name already exists as a folder")
                    }

                    existing.detach()
                    val file = MemoryFile(name, this)
                    children[name] = file
                    CreateResult(
                        file,
                        listOf(
                            FolderChange(FolderChangeType.REMOVED, existing),
                            FolderChange(FolderChangeType.ADDED, file),
                        ),
                    )
                }

                else -> throw IllegalStateException("Unsupported child type: ${existing::class.qualifiedName}")
            }
        }

        result.changes.forEach(::notifyWatchers)
        return result.child as ChildFile
    }

    internal fun unregisterWatcher(watcher: MemoryFolderWatcher) = lock.withLock {
        if (this.watcher === watcher) {
            this.watcher = null
        }
    }

    private fun snapshotChildren(type: StorableType): List<StorableChild> = lock.withLock {
        require(type != StorableType.NONE) {
            "StorableType.NONE is invalid when enumerating folder contents."
        }

        children.values.filter { child ->
            when (type) {
                StorableType.ALL -> true
                StorableType.FILE -> child is ChildFile
                StorableType.FOLDER -> child is ChildFolder
                StorableType.NONE -> error("StorableType.NONE must be rejected before enumeration.")
            }
        }
    }

    private fun notifyWatchers(change: FolderChange) {
        lock.withLock {
            watcher
        }?.emit(change)
    }

    private fun detachChild(child: StorableChild) {
        when (child) {
            is MemoryFile -> child.detach()
            is MemoryFolder -> child.detach()
        }
    }

    private fun detach() {
        parentFolder = null
    }

    private data class CreateResult(
        val child: StorableChild,
        val changes: List<FolderChange>,
    ) {
        constructor(child: StorableChild, change: FolderChange?) : this(
            child,
            change?.let(::listOf).orEmpty(),
        )
    }
}
