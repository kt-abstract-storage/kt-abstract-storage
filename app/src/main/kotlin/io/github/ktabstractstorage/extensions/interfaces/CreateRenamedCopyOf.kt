package io.github.ktabstractstorage.extensions.interfaces

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.File
import io.github.ktabstractstorage.ModifiableFolder

/**
 * A delegate that provides a fallback for [CreateRenamedCopyOf.createCopyOfAsync].
 */
typealias CreateRenamedCopyOfDelegate = suspend (
    destination: ModifiableFolder,
    fileToCopy: File,
    overwrite: Boolean,
    newName: String,
) -> ChildFile

/**
 * Provides a fast-path for copying and renaming files into a folder.
 */
interface CreateRenamedCopyOf : CreateCopyOf {
    /**
     * Copies [fileToCopy] into this folder as [newName] or delegates to [fallback].
     *
     * @param fileToCopy Source file to copy.
     * @param overwrite Whether to overwrite an existing destination file.
     * @param newName Name to assign to the created copy.
     * @param fallback Fallback implementation when a specialized fast-path is not used.
     */
    suspend fun createCopyOfAsync(
        fileToCopy: File,
        overwrite: Boolean,
        newName: String,
        fallback: CreateRenamedCopyOfDelegate,
    ): ChildFile
}

