package io.github.ktabstractstorage.extensions.interfaces

import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.StorableChild
import io.github.ktabstractstorage.errors.StorageFileNotFoundException

/**
 * Provides a fast-path for retrieving items by name.
 */
interface GetFirstByName : Folder {
    /**
     * Retrieves the first [StorableChild] item which has the provided [name].
     *
     * @param name Name to match.
     *
     * @throws StorageFileNotFoundException if the item is not found.
     */
    suspend fun getFirstByNameAsync(name: String): StorableChild
}

