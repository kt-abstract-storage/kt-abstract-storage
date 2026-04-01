package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.StorableType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A [Folder] wrapper that recursively traverses all descendants using depth-first search.
 *
 * @param rootFolder The root folder to traverse.
 * @param maxDepth Optional depth limit where direct children of [rootFolder] are depth 1.
 */
class DepthFirstRecursiveFolder(
    val rootFolder: Folder,
    val maxDepth: Int? = null,
) : Folder {
    init {
        require(maxDepth == null || maxDepth >= 1) {
            "maxDepth must be null or greater than or equal to 1."
        }
    }

    override val id: String
        get() = rootFolder.id

    override val name: String
        get() = rootFolder.name

    override fun getItemsAsync(type: StorableType): Flow<StorableChild> = flow {
        require(type != StorableType.NONE) {
            "StorableType.NONE is invalid when enumerating folder contents."
        }

        suspend fun traverse(folder: Folder, depth: Int) {
            folder.getItemsAsync(StorableType.ALL).collect { item ->
                if (matchesType(item, type)) {
                    emit(item)
                }

                if (item is ChildFolder && (maxDepth == null || depth < maxDepth)) {
                    traverse(item, depth + 1)
                }
            }
        }

        traverse(rootFolder, 1)
    }

    private fun matchesType(item: StorableChild, type: StorableType): Boolean = when (type) {
        StorableType.ALL -> true
        StorableType.FILE -> item is ChildFile
        StorableType.FOLDER -> item is ChildFolder
        StorableType.NONE -> false
    }
}
