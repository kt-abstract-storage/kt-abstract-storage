package io.github.ktabstractstorage

/**
 * Represents a folder whose content can change.
 */
interface MutableFolder : Folder {
    /**
     * Asynchronously retrieves a closeable object which can notify of changes to the folder.
     *
     * @return A closeable object which can notify of changes to the folder.
     */
    suspend fun getFolderWatcherAsync(): FolderWatcher
}
