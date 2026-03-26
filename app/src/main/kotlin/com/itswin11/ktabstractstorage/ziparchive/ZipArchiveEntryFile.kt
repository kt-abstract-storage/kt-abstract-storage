package com.itswin11.ktabstractstorage.ziparchive

import com.itswin11.ktabstractstorage.ChildFile
import com.itswin11.ktabstractstorage.Folder
import com.itswin11.ktabstractstorage.enums.FileAccessMode
import com.itswin11.ktabstractstorage.streams.UnifiedStream
import java.io.IOException

class ZipArchiveEntryFile internal constructor(
    private val owner: ReadOnlyZipArchiveFolder,
    val entryPath: String,
    private val parent: Folder,
    private val isReadOnly: Boolean,
) : ChildFile {
    override val id: String
        get() = owner.createEntryId(entryPath, isFolder = false)

    override val name: String
        get() = entryPath.substringAfterLast('/')

    override suspend fun getParentAsync(): Folder = parent

    override suspend fun openStreamAsync(accessMode: FileAccessMode): UnifiedStream {
        if (isReadOnly && accessMode != FileAccessMode.READ) {
            throw IOException("ZIP entry '$entryPath' is read-only.")
        }

        return owner.openEntryStreamForNode(entryPath, accessMode)
    }
}

