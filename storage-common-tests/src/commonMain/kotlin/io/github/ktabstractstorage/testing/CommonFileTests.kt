package io.github.ktabstractstorage.testing

import io.github.ktabstractstorage.File
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.streams.UnifiedStream
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse

/**
 * Common reusable contracts for [File] implementations.
 */
abstract class CommonFileTests {
    protected data class FileFixture(
        val file: File,
        val cleanup: suspend () -> Unit = {},
    )

    protected open val supportsWriting: Boolean = true

    protected abstract suspend fun createFileFixtureAsync(initialContent: ByteArray = byteArrayOf()): FileFixture

    protected suspend fun runAllCommonFileContracts() {
        runConstructorCallValidParametersContract()
        runIdNotBlankContract()
        runOpenReadContract()
        runOpenWriteContract()
        runOpenReadWriteContract()
    }

    protected suspend fun runConstructorCallValidParametersContract() {
        val fixture = createFileFixtureAsync()
        fixture.cleanup()
    }

    protected suspend fun runIdNotBlankContract() {
        val fixture = createFileFixtureAsync()
        try {
            assertFalse(fixture.file.id.isBlank())
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runOpenReadContract() {
        val expected = byteArrayOf(1, 2, 3, 4, 5)
        val fixture = createFileFixtureAsync(initialContent = expected)

        try {
            fixture.file.openStreamAsync(FileAccessMode.READ).use { stream ->
                val actual = readAllBytes(stream)
                assertContentEquals(expected, actual)
            }
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runOpenWriteContract() {
        if (!supportsWriting) {
            return
        }

        val expected = byteArrayOf(10, 20, 30, 40)
        val fixture = createFileFixtureAsync(initialContent = byteArrayOf())

        try {
            fixture.file.openStreamAsync(FileAccessMode.WRITE).use { stream ->
                stream.write(expected, 0, expected.size)
                stream.flush()
            }

            fixture.file.openStreamAsync(FileAccessMode.READ).use { stream ->
                val actual = readAllBytes(stream)
                assertContentEquals(expected, actual)
            }
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runOpenReadWriteContract() {
        if (!supportsWriting) {
            return
        }

        val expected = byteArrayOf(7, 8, 9)
        val fixture = createFileFixtureAsync(initialContent = byteArrayOf())

        try {
            fixture.file.openStreamAsync(FileAccessMode.READ_AND_WRITE).use { stream ->
                stream.write(expected, 0, expected.size)
                stream.flush()
                stream.seek(0)

                val actual = readAllBytes(stream)
                assertContentEquals(expected, actual)
            }
        } finally {
            fixture.cleanup()
        }
    }

    protected fun readAllBytes(stream: UnifiedStream): ByteArray {
        val chunk = ByteArray(1024)
        val chunks = mutableListOf<ByteArray>()
        var totalSize = 0

        while (true) {
            val read = stream.read(chunk, 0, chunk.size)
            if (read <= 0) {
                break
            }

            val part = ByteArray(read)
            chunk.copyInto(part, 0, 0, read)
            chunks += part
            totalSize += read
        }

        val all = ByteArray(totalSize)
        var offset = 0
        for (part in chunks) {
            part.copyInto(all, offset, 0, part.size)
            offset += part.size
        }

        return all
    }
}


