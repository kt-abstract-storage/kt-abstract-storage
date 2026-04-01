package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.StorableType
import io.github.ktabstractstorage.memory.MemoryFolder
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryStorageTests {
    @Test
    fun memory_folder_create_list_delete_roundtrip() = runTest {
        val root = MemoryFolder("root")

        val file = root.createFileAsync("a.txt")
        val folder = root.createFolderAsync("sub")

        val allNames = root.getItemsAsync(StorableType.ALL).toList().map { it.name }.sorted()
        assertEquals(listOf("a.txt", "sub"), allNames)

        root.deleteAsync(file)
        val remaining = root.getItemsAsync(StorableType.ALL).toList().map { it.name }
        assertEquals(listOf("sub"), remaining)

        root.deleteAsync(folder)
        assertTrue(root.getItemsAsync(StorableType.ALL).toList().isEmpty())
    }
}

