package com.itswin11.ktabstractstorage.streams.extensions

import com.itswin11.ktabstractstorage.streams.UnifiedStream
import java.io.IOException
import java.io.InputStream

internal class UnifiedInputStreamAdapter(
    private val stream: UnifiedStream,
    private val closeUnifiedStreamOnClose: Boolean,
) : InputStream() {
    private var closed = false
    private var markedPosition: Long = -1L

    override fun read(): Int {
        val single = ByteArray(1)
        val bytesRead = read(single, 0, 1)
        return if (bytesRead <= 0) -1 else single[0].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        ensureOpen()
        ensureReadable()

        if (len == 0) {
            return 0
        }

        val bytesRead = stream.read(b, off, len)
        return if (bytesRead <= 0) -1 else bytesRead
    }

    override fun skip(n: Long): Long {
        ensureOpen()
        ensureReadable()

        if (n <= 0) {
            return 0L
        }

        if (stream.canSeek) {
            val before = stream.position
            val target = (before + n).coerceAtMost(stream.length)
            stream.seek(target)
            return stream.position - before
        }

        var remaining = n
        val scratch = ByteArray(8192)
        while (remaining > 0) {
            val read = read(scratch, 0, minOf(scratch.size.toLong(), remaining).toInt())
            if (read < 0) {
                break
            }
            remaining -= read.toLong()
        }

        return n - remaining
    }

    override fun available(): Int {
        ensureOpen()

        if (!stream.canSeek) {
            return 0
        }

        val remaining = (stream.length - stream.position).coerceAtLeast(0)
        return remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    override fun mark(readlimit: Int) {
        if (!stream.canSeek || closed) {
            return
        }

        markedPosition = stream.position
    }

    override fun reset() {
        ensureOpen()

        if (!stream.canSeek || markedPosition < 0) {
            throw IOException("Mark/reset is not supported by this stream.")
        }

        stream.seek(markedPosition)
    }

    override fun markSupported(): Boolean = stream.canSeek

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
            throw IOException("InputStream is closed.")
        }
    }

    private fun ensureReadable() {
        if (!stream.canRead) {
            throw IOException("Underlying UnifiedStream is not readable.")
        }
    }
}

