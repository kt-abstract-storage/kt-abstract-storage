package io.github.ktabstractstorage.system.io

import io.github.ktabstractstorage.File
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.errors.StorageIOException
import io.github.ktabstractstorage.streams.NonDisposableUnifiedStreamWrapper
import io.github.ktabstractstorage.streams.UnifiedStream

/**
 * A [File] implementation that surfaces a provided [UnifiedStream].
 *
 * The wrapped stream is returned either directly or through a non-disposing
 * wrapper depending on [shouldDispose].
 */
class StreamFile(
    val stream: UnifiedStream,
    override val id: String = stream.hashCode().toString(),
    override val name: String = stream.hashCode().toString(),
) : File {
    /**
     * When `true`, [openStreamAsync] returns [stream] directly and closing the
     * returned instance closes the underlying stream.
     *
     * When `false`, [openStreamAsync] returns a wrapper that ignores [UnifiedStream.close].
     */
    var shouldDispose: Boolean = false

    override suspend fun openStreamAsync(accessMode: FileAccessMode): UnifiedStream {
        validateAccessMode(accessMode)
        return if (shouldDispose) stream else NonDisposableUnifiedStreamWrapper(stream)
    }

    private fun validateAccessMode(accessMode: FileAccessMode) {
        if ((accessMode == FileAccessMode.READ || accessMode == FileAccessMode.READ_AND_WRITE) && !stream.canRead) {
            throw StorageIOException("Requested $accessMode but wrapped stream is not readable.")
        }

        if ((accessMode == FileAccessMode.WRITE || accessMode == FileAccessMode.READ_AND_WRITE) && !stream.canWrite) {
            throw StorageIOException("Requested $accessMode but wrapped stream is not writable.")
        }
    }
}

