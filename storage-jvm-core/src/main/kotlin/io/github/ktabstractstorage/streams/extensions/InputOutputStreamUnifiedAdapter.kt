package io.github.ktabstractstorage.streams.extensions

import io.github.ktabstractstorage.streams.UnifiedStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal class InputOutputStreamUnifiedAdapter(
    private val input: InputStream,
    private val output: OutputStream,
    private val closeInputOnClose: Boolean,
    private val closeOutputOnClose: Boolean,
) : UnifiedStream() {
    private var closed = false
    private var currentPosition = 0L

    override val canRead: Boolean
        get() = !closed

    override val canWrite: Boolean
        get() = !closed

    override val canSeek: Boolean = false

    override val length: Long
        get() = throw UnsupportedOperationException("Length is not available for non-seekable Input/Output stream adapters.")

    override var position: Long
        get() = currentPosition
        set(_) {
            throw UnsupportedOperationException("Input/Output stream adapter does not support seeking.")
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
        ensureOpen()

        if (count == 0) {
            return
        }

        output.write(buffer, offset, count)
        currentPosition += count
    }

    override fun seek(offset: Long) {
        throw UnsupportedOperationException("Input/Output stream adapter does not support seeking.")
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
        var closeError: Throwable? = null

        if (closeInputOnClose) {
            runCatching { input.close() }.onFailure { closeError = it }
        }

        if (closeOutputOnClose) {
            runCatching { output.close() }.onFailure {
                if (closeError == null) closeError = it
            }
        }

        if (closeError != null) {
            throw closeError
        }
    }

    override suspend fun readAsync(buffer: ByteArray, offset: Int, count: Int): Int =
        read(buffer, offset, count)

    override suspend fun writeAsync(buffer: ByteArray, offset: Int, count: Int) {
        write(buffer, offset, count)
    }

    override suspend fun flushAsync() {
        flush()
    }

    private fun ensureOpen() {
        if (closed) {
            throw IOException("UnifiedStream is closed.")
        }
    }
}
