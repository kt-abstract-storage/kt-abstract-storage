package io.github.ktabstractstorage.memory

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.File
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.errors.StorageIOException
import io.github.ktabstractstorage.streams.UnifiedStream
import kotlin.math.min
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An in-memory implementation of [File].
 *
 * @param name The display name of the file.
 * @param parentFolder The containing folder, if any.
 * @param id Stable identifier for this file instance.
 */
class MemoryFile internal constructor(
    override val name: String,
    private var parentFolder: MemoryFolder? = null,
    override val id: String = name.hashCode().toString(),
) : ChildFile {
    /**
     * Creates a new instance of [MemoryFile] with a derived id from the hash code.
     *
     * @param name The display name of the file.
     */
    internal constructor(name: String) : this(name, parentFolder = null, id = name.hashCode().toString())

    private var content: ByteArray = ByteArray(0)
    private val mutex = Mutex()

    override suspend fun getParentAsync(): Folder? = parentFolder

    override suspend fun openStreamAsync(accessMode: FileAccessMode): UnifiedStream =
        MemoryFileStream(this, accessMode)

    internal fun detach() {
        parentFolder = null
    }

    internal suspend fun clearContent() = mutex.withLock {
        content = ByteArray(0)
    }

    private class MemoryFileStream(
        private val file: MemoryFile,
        accessMode: FileAccessMode,
    ) : UnifiedStream() {
        private var closed = false
        private var cursor = 0L

        override val canRead: Boolean = accessMode != FileAccessMode.WRITE
        override val canWrite: Boolean = accessMode != FileAccessMode.READ
        override val canSeek: Boolean = true

        /**
         * Returns the current length of the file content.
         *
         * **Note:** This is not synchronized. For consistent results in concurrent
         * scenarios, prefer [readAsync]/[writeAsync] which are protected by [file.mutex].
         */
        override val length: Long
            get() {
                ensureOpen()
                return file.content.size.toLong()
            }

        override var position: Long
            get() {
                ensureOpen()
                return cursor
            }
            set(value) {
                ensureOpen()
                cursor = value.coerceIn(0, length)
            }

        /**
         * Reads synchronously. Not mutex-protected; use [readAsync] for thread-safe access.
         */
        override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
            ensureOpen()
            ensureReadable()

            if (cursor >= file.content.size) return 0

            val startIndex = cursor.toInt()
            val toRead = min(count, file.content.size - startIndex)
            file.content.copyInto(buffer, offset, startIndex, startIndex + toRead)
            cursor += toRead
            return toRead
        }

        /**
         * Writes synchronously. Not mutex-protected; use [writeAsync] for thread-safe access.
         */
        override fun write(buffer: ByteArray, offset: Int, count: Int) {
            ensureOpen()
            ensureWritable()

            val requiredSize = cursor.toInt() + count
            if (requiredSize > file.content.size) {
                file.content = file.content.copyOf(requiredSize)
            }

            buffer.copyInto(file.content, cursor.toInt(), offset, offset + count)
            cursor += count
        }

        override fun seek(offset: Long) {
            ensureOpen()
            cursor = offset.coerceIn(0, length)
        }

        override fun flush() {
            ensureOpen()
        }

        override fun close() {
            closed = true
        }

        override suspend fun readAsync(buffer: ByteArray, offset: Int, count: Int): Int =
            file.mutex.withLock { read(buffer, offset, count) }

        override suspend fun writeAsync(buffer: ByteArray, offset: Int, count: Int) =
            file.mutex.withLock { write(buffer, offset, count) }

        override suspend fun seekAsync(offset: Long) = seek(offset)

        override suspend fun flushAsync() = flush()

        private fun ensureOpen() {
            if (closed) throw StorageIOException("Stream is closed")
        }

        private fun ensureReadable() {
            if (!canRead) throw StorageIOException("Stream does not support reading")
        }

        private fun ensureWritable() {
            if (!canWrite) throw StorageIOException("Stream does not support writing")
        }
    }
}
