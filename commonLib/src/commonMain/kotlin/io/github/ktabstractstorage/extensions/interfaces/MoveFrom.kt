package io.github.ktabstractstorage.extensions.interfaces

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.ModifiableFolder

/**
 * A delegate that provides a fallback for [MoveFrom.moveFromAsync].
 */
typealias MoveFromDelegate = suspend (
    destination: ModifiableFolder,
    file: ChildFile,
    source: ModifiableFolder,
    overwrite: Boolean,
) -> ChildFile

/**
 * Provides a fast-path for moving files between folders.
 */
interface MoveFrom : ModifiableFolder {
    /**
     * Moves [fileToMove] from [source] into this folder or delegates to [fallback].
     *
     * @param fileToMove File to move.
     * @param source Folder currently containing [fileToMove].
     * @param overwrite Whether to overwrite an existing destination file.
     * @param fallback Fallback implementation when a specialized fast-path is not used.
     */
    suspend fun moveFromAsync(
        fileToMove: ChildFile,
        source: ModifiableFolder,
        overwrite: Boolean,
        fallback: MoveFromDelegate,
    ): ChildFile
}

