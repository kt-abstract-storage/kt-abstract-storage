package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.StorableType
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

/**
 * A [Folder] wrapper that recursively traverses all descendants using breadth-first search.
 *
 * @param rootFolder The root folder to traverse.
 * @param maxDepth Optional depth limit where direct children of [rootFolder] are depth 1.
 */
class BreadthFirstRecursiveFolder(
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

        if (maxDepth == 0) {
            return@flow
        }

        val queue = ArrayDeque<Pair<Folder, Int>>()
        queue.addLast(rootFolder to 1)

        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()

            val (folder, depth) = queue.removeFirst()
            val children = folder.getItemsAsync(StorableType.ALL).toList()

            for (item in children) {
                if (matchesType(item, type)) {
                    emit(item)
                }

                if (item is ChildFolder && (maxDepth == null || depth < maxDepth)) {
                    queue.addLast(item to (depth + 1))
                }
            }
        }
    }

    private fun matchesType(item: StorableChild, type: StorableType): Boolean = when (type) {
        StorableType.ALL -> true
        StorableType.FILE -> item is ChildFile
        StorableType.FOLDER -> item is ChildFolder
        StorableType.NONE -> false
    }
}

