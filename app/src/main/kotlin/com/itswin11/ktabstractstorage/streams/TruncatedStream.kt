package com.itswin11.ktabstractstorage.streams

import java.io.IOException
import kotlin.math.min

/**
 * A [UnifiedStream] wrapper that exposes at most [maxLength] bytes from the
 * wrapped [stream].
 *
 * For seekable streams, the truncation window starts at the wrapped stream's
 * current position when this instance is created. For non-seekable streams,
 * the wrapper tracks how many bytes have been consumed or written so it can
 * enforce the same maximum window sequentially.
 *
 * Reads stop at the exposed boundary, and writes that would exceed it fail.
 *
 * @param stream The [UnifiedStream] to wrap.
 * @param maxLength The maximum number of bytes visible through this stream.
 * @param closeWrappedStreamOnClose When `true`, closing this stream also closes
 * the wrapped [stream].
 *
 * @throws IllegalArgumentException if [maxLength] is negative.
 */
class TruncatedStream(
    private val stream: UnifiedStream,
    private val maxLength: Long,
    private val closeWrappedStreamOnClose: Boolean = true,
) : UnifiedStream() {
    private val startOffset = if (stream.canSeek) stream.position else 0L
    private var currentPosition = 0L
    private var consumed = 0L
    private var closed = false

    init {
        require(maxLength >= 0) { "maxLength must be non-negative." }
    }

    override val canRead: Boolean
        get() = !closed && stream.canRead

    override val canWrite: Boolean
        get() = !closed && stream.canWrite

    override val canSeek: Boolean
        get() = !closed && stream.canSeek

    override val length: Long
        get() {
            ensureOpen()
            return if (stream.canSeek) visibleLength() else maxLength
        }

    override var position: Long
        get() {
            ensureOpen()
            return if (stream.canSeek) currentPosition else consumed
        }
        set(value) {
            ensureOpen()
            if (!stream.canSeek) {
                throw UnsupportedOperationException("TruncatedStream does not support seeking when the wrapped stream is non-seekable.")
            }

            currentPosition = value.coerceIn(0, visibleLength())
        }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        ensureOpen()
        ensureReadable()
        validateRange(buffer, offset, count)

        if (count == 0) {
            return 0
        }

        val available = remainingCapacity()
        if (available <= 0) {
            return 0
        }

        val toRead = min(count.toLong(), available).toInt()
        alignWrappedPosition()
        val read = stream.read(buffer, offset, toRead)

        if (read <= 0) {
            return 0
        }

        advance(read.toLong())
        return read
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        ensureOpen()
        ensureWritable()
        validateRange(buffer, offset, count)

        if (count == 0) {
            return
        }

        val remainingCapacity = remainingCapacity()
        if (count.toLong() > remainingCapacity) {
            throw IOException("Write exceeds the truncated stream length.")
        }

        alignWrappedPosition()
        stream.write(buffer, offset, count)
        advance(count.toLong())
    }

    override suspend fun readAsync(buffer: ByteArray, offset: Int, count: Int): Int {
        ensureOpen()
        ensureReadable()
        validateRange(buffer, offset, count)

        if (count == 0) {
            return 0
        }

        val available = remainingCapacity()
        if (available <= 0) {
            return 0
        }

        val toRead = min(count.toLong(), available).toInt()
        alignWrappedPositionAsync()
        val read = stream.readAsync(buffer, offset, toRead)

        if (read <= 0) {
            return 0
        }

        advance(read.toLong())
        return read
    }

    override suspend fun writeAsync(buffer: ByteArray, offset: Int, count: Int) {
        ensureOpen()
        ensureWritable()
        validateRange(buffer, offset, count)

        if (count == 0) {
            return
        }

        val remainingCapacity = remainingCapacity()
        if (count.toLong() > remainingCapacity) {
            throw IOException("Write exceeds the truncated stream length.")
        }

        alignWrappedPositionAsync()
        stream.writeAsync(buffer, offset, count)
        advance(count.toLong())
    }

    override fun seek(offset: Long) {
        ensureOpen()
        if (!stream.canSeek) {
            throw UnsupportedOperationException("TruncatedStream does not support seeking when the wrapped stream is non-seekable.")
        }

        currentPosition = offset.coerceIn(0, visibleLength())
    }

    override suspend fun seekAsync(offset: Long) {
        ensureOpen()
        if (!stream.canSeek) {
            throw UnsupportedOperationException("TruncatedStream does not support seeking when the wrapped stream is non-seekable.")
        }

        currentPosition = offset.coerceIn(0, visibleLength())
    }

    override fun flush() {
        ensureOpen()
        stream.flush()
    }

    override suspend fun flushAsync() {
        ensureOpen()
        stream.flushAsync()
    }

    override fun close() {
        if (closed) {
            return
        }

        closed = true
        if (closeWrappedStreamOnClose) {
            stream.close()
        }
    }

    private fun visibleLength(): Long {
        val remaining = (stream.length - startOffset).coerceAtLeast(0)
        return min(maxLength, remaining)
    }

    private fun remainingCapacity(): Long =
        if (stream.canSeek) {
            (maxLength - currentPosition).coerceAtLeast(0)
        } else {
            (maxLength - consumed).coerceAtLeast(0)
        }

    private fun alignWrappedPosition() {
        if (stream.canSeek) {
            stream.position = startOffset + currentPosition
        }
    }

    private suspend fun alignWrappedPositionAsync() {
        if (stream.canSeek) {
            stream.seekAsync(startOffset + currentPosition)
        }
    }

    private fun advance(count: Long) {
        if (stream.canSeek) {
            currentPosition += count
        } else {
            consumed += count
        }
    }

    private fun ensureOpen() {
        if (closed) {
            throw IOException("Stream is closed")
        }
    }

    private fun ensureReadable() {
        if (!stream.canRead) {
            throw IOException("Stream does not support reading")
        }
    }

    private fun ensureWritable() {
        if (!stream.canWrite) {
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

/**
 * Creates a [TruncatedStream] view over this [UnifiedStream]. For seekable
 * streams the view begins at the current position. For non-seekable streams the
 * view is enforced sequentially from the next byte read or written.
 *
 * @param maxLength Maximum number of bytes exposed by the returned view.
 * @param closeWrappedStreamOnClose Whether closing the returned stream closes this stream.
 */
fun UnifiedStream.truncated(
    maxLength: Long,
    closeWrappedStreamOnClose: Boolean = true,
): UnifiedStream = TruncatedStream(this, maxLength, closeWrappedStreamOnClose)
