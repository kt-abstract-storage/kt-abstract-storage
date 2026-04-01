package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.StorableType
import kotlinx.coroutines.flow.Flow

/**
 * The simplest possible representation of a folder.
 */
interface Folder : Storable {
    /**
     * Retrieves the items in this folder.
     *
     * @param type The type of items to retrieve.
     * [StorableType.NONE] is invalid for enumeration and causes [IllegalArgumentException]
     * to be thrown before enumeration begins.
     * @return A [Flow] that asynchronously emits the requested items.
     */
    fun getItemsAsync(type: StorableType = StorableType.ALL): Flow<StorableChild>
}
