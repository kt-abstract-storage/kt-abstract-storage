package io.github.ktabstractstorage.extensions.interfaces

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.ModifiableFolder

/**
 * A delegate that provides a fallback for [MoveRenamedFrom.moveFromAsync].
 */
typealias MoveRenamedFromDelegate = suspend (
    destination: ModifiableFolder,
    file: ChildFile,
    source: ModifiableFolder,
    overwrite: Boolean,
    newName: String,
) -> ChildFile

/**
 * Provides a fast-path for moving and renaming files between folders.
 */
interface MoveRenamedFrom : MoveFrom {
    /**
     * Moves [fileToMove] from [source] into this folder as [newName] or delegates to [fallback].
     *
     * @param fileToMove File to move.
     * @param source Folder currently containing [fileToMove].
     * @param overwrite Whether to overwrite an existing destination file.
     * @param newName Name to assign to the moved file.
     * @param fallback Fallback implementation when a specialized fast-path is not used.
     */
    suspend fun moveFromAsync(
        fileToMove: ChildFile,
        source: ModifiableFolder,
        overwrite: Boolean,
        newName: String,
        fallback: MoveRenamedFromDelegate,
    ): ChildFile
}

