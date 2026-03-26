package com.itswin11.ktabstractstorage.extensions

import com.itswin11.ktabstractstorage.ChildFile
import com.itswin11.ktabstractstorage.ChildFolder
import com.itswin11.ktabstractstorage.Folder
import com.itswin11.ktabstractstorage.Storable
import com.itswin11.ktabstractstorage.StorableChild
import com.itswin11.ktabstractstorage.enums.StorableType
import com.itswin11.ktabstractstorage.extensions.interfaces.GetFirstByName
import com.itswin11.ktabstractstorage.extensions.interfaces.GetItem
import com.itswin11.ktabstractstorage.extensions.interfaces.GetItemRecursive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import java.io.FileNotFoundException

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
 * @throws FileNotFoundException if no item with [id] exists in this folder.
 */
suspend fun Folder.getItemAsync(id: String): StorableChild {
    if (this is GetItem) {
        return getItemAsync(id)
    }

    val targetItem = getItemsAsync(StorableType.ALL).firstOrNull { it.id == id }
    return targetItem ?: throw FileNotFoundException("No storage item with the id '$id' could be found.")
}

/**
 * Recursively crawls this folder tree for an item with the provided [id].
 *
 * @param id Identifier of the child item to locate recursively.
 *
 * @throws FileNotFoundException if no item with [id] exists in this folder tree.
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
    } catch (_: FileNotFoundException) {
        // Continue recursively through subfolders.
    }

    var found: StorableChild? = null
    getFoldersAsync().collect { subFolder ->
        if (found != null) {
            return@collect
        }

        found = try {
            subFolder.getItemRecursiveAsync(id)
        } catch (_: FileNotFoundException) {
            null
        }
    }

    if (found != null) {
        return found!!
    }

    throw FileNotFoundException("No storage item with the id '$id' could be found.")
}

/**
 * Retrieves the first child item that has the provided [name].
 *
 * @param name Name of the child item to locate.
 *
 * @throws FileNotFoundException if no item with [name] exists in this folder.
 */
suspend fun Folder.getFirstByNameAsync(name: String): StorableChild {
    if (this is GetFirstByName) {
        return getFirstByNameAsync(name)
    }

    val targetItem = getItemsAsync(StorableType.ALL).firstOrNull { it.name == name }
    return targetItem ?: throw FileNotFoundException("No storage item with the name '$name' could be found.")
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
        is com.itswin11.ktabstractstorage.File -> "/${pathComponents.joinToString("/")}"
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


