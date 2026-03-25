package com.itswin11.ktabstractstorage.streams

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

/**
 * Represents a stream of data that can be read from, written to, and/or seeked within.
 * This is a unified interface that can be implemented by various types of streams,
 * such as file streams, memory streams, network streams, etc.
 *
 * **Remarks:**
 * - The specific capabilities of the stream (e.g. whether it can read, write, or seek)
 * are determined by the properties of the implementation.
 */
abstract class UnifiedStream : Closeable {
    /**
     * Determines whether the stream can be read from.
     */
    abstract val canRead: Boolean

    /**
     * Determines whether the stream can be written to.
     */
    abstract val canWrite: Boolean

    /**
     * Determines whether the stream can be seeked.
     */
    abstract val canSeek: Boolean

    /**
     * The stream length.
     */
    abstract val length: Long

    /**
     * The stream position.
     */
    abstract var position: Long

    /**
     * Reads up to [count] bytes from the current stream position into
     * [buffer], starting at [offset].
     *
     * Implementations are expected to advance [position] by the number
     * of bytes read.
     *
     * @param buffer Destination array that receives the bytes.
     * @param offset Start index in [buffer] where bytes are written.
     * @param count Maximum number of bytes to read.
     * @return The number of bytes actually read, or `0` when the end of stream
     * is reached.
     */
    abstract fun read(buffer: ByteArray, offset: Int, count: Int): Int

    /**
     * Writes [count] bytes from [buffer], starting at [offset], to the
     * current stream position.
     *
     * Implementations are expected to advance [position] by the number
     * of bytes written.
     *
     * @param buffer Source array that provides bytes to write.
     * @param offset Start index in [buffer] to read bytes from.
     * @param count Number of bytes to write.
     */
    abstract fun write(buffer: ByteArray, offset: Int, count: Int)

    /**
     * Moves the stream cursor to [offset].
     *
     * The exact interpretation of [offset] (absolute/relative bounds and
     * validation) is
     * implementation-defined.
     *
     * @param offset Target position argument accepted by the implementation.
     */
    abstract fun seek(offset: Long)

    /**
     * Flushes any buffered data to the underlying storage.
     */
    abstract fun flush()

    /**
     * Asynchronously reads up to [count] bytes into [buffer], starting
     * at [offset].
     *
     * @param buffer Destination array that receives the bytes.
     * @param offset Start index in [buffer] where bytes are written.
     * @param count Maximum number of bytes to read.
     *
     * **Default implementation remarks:**
     * - Delegates to [read] inside `withContext(Dispatchers.IO)`.
     * - This fallback executes blocking I/O on the I/O dispatcher.
     * - Override when a backend provides true non-blocking/native async reads.
     */
    open suspend fun readAsync(buffer: ByteArray, offset: Int, count: Int): Int =
        withContext(Dispatchers.IO) { read(buffer, offset, count) }

    /**
     * Asynchronously writes [count] bytes from [buffer], starting at [offset].
     *
     * @param buffer Source array that provides bytes to write.
     * @param offset Start index in [buffer] to read bytes from.
     * @param count Number of bytes to write.
     *
     * **Default implementation remarks:**
     * - Delegates to [write] inside `withContext(Dispatchers.IO)`.
     * - This fallback executes blocking I/O on the I/O dispatcher.
     * - Override when a backend provides true non-blocking/native async writes.
     */
    open suspend fun writeAsync(buffer: ByteArray, offset: Int, count: Int) =
        withContext(Dispatchers.IO) { write(buffer, offset, count) }

    /**
     * Asynchronously changes the stream position using [offset].
     *
     * @param offset Target position argument accepted by the implementation.
     *
     * **Default implementation remarks:**
     * - Delegates to [seek] inside `withContext(Dispatchers.IO)`.
     * - Override when a backend provides a native async seek operation.
     */
    open suspend fun seekAsync(offset: Long) =
        withContext(Dispatchers.IO) { seek(offset) }

    /**
     * Asynchronously flushes buffered data.
     *
     * **Default implementation remarks:**
     * - Delegates to [flush] inside `withContext(Dispatchers.IO)`.
     * - Override when a backend provides a native async flush operation.
     */
    open suspend fun flushAsync() =
        withContext(Dispatchers.IO) { flush() }
}