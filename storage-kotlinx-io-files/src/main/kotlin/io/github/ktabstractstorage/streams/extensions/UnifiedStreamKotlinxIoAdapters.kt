package io.github.ktabstractstorage.streams.extensions

import io.github.ktabstractstorage.streams.UnifiedStream
import java.io.EOFException
import java.io.IOException
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source

internal class UnifiedStreamRawSourceAdapter(
    private val stream: UnifiedStream,
    private val closeUnifiedStreamOnClose: Boolean,
) : RawSource {
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        ensureReadable()
        require(byteCount >= 0) { "byteCount must be non-negative." }

        if (byteCount == 0L) return 0L

        val readBuffer = ByteArray(minOf(byteCount, 8192L).toInt())
        val read = stream.read(readBuffer, 0, readBuffer.size)
        if (read <= 0) return -1L

        sink.write(readBuffer, 0, read)
        return read.toLong()
    }

    override fun close() {
        if (closeUnifiedStreamOnClose) {
            stream.close()
        }
    }

    private fun ensureReadable() {
        if (!stream.canRead) {
            throw IOException("Underlying UnifiedStream is not readable.")
        }
    }
}

internal class UnifiedStreamRawSinkAdapter(
    private val stream: UnifiedStream,
    private val closeUnifiedStreamOnClose: Boolean,
) : RawSink {
    override fun write(source: Buffer, byteCount: Long) {
        ensureWritable()
        require(byteCount >= 0) { "byteCount must be non-negative." }

        var remaining = byteCount
        val writeBuffer = ByteArray(8192)

        while (remaining > 0) {
            val toRead = minOf(writeBuffer.size.toLong(), remaining).toInt()
            val read = source.readAtMostTo(writeBuffer, 0, toRead)
            if (read <= 0) {
                throw EOFException("Source exhausted before $byteCount bytes were written.")
            }

            stream.write(writeBuffer, 0, read)
            remaining -= read.toLong()
        }
    }

    override fun flush() {
        ensureWritable()
        stream.flush()
    }

    override fun close() {
        if (closeUnifiedStreamOnClose) {
            stream.close()
        }
    }

    private fun ensureWritable() {
        if (!stream.canWrite) {
            throw IOException("Underlying UnifiedStream is not writable.")
        }
    }
}

internal class SourceUnifiedStreamAdapter(
    private val source: Source,
    private val closeSourceOnClose: Boolean,
) : UnifiedStream() {
    private var closed = false
    private var currentPosition = 0L

    override val canRead: Boolean
        get() = !closed

    override val canWrite: Boolean = false

    override val canSeek: Boolean = false

    override val length: Long
        get() = throw UnsupportedOperationException("Length is not available for non-seekable Source adapters.")

    override var position: Long
        get() = currentPosition
        set(_) {
            throw UnsupportedOperationException("Source adapter does not support seeking.")
        }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        ensureOpen()

        if (count == 0) return 0

        val read = source.readAtMostTo(buffer, offset, count)
        if (read > 0) {
            currentPosition += read
            return read
        }

        return 0
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        throw IOException("Source adapter is not writable.")
    }

    override fun seek(offset: Long) {
        throw UnsupportedOperationException("Source adapter does not support seeking.")
    }

    override fun flush() {
        ensureOpen()
        // Source has no flush operation.
    }

    override suspend fun flushAsync() {
        flush()
    }

    override fun close() {
        if (closed) return

        closed = true
        if (closeSourceOnClose) {
            source.close()
        }
    }

    private fun ensureOpen() {
        if (closed) {
            throw IOException("UnifiedStream is closed.")
        }
    }
}

internal class SinkUnifiedStreamAdapter(
    private val sink: Sink,
    private val closeSinkOnClose: Boolean,
) : UnifiedStream() {
    private var closed = false
    private var currentPosition = 0L

    override val canRead: Boolean = false

    override val canWrite: Boolean
        get() = !closed

    override val canSeek: Boolean = false

    override val length: Long
        get() = throw UnsupportedOperationException("Length is not available for non-seekable Sink adapters.")

    override var position: Long
        get() = currentPosition
        set(_) {
            throw UnsupportedOperationException("Sink adapter does not support seeking.")
        }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        throw IOException("Sink adapter is not readable.")
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        ensureOpen()

        if (count == 0) return

        sink.write(buffer, offset, count)
        currentPosition += count
    }

    override fun seek(offset: Long) {
        throw UnsupportedOperationException("Sink adapter does not support seeking.")
    }

    override fun flush() {
        ensureOpen()
        sink.flush()
    }

    override fun close() {
        if (closed) return

        closed = true
        if (closeSinkOnClose) {
            sink.close()
        }
    }

    private fun ensureOpen() {
        if (closed) {
            throw IOException("UnifiedStream is closed.")
        }
    }
}

