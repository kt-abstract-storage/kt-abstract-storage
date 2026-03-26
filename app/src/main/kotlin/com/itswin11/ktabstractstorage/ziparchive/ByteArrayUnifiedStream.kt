package com.itswin11.ktabstractstorage.ziparchive

import com.itswin11.ktabstractstorage.streams.UnifiedStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

internal class ByteArrayUnifiedStream(
    initialSource: UnifiedStream?,
) : UnifiedStream() {
    private var closed = false
    private var buffer = ByteArray(0)
    private var lengthValue = 0
    private var positionValue = 0

    init {
        if (initialSource != null) {
            val bytes = readAll(initialSource)
            buffer = bytes
            lengthValue = bytes.size
            positionValue = 0
            initialSource.close()
        }
    }

    override val canRead: Boolean = true
    override val canWrite: Boolean = true
    override val canSeek: Boolean = true

    override val length: Long
        get() {
            ensureOpen()
            return lengthValue.toLong()
        }

    override var position: Long
        get() {
            ensureOpen()
            return positionValue.toLong()
        }
        set(value) {
            ensureOpen()
            positionValue = value.coerceIn(0, lengthValue.toLong()).toInt()
        }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        ensureOpen()
        validateRange(buffer, offset, count)

        if (count == 0) return 0
        if (positionValue >= lengthValue) return 0

        val toRead = min(count, lengthValue - positionValue)
        this.buffer.copyInto(buffer, offset, positionValue, positionValue + toRead)
        positionValue += toRead
        return toRead
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        ensureOpen()
        validateRange(buffer, offset, count)
        if (count == 0) return

        val required = positionValue + count
        if (required > this.buffer.size) {
            this.buffer = this.buffer.copyOf(required)
        }

        buffer.copyInto(this.buffer, positionValue, offset, offset + count)
        positionValue += count
        lengthValue = max(lengthValue, positionValue)
    }

    override fun seek(offset: Long) {
        position = offset
    }

    override fun flush() {
        ensureOpen()
    }

    override fun close() {
        closed = true
    }

    private fun ensureOpen() {
        if (closed) throw IOException("Stream is closed")
    }

    private fun validateRange(buffer: ByteArray, offset: Int, count: Int) {
        require(offset >= 0) { "offset must be non-negative." }
        require(count >= 0) { "count must be non-negative." }
        require(offset <= buffer.size && count <= buffer.size - offset) {
            "offset and count must describe a valid range within the buffer."
        }
    }

    private fun readAll(source: UnifiedStream): ByteArray {
        source.seek(0)
        val out = ArrayList<ByteArray>()
        var total = 0
        val temp = ByteArray(DEFAULT_BUFFER_SIZE)

        while (true) {
            val read = source.read(temp, 0, temp.size)
            if (read <= 0) break
            out += temp.copyOf(read)
            total += read
        }

        val merged = ByteArray(total)
        var offset = 0
        out.forEach { chunk ->
            chunk.copyInto(merged, offset)
            offset += chunk.size
        }
        return merged
    }
}

