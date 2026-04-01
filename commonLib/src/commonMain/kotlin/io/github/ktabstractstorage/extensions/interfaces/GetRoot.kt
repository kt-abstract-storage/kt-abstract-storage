package io.github.ktabstractstorage.extensions.interfaces

import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.StorableChild

/**
 * Provides a fast-path for retrieving the root folder for a [StorableChild].
 */
interface GetRoot : StorableChild {
    /**
     * Retrieves the root folder, if available.
     */
    suspend fun getRootAsync(): Folder?
}

