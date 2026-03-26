package com.itswin11.ktabstractstorage.ziparchive

import com.itswin11.ktabstractstorage.File as StorageFile
import com.itswin11.ktabstractstorage.enums.FileAccessMode
import com.itswin11.ktabstractstorage.streams.NonDisposableUnifiedStreamWrapper
import com.itswin11.ktabstractstorage.streams.UnifiedStream
import java.io.IOException

internal interface ZipArchiveIo {
    val idHint: String?
    val isReadOnly: Boolean

    suspend fun openRead(): UnifiedStream
    suspend fun openWrite(): UnifiedStream
}

internal class StorageFileZipArchiveIo(
    private val file: StorageFile,
    override val idHint: String? = null,
    override val isReadOnly: Boolean,
) : ZipArchiveIo {
    override suspend fun openRead(): UnifiedStream = file.openStreamAsync(FileAccessMode.READ)

    override suspend fun openWrite(): UnifiedStream {
        if (isReadOnly) {
            throw IOException("ZIP archive is read-only.")
        }

        return file.openStreamAsync(FileAccessMode.WRITE)
    }
}

internal class StreamZipArchiveIo(
    private val stream: UnifiedStream,
    override val idHint: String? = null,
    override val isReadOnly: Boolean,
) : ZipArchiveIo {
    override suspend fun openRead(): UnifiedStream {
        if (stream.canSeek) {
            stream.seekAsync(0)
        }

        return NonDisposableUnifiedStreamWrapper(stream)
    }

    override suspend fun openWrite(): UnifiedStream {
        if (isReadOnly) {
            throw IOException("ZIP archive is read-only.")
        }
        if (!stream.canWrite) {
            throw IOException("ZIP archive stream is not writable.")
        }
        if (stream.canSeek) {
            stream.seekAsync(0)
        }

        return NonDisposableUnifiedStreamWrapper(stream)
    }
}

