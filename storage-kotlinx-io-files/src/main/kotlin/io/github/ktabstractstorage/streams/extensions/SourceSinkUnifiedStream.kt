package io.github.ktabstractstorage.streams.extensions

import io.github.ktabstractstorage.streams.UnifiedStream
import java.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * A non-seekable [UnifiedStream] that reads from [source] and writes to [sink].
 */
internal class SourceSinkUnifiedStream(
    private val source: Source,
    private val sink: Sink,
    private val closeSourceOnClose: Boolean,
    private val closeSinkOnClose: Boolean,
) : UnifiedStream() {
    private var closed = false
    private var currentPosition = 0L

    override val canRead: Boolean
        get() = !closed

    override val canWrite: Boolean
        get() = !closed

    override val canSeek: Boolean = false

    override val length: Long
        get() = throw UnsupportedOperationException("Length is not available for non-seekable Source/Sink adapters.")

    override var position: Long
        get() = currentPosition
        set(_) {
            throw UnsupportedOperationException("Source/Sink adapter does not support seeking.")
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
        ensureOpen()
        if (count == 0) return

        sink.write(buffer, offset, count)
        currentPosition += count
    }

    override fun seek(offset: Long) {
        throw UnsupportedOperationException("Source/Sink adapter does not support seeking.")
    }

    override fun flush() {
        ensureOpen()
        sink.flush()
    }

    override fun close() {
        if (closed) return
        closed = true

        var closeError: Throwable? = null

        if (closeSourceOnClose) {
            runCatching { source.close() }.onFailure { closeError = it }
        }

        if (closeSinkOnClose) {
            runCatching { sink.close() }.onFailure { if (closeError == null) closeError = it }
        }

        if (closeError != null) {
            throw closeError
        }
    }

    private fun ensureOpen() {
        if (closed) {
            throw IOException("UnifiedStream is closed.")
        }
    }
}
