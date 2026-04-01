package io.github.ktabstractstorage.android

import androidx.core.util.AtomicFile
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.streams.UnifiedStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * A write stream that commits changes through AndroidX [AtomicFile] on close.
 *
 * Writes are staged in a sibling temp file to keep random-access semantics.
 * On close, staged bytes are committed via [AtomicFile.startWrite]/finishWrite.
 */
class AtomicStream(
    targetFile: File,
    mode: FileAccessMode = FileAccessMode.READ_AND_WRITE,
) : UnifiedStream() {

    private val atomicFile = AtomicFile(targetFile)
    private val accessMode = mode
    private val tempFile: File
    private val raf: RandomAccessFile
    private var closed = false

    init {
        require(mode != FileAccessMode.READ) {
            "AtomicStream requires a writable access mode."
        }

        val parent = targetFile.parentFile
            ?: throw IOException("Target file must have a parent directory: ${targetFile.absolutePath}")

        tempFile = File.createTempFile(".${targetFile.name}.", ".atomic", parent)

        if (mode == FileAccessMode.READ_AND_WRITE && targetFile.exists()) {
            targetFile.copyTo(tempFile, overwrite = true)
        }

        raf = RandomAccessFile(tempFile, "rw")

        if (mode == FileAccessMode.WRITE) {
            raf.setLength(0L)
        }
    }

    override val canRead: Boolean = accessMode != FileAccessMode.WRITE
    override val canWrite: Boolean = accessMode != FileAccessMode.READ
    override val canSeek: Boolean = true

    override val length: Long
        get() = raf.length()

    override var position: Long
        get() = raf.filePointer
        set(value) {
            raf.seek(value)
        }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int =
        raf.read(buffer, offset, count).coerceAtLeast(0)

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        raf.write(buffer, offset, count)
    }

    override fun seek(offset: Long) {
        raf.seek(offset)
    }

    override fun flush() {
        raf.fd.sync()
    }

    override fun close() {
        if (closed) return
        closed = true

        var pending: Throwable? = null

        try {
            flush()
            raf.close()
            commitWithAtomicFile()
        } catch (t: Throwable) {
            pending = t
        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                tempFile.deleteOnExit()
            }
        }

        if (pending != null) {
            if (pending is IOException) throw pending
            throw IOException("Atomic write failed", pending)
        }
    }

    private fun commitWithAtomicFile() {
        var output: FileOutputStream? = null
        try {
            output = atomicFile.startWrite()
            tempFile.inputStream().use { input ->
                input.copyTo(output)
            }
            output.fd.sync()
            atomicFile.finishWrite(output)
            output = null
        } catch (t: Throwable) {
            output?.let(atomicFile::failWrite)
            throw t
        }
    }
}

