package io.github.ktabstractstorage

/**
 * Represents a storable resource that resides within a traversable folder structure.
 */
interface StorableChild : Storable {
    /**
     * Gets the containing folder for this item, if any.
     *
     * @return The containing parent folder, if any.
     */
    suspend fun getParentAsync(): Folder?
}

