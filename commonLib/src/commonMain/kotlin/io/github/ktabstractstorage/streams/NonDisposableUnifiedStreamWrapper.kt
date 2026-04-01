package io.github.ktabstractstorage.streams

/**
 * A [UnifiedStream] wrapper that forwards all operations to [inner] except [close],
 * which is intentionally ignored.
 */
class NonDisposableUnifiedStreamWrapper(
    private val inner: UnifiedStream,
) : UnifiedStream() {
    override val canRead: Boolean
        get() = inner.canRead

    override val canWrite: Boolean
        get() = inner.canWrite

    override val canSeek: Boolean
        get() = inner.canSeek

    override val length: Long
        get() = inner.length

    override var position: Long
        get() = inner.position
        set(value) {
            inner.position = value
        }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int =
        inner.read(buffer, offset, count)

    override fun write(buffer: ByteArray, offset: Int, count: Int) =
        inner.write(buffer, offset, count)

    override fun seek(offset: Long) = inner.seek(offset)

    override fun flush() = inner.flush()

    override suspend fun readAsync(buffer: ByteArray, offset: Int, count: Int): Int =
        inner.readAsync(buffer, offset, count)

    override suspend fun writeAsync(buffer: ByteArray, offset: Int, count: Int) =
        inner.writeAsync(buffer, offset, count)

    override suspend fun seekAsync(offset: Long) = inner.seekAsync(offset)

    override suspend fun flushAsync() = inner.flushAsync()

    override fun close() {
        // Intentionally no-op: wrapper lifetime is decoupled from inner stream lifetime.
    }
}
