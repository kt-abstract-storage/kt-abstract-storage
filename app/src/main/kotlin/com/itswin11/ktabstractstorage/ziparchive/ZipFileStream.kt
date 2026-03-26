package com.itswin11.ktabstractstorage.ziparchive

import com.itswin11.ktabstractstorage.enums.FileAccessMode
import com.itswin11.ktabstractstorage.streams.UnifiedStream
import java.io.IOException

/**
 * A seekable [UnifiedStream] used for ZIP entry content.
 *
 * @param baseStream Base stream representing raw entry content.
 * @param accessMode Access mode for this stream.
 * @param onCommit Callback invoked on close when this stream is writable.
 */
class ZipFileStream(
    private val baseStream: UnifiedStream,
    accessMode: FileAccessMode,
    private val onCommit: (UnifiedStream) -> Unit,
) : UnifiedStream() {
    private var isClosed = false
    private var committed = false

    override val canRead: Boolean = accessMode != FileAccessMode.WRITE && baseStream.canRead
    override val canWrite: Boolean = accessMode != FileAccessMode.READ && baseStream.canWrite
    override val canSeek: Boolean = baseStream.canSeek

    override val length: Long
        get() {
            ensureOpen()
            return baseStream.length
        }

    override var position: Long
        get() {
            ensureOpen()
            return baseStream.position
        }
        set(value) {
            ensureOpen()
            if (!canSeek) {
                throw UnsupportedOperationException("Base stream does not support seeking.")
            }
            baseStream.position = value.coerceIn(0, baseStream.length)
        }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        ensureOpen()
        ensureReadable()
        validateRange(buffer, offset, count)

        if (count == 0) {
            return 0
        }

        return baseStream.read(buffer, offset, count)
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        ensureOpen()
        ensureWritable()
        validateRange(buffer, offset, count)

        if (count == 0) {
            return
        }

        baseStream.write(buffer, offset, count)
    }

    override fun seek(offset: Long) {
        ensureOpen()
        if (!canSeek) {
            throw UnsupportedOperationException("Base stream does not support seeking.")
        }
        baseStream.seek(offset)
    }

    override fun flush() {
        ensureOpen()
        baseStream.flush()
    }

    override fun close() {
        if (isClosed) {
            return
        }

        if (canWrite && !committed) {
            committed = true
            onCommit(baseStream)
        }

        isClosed = true
        baseStream.close()
    }

    private fun ensureOpen() {
        if (isClosed) {
            throw IOException("Stream is closed")
        }
    }

    private fun ensureReadable() {
        if (!canRead) {
            throw IOException("Stream does not support reading")
        }
    }

    private fun ensureWritable() {
        if (!canWrite) {
            throw IOException("Stream does not support writing")
        }
    }

    private fun validateRange(buffer: ByteArray, offset: Int, count: Int) {
        require(offset >= 0) { "offset must be non-negative." }
        require(count >= 0) { "count must be non-negative." }
        require(offset <= buffer.size && count <= buffer.size - offset) {
            "offset and count must describe a valid range within the buffer."
        }
    }
}



