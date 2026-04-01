package io.github.ktabstractstorage

import io.github.ktabstractstorage.extensions.readTextAsync
import io.github.ktabstractstorage.extensions.writeTextAsync
import io.github.ktabstractstorage.memory.MemoryFolder
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FileRangeReadWriteTests {
    private suspend fun createFile(name: String = "a.txt"): File {
        val root = MemoryFolder("root")
        return root.createFileAsync(name)
    }

    @Test
    fun read_lines_range_all_columns() = runTest {
        val file = createFile()
        val content = listOf("L0", "L1", "L2", "L3", "L4").joinToString("\n")
        file.writeTextAsync(content)

        val lines = file.readTextAsync(1 to 4).toList()
        assertContentEquals(listOf("L1", "L2", "L3"), lines)
    }

    @Test
    fun read_lines_and_columns_range() = runTest {
        val file = createFile()
        val content = listOf("abcd", "efgh", "ijkl", "mnop").joinToString("\n")
        file.writeTextAsync(content)

        val lines = file.readTextAsync(1 to 3, 1 to 3).toList()
        assertContentEquals(listOf("fg", "jk"), lines)
    }

    @Test
    fun write_lines_range_all_columns() = runTest {
        val file = createFile("dst.txt")
        val content = listOf("A0", "A1", "A2", "A3").joinToString("\n")

        file.writeTextAsync(content, 1 to 3)

        val output = file.readTextAsync().replace("\r\n", "\n")
        assertEquals("A1\nA2\nA3", output)
    }

    @Test
    fun write_lines_and_columns_range() = runTest {
        val file = createFile("dst.txt")
        val content = listOf("abcd", "efgh", "ijkl", "mnop").joinToString("\n")

        file.writeTextAsync(content, 1 to 3, 1 to 3)

        val output = file.readTextAsync().replace("\r\n", "\n")
        assertEquals("fg\njk\nno", output)
    }

    @Test
    fun read_lines_completely_beyond_eof_returns_empty() = runTest {
        val file = createFile("eof.txt")
        file.writeTextAsync("L0\nL1")

        val lines = file.readTextAsync(5 to 8).toList()
        assertEquals(0, lines.size)
    }

    @Test
    fun read_lines_partially_beyond_eof_returns_remaining() = runTest {
        val file = createFile("partial.txt")
        file.writeTextAsync("A\nB\nC")

        val lines = file.readTextAsync(1 to 5).toList()
        assertContentEquals(listOf("B", "C"), lines)
    }
}

