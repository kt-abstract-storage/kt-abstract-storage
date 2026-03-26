package com.itswin11.ktabstractstorage.streams

import com.itswin11.ktabstractstorage.enums.FileAccessMode
import java.io.File
import java.io.RandomAccessFile

/**
 * A [UnifiedStream] implementation that reads from and writes
 * to a file on disk, using a [RandomAccessFile] internally.
 *
 * @param file The [File] to access using this stream.
 * @param mode The [FileAccessMode] for this stream.
 * @param forceWrites When `true` and [mode] is [FileAccessMode.READ_AND_WRITE],
 * opens the file with the `"rws"` flag, which instructs the OS
 * to flush every write — including file-metadata changes — synchronously
 * to the underlying storage device before returning.
 *
 * Has no effect when [mode] is [FileAccessMode.READ] or [FileAccessMode.WRITE],
 * since those modes do not append the `"s"` flag.
 *
 * This guarantees durability at the cost of write throughput. Set to `false`
 * if buffered writes are acceptable and performance is preferred.
 */
class FileStream(
    file: File,
    mode: FileAccessMode = FileAccessMode.READ_AND_WRITE,
    forceWrites: Boolean = true
) : UnifiedStream() {
    private val raf = RandomAccessFile(file, getModeString(mode))
    private val _forceWrites = forceWrites

    override val canRead = mode != FileAccessMode.WRITE
    override val canWrite = mode != FileAccessMode.READ
    override val canSeek = true

    override val length: Long get() = raf.length()

    override var position: Long
        get() = raf.filePointer
        set(value) = raf.seek(value)

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int =
        raf.read(buffer, offset, count).coerceAtLeast(0)

    override fun write(buffer: ByteArray, offset: Int, count: Int)
        = raf.write(buffer, offset, count)

    override fun seek(offset: Long) = raf.seek(offset)

    override fun flush() { /* RandomAccessFile writes directly to disk descriptor */ }

    override fun close() = raf.close()

    private fun getModeString(accessMode: FileAccessMode) : String {
        var modeString = when (accessMode) {
            FileAccessMode.READ -> "r"
            FileAccessMode.WRITE -> "w"
            FileAccessMode.READ_AND_WRITE -> "rw"
        }

        if (accessMode == FileAccessMode.READ_AND_WRITE && _forceWrites)
            modeString += "s"

        return modeString
    }
}