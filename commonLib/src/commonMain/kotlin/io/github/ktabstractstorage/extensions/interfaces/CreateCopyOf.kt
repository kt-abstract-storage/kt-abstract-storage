package io.github.ktabstractstorage.extensions.interfaces

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.File
import io.github.ktabstractstorage.ModifiableFolder

/**
 * A delegate that provides a fallback for [CreateCopyOf.createCopyOfAsync].
 */
typealias CreateCopyOfDelegate = suspend (
    destination: ModifiableFolder,
    fileToCopy: File,
    overwrite: Boolean,
) -> ChildFile

/**
 * Provides a fast-path for copying files into a folder.
 */
interface CreateCopyOf : ModifiableFolder {
    /**
     * Copies [fileToCopy] into this folder or delegates to [fallback].
     *
     * @param fileToCopy Source file to copy.
     * @param overwrite Whether to overwrite an existing destination file.
     * @param fallback Fallback implementation when a specialized fast-path is not used.
     */
    suspend fun createCopyOfAsync(
        fileToCopy: File,
        overwrite: Boolean,
        fallback: CreateCopyOfDelegate,
    ): ChildFile
}

