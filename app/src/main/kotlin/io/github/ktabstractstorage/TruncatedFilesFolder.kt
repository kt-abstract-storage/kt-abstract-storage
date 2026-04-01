package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.StorableType
import io.github.ktabstractstorage.extensions.getFirstByNameAsync
import io.github.ktabstractstorage.extensions.getItemAsync
import io.github.ktabstractstorage.extensions.getItemRecursiveAsync
import io.github.ktabstractstorage.extensions.interfaces.GetFirstByName
import io.github.ktabstractstorage.extensions.interfaces.GetItem
import io.github.ktabstractstorage.extensions.interfaces.GetItemRecursive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A [Folder] wrapper that limits every file it returns to [maxFileLength] bytes.
 *
 * File children are wrapped as [TruncatedFile] instances and re-parented to this
 * wrapper using [ParentOverrideChildFile]. Non-file children are returned as-is,
 * matching the original folder traversal behavior.
 *
 * @param folder The wrapped folder.
 * @param maxFileLength The maximum number of bytes exposed by wrapped child files.
 * @param parentOverride Optional parent to report instead of the wrapped folder's parent.
 */
class TruncatedFilesFolder(
    val folder: Folder,
    val maxFileLength: Long,
    private val parentOverride: Folder? = null,
) : GetItem, GetItemRecursive, GetFirstByName, ChildFolder {
    init {
        require(maxFileLength >= 0) { "maxFileLength must be non-negative." }
    }

    override val id: String
        get() = folder.id

    override val name: String
        get() = folder.name

    override suspend fun getParentAsync(): Folder? =
        parentOverride ?: (folder as? StorableChild)?.getParentAsync()

    private fun wrapChild(item: StorableChild): StorableChild = when (item) {
        is ChildFile -> ParentOverrideChildFile(
            inner = TruncatedFile(item, maxFileLength),
            parent = this,
        )
        else -> item
    }

    override fun getItemsAsync(type: StorableType): Flow<StorableChild> =
        folder.getItemsAsync(type).map(::wrapChild)

    override suspend fun getItemAsync(id: String): StorableChild =
        wrapChild(folder.getItemAsync(id))

    override suspend fun getItemRecursiveAsync(id: String): StorableChild =
        wrapChild(folder.getItemRecursiveAsync(id))

    override suspend fun getFirstByNameAsync(name: String): StorableChild =
        wrapChild(folder.getFirstByNameAsync(name))
}
