package com.itswin11.ktabstractstorage.streams

import kotlin.math.max
import kotlin.math.min
import java.io.IOException

/**
 * A [UnifiedStream] implementation backed by an in-memory byte array.
 *
 * The internal buffer grows automatically only when writes exceed current capacity,
 * resizing exactly to the required size.
 *
 * All three stream capabilities ([canRead], [canWrite], [canSeek]) are available
 * until the stream is [close]d, after which any operation throws [java.io.IOException].
 *
 * @param initialCapacity Initial size of the internal byte buffer in bytes.
 * Choosing a value close to the expected data size avoids unnecessary reallocations.
 * Defaults to `1024` bytes.
 */
class MemoryStream(initialCapacity: Int = 1024) : UnifiedStream() {
    private var buffer: ByteArray? = ByteArray(initialCapacity)
    private var _length: Long = 0
    private var _position: Long = 0
    private var isClosed = false

    override val canRead: Boolean get() = !isClosed
    override val canWrite: Boolean get() = !isClosed
    override val canSeek: Boolean get() = !isClosed

    override val length: Long
        get() {
            ensureNotClosed()
            return _length
        }

    override var position: Long
        get() {
            ensureNotClosed()
            return _position
        }
        set(value) {
            ensureNotClosed()
            _position = value.coerceIn(0, _length)
        }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        ensureNotClosed()
        val internalBuffer = this.buffer!!

        if (_position >= _length) return 0

        val available = (_length - _position).toInt()
        val toRead = min(count, available)

        internalBuffer.copyInto(
            buffer,
            offset,
            _position.toInt(),
            _position.toInt() + toRead
        )
        _position += toRead
        return toRead
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        ensureNotClosed()
        ensureCapacity(_position + count)

        buffer.copyInto(
            this.buffer!!,
            _position.toInt(),
            offset,
            offset + count
        )
        _position += count
        _length = max(_length, _position)
    }

    override fun close() {
        if (!isClosed) {
            buffer = null
            isClosed = true
        }
    }

    override fun seek(offset: Long) {
        ensureNotClosed()
        _position = offset.coerceIn(0, _length)
    }

    override fun flush() { /* No-op */ }

    /**
     * Asynchronously reads up to [count] bytes into [buffer], starting
     * at [offset].
     *
     * @param buffer Destination array that receives the bytes.
     * @param offset Start index in [buffer] where bytes are written.
     * @param count Maximum number of bytes to read.
     */
    override suspend fun readAsync(buffer: ByteArray, offset: Int, count: Int): Int
            = read(buffer, offset, count)

    /**
     * Asynchronously writes [count] bytes from [buffer], starting at [offset].
     *
     * @param buffer Source array that provides bytes to write.
     * @param offset Start index in [buffer] to read bytes from.
     * @param count Number of bytes to write.
     */
    override suspend fun writeAsync(buffer: ByteArray, offset: Int, count: Int)
            = write(buffer, offset, count)

    /**
     * Asynchronously changes the stream position using [offset].
     *
     * @param offset Target position argument accepted by the implementation.
     */
    override suspend fun seekAsync(offset: Long) = seek(offset)

    /**
     * Asynchronously flushes buffered data.
     */
    override suspend fun flushAsync() = flush()
    
    private fun ensureNotClosed() {
        if (isClosed || buffer == null) throw IOException("Stream is closed")
    }

    private fun ensureCapacity(required: Long) {
        val current = buffer!!
        if (required > current.size) {
            buffer = current.copyOf(required.toInt())
        }
    }
}
