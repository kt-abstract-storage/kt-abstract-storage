package io.github.ktabstractstorage.extensions

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.ChildFolder
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.Storable
import io.github.ktabstractstorage.StorableChild
import io.github.ktabstractstorage.enums.StorableType
import io.github.ktabstractstorage.errors.StorageFileNotFoundException
import io.github.ktabstractstorage.extensions.interfaces.GetFirstByName
import io.github.ktabstractstorage.extensions.interfaces.GetItem
import io.github.ktabstractstorage.extensions.interfaces.GetItemRecursive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

/**
 * Retrieves files in this folder.
 */
fun Folder.getFilesAsync(): Flow<ChildFile> =
    getItemsAsync(StorableType.FILE).filterIsInstance<ChildFile>()

/**
 * Retrieves folders in this folder.
 */
fun Folder.getFoldersAsync(): Flow<ChildFolder> =
    getItemsAsync(StorableType.FOLDER).filterIsInstance<ChildFolder>()

/**
 * Retrieves the child item that has the provided [id].
 *
 * @param id Identifier of the child item to locate.
 *
 * @throws StorageFileNotFoundException if no item with [id] exists in this folder.
 */
suspend fun Folder.getItemAsync(id: String): StorableChild {
    if (this is GetItem) {
        return getItemAsync(id)
    }

    val targetItem = getItemsAsync(StorableType.ALL).firstOrNull { it.id == id }
    return targetItem ?: throw StorageFileNotFoundException("No storage item with the id '$id' could be found.")
}

/**
 * Recursively crawls this folder tree for an item with the provided [id].
 *
 * @param id Identifier of the child item to locate recursively.
 *
 * @throws StorageFileNotFoundException if no item with [id] exists in this folder tree.
 */
suspend fun Folder.getItemRecursiveAsync(id: String): StorableChild {
    if (this is GetItemRecursive) {
        val item = getItemRecursiveAsync(id)
        require(item.id == id) {
            "Fast-path getItemRecursiveAsync returned an unexpected item id. Expected '$id', actual '${item.id}'."
        }
        return item
    }

    try {
        return getItemAsync(id)
    } catch (_: StorageFileNotFoundException) {
        // Continue recursively through subfolders.
    }

    var found: StorableChild? = null
    getFoldersAsync().collect { subFolder ->
        if (found != null) {
            return@collect
        }

        found = try {
            subFolder.getItemRecursiveAsync(id)
        } catch (_: StorageFileNotFoundException) {
            null
        }
    }

    if (found != null) {
        return found!!
    }

    throw StorageFileNotFoundException("No storage item with the id '$id' could be found.")
}

/**
 * Retrieves the first child item that has the provided [name].
 *
 * @param name Name of the child item to locate.
 *
 * @throws StorageFileNotFoundException if no item with [name] exists in this folder.
 */
suspend fun Folder.getFirstByNameAsync(name: String): StorableChild {
    if (this is GetFirstByName) {
        return getFirstByNameAsync(name)
    }

    val targetItem = getItemsAsync(StorableType.ALL).firstOrNull { it.name == name }
    return targetItem ?: throw StorageFileNotFoundException("No storage item with the name '$name' could be found.")
}

/**
 * Computes a relative path from this folder to [to].
 *
 * @param to Destination item for which the relative path is calculated.
 */
suspend fun Folder.getRelativePathToAsync(to: StorableChild): String {
    if (this.id == to.id) {
        return "/"
    }

    val pathComponents = mutableListOf(to.name)
    var current: StorableChild? = to

    while (current != null) {
        val parent = current.getParentAsync() ?: break
        if (parent.id == this.id) {
            break
        }

        pathComponents.add(0, parent.name)
        current = parent as? StorableChild
    }

    return when (to) {
        is Folder -> "/${pathComponents.joinToString("/")}/"
        is io.github.ktabstractstorage.File -> "/${pathComponents.joinToString("/")}"
        else -> throw UnsupportedOperationException(
            "${to::class.qualifiedName} is not a file or folder. Unable to generate a path.",
        )
    }
}

/**
 * Yields items along the relative path from this folder to [to], excluding this folder.
 *
 * @param to Destination item whose path chain will be emitted.
 */
fun Folder.getItemsAlongRelativePathToAsync(to: StorableChild): Flow<Storable> = flow {
    if (this@getItemsAlongRelativePathToAsync.id == to.id) {
        return@flow
    }

    val chain = mutableListOf<Storable>()
    var current: StorableChild? = to

    while (current != null) {
        chain += current

        val parent = current.getParentAsync() ?: break
        if (parent.id == this@getItemsAlongRelativePathToAsync.id) {
            break
        }

        current = parent as? StorableChild
    }

    for (i in chain.lastIndex downTo 0) {
        emit(chain[i])
    }
}


