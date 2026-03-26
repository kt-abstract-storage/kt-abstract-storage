package com.itswin11.ktabstractstorage

import com.itswin11.ktabstractstorage.enums.FileAccessMode
import com.itswin11.ktabstractstorage.streams.UnifiedStream

/**
 * A [ChildFile] wrapper that reports a custom parent for any wrapped [File].
 *
 * @param inner The wrapped file.
 * @param parent The parent folder to return from [getParentAsync].
 */
class ParentOverrideChildFile(
    val inner: File,
    val parent: Folder?,
) : ChildFile {
    override val id: String
        get() = inner.id

    override val name: String
        get() = inner.name

    override suspend fun getParentAsync(): Folder? = parent

    override suspend fun openStreamAsync(accessMode: FileAccessMode): UnifiedStream =
        inner.openStreamAsync(accessMode)
}

