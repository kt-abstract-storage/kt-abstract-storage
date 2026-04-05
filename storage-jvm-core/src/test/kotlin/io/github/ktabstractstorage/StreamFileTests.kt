package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.streams.MemoryStream
import io.github.ktabstractstorage.streams.NonDisposableUnifiedStreamWrapper
import io.github.ktabstractstorage.system.io.StreamFile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StreamFileTests {
    private fun createStreamFile(shouldDispose: Boolean): Pair<MemoryStream, StreamFile> {
        val stream = MemoryStream()
        val file = StreamFile(stream).also { it.shouldDispose = shouldDispose }
        return stream to file
    }

    @Test
    fun should_dispose_defaults_false() {
        val file = StreamFile(MemoryStream())
        assertFalse(file.shouldDispose)
    }

    @Test
    fun open_stream_returns_underlying_stream_when_should_dispose_true() = runTest {
        val (stream, file) = createStreamFile(shouldDispose = true)

        val opened = file.openStreamAsync(FileAccessMode.READ_AND_WRITE)
        assertSame(stream, opened)
    }

    @Test
    fun open_stream_returns_non_disposable_wrapper_when_should_dispose_false() = runTest {
        val (_, file) = createStreamFile(shouldDispose = false)

        val opened = file.openStreamAsync(FileAccessMode.READ_AND_WRITE)
        assertIs<NonDisposableUnifiedStreamWrapper>(opened)
    }

    @Test
    fun multiple_opens_with_wrapper_are_distinct_instances() = runTest {
        val (_, file) = createStreamFile(shouldDispose = false)

        val first = file.openStreamAsync(FileAccessMode.READ_AND_WRITE)
        val second = file.openStreamAsync(FileAccessMode.READ_AND_WRITE)

        assertTrue(first !== second)
        assertIs<NonDisposableUnifiedStreamWrapper>(first)
        assertIs<NonDisposableUnifiedStreamWrapper>(second)
    }

    @Test
    fun wrapped_close_does_not_close_underlying_stream() = runTest {
        val (stream, file) = createStreamFile(shouldDispose = false)

        val opened = file.openStreamAsync(FileAccessMode.READ_AND_WRITE)
        opened.close()

        val payload = byteArrayOf(1, 2, 3)
        stream.write(payload, 0, payload.size)
        assertEquals(3, stream.length)
    }
}

