package io.github.ktabstractstorage.ziparchive

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.streams.UnifiedStream
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

