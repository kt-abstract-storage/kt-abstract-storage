package io.github.ktabstractstorage

import io.github.ktabstractstorage.streams.asUnifiedStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnifiedStreamJavaIoExtensionsTests {
    @Test
    fun input_and_output_as_unified_stream_is_duplex() {
        val inputBytes = "read-part".encodeToByteArray()
        val input = ByteArrayInputStream(inputBytes)
        val output = ByteArrayOutputStream()

        val stream = input.asUnifiedStream(output)

        val readBack = ByteArray(inputBytes.size)
        val read = stream.read(readBack, 0, readBack.size)

        val writeBytes = "write-part".encodeToByteArray()
        stream.write(writeBytes, 0, writeBytes.size)
        stream.flush()
        stream.close()

        assertEquals(inputBytes.size, read)
        assertContentEquals(inputBytes, readBack)
        assertContentEquals(writeBytes, output.toByteArray())
    }

    @Test
    fun input_and_output_as_unified_stream_respects_close_flags() {
        val input = CloseTrackingInputStream(ByteArrayInputStream("abc".encodeToByteArray()))
        val output = CloseTrackingOutputStream(ByteArrayOutputStream())

        val stream = input.asUnifiedStream(
            output = output,
            closeInputStreamOnClose = false,
            closeOutputStreamOnClose = false,
        )

        stream.close()

        assertFalse(input.closedByAdapter)
        assertFalse(output.closedByAdapter)

        val byte = input.read()
        assertEquals('a'.code, byte)

        output.write(42)
        output.flush()
        assertContentEquals(byteArrayOf(42), output.delegate.toByteArray())
    }

    @Test
    fun output_and_input_as_unified_stream_closes_both_by_default() {
        val input = CloseTrackingInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        val output = CloseTrackingOutputStream(ByteArrayOutputStream())

        val stream = output.asUnifiedStream(input)
        stream.close()

        assertTrue(input.closedByAdapter)
        assertTrue(output.closedByAdapter)
    }

    private class CloseTrackingInputStream(val delegate: InputStream) : InputStream() {
        var closedByAdapter: Boolean = false
            private set

        override fun read(): Int = delegate.read()

        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)

        override fun close() {
            closedByAdapter = true
            delegate.close()
        }
    }

    private class CloseTrackingOutputStream(val delegate: ByteArrayOutputStream) : OutputStream() {
        var closedByAdapter: Boolean = false
            private set

        override fun write(b: Int) {
            delegate.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
        }

        override fun flush() {
            delegate.flush()
        }

        override fun close() {
            closedByAdapter = true
            delegate.close()
        }
    }
}

