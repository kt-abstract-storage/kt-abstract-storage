package com.itswin11.ktabstractstorage.extensions.interfaces

import com.itswin11.ktabstractstorage.ChildFile
import com.itswin11.ktabstractstorage.File
import com.itswin11.ktabstractstorage.ModifiableFolder

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

