package io.github.ktabstractstorage.ziparchive

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.ChildFolder
import io.github.ktabstractstorage.File
import io.github.ktabstractstorage.FolderWatcher
import io.github.ktabstractstorage.ModifiableFolder
import io.github.ktabstractstorage.StorableChild
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.enums.FolderChangeType
import io.github.ktabstractstorage.streams.UnifiedStream
import java.io.FileNotFoundException

class ZipArchiveFolder internal constructor(
    sharedState: ZipArchiveSharedState,
    rootId: String,
    rootName: String,
    entryPath: String,
    parent: ZipArchiveFolder?,
) : ReadOnlyZipArchiveFolder(sharedState, rootId, rootName, entryPath, parent), ModifiableFolder {
    constructor(
        archiveFile: File,
        id: String = "zip:${archiveFile.id}",
        name: String = archiveFile.name,
        persistCoalescingWindowMs: Long = 0,
    ) : this(
        sharedState = ZipArchiveSharedState(
            ZipArchiveStore(
                StorageFileZipArchiveIo(archiveFile, idHint = id, isReadOnly = false),
                persistCoalescingWindowMs = persistCoalescingWindowMs,
            ),
        ),
        rootId = id,
        rootName = name,
        entryPath = "",
        parent = null,
    )

    constructor(
        zipStream: UnifiedStream,
        id: String = "zip:${System.identityHashCode(zipStream)}",
        name: String = "archive.zip",
        persistCoalescingWindowMs: Long = 0,
    ) : this(
        sharedState = ZipArchiveSharedState(
            ZipArchiveStore(
                StreamZipArchiveIo(zipStream, idHint = id, isReadOnly = false),
                persistCoalescingWindowMs = persistCoalescingWindowMs,
            ),
        ),
        rootId = id,
        rootName = name,
        entryPath = "",
        parent = null,
    )

    override suspend fun createFolderAsync(name: String, overwrite: Boolean): ChildFolder {
        val fullPath = combinePath(entryPath, name)
        val existedAsFolder = sharedState.store.directoryExists(fullPath)
        val existedAsFile = sharedState.store.fileExists(fullPath)

        sharedState.store.createFolder(fullPath, overwrite)

        sharedState.mutations.tryEmit(
            ZipArchiveMutation(
                type = if (existedAsFolder || existedAsFile) FolderChangeType.UPDATED else FolderChangeType.ADDED,
                path = fullPath,
                isFolder = true,
            ),
        )

        return createFolderNode(fullPath)
    }

    override suspend fun createFileAsync(name: String, overwrite: Boolean): ChildFile {
        val fullPath = combinePath(entryPath, name)
        val existedAsFile = sharedState.store.fileExists(fullPath)
        val existedAsFolder = sharedState.store.directoryExists(fullPath)

        sharedState.store.createFile(fullPath, overwrite)

        sharedState.mutations.tryEmit(
            ZipArchiveMutation(
                type = if (existedAsFile || existedAsFolder) FolderChangeType.UPDATED else FolderChangeType.ADDED,
                path = fullPath,
                isFolder = false,
            ),
        )

        return createFileNode(fullPath)
    }

    override suspend fun deleteAsync(item: StorableChild) {
        val targetPath = when (item) {
            is ZipArchiveEntryFile -> item.entryPath
            is ReadOnlyZipArchiveFolder -> item.entryPath
            else -> sharedState.store.listChildren(entryPath)
                .firstOrNull { it.name == item.name }
                ?.path
        } ?: throw FileNotFoundException("Item ${item.name} was not found in folder $name")

        val isFolder = sharedState.store.directoryExists(targetPath) && !sharedState.store.fileExists(targetPath)
        sharedState.store.deletePath(targetPath)
        sharedState.mutations.tryEmit(ZipArchiveMutation(FolderChangeType.REMOVED, targetPath, isFolder))
    }

    override suspend fun getFolderWatcherAsync(): FolderWatcher =
        ZipArchiveFolderWatcher(this, entryPath)

    override suspend fun openEntryStream(path: String, accessMode: FileAccessMode): UnifiedStream {
        if (accessMode == FileAccessMode.READ) {
            val readBase = sharedState.store.openEntryReadStream(path)
            return ZipFileStream(readBase, FileAccessMode.READ) { }
        }

        val existing = try {
            sharedState.store.openEntryReadStream(path)
        } catch (_: FileNotFoundException) {
            null
        }

        val base = ByteArrayUnifiedStream(existing)

        return ZipFileStream(base, accessMode) { updatedStream ->
            kotlinx.coroutines.runBlocking {
                sharedState.store.upsertFile(path, readAllBytesFrom(updatedStream))
                sharedState.mutations.tryEmit(
                    ZipArchiveMutation(FolderChangeType.UPDATED, path, isFolder = false),
                )
            }
        }
    }

    override fun createFolderNode(path: String): ReadOnlyZipArchiveFolder =
        ZipArchiveFolder(sharedState, rootId, rootName, path, this)

    override fun createFileNode(path: String): ZipArchiveEntryFile =
        ZipArchiveEntryFile(this, path, this, isReadOnly = false)

    private fun readAllBytesFrom(stream: UnifiedStream): ByteArray {
        stream.seek(0)
        val length = stream.length.toInt().coerceAtLeast(0)
        if (length == 0) {
            return ByteArray(0)
        }

        val data = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = stream.read(data, offset, length - offset)
            if (read <= 0) {
                break
            }
            offset += read
        }

        return if (offset == length) data else data.copyOf(offset)
    }
}


