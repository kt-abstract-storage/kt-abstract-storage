package io.github.ktabstractstorage.streams.extensions

import io.github.ktabstractstorage.streams.UnifiedStream
import java.io.IOException
import java.io.InputStream

internal class InputStreamUnifiedAdapter(
    private val input: InputStream,
    private val closeInputOnClose: Boolean,
) : UnifiedStream() {
    private var closed = false
    private var currentPosition = 0L

    override val canRead: Boolean
        get() = !closed

    override val canWrite: Boolean = false

    override val canSeek: Boolean = false

    override val length: Long
        get() = throw UnsupportedOperationException("Length is not available for non-seekable InputStream adapters.")

    override var position: Long
        get() = currentPosition
        set(_) {
            throw UnsupportedOperationException("InputStream adapter does not support seeking.")
        }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        ensureOpen()

        if (count == 0) {
            return 0
        }

        val read = input.read(buffer, offset, count)
        if (read > 0) {
            currentPosition += read
            return read
        }

        return 0
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        throw IOException("InputStream adapter is not writable.")
    }

    override fun seek(offset: Long) {
        throw UnsupportedOperationException("InputStream adapter does not support seeking.")
    }

    override fun flush() {
        ensureOpen()
        // InputStream does not expose flushing.
    }

    override fun close() {
        if (closed) {
            return
        }

        closed = true
        if (closeInputOnClose) {
            input.close()
        }
    }

    private fun ensureOpen() {
        if (closed) {
            throw IOException("UnifiedStream is closed.")
        }
    }
}

