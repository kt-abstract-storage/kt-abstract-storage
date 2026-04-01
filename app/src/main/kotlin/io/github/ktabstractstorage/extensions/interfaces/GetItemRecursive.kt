package io.github.ktabstractstorage.extensions.interfaces

import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.StorableChild
import java.io.FileNotFoundException

/**
 * Provides a fast-path for recursively retrieving items by ID.
 */
interface GetItemRecursive : Folder {
    /**
     * Crawls this folder and all subfolders for an item with the provided [id].
     *
     * @param id Identifier to match.
     *
     * @throws FileNotFoundException if the item is not found.
     */
    suspend fun getItemRecursiveAsync(id: String): StorableChild
}

