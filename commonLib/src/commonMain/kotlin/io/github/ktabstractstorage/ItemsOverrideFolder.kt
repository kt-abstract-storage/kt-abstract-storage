package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.StorableType
import kotlinx.coroutines.flow.Flow

/**
 * Overrides the items returned by a wrapped [Folder]'s [Folder.getItemsAsync].
 *
 * @param folder The folder being overridden.
 * @param itemsOverrideFunc The function used to override the items yielded by [getItemsAsync].
 */
class ItemsOverrideFolder(
    val folder: Folder,
    val itemsOverrideFunc: (Flow<StorableChild>) -> Flow<StorableChild>,
) : Folder {
    override val id: String
        get() = folder.id

    override val name: String
        get() = folder.name

    override fun getItemsAsync(type: StorableType): Flow<StorableChild> =
        itemsOverrideFunc(folder.getItemsAsync(type))
}
