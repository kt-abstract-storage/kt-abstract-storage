package io.github.ktabstractstorage.extensions

import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.Storable
import io.github.ktabstractstorage.StorableChild
import io.github.ktabstractstorage.extensions.interfaces.GetRoot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.FileNotFoundException

/**
 * Retrieves the root folder for this item.
 */
suspend fun StorableChild.getRootAsync(): Folder? {
    if (this is GetRoot) {
        return getRootAsync()
    }

    val parent = getParentAsync() ?: return null
    val parentAsChild = parent as? StorableChild ?: return null
    return parentAsChild.getRootAsync()
}

/**
 * Resolves [relativePath] starting from this item and returns the target item.
 *
 * @param relativePath Relative path to resolve.
 */
suspend fun Storable.getItemByRelativePathAsync(relativePath: String): Storable {
    var current: Storable = this
    val pathParts = relativePath
        .replace('\\', '/')
        .split('/')
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != "." }

    if (pathParts.isEmpty()) {
        return this
    }

    for (segment in pathParts) {
        if (segment == "..") {
            val child = current as? StorableChild
                ?: throw IllegalArgumentException(
                    "A parent folder was requested, but '${current.name}' is not the child of a directory.",
                )

            current = child.getParentAsync()
                ?: throw IllegalArgumentException(
                    "A parent folder was requested, but '${current.name}' did not return a parent.",
                )
            continue
        }

        val folder = current as? Folder
            ?: throw IllegalArgumentException(
                "An item named '$segment' was requested from '${current.name}', but it is not a folder.",
            )

        current = try {
            folder.getFirstByNameAsync(segment)
        } catch (_: FileNotFoundException) {
            throw FileNotFoundException(
                "An item named '$segment' was requested from '${current.name}', but it was not found.",
            )
        }
    }

    return current
}

/**
 * Resolves [relativePath] starting from this item and yields each visited item in order.
 *
 * @param relativePath Relative path to resolve and emit along.
 */
fun Storable.getItemsAlongRelativePathAsync(relativePath: String): Flow<Storable> = flow {
    var current: Storable = this@getItemsAlongRelativePathAsync
    val parts = relativePath
        .replace('\\', '/')
        .split('/')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    for (segment in parts) {
        if (segment == ".") {
            continue
        }

        if (segment == "..") {
            val child = current as? StorableChild
                ?: throw IllegalArgumentException(
                    "A parent folder was requested, but '${current.name}' is not the child of a directory.",
                )

            val parent = child.getParentAsync()
                ?: throw IllegalArgumentException(
                    "A parent folder was requested, but '${current.name}' did not return a parent.",
                )

            current = parent
            emit(parent)
            continue
        }

        val folder = current as? Folder
            ?: throw IllegalArgumentException(
                "The item '${current.name}' is not a folder and cannot contain '$segment'.",
            )

        val next = folder.getFirstByNameAsync(segment)
        current = next
        emit(next)
    }
}

