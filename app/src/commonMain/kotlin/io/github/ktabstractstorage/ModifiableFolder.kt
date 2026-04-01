package io.github.ktabstractstorage

import io.github.ktabstractstorage.errors.StorageFileNotFoundException

/**
 * Represents a folder that can be modified.
 */
interface ModifiableFolder : MutableFolder {
    /**
     * Deletes the provided storable item from this folder.
     *
     * @param item The item to be removed from this folder.
     * @throws StorageFileNotFoundException The item was not found in this folder.
     */
    suspend fun deleteAsync(item: StorableChild)

    /**
     * Creates a new folder with the desired name inside this folder.
     *
     * @param name The name of the new folder.
     * @param overwrite `true` if the destination folder can be overwritten; otherwise, `false`.
     * @return The newly created folder, or the existing folder if one is opened instead.
     */
    suspend fun createFolderAsync(name: String, overwrite: Boolean = false): ChildFolder

    /**
     * Creates a new file with the desired name inside this folder.
     *
     * @param name The name of the new file.
     * @param overwrite `true` if the destination file can be overwritten; otherwise, `false`.
     * @return The newly created file, or the existing file if one is opened instead.
     */
    suspend fun createFileAsync(name: String, overwrite: Boolean = false): ChildFile
}
