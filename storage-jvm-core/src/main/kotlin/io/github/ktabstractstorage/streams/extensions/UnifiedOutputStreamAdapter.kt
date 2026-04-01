package io.github.ktabstractstorage.streams.extensions

import io.github.ktabstractstorage.streams.UnifiedStream
import java.io.IOException
import java.io.OutputStream

internal class UnifiedOutputStreamAdapter(
    private val stream: UnifiedStream,
    private val closeUnifiedStreamOnClose: Boolean,
) : OutputStream() {
    private var closed = false

    override fun write(b: Int) {
        val single = byteArrayOf(b.toByte())
        write(single, 0, 1)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        ensureOpen()
        ensureWritable()

        if (len == 0) {
            return
        }

        stream.write(b, off, len)
    }

    override fun flush() {
        ensureOpen()
        stream.flush()
    }

    override fun close() {
        if (closed) {
            return
        }

        closed = true
        if (closeUnifiedStreamOnClose) {
            stream.close()
        }
    }

    private fun ensureOpen() {
        if (closed) {
            throw IOException("OutputStream is closed.")
        }
    }

    private fun ensureWritable() {
        if (!stream.canWrite) {
            throw IOException("Underlying UnifiedStream is not writable.")
        }
    }
}

