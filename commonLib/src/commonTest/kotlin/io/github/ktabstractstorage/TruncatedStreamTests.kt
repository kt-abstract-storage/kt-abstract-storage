package io.github.ktabstractstorage

import io.github.ktabstractstorage.errors.StorageIOException
import io.github.ktabstractstorage.streams.MemoryStream
import io.github.ktabstractstorage.streams.truncated
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TruncatedStreamTests {
    private fun createTruncatedStream(totalBytes: Int, maxLength: Long): Pair<ByteArray, io.github.ktabstractstorage.streams.UnifiedStream> {
        val data = ByteArray(totalBytes) { it.toByte() }
        val source = MemoryStream()
        source.write(data, 0, data.size)
        source.seek(0)
        return data to source.truncated(maxLength)
    }

    @Test
    fun read_respects_max_length_and_stops() {
        val (data, truncated) = createTruncatedStream(totalBytes = 100, maxLength = 50)
        val buffer = ByteArray(100)

        val read1 = truncated.read(buffer, 0, buffer.size)
        val read2 = truncated.read(buffer, 0, buffer.size)

        assertEquals(50, read1)
        assertEquals(0, read2)
        assertContentEquals(data.copyOfRange(0, 50), buffer.copyOfRange(0, 50))
    }

    @Test
    fun seek_to_beginning_resets_read_window_for_seekable_stream() {
        val (data, truncated) = createTruncatedStream(totalBytes = 100, maxLength = 50)
        val buffer = ByteArray(100)

        val firstRead = truncated.read(buffer, 0, 10)
        assertEquals(10, firstRead)

        truncated.seek(0)
        val readAfterSeek = truncated.read(buffer, 0, 50)

        assertEquals(50, readAfterSeek)
        assertContentEquals(data.copyOfRange(0, 50), buffer.copyOfRange(0, 50))
    }

    @Test
    fun write_beyond_max_length_throws() {
        val source = MemoryStream()
        val truncated = source.truncated(8)
        val payload = ByteArray(9) { 1 }

        assertFailsWith<StorageIOException> {
            truncated.write(payload, 0, payload.size)
        }
    }
}

