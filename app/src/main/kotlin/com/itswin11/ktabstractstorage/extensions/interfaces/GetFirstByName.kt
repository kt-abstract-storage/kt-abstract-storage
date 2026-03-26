package com.itswin11.ktabstractstorage.extensions.interfaces

import com.itswin11.ktabstractstorage.Folder
import com.itswin11.ktabstractstorage.StorableChild
import java.io.FileNotFoundException

/**
 * Provides a fast-path for retrieving items by name.
 */
interface GetFirstByName : Folder {
    /**
     * Retrieves the first [StorableChild] item which has the provided [name].
     *
     * @param name Name to match.
     *
     * @throws FileNotFoundException if the item is not found.
     */
    suspend fun getFirstByNameAsync(name: String): StorableChild
}

