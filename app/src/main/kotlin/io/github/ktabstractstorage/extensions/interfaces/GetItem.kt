package io.github.ktabstractstorage.extensions.interfaces

import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.StorableChild
import java.io.FileNotFoundException

/**
 * Provides a fast-path for retrieving items by ID.
 */
interface GetItem : Folder {
    /**
     * Retrieves the [StorableChild] item which has the provided [id].
     *
     * @param id Identifier to match.
     *
     * @throws FileNotFoundException if the item is not found.
     */
    suspend fun getItemAsync(id: String): StorableChild
}

