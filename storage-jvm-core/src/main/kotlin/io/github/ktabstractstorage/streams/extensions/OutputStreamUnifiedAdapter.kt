package io.github.ktabstractstorage.streams.extensions

import io.github.ktabstractstorage.streams.UnifiedStream
import java.io.IOException
import java.io.OutputStream

internal class OutputStreamUnifiedAdapter(
    private val output: OutputStream,
    private val closeOutputOnClose: Boolean,
) : UnifiedStream() {
    private var closed = false
    private var currentPosition = 0L

    override val canRead: Boolean = false

    override val canWrite: Boolean
        get() = !closed

    override val canSeek: Boolean = false

    override val length: Long
        get() = throw UnsupportedOperationException("Length is not available for non-seekable OutputStream adapters.")

    override var position: Long
        get() = currentPosition
        set(_) {
            throw UnsupportedOperationException("OutputStream adapter does not support seeking.")
        }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        throw IOException("OutputStream adapter is not readable.")
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        ensureOpen()

        if (count == 0) {
            return
        }

        output.write(buffer, offset, count)
        currentPosition += count
    }

    override fun seek(offset: Long) {
        throw UnsupportedOperationException("OutputStream adapter does not support seeking.")
    }

    override fun flush() {
        ensureOpen()
        output.flush()
    }

    override fun close() {
        if (closed) {
            return
        }

        closed = true
        if (closeOutputOnClose) {
            output.close()
        }
    }

    private fun ensureOpen() {
        if (closed) {
            throw IOException("UnifiedStream is closed.")
        }
    }
}

