package io.github.ktabstractstorage.streams

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

class UnifiedStreamKotlinxIoExtensionsTests {
    @Test
    fun unified_stream_as_source_reads_content() {
        val stream = MemoryStream()
        val bytes = "hello".encodeToByteArray()
        stream.write(bytes, 0, bytes.size)
        stream.position = 0

        val source = stream.asSource(closeUnifiedStreamOnClose = false)
        val readBuffer = ByteArray(bytes.size)
        val read = source.readAtMostTo(readBuffer)
        source.close()

        assertEquals(bytes.size, read)
        assertContentEquals(bytes, readBuffer)

        stream.position = 0
        val verify = ByteArray(bytes.size)
        val verifyRead = stream.read(verify, 0, verify.size)
        assertEquals(bytes.size, verifyRead)
        assertContentEquals(bytes, verify)
    }

    @Test
    fun unified_stream_as_sink_writes_content() {
        val stream = MemoryStream()
        val bytes = "abc".encodeToByteArray()

        val sink = stream.asSink(closeUnifiedStreamOnClose = false)
        sink.write(bytes)
        sink.flush()
        sink.close()

        stream.position = 0
        val readBack = ByteArray(bytes.size)
        val read = stream.read(readBack, 0, readBack.size)
        assertEquals(bytes.size, read)
        assertContentEquals(bytes, readBack)
    }

    @Test
    fun source_as_unified_stream_reads_content() {
        val bytes = byteArrayOf(9, 8, 7)
        val source = ByteArrayInputStream(bytes).asSource().buffered()

        val unified = source.asUnifiedStream(closeSourceOnClose = true)
        val readBack = ByteArray(bytes.size)
        val read = unified.read(readBack, 0, readBack.size)
        unified.close()

        assertEquals(bytes.size, read)
        assertContentEquals(bytes, readBack)
    }

    @Test
    fun sink_as_unified_stream_writes_content() {
        val output = ByteArrayOutputStream()
        val sink = output.asSink().buffered()
        val bytes = byteArrayOf(1, 2, 3, 4)

        val unified = sink.asUnifiedStream(closeSinkOnClose = true)
        unified.write(bytes, 0, bytes.size)
        unified.flush()
        unified.close()

        assertContentEquals(bytes, output.toByteArray())
    }

    @Test
    fun source_as_unified_stream_read_async_reads_content() = runTest {
        val bytes = "async".encodeToByteArray()
        val source = ByteArrayInputStream(bytes).asSource().buffered()
        val unified = source.asUnifiedStream(closeSourceOnClose = true)

        val readBack = ByteArray(bytes.size)
        val read = unified.readAsync(readBack, 0, readBack.size)
        unified.close()

        assertEquals(bytes.size, read)
        assertContentEquals(bytes, readBack)
    }

    @Test
    fun sink_as_unified_stream_write_async_writes_content() = runTest {
        val output = ByteArrayOutputStream()
        val sink = output.asSink().buffered()
        val unified = sink.asUnifiedStream(closeSinkOnClose = true)
        val bytes = "async-write".encodeToByteArray()

        unified.writeAsync(bytes, 0, bytes.size)
        unified.flushAsync()
        unified.close()

        assertContentEquals(bytes, output.toByteArray())
    }

    @Test
    fun source_and_sink_as_unified_stream_is_duplex() {
        val sourceBytes = "read-part".encodeToByteArray()
        val source = ByteArrayInputStream(sourceBytes).asSource().buffered()
        val output = ByteArrayOutputStream()
        val sink = output.asSink().buffered()

        val unified = UnifiedStream.combineIoStreams(
            source = source,
            sink = sink,
            closeSourceOnClose = true,
            closeSinkOnClose = true,
        )

        val readBack = ByteArray(sourceBytes.size)
        val read = unified.read(readBack, 0, readBack.size)

        val writeBytes = "write-part".encodeToByteArray()
        unified.write(writeBytes, 0, writeBytes.size)
        unified.flush()
        unified.close()

        assertEquals(sourceBytes.size, read)
        assertContentEquals(sourceBytes, readBack)
        assertContentEquals(writeBytes, output.toByteArray())
    }

    @Test
    fun source_and_sink_as_unified_stream_respects_close_flags() {
        val sourceBytes = "keep-open".encodeToByteArray()
        val source = ByteArrayInputStream(sourceBytes).asSource().buffered()
        val output = ByteArrayOutputStream()
        val sink = output.asSink().buffered()

        val unified = UnifiedStream.combineIoStreams(
            source = source,
            sink = sink,
            closeSourceOnClose = false,
            closeSinkOnClose = false,
        )
        unified.close()

        // Source is still open because closeSourceOnClose=false.
        val sourceReadBack = ByteArray(sourceBytes.size)
        val sourceRead = source.readAtMostTo(sourceReadBack)
        assertEquals(sourceBytes.size, sourceRead)
        assertContentEquals(sourceBytes, sourceReadBack)

        // Sink is still open because closeSinkOnClose=false.
        sink.writeByte(42)
        sink.flush()
        assertContentEquals(byteArrayOf(42), output.toByteArray())
    }

    @Test
    fun companion_combine_io_streams_creates_duplex_stream() {
        val sourceBytes = "companion-kio".encodeToByteArray()
        val source = ByteArrayInputStream(sourceBytes).asSource().buffered()
        val output = ByteArrayOutputStream()
        val sink = output.asSink().buffered()

        val unified = UnifiedStream.combineIoStreams(source, sink)

        val readBack = ByteArray(sourceBytes.size)
        val read = unified.read(readBack, 0, readBack.size)
        unified.write(byteArrayOf(5, 6), 0, 2)
        unified.flush()
        unified.close()

        assertEquals(sourceBytes.size, read)
        assertContentEquals(sourceBytes, readBack)
        assertContentEquals(byteArrayOf(5, 6), output.toByteArray())
    }
}
