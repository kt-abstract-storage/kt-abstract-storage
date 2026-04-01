package io.github.ktabstractstorage.enums

/**
 * Describes the kind of change reported by a [io.github.ktabstractstorage.FolderWatcher].
 */
enum class FolderChangeType {
    /**
     * A new item was added to the watched folder.
     */
    ADDED,

    /**
     * An existing item was removed from the watched folder.
     */
    REMOVED,

    /**
     * An existing item in the watched folder was updated.
     */
    UPDATED,
}