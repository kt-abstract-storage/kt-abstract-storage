package io.github.ktabstractstorage.extensions

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.ChildFolder
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.ModifiableFolder
import io.github.ktabstractstorage.Storable
import io.github.ktabstractstorage.StorableChild
import io.github.ktabstractstorage.enums.StorableType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Creates an item by [relativePath] starting from this folder.
 *
 * @param relativePath Path to create relative to this folder.
 * @param targetType The terminal item type to create.
 * @param overwrite Whether to overwrite existing terminal item when supported.
 */
suspend fun Folder.createByRelativePathAsync(
    relativePath: String,
    targetType: StorableType,
    overwrite: Boolean = false,
): Storable = createByRelativePathCoreAsync(this, relativePath, targetType, overwrite)

/**
 * Creates an item by [relativePath] starting from this child file.
 *
 * @param relativePath Path to create relative to this file's parent chain.
 * @param targetType The terminal item type to create.
 * @param overwrite Whether to overwrite existing terminal item when supported.
 */
suspend fun ChildFile.createByRelativePathAsync(
    relativePath: String,
    targetType: StorableType,
    overwrite: Boolean = false,
): Storable = createByRelativePathCoreAsync(this, relativePath, targetType, overwrite)

/**
 * Creates a folder by [relativePath] starting from this folder.
 *
 * @param relativePath Path to create relative to this folder.
 * @param overwrite Whether to overwrite the terminal item when supported.
 */
suspend fun Folder.createFolderByRelativePathAsync(
    relativePath: String,
    overwrite: Boolean = false,
): ChildFolder = createByRelativePathAsync(relativePath, StorableType.FOLDER, overwrite) as ChildFolder

/**
 * Creates a folder by [relativePath] starting from this child file.
 *
 * @param relativePath Path to create relative to this file's parent chain.
 * @param overwrite Whether to overwrite the terminal item when supported.
 */
suspend fun ChildFile.createFolderByRelativePathAsync(
    relativePath: String,
    overwrite: Boolean = false,
): ChildFolder = createByRelativePathAsync(relativePath, StorableType.FOLDER, overwrite) as ChildFolder

/**
 * Creates a file by [relativePath] starting from this folder.
 *
 * @param relativePath Path to create relative to this folder.
 * @param overwrite Whether to overwrite the terminal file when supported.
 */
suspend fun Folder.createFileByRelativePathAsync(
    relativePath: String,
    overwrite: Boolean = false,
): ChildFile = createByRelativePathAsync(relativePath, StorableType.FILE, overwrite) as ChildFile

/**
 * Creates a file by [relativePath] starting from this child file.
 *
 * @param relativePath Path to create relative to this file's parent chain.
 * @param overwrite Whether to overwrite the terminal file when supported.
 */
suspend fun ChildFile.createFileByRelativePathAsync(
    relativePath: String,
    overwrite: Boolean = false,
): ChildFile = createByRelativePathAsync(relativePath, StorableType.FILE, overwrite) as ChildFile

/**
 * Creates folders along [relativePath] and emits each created or resolved folder.
 *
 * @param relativePath Path to create relative to this folder.
 * @param overwrite Whether to overwrite an existing terminal item when supported.
 */
fun Folder.createFoldersAlongRelativePathAsync(
    relativePath: String,
    overwrite: Boolean = false,
): Flow<Folder> = flow {
    val folderOnlyPath = normalizeFolderOnlyRelativePath(relativePath)
    createAlongRelativePathCoreAsync(this@createFoldersAlongRelativePathAsync, folderOnlyPath, StorableType.FOLDER, overwrite)
        .collect { if (it is Folder) emit(it) }
}

/**
 * Creates folders along [relativePath] and emits each created or resolved folder.
 *
 * @param relativePath Path to create relative to this file's parent chain.
 * @param overwrite Whether to overwrite an existing terminal item when supported.
 */
fun ChildFile.createFoldersAlongRelativePathAsync(
    relativePath: String,
    overwrite: Boolean = false,
): Flow<Folder> = flow {
    val folderOnlyPath = normalizeFolderOnlyRelativePath(relativePath)
    createAlongRelativePathCoreAsync(this@createFoldersAlongRelativePathAsync, folderOnlyPath, StorableType.FOLDER, overwrite)
        .collect { if (it is Folder) emit(it) }
}

/**
 * Creates items along [relativePath] and emits each created or resolved item.
 *
 * @param relativePath Path to create relative to this folder.
 * @param targetType The terminal item type to create.
 * @param overwrite Whether to overwrite an existing terminal item when supported.
 */
fun Folder.createAlongRelativePathAsync(
    relativePath: String,
    targetType: StorableType,
    overwrite: Boolean = false,
): Flow<Storable> = createAlongRelativePathCoreAsync(this, relativePath, targetType, overwrite)

/**
 * Creates items along [relativePath] and emits each created or resolved item.
 *
 * @param relativePath Path to create relative to this file's parent chain.
 * @param targetType The terminal item type to create.
 * @param overwrite Whether to overwrite an existing terminal item when supported.
 */
fun ChildFile.createAlongRelativePathAsync(
    relativePath: String,
    targetType: StorableType,
    overwrite: Boolean = false,
): Flow<Storable> = createAlongRelativePathCoreAsync(this, relativePath, targetType, overwrite)

private suspend fun createByRelativePathCoreAsync(
    from: Storable,
    relativePath: String,
    targetType: StorableType,
    overwrite: Boolean,
): Storable {
    var current = from
    val normalized = relativePath.replace('\\', '/')
    val parts = normalized.split('/').map { it.trim() }.filter { it.isNotEmpty() }

    require(targetType == StorableType.FILE || targetType == StorableType.FOLDER) {
        "Only FILE and FOLDER target types are supported."
    }

    if (targetType == StorableType.FILE) {
        require(!normalized.endsWith('/')) { "File target cannot end with '/'." }
        require(parts.isNotEmpty()) { "File target requires a non-empty path." }
    }

    val lastIndex = if (targetType == StorableType.FILE) parts.lastIndex else parts.lastIndex

    for (index in parts.indices) {
        val segment = parts[index]
        val isLast = index == lastIndex

        if (segment == ".") {
            continue
        }

        if (segment == "..") {
            val child = current as? StorableChild
                ?: throw IllegalArgumentException("A parent folder was requested, but '${current.name}' is not addressable.")
            current = child.getParentAsync()
                ?: throw IllegalArgumentException("A parent folder was requested, but '${current.name}' did not return a parent.")
            continue
        }

        val folder = current as? ModifiableFolder
            ?: throw IllegalArgumentException("'${current.name}' is not a modifiable folder and cannot contain '$segment'.")

        current = if (targetType == StorableType.FILE && isLast) {
            folder.createFileAsync(segment, overwrite)
        } else {
            folder.createFolderAsync(segment, overwrite)
        }
    }

    return current
}

private fun createAlongRelativePathCoreAsync(
    from: Storable,
    relativePath: String,
    targetType: StorableType,
    overwrite: Boolean,
): Flow<Storable> = flow {
    var current = from
    val normalized = relativePath.replace('\\', '/')
    val parts = normalized.split('/').map { it.trim() }.filter { it.isNotEmpty() }

    require(targetType == StorableType.FILE || targetType == StorableType.FOLDER) {
        "Only FILE and FOLDER target types are supported."
    }

    if (targetType == StorableType.FILE) {
        require(!normalized.endsWith('/')) { "File target cannot end with '/'." }
        require(parts.isNotEmpty()) { "File target requires a non-empty path." }
    }

    for (index in parts.indices) {
        val segment = parts[index]
        val isLast = index == parts.lastIndex

        if (segment == ".") {
            continue
        }

        if (segment == "..") {
            val child = current as? StorableChild
                ?: throw IllegalArgumentException("A parent folder was requested, but '${current.name}' is not addressable.")
            val parent = child.getParentAsync()
                ?: throw IllegalArgumentException("A parent folder was requested, but '${current.name}' did not return a parent.")
            current = parent
            emit(parent)
            continue
        }

        val folder = current as? ModifiableFolder
            ?: throw IllegalArgumentException("'${current.name}' is not a modifiable folder and cannot contain '$segment'.")

        val next = if (targetType == StorableType.FILE && isLast) {
            folder.createFileAsync(segment, overwrite)
        } else {
            folder.createFolderAsync(segment, overwrite)
        }

        current = next
        emit(next)
    }

    if (parts.isEmpty() && current is Folder) {
        emit(current)
    }
}

private fun normalizeFolderOnlyRelativePath(relativePath: String): String {
    val normalized = relativePath.replace('\\', '/').trim()
    val parts = normalized.split('/').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    if (parts.isNotEmpty()) {
        val last = parts.last()
        if (last != "." && last != ".." && last.contains('.')) {
            parts.removeAt(parts.lastIndex)
        }
    }
    return parts.joinToString("/")
}

