package io.github.ktabstractstorage.memory

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.File
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.streams.UnifiedStream
import java.io.IOException
import java.util.UUID
import kotlin.math.min

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

    override suspend fun getParentAsync(): Folder? = parentFolder

    override suspend fun openStreamAsync(accessMode: FileAccessMode): UnifiedStream =
        MemoryFileStream(this, accessMode)

    internal fun detach() {
        parentFolder = null
    }

    internal fun clearContent() {
        synchronized(this) {
            content = ByteArray(0)
        }
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

        override val length: Long
            get() = synchronized(file) {
                ensureOpen()
                file.content.size.toLong()
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

        override fun read(buffer: ByteArray, offset: Int, count: Int): Int = synchronized(file) {
            ensureOpen()
            ensureReadable()

            if (cursor >= file.content.size) return@synchronized 0

            val startIndex = cursor.toInt()
            val toRead = min(count, file.content.size - startIndex)
            file.content.copyInto(buffer, offset, startIndex, startIndex + toRead)
            cursor += toRead
            toRead
        }

        override fun write(buffer: ByteArray, offset: Int, count: Int) = synchronized(file) {
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
            read(buffer, offset, count)

        override suspend fun writeAsync(buffer: ByteArray, offset: Int, count: Int) =
            write(buffer, offset, count)

        override suspend fun seekAsync(offset: Long) = seek(offset)

        override suspend fun flushAsync() = flush()

        private fun ensureOpen() {
            if (closed) throw IOException("Stream is closed")
        }

        private fun ensureReadable() {
            if (!canRead) throw IOException("Stream does not support reading")
        }

        private fun ensureWritable() {
            if (!canWrite) throw IOException("Stream does not support writing")
        }
    }
}
